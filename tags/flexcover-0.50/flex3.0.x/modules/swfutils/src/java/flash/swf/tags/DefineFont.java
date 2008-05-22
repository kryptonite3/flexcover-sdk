////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.swf.tags;

import flash.swf.TagHandler;
import flash.swf.types.KerningRecord;
import flash.swf.types.Rect;
import flash.swf.types.Shape;
import flash.util.ArrayUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Clement Wong
 */
public class DefineFont extends DefineTag
{
    public DefineFont(int code)
    {
        super(code);
    }

    public void visit(TagHandler h)
	{
        if (code == stagDefineFont)
    		h.defineFont(this);
        else if (code == stagDefineFont2)
            h.defineFont2(this);
        else
            h.defineFont3(this);
	}

    public void visitDefs(TagHandler h)
    {
        for (int i=0; i < glyphShapeTable.length; i++)
        {
            glyphShapeTable[i].visitDependents(h);
        }
    }

    public Iterator getReferences()
    {
        // This is yucky.
        List refs = new LinkedList();

        for (int i = 0; i < glyphShapeTable.length; i++)
            glyphShapeTable[i].getReferenceList( refs );

        return refs.iterator();
    }

	public Shape[] glyphShapeTable;

    public DefineFontInfo fontInfo;

    public boolean smallText;
    public boolean hasLayout;
    public boolean shiftJIS;
    public boolean ansi;
    public boolean wideOffsets; // not in equality check- sometimes determined from other vars at encoding time
    public boolean wideCodes; // ditto
    public boolean italic;
    public boolean bold;
    public int langCode;
    public String fontName;

    /** only valid on defineFont2/defineFont3.  U16 if wideOffsets == true, U8 otherwise */
    public char[] codeTable;
    public int ascent;
    public int descent;
    public int leading;

    public short[] advanceTable;
    public Rect[] boundsTable;
    public int kerningCount;
    public KerningRecord[] kerningTable;
    public DefineFontAlignZones zones;
	public DefineFontName license;

    public boolean equals(Object object)
    {
        boolean isEqual = false;

        if (super.equals(object) && (object instanceof DefineFont))
        {
            DefineFont defineFont = (DefineFont) object;

            if ( ArrayUtil.equals(defineFont.glyphShapeTable, this.glyphShapeTable) &&
                 equals(defineFont.fontInfo, this.fontInfo) &&
                 (defineFont.hasLayout == this.hasLayout) &&
                 (defineFont.shiftJIS == this.shiftJIS) &&
                 (defineFont.ansi == this.ansi) &&
                 (defineFont.italic == this.italic) &&
                 (defineFont.bold == this.bold) &&
                 (defineFont.langCode == this.langCode) &&
                 equals(defineFont.name, this.name) &&
                 equals(defineFont.fontName, this.fontName) &&
                 ArrayUtil.equals(defineFont.codeTable, this.codeTable) &&
                 (defineFont.ascent == this.ascent) &&
                 (defineFont.descent == this.descent) &&
                 (defineFont.leading == this.leading) &&
                 ArrayUtil.equals(defineFont.advanceTable, this.advanceTable) &&
                 ArrayUtil.equals(defineFont.boundsTable, this.boundsTable) &&
                 (defineFont.kerningCount == this.kerningCount) &&
                 ArrayUtil.equals(defineFont.kerningTable, this.kerningTable) )
            {
                isEqual = true;
            }
        }

        return isEqual;
    }
}
