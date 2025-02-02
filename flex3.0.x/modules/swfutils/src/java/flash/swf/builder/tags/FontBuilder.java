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

package flash.swf.builder.tags;

import flash.fonts.FontFace;
import flash.fonts.FontManager;
import flash.fonts.FSType;
import flash.swf.Tag;
import flash.swf.TagValues;
import flash.swf.builder.types.ZoneRecordBuilder;
import flash.swf.tags.DefineFont;
import flash.swf.tags.DefineTag;
import flash.swf.tags.DefineFontName;
import flash.swf.tags.DefineFontAlignZones;
import flash.swf.tags.ZoneRecord;
import flash.swf.types.GlyphEntry;
import flash.swf.types.KerningRecord;
import flash.swf.types.Rect;
import flash.swf.types.Shape;
import flash.util.IntMap;
import flash.util.Trace;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * A utility class to build a DefineFont, DefineFont2, or DefineFont3 tag. One
 * must supply a font family name and style to establish a default font face
 * and a <code>FontManager</code> to locate and cache fonts and glyphs.
 *
 * @author Peter Farland
 */
public final class FontBuilder implements TagBuilder
{
    public DefineFont tag;

    private boolean flashType;
    private IntMap glyphEntryMap; // Code-point ordered collection of glyphs
    private FontFace defaultFace;
    private double fontHeight;
    private ZoneRecordBuilder zoneRecordBuilder;

    private static final Rect IDENTITY_RECT = new Rect();
    private static boolean useLicenseTag = true;

    private FontBuilder(int code, boolean hasLayout, boolean useFlashType)
    {
        tag = new DefineFont(code);
        tag.hasLayout = hasLayout;
        glyphEntryMap = new IntMap(100); //Sorted by code point order
	    flashType = useFlashType;
    }

    /**
     * Build a DefineFont, DefineFont2 or DefineFont3 tag for a given FontFace.
     * 
     * Note that with this constructor, flashType is assumed to be false.
     * 
     * @param code Determines the version of DefineFont SWF tag to use.
     * @param fontFace The FontFace to build into a DefineFont tag.
     * @param alias The name used to bind a DefineFont tag to other SWF tags
     * (such as DefineEditText).
     */
    public FontBuilder(int code, FontFace fontFace, String alias)
    {
       this(code, true, false); 
       defaultFace = fontFace;
       init(alias);
    }

    /**
     * Build a DefineFont tag from a system font by family name.
     *
     * @param code Determines the version of DefineFont SWF tag to use.
     * @param manager A FontManager resolves the fontFamily and style to 
     * a FontFace.
     * @param alias The name used to bind a DefineFont tag to other SWF tags
     * (such as DefineEditText).
     * @param fontFamily The name of the font family.
     * @param style An integer describing the style variant of the FontFace, 
     * either plain, bold, italic, or bolditalic.
     * @param hasLayout Determines whether font layout metrics should be encoded.
     * @param flashType Determines whether FlashType advanced anti-aliasing
     * information should be included.
     */
    public FontBuilder(int code, FontManager manager, String alias,
            String fontFamily, int style, boolean hasLayout, boolean flashType)
    {
        this(code, hasLayout, flashType);

        if (manager == null)
            throw new NoFontManagerException();

        if (Trace.font)
            Trace.trace("Locating font using FontManager '" + manager.getClass().getName() + "'");

	    boolean useTwips = code != TagValues.stagDefineFont && code != TagValues.stagDefineFont2;
        FontFace fontFace = manager.getEntryFromSystem(fontFamily, style, useTwips);

        if (fontFace == null)
            throwFontNotFound(alias, fontFamily, style, null);

        if (Trace.font)
            Trace.trace("Initializing font '" + fontFamily + "' as '" + alias + "'");

        this.defaultFace = fontFace;

        init(alias);
    }

    /**
     * Load a font from a URL
     *
     * @param code
     * @param alias The name used to bind a DefineFont2 tag to a DefineEditText tag.
     * @param location remote url or a relative, local file path
     * @param style
     * @param hasLayout
     */
    public FontBuilder(int code, FontManager manager, String alias,
            URL location, int style, boolean hasLayout, boolean flashType)
    {
        this(code, hasLayout, flashType);

        if (manager == null)
            throw new NoFontManagerException();

        if (Trace.font)
            Trace.trace("Locating font using FontManager '" + manager.getClass().getName() + "'");

	    boolean useTwips = code != TagValues.stagDefineFont && code != TagValues.stagDefineFont2;
        FontFace fontFace = manager.getEntryFromLocation(location, style, useTwips);

        if (fontFace == null)
            throwFontNotFound(alias, null, style, location.toString());

        if (Trace.font)
            Trace.trace("Initializing font at '" + location.toString() + "' as '" + alias + "'");

        this.defaultFace = fontFace;

        init(alias);
    }

    private void init(String alias)
    {
        fontHeight = defaultFace.getPointSize();

        if (tag.code != Tag.stagDefineFont)
        {
            tag.fontName = alias;
            tag.bold = defaultFace.isBold();
            tag.italic = defaultFace.isItalic();

            if (tag.hasLayout)
            {
                tag.ascent = defaultFace.getAscent();
                tag.descent = defaultFace.getDescent();
                tag.leading = defaultFace.getLineGap();

                if (Trace.font)
                {
                    Trace.trace("\tBold: " + tag.bold);
                    Trace.trace("\tItalic: " + tag.italic);
                    Trace.trace("\tAscent: " + tag.ascent);
                    Trace.trace("\tDescent: " + tag.descent);
                    Trace.trace("\tLeading: " + tag.leading);
                }
            }
        }

        // If flashType enabled we must have z, Z, l, L
        if (flashType)
        {
            GlyphEntry adfGE = defaultFace.getGlyphEntry('z');
            if (adfGE == null)
                flashType = false;

            adfGE = defaultFace.getGlyphEntry('Z');
            if (adfGE == null)
                flashType = false;

            adfGE = defaultFace.getGlyphEntry('l');
            if (adfGE == null)
                flashType = false;

            adfGE = defaultFace.getGlyphEntry('L');
            if (adfGE == null)
                flashType = false;
        }

        if (flashType)
        {
            zoneRecordBuilder = ZoneRecordBuilder.createInstance();
            if (zoneRecordBuilder != null)
            {
                zoneRecordBuilder.setFontAlias(alias);
                zoneRecordBuilder.setFontBuilder(this);
                zoneRecordBuilder.setFontFace(defaultFace);
            }
        }

        addChar(' '); // Add at least a space char by default
    }

    /**
     * Creates a DefineFont tag
     */
    public DefineTag build()
    {
        int count = glyphEntryMap.size();

        if (Trace.font)
            Trace.trace("Building font '" + tag.fontName + "' with " + count + " characters.");

	    if (flashType)
	    {
		    tag.zones = new DefineFontAlignZones();
		    tag.zones.font = tag;
		    tag.zones.zoneTable = new ZoneRecord[count];
		    tag.zones.csmTableHint = 1;
	    }

        tag.glyphShapeTable = new Shape[count];

        if (tag.code != Tag.stagDefineFont)
        {
            tag.codeTable = new char[count];

            if (tag.hasLayout)
            {
                tag.advanceTable = new short[count];
                tag.boundsTable = new Rect[count];
            }
        }

        // Process each GlyphEntry
        Iterator it = glyphEntryMap.iterator();
        int i = 0;

	    // long flashTypeTime = 0;
        while (it.hasNext() && i < count)
        {
            GlyphEntry ge = (GlyphEntry)((Map.Entry)it.next()).getValue();

            if (flashType)
                tag.zones.zoneTable[i] = ge.zoneRecord;

            // Note: offsets to shape table entries calculated on encoding
            tag.glyphShapeTable[i] = ge.shape;

            // IMPORTANT! Update GlyphEntry Index
            ge.setIndex(i);

            // DEFINEFONT2/3 specific properties
            if (tag.code != Tag.stagDefineFont)
            {
                tag.codeTable[i] = ge.character; // unsigned code point

                // Layout information
                if (tag.hasLayout)
                {
                    tag.advanceTable[i] = (short)ge.advance; //advance in emScale
	                // The player doesn't need ge.bounds, so we ignore it. 
                    // We must still generate this value, however, for ADF use.
                    tag.boundsTable[i] = IDENTITY_RECT;
                }
                else
                {
                    if (Trace.font)
                        Trace.trace("Warning: font tag created without layout information.");
                }
            }

            i++;
        }

        if (tag.hasLayout)
        {
            tag.kerningTable = new KerningRecord[0];
        }

	    // FIXME: we should allow the user to set the language code
	    //tag.langCode = 1;

	    // if we have any license info, create a DefineFontName tag
	    if (useLicenseTag && ((getFSType() != null && ! getFSType().installable) || getCopyright() != null || getName() != null))
	    {
		    tag.license = new DefineFontName();
		    tag.license.font = tag;
		    tag.license.fontName = getName();
		    tag.license.copyright = getCopyright();
	    }

       return tag;
    }

    /**
     * Adds all supported characters from 0 to the highest glyph
     * contained in the default font face.
     */
    public void addAllChars()
    {
        addAllChars(defaultFace);
    }

    /**
     * Adds all supported characters from 0 to the highest glyph
     * contained in the given font face.
     *
     * @param face
     */
    public void addAllChars(FontFace face)
    {
        int min = face.getFirstChar();
        int count = face.getNumGlyphs();

        if (Trace.font)
            Trace.trace("\tAdding " + count + " chars, starting from " + min);

        addCharset(min, count);
    }

    /**
     * Adds supported characters in the specified range from the default
     * font face.
     *
     * @param fromChar
     * @param count
     */
    public void addCharset(int fromChar, int count)
    {
        addCharset(defaultFace, fromChar, count);
    }

    /**
     * Adds supported characters in the specified range from the given
     * font face.
     *
     * @param face
     * @param fromChar
     * @param count
     */
    public void addCharset(FontFace face, int fromChar, int count)
    {
        int remaining = count;

        for (int i = fromChar; remaining > 0 && i < Character.MAX_VALUE; i++)
        {
            char c = (char)i;
            GlyphEntry ge = addChar(face, c);
            if (ge != null)
            {
	            remaining--;
            }
        }
    }

	/**
	 * Adds all supported characters in the given array from the
	 * default font face.
	 *
	 * @param chars
	 */
	public void addCharset(char[] chars)
	{
	    addCharset(defaultFace, chars);
	}

    /**
     * Adds all supported characters in array from the given font face.
     *
     * @param face
     * @param chars
     */
    public void addCharset(FontFace face, char[] chars)
    {
        //TODO: Sort before adding to optimize IntMap addition
        for (int i = 0; i < chars.length; i++)
        {
            char c = chars[i];
            addChar(face, c);
        }
    }

    /**
     * If supported, includes a given character from the default font face.
     *
     * @param c
     */
    public void addChar(char c)
    {
        addChar(defaultFace, c);
    }

    /**
     * If supported, includes a character from the given font face.
     *
     * @param c
     */
    public GlyphEntry addChar(FontFace face, char c)
    {
        GlyphEntry ge = (GlyphEntry)glyphEntryMap.get(c);

        if (ge == null)
        {
            ge = face.getGlyphEntry(c);

            if (ge != null)
            {
                //Add to this tag's collection
                glyphEntryMap.put(c, ge);
            }
        }

        if (flashType && ge != null && ge.zoneRecord == null && zoneRecordBuilder != null)
        {
            ge.zoneRecord = zoneRecordBuilder.build(c);
        }

        return ge;
    }

	public String getCopyright()
	{
		return defaultFace.getCopyright();
	}

	public String getName()
	{
		return defaultFace.getFamily();
	}

	public FSType getFSType()
	{
		return defaultFace.getFSType();
	}

    public void setLangcode(int code)
    {
        if (code >= 0 && code < 6)
            tag.langCode = code;
    }

    public GlyphEntry getGlyph(char c)
    {
        return (GlyphEntry)glyphEntryMap.get(c);
    }

    public double getFontHeight()
    {
        return fontHeight;
    }

    public int size()
    {
        return glyphEntryMap.size();
    }

    private void throwFontNotFound(String alias, String fontFamily, int style, String location)
    {
        StringBuffer message = new StringBuffer("Font for alias '");
        message.append(alias).append("' ");
        if (style == FontFace.BOLD)
        {
            message.append("with bold weight ");
        }
        else if (style == FontFace.ITALIC)
        {
            message.append("with italic style ");
        }
        else if (style == (FontFace.BOLD + FontFace.ITALIC))
        {
            message.append("with bold weight and italic style ");
        }
        else
        {
            message.append("with plain weight and style ");
        }

        if (location != null)
        {
            message.append("was not found at: ").append(location.toString());
        }
        else
        {
            message.append("was not found by family name '").append(fontFamily).append("'");
        }
        throw new FontNotFoundException(message.toString());
    }

    public static final class FontNotFoundException extends RuntimeException
    {
        private static final long serialVersionUID = -2385779348825570473L;

        public FontNotFoundException(String message)
        {
            super(message);
        }
    }

    public static final class NoFontManagerException extends RuntimeException
    {
        private static final long serialVersionUID = 755054716704678420L;

        public NoFontManagerException()
        {
            super("No FontManager provided. Cannot build font.");
        }
    }

    public static final class SWFFontNotSupportedException extends RuntimeException
    {
        private static final long serialVersionUID = -7381079883711386211L;

        public SWFFontNotSupportedException(String message)
        {
            super(message);
        }
    }
}
