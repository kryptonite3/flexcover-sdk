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

import flash.swf.Movie;
import flex2.compiler.FileSpec;
import flex2.compiler.ResourceContainer;
import flex2.compiler.SourceList;
import flex2.compiler.SourcePath;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.common.Configuration;
import flex2.compiler.swc.SwcCache;
import flex2.linker.ConsoleApplication;

import java.util.List;
import java.util.Map;

import macromedia.asc.util.ContextStatics;

/**
 * @author Clement Wong
 */
public class Target
{
	public int id;
	public String[] args;
	public int checksum;
	public FileSpec fileSpec;
	public SourceList sourceList;
	public SourcePath sourcePath;
	public ResourceContainer resources;
	public ResourceBundlePath bundlePath;
	public List units;
	public Map rbFiles;
	public String cacheName;
	public String outputName;
	public Configuration configuration;
	public ContextStatics perCompileData;
	public SwcCache swcCache;
	public Movie movie;
	public ConsoleApplication app;
}
