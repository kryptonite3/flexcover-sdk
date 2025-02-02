////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.util;

import java.util.Date;

/**
 * Primitive run-time tracing class
 *
 * Code as follows:
 * if (Trace.foo)
 *     Trace.trace("trace msg"...);
 *
 * Enable as follows:
 * java -Dtrace.foo -Dtrace.foo2 -Dtrace.foo3 or -Dtrace.all
 *
 * Special flags:
 * -Dtrace.flex                -- enables all tracing
 * -Dtrace.foo                   -- enables tracing on foo subsystem
 * -Dtrace.timeStamp             -- timeStamp all output lines
 * -Dtrace.caller                -- print the Class:method caller
 * -Dtrace.stackLines=10         -- print 10 stack lines
 * -Dtrace.stackPrefix=java.lang -- print the stack up to java.lang
 *
 * Add new xxx members as desired.
 */
public class Trace
{
    public static final boolean all = (System.getProperty("trace.flex") != null);
    //public static final boolean asc = all || (System.getProperty("trace.asc") != null);
    public static final boolean accessible = all || (System.getProperty("trace.accessible") != null);
	public static final boolean asdoc = all || (System.getProperty("trace.asdoc") != null);		
    //public static final boolean benchmark = all || (System.getProperty("trace.benchmark") != null);
    public static final boolean cache = all || (System.getProperty("trace.cache") != null);
    //public static final boolean compileTime = all || (System.getProperty("trace.compileTime") != null);
    public static final boolean css = all || (System.getProperty("trace.css") != null);
    public static final boolean dependency = all || (System.getProperty("trace.dependency") != null);
    public static final boolean config = all || (System.getProperty("trace.config") != null);
    public static final boolean embed = all || (System.getProperty("trace.embed") != null);
    public static final boolean error = all || (System.getProperty("trace.error") != null);
    public static final boolean font = all || (System.getProperty("trace.font") != null);
    public static final boolean font_cubic = all || (System.getProperty("trace.font.cubic") != null);
    //public static final boolean image = all || (System.getProperty("trace.image") != null);
    //public static final boolean lib = all || (System.getProperty("trace.lib") != null);
    public static final boolean license = all || (System.getProperty("trace.license") != null);
    //public static final boolean linker = all || (System.getProperty("trace.linker") != null);
    public static final boolean mxml = all || (System.getProperty("trace.mxml") != null);
    //public static final boolean parser = all || (System.getProperty("trace.parser") != null);
    public static final boolean profiler = all || (System.getProperty("trace.profiler") != null);
    //public static final boolean schema = all || (System.getProperty("trace.schema") != null);
    public static final boolean swc = all || (System.getProperty("trace.swc") != null);
    //public static final boolean swf = all || (System.getProperty("trace.swf") != null);
    public static final boolean pathResolver = all || (System.getProperty("trace.pathResolver") != null);
    public static final boolean binding = all || (System.getProperty("trace.binding") != null);

    // print just the stack caller
    public static final boolean caller = (System.getProperty("trace.caller") != null);
    // print stack up to the prefix
    public static final String stackPrefix = System.getProperty("trace.stackPrefix");

    // print this number of stack lines
    public static int stackLines = 0;
    static {
        try {
            stackLines = Integer.parseInt(System.getProperty("trace.stackLines"));
        } catch (NumberFormatException e) {
        }
    }
    // print a timestamp with each line
    public static final boolean timeStamp = (System.getProperty("trace.timeStamp") != null);

    // print debug information related to the swc-checksum option
    public static final boolean swcChecksum = all || (System.getProperty("trace.swcChecksum") != null);
    
    /**
     * Write the string as a line to the trace stream. If the
     * "stack" property is enabled, then the caller's stack call
     * is also shown in the date.
     */
    public static void trace(String str) {
        if (timeStamp)
            System.err.print(new Date());

        if(caller)
            System.err.print(ExceptionUtil.getCallAt(new Throwable(), 1) + " ");

        System.err.println(str);

        if (stackLines > 0)
            System.err.println(ExceptionUtil.getStackTraceLines(new Throwable(), stackLines));
        else if (stackPrefix != null)
            System.err.println(ExceptionUtil.getStackTraceUpTo(new Throwable(), stackPrefix));
    }
}

