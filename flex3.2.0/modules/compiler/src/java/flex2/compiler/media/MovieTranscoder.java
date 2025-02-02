////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.media;

import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.common.PathResolver;
import flex2.compiler.util.ThreadLocalToolkit;
import flash.swf.*;
import flash.swf.types.ButtonCondAction;
import flash.swf.types.Rect;
import flash.swf.tags.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Transcodes SWFs into DefineTags for embedding
 *
 * @author Clement Wong
 */
public class MovieTranscoder extends AbstractTranscoder
{
    public MovieTranscoder()
    {
        super(new String[]{MimeMappings.FLASH}, null, false);
    }

    public static final String SYMBOL = "symbol";

    private Map dictionaryMap = new HashMap();
    private Map embedProps = new HashMap();

    public boolean isSupportedAttribute(String attr)
    {
        return SYMBOL.equals( attr ) || SCALE9TOP.equals( attr ) || SCALE9LEFT.equals( attr ) || SCALE9BOTTOM.equals( attr ) || SCALE9RIGHT.equals( attr )
                || FONTNAME.equals( attr ) || FONTFAMILY.equals( attr ) || FONTWEIGHT.equals( attr ) || FONTSTYLE.equals( attr ) ;
    }

    public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
        throws TranscoderException
    {
        TranscodingResults results = new TranscodingResults( resolveSource( context, args ));
        BufferedInputStream swfIn = null;

        try
        {
            swfIn = new BufferedInputStream(results.assetSource.getInputStream());

            String symbolName = (String) args.get(SYMBOL);
            String fontName = (String) args.get(FONTNAME);
            String fontFamily = (String) args.get(FONTFAMILY);
            String fontWeight = (String) args.get(FONTWEIGHT);
            String fontStyle = (String) args.get(FONTSTYLE);

            if (fontName == null)
            {
                fontName = fontFamily;
            }

            if (fontName != null)
            {
                if (symbolName != null)
                {
                    throw new IncompatibleTranscoderParameters( SYMBOL, FONTNAME );
                }
                extractDefineFont( results, swfIn, fontName,
                                   FontTranscoder.isBold( fontWeight ), FontTranscoder.isItalic( fontStyle ) );
                if (generateSource)
                    generateSource( results, className, args, embedProps );
            }
            else if (symbolName != null)
            {
                if (fontWeight != null)
                    throw new IncompatibleTranscoderParameters( SYMBOL, FONTWEIGHT );
                if (fontStyle != null)
                    throw new IncompatibleTranscoderParameters( SYMBOL, FONTSTYLE );

                extractDefineTag( results, swfIn, symbolName, args );

                if (generateSource)
                    generateSource( results, className, args, embedProps );
            }
            else
            {
                results.defineTag = new DefineSprite();

                if (generateSource)
                {
                    generateTopLevelLoaderSource( results, className, args, getSwfSize( swfIn ) );
                }
                else
                    throw new EmbedRequiresCodegen( results.assetSource.getName(), className );
            }

            if (args.containsKey(SCALE9LEFT) || args.containsKey(SCALE9RIGHT) || args.containsKey(SCALE9TOP) || args.containsKey(SCALE9BOTTOM))
            {
                if (!(results.defineTag instanceof DefineSprite)) // button as well, but we don't make those
                {
                    throw new BadScalingGridTarget( TagValues.names[results.defineTag.code] );
                }
                DefineSprite sprite = (DefineSprite) results.defineTag;

                // We are going to actually change this sprite, so we need to make a new one :-(

                DefineSprite spriteCopy = new DefineSprite(sprite);
                defineScalingGrid( spriteCopy, args );
                results.defineTag = spriteCopy;
            }

            return results;
        }
        catch (IOException ex)
        {
            throw new AbstractTranscoder.ExceptionWhileTranscoding( ex );
        }
        finally
        {
            if (swfIn != null)
            {
                try
                {
                    swfIn.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }

    private Rect getSwfSize( InputStream swfIn ) throws IOException
    {
        TagDecoder t = new TagDecoder(swfIn);
        HeaderSnarfer snarf = new HeaderSnarfer();
        t.parse(snarf);
        return snarf.swfHeader.size;
    }

    private Dictionary getDictionary( TranscodingResults results, InputStream swfIn ) throws IOException
    {
        Dictionary dict = (Dictionary)dictionaryMap.get(results.assetSource.getName());
        if (dict == null)
        {
            TagLocator locator = new TagLocator();
            new TagDecoder(swfIn).parse(locator);
            dict = locator.dict;
            dictionaryMap.put(results.assetSource.getName(), dict);
        }
        return dict;
    }

    public void extractDefineFont( TranscodingResults results, InputStream swfIn, String fontName, boolean bold, boolean italic )
            throws IOException, TranscoderException
    {
        Dictionary dict = getDictionary( results, swfIn );
        DefineFont font = dict.getFontFace( fontName, bold, italic );

        if (font != null)
        {
            results.defineTag = font;
        }
        else
        {
            throw new MissingFontFace( fontName, bold, italic );
        }
    }

    public void extractDefineTag( TranscodingResults results, InputStream swfIn, String symbolName, Map args )
                throws TranscoderException, IOException
    {
        Dictionary dict = getDictionary( results, swfIn );
        DefineTag definition = dict.getTag(symbolName);

        if (definition != null)
        {
            if (purge( definition ))
            {
                IgnoringAS2 ignoringAS2 = new IgnoringAS2(symbolName);
                String path = (String) args.get(Transcoder.FILE);
                String pathSep = (String) args.get(Transcoder.PATHSEP);
                if ("true".equals(pathSep))
                {
                    path = path.replace('/', '\\');
                }
                ignoringAS2.path = path;
                if (args.containsKey(Transcoder.LINE))
                {
                    int line = Integer.parseInt( (String) args.get(Transcoder.LINE) );
                    ignoringAS2.line = line; 
                }
                ThreadLocalToolkit.log(ignoringAS2);
            }

            results.defineTag = definition;

            // FIXME:
            // Temporary(?) workaround for issue where you can't actually associate
            // with StaticText because its impossible to "new" those.

            if (definition instanceof DefineText)
            {
                DefineSprite s = new DefineSprite();
                PlaceObject po = new PlaceObject( definition, 0 );
                s.tagList.tags.add( po );
                results.defineTag = s;
            }
        }
        else
        {
            throw new MissingSymbolDefinition( symbolName );
        }
    }

    public void generateTopLevelLoaderSource(TranscodingResults asset, String fullClassName, Map embedMap, Rect size )
            throws TranscoderException
    {
        String packageName = "";
        String className = fullClassName;
        int dot = fullClassName.lastIndexOf( '.' );
        if (dot != -1)
        {
            packageName = fullClassName.substring( 0, dot );
            className = fullClassName.substring( dot + 1 );
        }

        String resolvedSource = asset.assetSource.getName().replace('\\', '/');
        ThreadLocalToolkit.addResolvedPath(resolvedSource, asset.assetSource);

        StringBuffer source = new StringBuffer( 1024 );
        source.append( "package " );
        source.append( packageName );
        source.append( "\n{\nimport mx.core.MovieClipLoaderAsset;\n" );
        source.append( "import flash.utils.ByteArray;\n\n" );
        source.append( "public class " );
        source.append( className );
        source.append( " extends MovieClipLoaderAsset\n{\n" );
        source.append( "\tpublic function " );
        source.append( className );
        source.append( "()\n\t{\n\t\tsuper();\n" );
        source.append( "\t\tinitialWidth=");
        source.append( size.getWidth());
        source.append( "/20;\n\t\tinitialHeight=");
        source.append( size.getHeight());
        source.append( "/20;\n\t}\n");
        source.append( "\tprivate static var bytes:ByteArray = null;\n\n" );
        source.append( "\toverride public function get movieClipData():ByteArray\n\t{\n");
        source.append( "\t\tif (bytes == null)\n\t\t{\n\t\t\tbytes = ByteArray( new dataClass() );\n\t\t}\n" );
        source.append( "\t\treturn bytes;\n\t}\n\n");
        source.append( "\t[Embed(" + Transcoder.RESOLVED_SOURCE + "='" );
        source.append( resolvedSource );
        source.append( "', mimeType='application/octet-stream')]\n\tpublic var dataClass:Class;\n" );
        source.append( "}\n}\n");

        asset.generatedCode = source.toString();
    }

    public static void defineScalingGrid( DefineSprite sprite, Map args ) throws TranscoderException
    {
        if (args.get(SCALE9LEFT)==null || args.get(SCALE9RIGHT)==null || args.get(SCALE9TOP)==null || args.get(SCALE9BOTTOM)==null)
        {
            throw new ImageTranscoder.ScalingGridException();
        }

        if (sprite.scalingGrid != null)
        {
            ScalingGridAlreadyDefined scalingGridAlreadyDefined = new ScalingGridAlreadyDefined();
            String path = (String) args.get(Transcoder.FILE);
            String pathSep = (String) args.get(Transcoder.PATHSEP);
            if ("true".equals(pathSep))
            {
                path = path.replace('/', '\\');
            }
            scalingGridAlreadyDefined.path = path;
            if (args.containsKey(Transcoder.LINE))
            {
                int line = Integer.parseInt( (String) args.get(Transcoder.LINE) );
                scalingGridAlreadyDefined.line = line; 
            }
            ThreadLocalToolkit.log(scalingGridAlreadyDefined);
        }

        sprite.scalingGrid = new DefineScalingGrid();
        sprite.scalingGrid.scalingTarget = sprite;
        String left = (String) args.get( SCALE9LEFT );
        sprite.scalingGrid.rect.xMin = Integer.parseInt( left ) * 20;
        String right = (String) args.get( SCALE9RIGHT );
        sprite.scalingGrid.rect.xMax = Integer.parseInt( right ) * 20;
        String top = (String) args.get( SCALE9TOP );
        sprite.scalingGrid.rect.yMin = Integer.parseInt( top ) * 20;
        String bottom = (String) args.get( SCALE9BOTTOM );
        sprite.scalingGrid.rect.yMax = Integer.parseInt( bottom  ) * 20;

        if ((sprite.scalingGrid.rect.xMin < 0) || (sprite.scalingGrid.rect.xMax < 0) || (sprite.scalingGrid.rect.xMin >= sprite.scalingGrid.rect.xMax))
        {
            throw new ScalingGridRange();
        }
        if ((sprite.scalingGrid.rect.yMin < 0) || (sprite.scalingGrid.rect.yMax < 0) || (sprite.scalingGrid.rect.yMin >= sprite.scalingGrid.rect.yMax))
        {
            throw new ScalingGridRange();
        }


    }

    private boolean purge( DefineTag tag )
    {
        boolean ret = false;
        if (tag instanceof DefineSprite)
        {
            DefineSprite sprite = (DefineSprite) tag;
            if (sprite.initAction != null)
            {
                ret = true;
                sprite.initAction = null;
            }

            for (Iterator iter = sprite.tagList.tags.iterator(); iter.hasNext();)
            {
                Tag tag1 = (Tag)iter.next();
                if ((tag1 instanceof DoAction) || (tag1 instanceof DoInitAction))
                {
                    ret = true;
                    iter.remove();
                }
                if (tag1 instanceof PlaceObject)
                {
                    PlaceObject placeObject = (PlaceObject) tag1;
                    if (placeObject.hasClipAction())
                    {
                        placeObject.setClipActions( null );
                        ret = true;
                    }
                    if (placeObject.hasCharID())
                    {
                        ret |= purge( placeObject.ref );
                    }
                    if (placeObject.name != null)
                    {
                        embedProps.put(placeObject.name, ASSET_TYPE);
                    }
                }
            }
        }
        else if (tag instanceof DefineButton)
        {
            DefineButton button = (DefineButton) tag;
            if (button.condActions.length > 0)
            {
                ret = true;
                button.condActions = new ButtonCondAction[0];
            }
        }
        return ret;
    }

    public void clear()
    {
        super.clear();
        if (dictionaryMap.size() != 0)
        {
            dictionaryMap = new HashMap();
        }
        
        if (embedProps.size() != 0)
        {
        	embedProps = new HashMap();
        }
    }

    class RootSpriteBuilder extends TagHandler
     {
         DefineSprite root;
         boolean strip;


        RootSpriteBuilder(String name, boolean strip)
        {
            root = new DefineSprite();
            root.name = name;
            this.strip = strip;
        }

        DefineSprite getRoot()
        {
            return root;
        }

        //	pulled from 1.x flex.compiler.reflect.SwfDepVisitor
        public void any(Tag tag)
         {
             // FIXME: We don't seem to do anything with DefineTags?

             if (!(tag instanceof DefineTag)
                 && !(tag instanceof DoInitAction)
                 && !(tag instanceof ExportAssets))
             {
                 boolean keep = true;
                 // set hasActions for any action-related tag not just doactions (doinitactions, button handlers, etc)
                 if (tag instanceof DoAction)
                 {
                     if (strip)
                        keep = false;

                     // TODO reject AS < 3
                     //if (handleDoAction(((DoAction)tag),

                     DoAction doAction = (DoAction) tag;

                     for (int i = 0; i < doAction.actionList.size(); ++i)
                     {
                         Action action = doAction.actionList.getAction( i );
                         if (action.code != ActionConstants.sactionStop)
                         {
                             keep = false;
                             break;
                         }
                     }

                 }
                 else if (tag instanceof PlaceObject)
                 {
                    PlaceObject placeObject = (PlaceObject) tag;

                    if (placeObject.hasName())
                    {
                       placeObject.setName(null);
                    	//embedProps(placeObject.name, "DisplayObject");
                    }
                 }

                 if (keep)
                    root.tagList.tags.add(tag);


             }
         }
    }

    static class HeaderSnarfer extends TagHandler
    {
        public Header swfHeader;
        public void header(Header h)
        {
            swfHeader = h;
        }
    }

    static class TagLocator extends TagHandler
    {
        public Dictionary dict;

        public void setDecoderDictionary(Dictionary dict)
        {
            this.dict = dict;
        }
    }

    public static final class MissingSymbolDefinition extends TranscoderException
    {
        public MissingSymbolDefinition( String symbol )
        {
            this.symbol = symbol;
        }
        public String symbol;
    }
    public static final class MissingFontFace extends TranscoderException
    {
        public MissingFontFace( String fontName, boolean bold, boolean italic )
        {
            this.fontName = fontName;
            this.weight = (bold? "bold" : "normal");
            this.style = (italic? "italic" : "regular");
        }
        public String fontName;
        public String weight;
        public String style;
    }
    public static final class UnableToBuildRootSprite extends TranscoderException
    {

    }
    public static final class BadScalingGridTarget extends TranscoderException
    {
        public BadScalingGridTarget( String resourceType )
        {
            this.resourceType = resourceType;
        }
        public String resourceType;
    }
    public static final class ScalingGridRange extends TranscoderException
    {

    }
    public static final class IgnoringAS2 extends CompilerMessage.CompilerWarning implements ILocalizableMessage
    {
        public IgnoringAS2( String symbol )
        {
            this.symbol = symbol;
        }
        public String symbol;

    }
    public static final class ScalingGridAlreadyDefined extends CompilerMessage.CompilerWarning implements ILocalizableMessage
    {
        public ScalingGridAlreadyDefined()
        {

        }
    }
}

