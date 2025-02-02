////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools;

import flex2.compiler.*;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.common.DefaultsConfigurator;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.FileUtil;
import flex2.compiler.swc.*;
import flex2.compiler.swc.API;
import flex2.compiler.util.*;
import flex2.linker.LinkerException;
import flash.localization.LocalizationManager;
import flash.localization.XLRLocalizer;
import flash.localization.ResourceBundleLocalizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Entry-point for compc, the command-line tool for compiling components.
 *
 * @author Brian Deitte
 */
public class Compc extends Tool
{
    public static void main(String[] args)
    {
	    compc(args);
	    System.exit(ThreadLocalToolkit.errorCount());
    }

	public static void compc(String[] args)
	{
		flex2.compiler.API.useAS3();

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CompcConfiguration.class, CompcConfiguration.getAliases());
        
        // Do not set this.  The include-classes should be included in the configuration buffer checksum so changes
        // to the class list can be detected during incremental builds.
        //cfgbuf.setDefaultVar("include-classes");

        try
        {
            // setup the path resolver
	        flex2.compiler.API.usePathResolver();

            // set up for localizing messages
            LocalizationManager l10n = new LocalizationManager();
            l10n.addLocalizer( new XLRLocalizer() );
            l10n.addLocalizer( new ResourceBundleLocalizer() );
            ThreadLocalToolkit.setLocalizationManager( l10n );

            // setup a console-based logger...
	        flex2.compiler.API.useConsoleLogger();

            // process configuration
	        DefaultsConfigurator.loadCompcDefaults( cfgbuf );
	        CompcConfiguration configuration = (CompcConfiguration) Compiler.processConfiguration(
                l10n, "compc", args, cfgbuf, CompcConfiguration.class, "include-classes");

	        // now set the logger up again with the configured settings
            flex2.compiler.API.useConsoleLogger(true, true, configuration.getWarnings(), true);
            
	        if (configuration.benchmark())
	        {
		        flex2.compiler.API.runBenchmark();
	        }
	        else
	        {
		        flex2.compiler.API.disableBenchmark();
	        }

            flex2.compiler.API.setupHeadless(configuration);

			String[] sourceMimeTypes = flex2.tools.API.getSourcePathMimeTypes();
            CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();

			// create a SourcePath...
			SourcePath sourcePath = new SourcePath(sourceMimeTypes, compilerConfig.allowSourcePathOverlap());
            sourcePath.addPathElements( compilerConfig.getSourcePath() );

			List[] array = flex2.compiler.API.getVirtualFileList(configuration.getIncludeSources(), configuration.getStylesheets().values(),
																 new HashSet(Arrays.asList(sourceMimeTypes)),
																 sourcePath.getPaths());

            // note: if Configuration is ever shared with other parts of the system, then this part will need
            // to change, since we're setting a compc-specific setting below
            compilerConfig.setMetadataExport(true);

            // create a FileSpec... can reuse based on appPath, debug settings, etc...
            FileSpec fileSpec = new FileSpec(array[0], flex2.tools.API.getFileSpecMimeTypes(), false);

            // create a SourceList...
            SourceList sourceList = new SourceList(array[1], compilerConfig.getSourcePath(), null,
            									   flex2.tools.API.getSourceListMimeTypes(), false);

	        ResourceContainer resources = new ResourceContainer();
	        ResourceBundlePath bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), null);

	        Map classes = new HashMap();
            NameMappings mappings = flex2.compiler.API.getNameMappings(configuration);
			List nsComponents = API.setupNamespaceComponents(configuration, mappings, sourcePath, classes);
			API.setupClasses(configuration, sourcePath, classes);

	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new Compiler.InitialSetup()));
	        }

            // load SWCs
            CompilerSwcContext swcContext = new CompilerSwcContext(configuration.getCompatibilityVersionString());
            SwcCache cache = new SwcCache();
	        // lazy read should only be set by mxmlc/compc
	        cache.setLazyRead(true);
	        // for compc the theme and include-libraries values have been purposely not passed in below.
	        // This is done because the theme attribute doesn't make sense and the include-libraries value
	        // actually causes issues when the default value is used with external libraries.
	        // FIXME: why don't we just get rid of these values from the configurator for compc?  That's a problem
	        // for include-libraries at least, since this value appears in flex-config.xml
            swcContext.load( compilerConfig.getLibraryPath(),
                             compilerConfig.getExternalLibraryPath(),
                             null,
                             compilerConfig.getIncludeLibraries(),
							 mappings,
							 I18nUtils.getTranslationFormat(compilerConfig),
							 cache );
	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new Compiler.LoadedSWCs(swcContext.getNumberLoaded())));
	        }
            configuration.addExterns( swcContext.getExterns() );
            configuration.addIncludes( swcContext.getIncludes() );
            configuration.addFiles( swcContext.getIncludeFiles() );

            // The output file.
            File swcFile = new File(configuration.getOutput());
                
            // Checksums to figure out if incremental compile can be done.
            String incrementalFileName = null;
            SwcChecksums swcChecksums = null;

            // Should we attempt to build incrementally using the incremental file?
            boolean recompile = true;   
            
            // If incremental compilation is enabled and the output file exists, 
            // use the persisted store to figure out if a compile/link is necessary.
            // link without a compile is not supported without changes to the
            // persistantStore since units for Sources of type isSwcScriptOwner()
            // aren't stored/restored properly. units contains null entries for those 
            // type of Source.  To force a rebuild, with -incremental specified, delete the 
            // incremental file. 
            if (configuration.getCompilerConfiguration().getIncremental())
            {
                swcChecksums = new SwcChecksums(swcContext, cfgbuf, configuration);

                // If incremental compilation is enabled, read the cached
                // compilation units...  Do not include the checksum in the file name so that
                // cache files don't pile up as the configuration changes.  There needs
                // to be a 1-to-1 mapping between the swc file and the cache file. 
                incrementalFileName = configuration.getOutput() + ".cache";
                
                // If the output file doesn't exist don't bother loading the
                // cache since a recompile is needed.
                if (swcFile.exists())
                {
                    RandomAccessFile incrementalFile = null;
                    try
                    {
                        incrementalFile = new RandomAccessFile(incrementalFileName, "r");

                        // For loadCompilationUnits, loadedChecksums[1] must match 
                        // the cached value else IOException is thrown.
                        int[] loadedChecksums = swcChecksums.copy();

                        flex2.compiler.API.loadCompilationUnits(configuration, fileSpec, sourceList, sourcePath, resources, bundlePath, null, /* sources */
                        null, /* units */
                        loadedChecksums, swcChecksums.getSwcDefSignatureChecksums(), swcChecksums.getSwcFileChecksums(), swcChecksums.getArchiveFileChecksums(), incrementalFile, incrementalFileName, null /* font manager */);

                        if (!swcChecksums.isRecompilationNeeded(loadedChecksums) && !swcChecksums.isRelinkNeeded(loadedChecksums))
                        {
                            recompile = false;
                        }
                    }
                    catch (FileNotFoundException ex)
                    {
                        ThreadLocalToolkit.logDebug(ex.getLocalizedMessage());
                    }
                    catch (IOException ex)
                    {
                        ThreadLocalToolkit.logInfo(ex.getLocalizedMessage());
                    }
                    finally
                    {
                        if (incrementalFile != null)
                        {
                            try
                            {
                                incrementalFile.close();
                            }
                            catch (IOException ex)
                            {
                            }
                            // If the load failed, or recompilation is needed, reset
                            // all the variables to their original state.
                            if (recompile)
                            {
                                fileSpec = new FileSpec(array[0], flex2.tools.API.getFileSpecMimeTypes(), false);

                                sourceList = new SourceList(array[1], compilerConfig.getSourcePath(), null, flex2.tools.API.getSourceListMimeTypes(), false);

                                sourcePath = new SourcePath(sourceMimeTypes, compilerConfig.allowSourcePathOverlap());
                                sourcePath.addPathElements(compilerConfig.getSourcePath());

                                resources = new ResourceContainer();
                                bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), null);

                                classes = new HashMap();
                                nsComponents = API.setupNamespaceComponents(configuration, mappings, sourcePath, classes);
                                API.setupClasses(configuration, sourcePath, classes);
                            }
                        }
                    }
                }
            }
            
            // Validate CompilationUnits in FileSpec and SourcePath.  If 
            // count > 0 something changed.
            int count = flex2.compiler.API.validateCompilationUnits(
                    fileSpec, sourceList, sourcePath, bundlePath, resources, 
                    swcContext, 
                    null    /* perCompileData */, 
                    false,  /* true is force recompile of all source units */ 
                    configuration);

            // This isn't really incremental since any change requires a total
            // recompile, relink and re-export, but it shouldn't update the output 
            // file if there are no changes to it's dependencies.
            if (recompile || count > 0 || !swcFile.exists())
            {
                // Get standard bundle of compilers, transcoders.
                Transcoder[] transcoders = flex2.tools.API.getTranscoders( configuration );
                flex2.compiler.Compiler[] compilers = flex2.tools.API.getCompilers(compilerConfig, mappings, transcoders);

                ArrayList sources = new ArrayList();
                Map rbFiles = new HashMap();
                List units = flex2.compiler.API.compile(
                        fileSpec, sourceList, classes.values(), sourcePath, resources, bundlePath, 
                        swcContext, configuration, compilers, 
                        new CompcPreLink(rbFiles, configuration.getIncludeResourceBundles()), 
                        configuration.getLicensesConfiguration().getLicenseMap(), sources);

                // FLEXCOVER: write out coverage metadata
                String swcStr = configuration.getOutput();

                // default coverage metadata output to sibling file of output
                if (configuration.getCompilerConfiguration().coverage()
                    && !configuration.generateCoverageMetadata())
                {
                    int dotIndex = swcStr.lastIndexOf('.');
                    String cvmName = (dotIndex >= 0) ? swcStr.substring(0, dotIndex) : swcStr;
                    configuration.setCoverageMetadataFileName(cvmName + ".cvm");
                }

                // Link the swc and then export it.
                flex2.compiler.swc.API.exportSwc(configuration, units, nsComponents, cache, rbFiles);
                                
                // If there were no errors and the swc exists then it was exported successfully.
                if (ThreadLocalToolkit.errorCount() == 0 && swcFile.isFile())
                {
                    ThreadLocalToolkit.log(new OutputMessage(FileUtil.getCanonicalPath(swcFile), Long.toString(swcFile.length())));
                }
                
                // If incremental compilation is enabled, save the compilation units.
                if (configuration.getCompilerConfiguration().getIncremental())
                {
                    // Make sure the checksums are all current.
                    swcChecksums.saveChecksums(units);                    

                    // These are files which don't necessarily have dependencies on them
                    // so the compiler won't figure out if they've been modified.
                    Map archiveFiles = new TreeMap();
                    if (configuration.getCSSArchiveFiles() != null) archiveFiles.putAll(configuration.getCSSArchiveFiles());
                    if (configuration.getL10NArchiveFiles() != null) archiveFiles.putAll(configuration.getL10NArchiveFiles());
                    if (configuration.getFiles() != null) archiveFiles.putAll(configuration.getFiles());
                    if (configuration.getStylesheets() != null) archiveFiles.putAll(configuration.getStylesheets());                    
                    swcChecksums.saveArchiveFilesChecksums(archiveFiles);
                    
                    RandomAccessFile incrementalFile = null;
                    try
                    {
                        incrementalFile = new RandomAccessFile(incrementalFileName, "rw");
                        
                        // In case we're reusing the file, clear it.
                        incrementalFile.setLength(0);

                        flex2.compiler.API.persistCompilationUnits(
                                configuration, fileSpec, sourceList, sourcePath, resources, bundlePath, 
                                null, /* sources */
                                null, /* units */ 
                                swcChecksums.getChecksums(),
                                swcChecksums.getSwcDefSignatureChecksums(),
                                swcChecksums.getSwcFileChecksums(),
                                swcChecksums.getArchiveFileChecksums(), 
                                "", incrementalFile);
                    }
                    catch (IOException ex)
                    {
                        ThreadLocalToolkit.logInfo(ex.getLocalizedMessage());
                        
                        // Get rid of the cache file since the write failed.
                        new File(incrementalFileName).deleteOnExit();
                    }
                    finally
                    {
                        if (incrementalFile != null)
                        {
                            try
                            {
                                incrementalFile.close();
                            }
                            catch (IOException ex)
                            {
                            }
                        }
                    }
                }
            }
            else
            {
                // swc is already up-to-date so no need to compile/link or rewrite file
                ThreadLocalToolkit.log(new NoUpdateMessage(FileUtil.getCanonicalPath(swcFile)));            
            }
        }
        catch (ConfigurationException ex)
        {
            displayStartMessage();
            Compiler.processConfigurationException(ex, "compc");
        }
        catch (LicenseException ex)
        {
            ThreadLocalToolkit.logError(ex.getMessage());
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (LinkerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (Throwable t) // IOException, Throwable
        {
            ThreadLocalToolkit.logError(t.getMessage());
            t.printStackTrace();
        }
	    finally
        {
	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().totalTime();
		        ThreadLocalToolkit.getBenchmark().peakMemoryUsage(true);
	        }

	        flex2.compiler.API.removePathResolver();
        }
    }

	
    public static final void displayStartMessage()
    {
	    LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();

	    System.out.println(l10n.getLocalizedTextString(new StartMessage(VersionInfo.buildMessage())));
    }

	public static class StartMessage extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = -3698237096980702681L;

        public StartMessage(String buildMessage)
		{
			super();
			this.buildMessage = buildMessage;
		}

		public final String buildMessage;
	}

    public static class OutputMessage extends CompilerMessage.CompilerInfo
    {
        private static final long serialVersionUID = 8747873783743625156L;

        public String name;
        public String length;

        public OutputMessage(String name, String length)
        {
            this.name = name;
            this.length = length;
        }
    }
    
    public static class NoUpdateMessage extends CompilerMessage.CompilerInfo
    {        
        private static final long serialVersionUID = -3432746769116961891L;
        
        public String name;

        public NoUpdateMessage(String name)
        {
            this.name = name;
        }
    }
}
