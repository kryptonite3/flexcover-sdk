////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Clement Wong
 */
public class ResourceFile implements VirtualFile
{
	public ResourceFile(String name, String[] locales, VirtualFile[] files, VirtualFile[] pathRoots)
	{
		this.name = name;
		this.files = new LinkedHashMap();
		this.roots = new LinkedHashMap();
		this.locales = new String[locales == null ? 0 : locales.length];
		
		for (int i = 0, len = this.locales.length; i < len; i++)
		{
			this.locales[i] = locales[i];
			
			if (locales[i] != null)
			{
				this.files.put(locales[i], files[i]);
				this.roots.put(locales[i], pathRoots[i]);
			}
		}
		
		setDefault();
	}
	
	private void setDefault()
	{
		defaultLocale = null;
		defaultFile = null;
		defaultRoot = null;
		currentLocale = null;
		
		for (int i = 0, len = this.locales.length; i < len; i++)
		{
			if (locales[i] != null && files.get(locales[i]) != null && defaultLocale == null && defaultFile == null && defaultRoot == null)
			{
				defaultLocale = locales[i];
				defaultFile = (VirtualFile) files.get(locales[i]);
				defaultRoot = (VirtualFile) roots.get(locales[i]);
				
				break;
			}
		}
		
		currentLocale = (locales == null || locales.length == 0) ? null : locales[0]; 
	}

	private Map files, roots;
	private String name;
	private String currentLocale, defaultLocale;
	private String[] locales;
	private VirtualFile defaultFile, defaultRoot;

	public void merge(ResourceFile f)
	{
		if (f != null)
		{
			for (int i = 0, len = locales == null ? 0 : locales.length ; i < len; i++)
			{
				String locale = locales[i];
				Object obj1 = files.get(locale), obj2 = f.files.get(locale);
				if (obj1 == null && obj2 != null)
				{
					files.put(locale, obj2);
					roots.put(locale, obj2);
				}
			}
			
			setDefault();
		}
	}
	
	public void setLocale(String locale)
	{
		this.currentLocale = locale;
	}
	
	public String getName()
	{
		return name;
	}

	public String getNameForReporting()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? getName() : f.getNameForReporting();
	}

	public String getURL()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.getURL();
	}

	public String getParent()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.getParent();
	}

	public boolean isDirectory()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? false : f.isDirectory();
	}

	/**
	 * Return file size...
	 */
	public long size()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? 0 : f.size();
	}

	/**
	 * Return mime type
	 */
	public String getMimeType()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.getMimeType();
	}

	/**
	 * Return input stream...
	 */
	public InputStream getInputStream() throws IOException
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.getInputStream();
	}

	public byte[] toByteArray() throws IOException
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.toByteArray();
	}

	/**
	 * Return last time the underlying source is modified.
	 */
	public long getLastModified()
	{
		long ts = -1;
		
		for (Iterator i = files.keySet().iterator(); i.hasNext(); )
		{
			String locale = (String) i.next();
			VirtualFile f = (VirtualFile) files.get(locale);
			if (f != null && f.getLastModified() > ts)
			{
				ts = f.getLastModified();
			}
		}
		
		return ts;
	}

	/**
	 * Return an instance of this interface which represents the specified relative path.
	 */
	public VirtualFile resolve(String relativeStr)
	{
		VirtualFile f = getVirtualFile();
		return f == null ? null : f.resolve(relativeStr);
	}

	/**
	 * Signal the hosting environment that this instance is no longer used.
	 */
	public void close()
	{
		VirtualFile f = getVirtualFile();
		if (f != null) f.close();
	}

	public boolean equals(Object obj)
	{
		VirtualFile f = getVirtualFile();
		return f == null ? (obj == null) : f.equals(obj);
	}

	public int hashCode()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? -1 : f.hashCode();
	}

	public boolean isTextBased()
	{
		VirtualFile f = getVirtualFile();
		return f == null ? false : f.isTextBased();
	}

	private VirtualFile getVirtualFile()
	{
		if (currentLocale == null)
		{
			return defaultFile;
		}
		
		VirtualFile f = (VirtualFile) files.get(currentLocale);

		if (f == null)
		{
			return defaultFile;
		}
		else
		{
			return f;
		}
	}

	public VirtualFile getResourcePathRoot()
	{
		if (currentLocale == null)
		{
			return defaultRoot;
		}
		
		VirtualFile f = (VirtualFile) roots.get(currentLocale);

		if (f == null)
		{
			return defaultRoot;
		}
		else
		{
			return f;
		}
	}
	
	public VirtualFile getResourceFile()
	{
		return getVirtualFile();
	}

	public VirtualFile[] getResourceFiles()
	{
		VirtualFile[] list = new VirtualFile[files.size()];
		int j = 0;
		for (Iterator i = files.keySet().iterator(); i.hasNext(); j++)
		{
			String locale = (String) i.next();
			list[j] = (VirtualFile) files.get(locale);
		}
		return list;
	}
	
	public VirtualFile[] getResourcePathRoots()
	{
		VirtualFile[] list = new VirtualFile[roots.size()];
		int j = 0;
		for (Iterator i = roots.keySet().iterator(); i.hasNext(); j++)
		{
			String locale = (String) i.next();
			list[j] = (VirtualFile) roots.get(locale);
		}
		return list;
	}
	
	public boolean complete()
	{
		for (int i = 0, len = locales.length; i < len; i++)
		{
			if (files.get(locales[i]) == null)
			{
				return false;
			}
		}
		return true;
	}
}


