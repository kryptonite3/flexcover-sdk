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
        cfgbuf.setDefaultVar("include-classes");

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

			NameMappings mappings = flex2.compiler.API.getNameMappings(configuration);

            // note: if Configuration is ever shared with other parts of the system, then this part will need
            // to change, since we're setting a compc-specific setting below
            compilerConfig.setMetadataExport(true);

			//	get standard bundle of compilers, transcoders
            Transcoder[] transcoders = flex2.tools.API.getTranscoders( configuration );
            flex2.compiler.Compiler[] compilers = flex2.tools.API.getCompilers(compilerConfig, mappings, transcoders);

            // create a FileSpec... can reuse based on appPath, debug settings, etc...
            FileSpec fileSpec = new FileSpec(array[0], flex2.tools.API.getFileSpecMimeTypes(), false);

            // create a SourceList...
            SourceList sourceList = new SourceList(array[1], compilerConfig.getSourcePath(), null,
            									   flex2.tools.API.getSourceListMimeTypes(), false);

	        ResourceContainer resources = new ResourceContainer();
	        ResourceBundlePath bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), null);

	        Map classes = new HashMap();
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

			// validate CompilationUnits in FileSpec and SourcePath
	        flex2.compiler.API.validateCompilationUnits(fileSpec, sourceList, sourcePath, bundlePath, resources, swcContext,
	        											null, false, configuration);

			Map licenseMap = configuration.getLicensesConfiguration().getLicenseMap();

            // compile
            swcContext.setLock(true); // FIXME, block compile from closing swcs
            Map rbFiles = new HashMap();
            List units = flex2.compiler.API.compile(fileSpec, sourceList, classes.values(), sourcePath, resources, bundlePath, swcContext,
	                                                   configuration, compilers, new CompcPreLink(rbFiles, configuration.getIncludeResourceBundles()), licenseMap, new ArrayList()); // List<CompilationUnit>
            swcContext.setLock(false);  // FIXME, allow swcs to be closed
	        // export SWC
            API.exportSwc( configuration, units, nsComponents, cache, rbFiles );
//            swcContext.close(); // FIXME, there's a close in compile() that we're blocking
            String swcStr = configuration.getOutput();
            if (swcStr != null && ThreadLocalToolkit.errorCount() == 0)
            {
	            File file = FileUtil.openFile(swcStr);
	            if (file != null && file.exists() && file.isFile())
	            {
	            	String name = FileUtil.getCanonicalPath(file);
	            	ThreadLocalToolkit.log(new OutputMessage(name, Long.toString(file.length())));
	            }
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
		public StartMessage(String buildMessage)
		{
			super();
			this.buildMessage = buildMessage;
		}

		public final String buildMessage;
	}

    public static class OutputMessage extends CompilerMessage.CompilerInfo
    {
        public String name;
        public String length;

        public OutputMessage(String name, String length)
        {
            this.name = name;
            this.length = length;
        }
    }
}
