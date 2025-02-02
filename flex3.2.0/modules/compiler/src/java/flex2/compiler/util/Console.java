////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import java.io.IOException;

import flash.localization.LocalizationManager;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.Logger;
import flex2.compiler.io.FileUtil;

/**
 * @author Clement Wong
 */
public class Console extends AbstractLogger implements Logger
{
    private static final String lineSeparator = System.getProperty("line.separator");

    public Console()
    {
        this(true, true, true, true);
    }

	public Console(boolean isInfoEnabled, boolean isDebugEnabled, boolean isWarningEnabled, boolean isErrorEnabled)
	{
		this.isInfoEnabled = isInfoEnabled;
		this.isDebugEnabled = isDebugEnabled;
		this.isWarningEnabled = isWarningEnabled;
		this.isErrorEnabled = isErrorEnabled;

		init(ThreadLocalToolkit.getLocalizationManager());
	}

    private boolean isInfoEnabled, isDebugEnabled, isWarningEnabled, isErrorEnabled;

    private int errorCount;
    private int warningCount;

    public int errorCount()
    {
        return errorCount;
    }

    public int warningCount()
    {
        return warningCount;
    }

    public void logInfo(String info)
    {
        if (!isInfoEnabled)
        {
            return;
        }
        printOut(info, false);
    }

    public void logDebug(String debug)
    {
        if (!isDebugEnabled)
        {
            return;
        }
        printOut(debug, false);
    }

    public void logWarning(String warning)
    {
        if (!isWarningEnabled)
        {
            return;
        }
        printWarning(WARNING + ": " + warning);
    }

    public void logError(String error)
    {
        if (!isErrorEnabled)
        {
            return;
        }
        printError(ERROR + ": " + error);
    }

    public void logInfo(String path, String info)
    {
        if (!isInfoEnabled)
        {
            return;
        }
        printOut(path + lineSeparator + lineSeparator + info);
    }

    public void logDebug(String path, String debug)
    {
        if (!isDebugEnabled)
        {
            return;
        }
        printOut(path + lineSeparator + lineSeparator + debug);
    }

    public void logWarning(String path, String warning)
    {
        if (!isWarningEnabled)
        {
            return;
        }
        printWarning(path + lineSeparator + lineSeparator + WARNING + ": " + warning);
    }

	public void logWarning(String path, String warning, int errorCode)
	{
		logWarning(path, warning);
	}

    public void logError(String path, String error)
    {
        if (!isErrorEnabled)
        {
            return;
        }
        printError(path + lineSeparator + lineSeparator + ERROR + ": " + error);
    }

	public void logError(String path, String error, int errorCode)
	{
		logError(path, error);
	}

    public void logInfo(String path, int line, String info)
    {
        if (!isInfoEnabled)
        {
            return;
        }
        printOut(path + "(" + line + "):" + " " + info);
    }

    public void logDebug(String path, int line, String debug)
    {
        if (!isDebugEnabled)
        {
            return;
        }
        printOut(path + "(" + line + "):" + " " + debug);
    }

    public void logWarning(String path, int line, String warning)
    {
        if (!isWarningEnabled)
        {
            return;
        }
        printWarning(path + "(" + line + "):" + " " + WARNING + ": " + warning + lineSeparator + lineSeparator + getLineText(path, line));
    }

	public void logWarning(String path, int line, String warning, int errorCode)
	{
		logWarning(path, line, warning);
	}

    public void logError(String path, int line, String error)
    {
        if (!isErrorEnabled)
        {
            return;
        }
        printError(path + "(" + line + "): " + " " + ERROR + ": " + error + lineSeparator + lineSeparator + getLineText(path, line));
    }

	public void logError(String path, int line, String error, int errorCode)
	{
		logError(path, line, error);
	}

	public void logInfo(String path, int line, int col, String info)
	{
		if (!isInfoEnabled)
		{
			return;
		}
		printOut(path + "(" + line + "): " + COL + ": " + col + " " + info);
	}

    public void logDebug(String path, int line, int col, String debug)
	{
		if (!isDebugEnabled)
		{
			return;
		}
		printOut(path + "(" + line + "): " + COL + ": " + col + " " + debug);
	}

	public void logWarning(String path, int line, int col, String warning)
	{
		if (!isWarningEnabled)
		{
			return;
		}
		// C: no source... must read from path...
		String lineText = getLineText(path, line);
		printWarning(path + "(" + line + "): " + COL + ": " + col + " " + WARNING + ": " + warning + lineSeparator + lineSeparator +
		         lineText + lineSeparator + getLinePointer(col, lineText));
	}

	public void log( ILocalizableMessage m)
	{
		log(m, null);
	}

    public void log( ILocalizableMessage m, String source)
    {
        if (m.getLevel() == ILocalizableMessage.ERROR)
        {
            if (!isErrorEnabled)
            {
                return;
            }
        }
        else if (m.getLevel() == ILocalizableMessage.WARNING)
        {
            if (!isWarningEnabled)
            {
                return;
            }
        }
        else
        {
            if (!isInfoEnabled)
            {
                return;
            }
        }
        String prefix = formatPrefix( getLocalizationManager(), m );
        boolean found = true;
	    LocalizationManager loc = getLocalizationManager();
        String text = loc.getLocalizedTextString( m );
        if (text == null)
        {
            text = m.getClass().getName();
            found = false;
        }
	    String exText = formatExceptionDetail(m, loc);
	    if (m.getPath() != null && m.getLine() != -1)
	    {
		    exText += lineSeparator + lineSeparator + (source == null ? getLineText(m.getPath(), m.getLine()) : source);
	    }
	    if (m.getColumn() != -1)
	    {
		    exText += lineSeparator + getLinePointer(m.getColumn(), source);
	    }
	    if (m.getLevel() == ILocalizableMessage.INFO)
	    {
		    printOut(prefix + text + exText, false);
	    }
	    else if (m.getLevel() == ILocalizableMessage.WARNING)
	    {
		    printWarning(prefix + text + exText);
	    }
	    else
	    {
		    printError(prefix + text + exText);
	    }
	    assert found : "Localized text missing for " + m.getClass().getName();
    }

	public void logError(String path, int line, int col, String error)
	{
		if (!isErrorEnabled)
		{
			return;
		}
		// C: no source... must read from path...
		String lineText = getLineText(path, line);
		printError(path + "(" + line + "): " + COL + ": " + col + " " + ERROR + ": " + error + lineSeparator + lineSeparator +
		           lineText + lineSeparator + getLinePointer(col, lineText));
	}

	public void logWarning(String path, int line, int col, String warning, String source)
	{
		if (!isWarningEnabled)
		{
			return;
		}
		printWarning(path + "(" + line + "): " + COL + ": " + col + " " + WARNING + ": " + warning + lineSeparator + lineSeparator +
		         source + lineSeparator + getLinePointer(col, source));
	}

	public void logWarning(String path, int line, int col, String warning, String source, int errorCode)
	{
		logWarning(path, line, col, warning, source);
	}

	public void logError(String path, int line, int col, String error, String source)
	{
		if (!isErrorEnabled)
		{
			return;
		}
		printError(path + "(" + line + "): " + COL + ": " + col + " " + ERROR + ": " + error + lineSeparator + lineSeparator +
		         source + lineSeparator + getLinePointer(col, source));
	}

	public void logError(String path, int line, int col, String error, String source, int errorCode)
	{
		logError(path, line, col, error, source);
	}

	public void needsCompilation(String path, String reason)
	{
		printOut(RECOMPILE + ": " + path, false);
		printOut(REASON + ": " + reason, false);
	}

	public void includedFileUpdated(String path)
	{
		printOut(INCLUDEUPDATED + ": " + path, false);
	}

	public void includedFileAffected(String path)
	{
		printOut(INCLUDEAFFECTED + ": " + path, false);
	}

    private void printOut(String message)
    {
        printOut(message, true);
    }

    private void printOut(String message, boolean extraLineBreak)
    {
        System.out.println(message + (extraLineBreak ? lineSeparator : ""));
    }

	private void printWarning(String message)
	{
        warningCount++;
        System.err.println(message + lineSeparator);
        if (message == null)
        {
        	Thread.dumpStack();
        }
	}

	private void printWarning(String message, byte[] more)
	{
        warningCount++;
        System.err.print(message);
        try
        {
        	System.err.write(more);
        }
        catch (IOException ex)
        {
        }
        System.err.println(lineSeparator);
	}

    private void printError(String message)
    {
        errorCount++;
        System.err.println(message + lineSeparator);
        if (message == null)
        {
        	Thread.dumpStack();
        }
    }

	private void printError(String message, byte[] more)
	{
        errorCount++;
        System.err.print(message);
        try
        {
        	System.err.write(more);
        }
        catch (IOException ex)
        {
        }
        System.err.println(lineSeparator);
	}

    private String getLineText(String path, int line)
    {
        String text = FileUtil.readLine(path, line);
    	return text == null ? "" : text;
    }

    private byte[] getLineBytes(String path, int line)
    {
        return FileUtil.readBytes(path, line);
    }

    private String getLinePointer(int col, String source)
    {
        if (col <= 0) // col == 0 is likely an error...
        {
            return "^";
        }

        StringBuffer b = new StringBuffer(col);
        for (int i = 0; i < col - 1; i++)
        {
	        if (source != null && i < source.length() && source.charAt(i) == '\t')
	        {
                b.append('\t');
	        }
	        else
	        {
		        b.append(' ');
	        }
        }

        b.append('^');

        return b.toString();
    }
}
