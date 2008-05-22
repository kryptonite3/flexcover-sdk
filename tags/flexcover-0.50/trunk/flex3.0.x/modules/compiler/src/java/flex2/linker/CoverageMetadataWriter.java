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

package flex2.linker;

import java.util.*;

import flex2.compiler.CompilationUnit;

/**
 * @author Joe Berkovitz
 *
 * Spit out a list of all the coverage keys collected during all the byte code emitted.
 *
 */
public class CoverageMetadataWriter
{

    public static String dump(List units)
    {
        StringBuffer buf = new StringBuffer( 2048 );
        for (Iterator it = units.iterator(); it.hasNext(); )
        {
            CompilationUnit unit = (CompilationUnit) it.next();
            if (unit.coverageKeys != null)
            {
                for (Iterator it2 = unit.coverageKeys.iterator(); it2.hasNext(); )
                {
                    buf.append(it2.next().toString());
                    buf.append('\n');
                }
            }
        }
        return buf.toString();
    }
}
