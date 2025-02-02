////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.fonts;

import java.io.Serializable;


/**
 * @author Brian Deitte
 */
public class LocalFont implements Serializable
{
	public String postscriptName;
	public String path;
	public int fsType;
	public String copyright;
	public String trademark;

	public LocalFont(String postscriptName, String path, int fsType, String copyright, String trademark)
	{
		this.postscriptName = postscriptName;
		this.path = path;
		this.fsType = fsType;
		this.copyright = copyright;
		this.trademark = trademark;
	}

	// we purposefully leave path out of equals()
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		final LocalFont localFont = (LocalFont)o;

		if (fsType != localFont.fsType)
		{
			return false;
		}
		if (copyright != null ? !copyright.equals(localFont.copyright) : localFont.copyright != null)
		{
			return false;
		}
		if (postscriptName != null ? !postscriptName.equals(localFont.postscriptName) : localFont.postscriptName != null)
		{
			return false;
		}
		if (trademark != null ? !trademark.equals(localFont.trademark) : localFont.trademark != null)
		{
			return false;
		}

		return true;
	}
}
