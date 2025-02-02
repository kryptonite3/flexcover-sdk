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

package flex2.compiler.common;

import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationException;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Roger Gonzalez
 *
 * This class makes the default values go in through the configuration buffer instead of just the
 * value set on the actual object.  This is useful for dependency checking and also for reporting
 * the source location of a particular configuration value when there is some sort of a conflict
 * between two values; i.e. "X (set in defaults) must be disabled to use Y (set on command line)"
 *
 * The other (main) reason it is used is for the FileConfigurator.formatBuffer - since getters
 * aren't part of the config system, the only values that can be printed are ones set via the
 * config setters.  Since its handy to be able to generate a full config file based on current
 * settings, having the default values set here makes the defaults show up too.
 *
 * Although the decentralization of the various config objects is somewhat broken by this class,
 * if you consider it to be basically a "baked in config file", then it might feel less strange.
 *
 * In the end, if it is too much of a pain, don't worry about it, just set your local defaults
 * inside your configuration object.  No big deal.
 */
public class DefaultsConfigurator
{
    static private void set( ConfigurationBuffer cfgbuf, String var, String val ) throws ConfigurationException
    {
        LinkedList args = new LinkedList();
        args.add( val );
        cfgbuf.setVar( var, args, "defaults", -1 );
    }

    static private void set( ConfigurationBuffer cfgbuf, String[] vars, String val ) throws ConfigurationException
    {
        for (int i = 0; i < vars.length; ++i)
        {
            set( cfgbuf, vars[i], val );
        }
    }

	// load defaults for normal compile
    static public void loadDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
    {
	    loadCommonDefaults( cfgbuf );
	    set( cfgbuf, "compiler.debug", "false" );
	    set( cfgbuf, "compiler.use-resource-bundle-metadata", "true" );
    }

	// load defaults for compc
	static public void loadCompcDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
	{
		loadCommonDefaults( cfgbuf );
		set( cfgbuf, "compiler.debug", "true" );
		set( cfgbuf, "compiler.use-resource-bundle-metadata", "false" );
		set( cfgbuf, "compiler.archive-classes-and-assets", "true" );
		set (cfgbuf, "directory", "false" );
	}

	static public void loadASDocDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
	{
		loadCommonDefaults( cfgbuf );
		set( cfgbuf, "compiler.debug", "false" );
		set( cfgbuf, "compiler.use-resource-bundle-metadata", "false" );		
		set( cfgbuf, "compiler.doc", "true" );
		set( cfgbuf, "output", "asdoc-output" );
		set( cfgbuf, "left-frameset-width", "-1" );
	}

	static public void loadOEMCompcDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
	{
		loadCommonDefaults( cfgbuf );
		set( cfgbuf, "compiler.debug", "true" );
		set( cfgbuf, "compiler.use-resource-bundle-metadata", "false" );
		set( cfgbuf, "compiler.archive-classes-and-assets", "true" );
	}

	
	static public void loadMinimumDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
	{
		List args = new ArrayList();
		
	    args.add( "${flexlib}/${configname}-config.xml" );
	    // we should probably have a dedicated subclass of DefaultsConfigurator for
	    // each product (i.e. webtier doesn't use this var), but for now, this seems
	    // like such a seductively easy workaround, lets see if this is adequate:
	    if (cfgbuf.isValidVar( "load-config" ))
	        cfgbuf.setVar("load-config", args, "defaults", -1, null, false);
		
	}
	
	
	private static void loadCommonDefaults( ConfigurationBuffer cfgbuf ) throws ConfigurationException
	{
        // specified in case people are using older flex-config.xml
        // also specified in flex-config.xml
        // 9.0.124 is the April 2008 security release
        set( cfgbuf, "target-player", "9.0.124" );
        
	    set( cfgbuf,
	         new String[]
	         {
	             "compiler.accessible",
		         "compiler.es",
	             "compiler.verbose-stacktraces",
	             "compiler.show-dependency-warnings",
	             "compiler.keep-generated-actionscript",
                 "compiler.keep-generated-signatures",
                 "compiler.disable-incremental-optimizations",
	             "compiler.allow-source-path-overlap",
	         }, "false" );

	    set( cfgbuf,
	         new String[]
	         {
		         "lazy-init",
		         "compiler.optimize",
		         "compiler.strict",
		         "compiler.show-actionscript-warnings",
		         "compiler.as3",
	             "compiler.show-deprecation-warnings",
                 "compiler.show-shadowed-device-font-warnings",
	             "compiler.show-binding-warnings",
			     "compiler.fonts.advanced-anti-aliasing",
	             "generate-frame-loader",
		         "use-network",
	         }, "true" );

	    set( cfgbuf, "debug-password", "" );
	    set( cfgbuf, "compiler.locale", java.util.Locale.getDefault().toString());
	    set( cfgbuf, "compiler.translation-format", "flex2.compiler.i18n.PropertyTranslationFormat");

	    set( cfgbuf, "default-frame-rate", "24" );
	    set( cfgbuf, "default-background-color", "0x869CA7" );

	    LinkedList args = new LinkedList();
	    args.add( "500" );
	    args.add( "375" );
	    cfgbuf.setVar( "default-size", args, "defaults", -1 );

	    args.clear();
	    args.add( "${flexlib}/${configname}-config.xml" );
	    // we should probably have a dedicated subclass of DefaultsConfigurator for
	    // each product (i.e. webtier doesn't use this var), but for now, this seems
	    // like such a seductively easy workaround, lets see if this is adequate:
	    if (cfgbuf.isValidVar( "load-config" ))
	        cfgbuf.setVar("load-config", args, "defaults", -1, null, false);

	    args.clear();

	    args.add( "1000" );
	    args.add( "60" );
	    cfgbuf.setVar( "default-script-limits", args, "defaults", -1 );

	    // set( cfgbuf,  "compiler.context-root", "" );

	    // Fonts
	    set( cfgbuf, "compiler.fonts.max-cached-fonts", "20" );
	    set( cfgbuf, "compiler.fonts.max-glyphs-per-face", "1000" );

	    List fontManagers = new ArrayList();
	    fontManagers.add( "flash.fonts.JREFontManager" );
	    fontManagers.add( "flash.fonts.BatikFontManager" );
	    cfgbuf.setVar( "compiler.fonts.managers", fontManagers, "defaults", -1 );

        String os = System.getProperty("os.name");
        if (os != null)
        {
            os = os.toLowerCase();
            if (os.startsWith( "windows xp") )
                set( cfgbuf, "compiler.fonts.local-fonts-snapshot", "${flexlib}/winFonts.ser" );
            else if (os.startsWith("mac os x"))
                set( cfgbuf, "compiler.fonts.local-fonts-snapshot", "${flexlib}/macFonts.ser" );
            else
                set( cfgbuf, "compiler.fonts.local-fonts-snapshot", "${flexlib}/localFonts.ser" );
        }
        else
            set( cfgbuf, "compiler.fonts.local-fonts-snapshot", "${flexlib}/localFonts.ser" );
    }
}
