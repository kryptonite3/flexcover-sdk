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

package flex2.compiler.as3;

import flex2.compiler.Source;
import flex2.compiler.util.LineNumberMap;
import macromedia.asc.embedding.avmplus.ActionBlockEmitter;
import macromedia.asc.embedding.avmplus.RuntimeConstants;
import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.util.ByteList;
import macromedia.asc.util.Context;
import macromedia.asc.util.Namespaces;
import macromedia.asc.util.StringPrintWriter;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Clement Wong
 */
public final class BytecodeEmitter extends ActionBlockEmitter
{
	public BytecodeEmitter(Context cx, Source source, boolean debug)
	{
		this(cx, source, debug, false, null);
	}

	public BytecodeEmitter(Context cx, Source source, boolean debug, boolean coverage, LineNumberMap map)
	{
		super(cx, source != null ? source.getName() : null, new StringPrintWriter(), new StringPrintWriter(), false, false, false, debug);
		this.map = map;
		this.source = source;
		this.cx = cx;
        this.coverage = coverage; 

		if (debug)
		{
			lines = new HashSet();
			key = new Line();
		}
	}

	private LineNumberMap map;
	private Source source;
	private String currentFileName;
	private Context cx;
	
	// C: not used when debug is false...
	private Set lines;
	private Line key;

	protected void DebugSlot(String name, int slot, int line)
	{
        // Short circuit if the line is beyond the end of the file.
		if (line > cx.input.lnNum)
		{
			return;
		}

		if (source.isDebuggable())
		{
			int newLine = calculateLineNumber(line);
			if (newLine != -1)
			{
				super.DebugSlot(name, slot, newLine);
			}
		}
	}

	protected String DebugFile(String name)
	{
		currentFileName = name;

		if (!source.isDebuggable())
		{
			return name;
		}

		if (map != null)
		{
			if (map.getNewName().equals(name))
			{
				name = map.getOldName();
			}
		}

		// C: reconstruct filenames based on the path;package;file format.
		//    apply to SourcePath files only...
		//    root is in FileSpec but we're considered it a special case. it's questionable...
		//	  Note ResourceContainer case added for inline components 
		if (source.isSourcePathOwner() ||
				source.isSourceListOwner() ||
				source.isResourceContainerOwner() ||
				source.isRoot())
		{
			String relativePath = source.getRelativePath().replace('/', File.separatorChar);
			if (relativePath.length() == 0)
			{
				int index = name.lastIndexOf(File.separatorChar);
				if (index != -1)
				{
					name = name.substring(0, index) + ";;" + name.substring(index + 1);
				}
			}
			else
			{
				// C: e.g. relativePath = mx\controls
				int separatorIndex = name.lastIndexOf(File.separatorChar);
				int index = separatorIndex > -1 ? name.lastIndexOf(relativePath, separatorIndex) : name.lastIndexOf(relativePath);
				if (index > 0)
				{
					name = name.substring(0, index - 1) + ";" + relativePath + ";" + name.substring(index + relativePath.length() + 1);
				}
			}
		}

		return super.DebugFile(name);
	}

	protected void DebugLine(ByteList code, int line)
	{
        // Short circuit if the line is beyond the end of the file.
		if (line > cx.input.lnNum)
		{
			return;
		}

		if (lines != null)
		{
			key.fileName = currentFileName;
			key.line = line;

			if (!lines.contains(key))
			{
				lines.add(new Line(currentFileName, line));
				source.lineCount = lines.size();
			}
		}

		if (!source.isDebuggable())
		{
			return;
		}

		int newLine = calculateLineNumber(line);
		if (newLine != -1)
		{
			super.DebugLine(code, newLine);
		}
	}
	
    /* (non-Javadoc)
     * @see macromedia.asc.embedding.avmplus.ActionBlockEmitter#RecordLineCoverage(java.lang.String, int, java.lang.String)
     */
    public boolean RecordLineCoverage(String functionName, int linenum, String debugFileName)
    {
        if (functionName == null
            || functionName.length() == 0
            || (!source.isDebuggable())
            || (!coverage))
        {
            return false;
        }

        // Short circuit if the line is beyond the end of the file.
        if (linenum > cx.input.lnNum)
        {
            return false;
        }

        int newLine = calculateLineNumber(linenum);
        if (newLine == -1)
        {
            return false;
        }
        
        String coverageKey = functionName + "@" + newLine;
        instrumentCoverage(coverageKey, debugFileName);
        return true;
    }

    /* (non-Javadoc)
     * @see macromedia.asc.embedding.avmplus.ActionBlockEmitter#RecordBranchCoverage(java.lang.String, boolean, int, int)
     */
    public boolean RecordBranchCoverage(String functionName, boolean isBranch, int linenum, int colnum)
    {
        if (functionName == null
            || functionName.length() == 0
            || (!source.isDebuggable())
            || (!coverage))
        {
            return false;
        }

        // Short circuit if the line is beyond the end of the file.
        if (linenum > cx.input.lnNum)
        {
            return false;
        }

        int newLine = calculateLineNumber(linenum);
        if (newLine == -1)
        {
            return false;
        }
        
        // Guard against recursive coverage recording calls from instrumentation bytecode output
        boolean saved_emit_debug_info = emit_debug_info;
        emit_debug_info = false;
        
        String branchLocation = linenum + "." + colnum;
        if (map != null)
        {
        	// If there is a map, then this indicates that actually we're in an MXML source file,
        	// and the line number and column might be meaningless, so emit the MXML line number
        	// followed by a "#" and the AS3 source's line and column.
        	//
            branchLocation = newLine + "#" + branchLocation;
        }
        
        String coverageKey = functionName + '@'
            + (isBranch ? '+' : '-') 
            + branchLocation;

        instrumentCoverage(coverageKey, null);
        
        emit_debug_info = saved_emit_debug_info;
        
        return true;
    }

    /**
     * Emit an instrumentation call to the top-level coverage() function containing the coverage key and,
     * optionally, the source file name.
     * 
     * @param coverageKey a coverage key describing some executable coverage element in the program
     * @param debugFileName an optional name to be recorded along with the key as part of the coverage metadata,
     * as an aid to finding the source later.  This name is not passed in the coverage() call.
     */
    private void instrumentCoverage(String coverageKey, String debugFileName)
    {
        final String COVERAGE = "coverage";
        ObjectValue n = cx.publicNamespace();
        Namespaces ns = cx.statics.internNamespaces.intern(n);
        
        boolean scopeStackEmpty = (cur_scope == 0); 
        if (scopeStackEmpty)
        {
	        LoadThis();
	        PushScope();   // Need to push scope to keep verifier happy if scope stack is empty
        }
        
        FindProperty(COVERAGE, ns, true, true, false);
        PushString(coverageKey);
        CallProperty(COVERAGE, ns, 1, true, false, false, false);
        Pop();
        
        if (scopeStackEmpty)
        {
        	PopScope();    // Pop the extra scope
        }
        
        if (debugFileName != null)
        {
            String fileName;
            int idx = debugFileName.indexOf(";;");
            if (idx >= 0)
            {
                fileName = debugFileName.substring(0, idx) + File.separator + debugFileName.substring(idx+2);
            }
            else
            {
                fileName = debugFileName.replace(';', File.separatorChar);
            }
            coverageKey += ";" + fileName;
        }
        cx.statics.addCoverageKey(coverageKey);
    }

	private int calculateLineNumber(int line)
	{
		if (map == null || !source.getName().equals(currentFileName))
		{
			// System.out.println(currentFileName + ": " + line);
			return line;
		}
		else
		{
			int newLine = map.get(line);
			if (newLine > 0)
			{
				// System.out.println(currentFileName + ": " + line + " --> " + map.getOldName() + ": " + newLine);
				return newLine;
			}
			else
			{
				// C: lines corresponding to internal code are not "debuggable".
				return -1;
			}
		}
	}

	private static class Line
	{
		Line()
		{
		}

		Line(String f, int l)
		{
			this.fileName = f;
			this.line = l;
		}

		public String fileName;
		public int line;

		public boolean equals(Object o)
		{
			if (o == this)
			{
				return true;
			}
			else if (o instanceof Line)
			{
				Line line = (Line) o;
				return fileName.equals(line.fileName) && this.line == line.line;
			}
			else
			{
				return false;
			}
		}

		public int hashCode()
		{
			return fileName.hashCode() ^ line;
		}
	}
}
