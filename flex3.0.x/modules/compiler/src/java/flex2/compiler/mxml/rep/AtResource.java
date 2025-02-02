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

package flex2.compiler.mxml.rep;

import flex2.compiler.Source;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.abc.MetaData;
import flex2.compiler.as3.MetaDataParser;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.NameFormatter;

/**
 *
 */
public class AtResource implements LineNumberMapped
{
    private String bundle; 			// e.g., "MyResources"
    private String key;    			// e.g., "OPEN"
    private String methodName;		// e.g., "getString"
    private int lineNumber;

    public AtResource(String bundle, String key, String methodName, int lineNumber)
    {
        this.bundle = bundle;
        this.key = key;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public String getBundle()
    {
        return bundle;
    }

    /**
     * Returns a string like 
     * ResourceManager.getInstance().getString("MyResources", "OPEN")
     * used as a property value
     * in the propertiesFactory of a ComponentDescriptor
     * when the developers used an @Resource() directive
     * for the MXML attribute value.
     */
    public String getValueExpression()
    {
        return "ResourceManager.getInstance()." + methodName + "(\"" + bundle + "\", \"" + key + "\")";
    }

    public int getXmlLineNumber()
    {
        return lineNumber;
    }

    public void setXmlLineNumber(int xmlLineNumber)
    {
        this.lineNumber = xmlLineNumber;
    }

    /**
     * Creates a new AtResource instance.
     * @param value The text of an @Resource() directive,
     * such as "@Resource(bundle='MyResources', key='OPEN')"
     * @param type Specifies the type (e.g., a String or an int)
     * for the MXML attribute
     */
    public static AtResource create(TypeTable typeTable, Source sourceFile, int beginLine, String value, Type type)
    {
        String methodName = null;
        if (type.isAssignableTo(typeTable.stringType))
        {
        	methodName = "getString";
        }
        else if (type.isAssignableTo(typeTable.booleanType))
        {
        	methodName = "getBoolean";
        }
        else if (type.isAssignableTo(typeTable.numberType))
        {
        	methodName = "getNumber";
        }
        else if (type.isAssignableTo(typeTable.intType))
        {
        	methodName = "getInt";
        }
        else if (type.isAssignableTo(typeTable.uintType))
        {
           	methodName = "getUint";
        }
        else if (type.isAssignableTo(typeTable.classType))
        {
           	methodName = "getClass";
        }
        else if (type.isAssignableTo(typeTable.arrayType))
        {
           	methodName = "getStringArray";
        }
        else
        {
           	methodName = "getObject";
        }

        // If we strip off the @ at the beginning of the directive,
        // we're left with a string like "Resource(bundle='MyResources', key='OPEN')"
        // that has the same syntax as a metadata body inside [...].
        // So we can parse it with MetaDataParser.
        MetaData metaData = MetaDataParser.parse(typeTable.getPerCompileData(), sourceFile, beginLine, value.substring(1));
        if (metaData == null)
        {
        	return null;
        }
        else if (metaData.count() == 0)
        {
			ThreadLocalToolkit.log(new NoResourceParams(), sourceFile.getNameForReporting(), beginLine);
			return null;
        }

        String key = metaData.getValue("key");
        
        // Support the syntax @Resource('OPEN')
        // where only the key is specified.        
        if (key == null)
        {
         	if ((metaData.getKey(0) == null) && (metaData.count() == 1))
            {
                key = metaData.getValue(0);
            }
	        else
            {
				ThreadLocalToolkit.log(new NoResourceKeyParam(), sourceFile.getNameForReporting(), beginLine);
				return null;
            }
        }
        String bundle = metaData.getValue("bundle");
		
        // If the bundle name isn't specified, use the class name.
        if (bundle == null)
		{
			if (metaData.count() > 1)
			{
				ThreadLocalToolkit.log(new NoResourceBundleParam(), sourceFile.getNameForReporting(), beginLine);
				return null;
			}
			bundle = NameFormatter.qNameFromSource(sourceFile).toString();
		}

        return new AtResource(bundle, key, methodName, beginLine);
    }

	/**
	 * CompilerMessages
	 */
	public static class NoResourceParams extends CompilerMessage.CompilerError {}
	public static class NoResourceKeyParam extends CompilerMessage.CompilerError {}
	public static class NoResourceBundleParam extends CompilerMessage.CompilerError {}
}
