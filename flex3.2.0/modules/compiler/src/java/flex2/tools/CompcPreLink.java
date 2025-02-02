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

import flex2.compiler.FileSpec;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.Source;
import flex2.compiler.SourceList;
import flex2.compiler.SourcePath;
import flex2.compiler.ResourceContainer;
import flex2.compiler.SymbolTable;
import flex2.compiler.CompilerSwcContext;
import flex2.compiler.CompilationUnit;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.swc.SwcException;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.common.Configuration;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;

import java.io.File;
import java.util.*;

/**
 * @author Brian Deitte
 */
public class CompcPreLink implements flex2.compiler.PreLink
{
	public CompcPreLink(Map rbFiles, List defaults)
	{
		this.rbFiles = rbFiles;
		this.defaults = defaults;
	}
	
	private Map rbFiles;
	private List defaults;
	
    public void run(List sources, List units,
                    FileSpec fileSpec, SourceList sourceList, SourcePath sourcePath, ResourceBundlePath bundlePath,
                    ResourceContainer resources, SymbolTable symbolTable, CompilerSwcContext swcContext,
                    Configuration configuration)
    {
    	postGenerateExtraSwcCode(sources, units, symbolTable, sourceList, sourcePath, bundlePath, resources, swcContext, configuration);
	    processResourceBundles(rbFiles, configuration, sources, defaults, symbolTable, bundlePath, swcContext);
    }

    private void postGenerateExtraSwcCode(List sources, List units, SymbolTable symbolTable, SourceList sourceList, SourcePath sourcePath,
    								  ResourceBundlePath bundlePath, ResourceContainer resources, CompilerSwcContext swcContext, Configuration configuration)
    {
    	LinkedList fonts = new LinkedList();
    	boolean isAccessible = configuration.getCompilerConfiguration().accessible();
    	Set accessibilityList = new HashSet(), externs = configuration.getExterns();

    	for (int i = 0, size = units == null ? 0 : units.size(); i < size; i++)
    	{
    		CompilationUnit u = (CompilationUnit) units.get(i);
    		if (u != null && !PreLink.isCompilationUnitExternal(u, externs) && 
    			!u.getSource().isInternal())
    		{
    			if (isAccessible) {
        			Set unitAccessibilityList = u.getAccessibilityClasses();
        			if (unitAccessibilityList != null)
        			{
        				accessibilityList.addAll(unitAccessibilityList);
        			}
    			}

    			// don't add font assets for definitions that have been externed.
    			List fontList = u.getAssets().getFonts();
    			if (fontList != null && !fontList.isEmpty())
    			{
    				fonts.addAll(fontList);    // save for later...
    			}
    		}
    	}

    	if (accessibilityList.size() > 0)
    	{
    		for (Iterator it = accessibilityList.iterator(); it.hasNext();)
    		{
    			String className = (String) it.next();
    			MultiName mName = new MultiName(NameFormatter.retrievePackageName(className), NameFormatter.retrieveClassName(className));
    			flex2.compiler.API.resolveMultiName(mName, sources, sourceList, sourcePath, resources, swcContext, symbolTable);
    		}
    	}
    	
	    codegenRootClass(sources, units, swcContext, configuration, fonts);
    }
     
    private void processResourceBundles(Map rbFiles, Configuration configuration, List sources, List defaults,
    									SymbolTable symbolTable, ResourceBundlePath bundlePath, CompilerSwcContext swcContext)
    {
	    if (rbFiles != null && !configuration.generateRBList())
	    {
    		String[] locales = configuration.getCompilerConfiguration().getLocales();
	    	Set s = new TreeSet(), externs = configuration.getExterns();
	    	
	    	for (int i = 0, size = sources.size(); i < size; i++)
	    	{
	    		Source src = (Source) sources.get(i);
	    		CompilationUnit unit = src == null ? null : src.getCompilationUnit();
	    		if (unit != null && !PreLink.isCompilationUnitExternal(unit, externs) && 
	    		    !src.isInternal())
	    		{
	    			s.addAll(unit.resourceBundleHistory);
	    		}
	    	}
	    	
	    	for (int i = 0, size = defaults == null ? 0 : defaults.size(); i < size; i++)
	    	{
	    		s.add(defaults.get(i));
	    	}

	    	for (Iterator i = s.iterator(); i.hasNext(); )
	    	{
	    		String rbName = NameFormatter.toColon((String) i.next());
	    		QName qName = new QName(rbName);
	    		
	    		VirtualFile[] files = bundlePath.findVirtualFiles(rbName);

	    		if (files == null)
	    		{
	    			files = swcContext.getVirtualFiles(locales, qName.getNamespace(), qName.getLocalPart());
	    		}

                if (files == null)
                {
                    // Handle Flex 2 style precompiled resource bundles.
                    QName precompiledQName = new QName(rbName + I18nUtils.CLASS_SUFFIX);
                    Source source = swcContext.getSource(precompiledQName.getNamespace(), 
                                                         precompiledQName.getLocalPart());

                    if (source != null)
                    {
                        externs.add(qName);
                        continue;
                    }
                }

	    		if (files == null && locales.length > 0)
	    		{
                    ThreadLocalToolkit.log(new SwcException.NoResourceBundleSource(rbName));
	    		}
	    		
	    		for (int j = 0, size = files == null ? 0 : files.length; j < size; j++)
	    		{
	    			if (files[j] != null)
	    			{
	    				String ext = MimeMappings.getExtension(files[j].getMimeType());
	    				String key = "locale/" + locales[j] + "/" + rbName.replace(':', '.').replace('.', '/') + ext;
	    				rbFiles.put(key, files[j]);
	    			}
	    		}
	    		
	    		if (files != null)
	    		{
	    			QName[] qNames = flex2.compiler.API.resolveResourceBundleName(rbName, sources, null, bundlePath, null, swcContext, symbolTable, locales);
	    			configuration.addExterns(qNames);
	    		}
	    	}
	    }
    }
    
    /**
     * Output code to create an RSL root class that is executed when the swf is loaded.
     * 
     * @param sources
     * @param units
     * @param swcContext
     * @param configuration
     * @param fonts
     */
	private void codegenRootClass(List sources, List units, 
						CompilerSwcContext swcContext, Configuration configuration,
						List fonts)
	{
		// if there are no fonts in the swc, then we don't need a root class.
		if (fonts.size() == 0) {
			return;
		}
		
	    if (configuration instanceof CompcConfiguration)
	    {
	    	String className = getSwcClassName(((CompcConfiguration)configuration).getOutput());
	    	String rootClassName = "_" + className + "_" + "mx_core_FlexModuleFactory";
	    	String sourceText = PreLink.codegenModuleFactory("flash.display.Sprite", 
	    						rootClassName, 
	    						null,
	    						null,
	    						null,
	    						null, 
	    						null, 
	    						fonts,
	    						null,
	    						null,
	    						null,
	    						null,
	    						configuration, 
	    						swcContext);
	    	
			String generatedLoaderFile = rootClassName + ".as";
			Source s = new Source(new TextFile(sourceText,
			                                        generatedLoaderFile,
			                                        null,
			                                        MimeMappings.getMimeType(generatedLoaderFile)),
			                       "", rootClassName, null, false, false, false);
			// C: It doesn't look like this Source needs any path resolution. null is fine...
			s.setPathResolver(null);
			sources.add(s);
			configuration.setRootClassName(rootClassName);

			if (configuration.getCompilerConfiguration().keepGeneratedActionScript())
			{
				PreLink.saveGenerated(generatedLoaderFile, sourceText, configuration.getCompilerConfiguration().getGeneratedDirectory());
			}
	    }
	}


	/**
	 * 
	 * @param pathName
	 * @return filename of the swc with the extension removed and the integer
	 */
	private String getSwcClassName(String pathName)
	{
		File file = new File(pathName);
		String fileName = file.getName();
		int ext = fileName.lastIndexOf(".");
		if (ext != -1) 
		{
			fileName = fileName.substring(0, ext);
		}
		
		// replace non-word characters with an underscore.
		fileName = fileName.replaceAll("[\\W]", "_");

		return fileName;
	}

}
