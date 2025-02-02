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

package flex2.compiler.as3.genext;

import flash.util.Trace;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.binding.ClassInfo;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import macromedia.asc.parser.DefinitionNode;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.VariableDefinitionNode;
import macromedia.asc.util.Context;

import java.util.*;

public abstract class GenerativeClassInfo
{
    private String className;
    private Map accessorMap;
    private ClassInfo classInfo;
    protected Context context;
    protected SymbolTable symbolTable;

    public GenerativeClassInfo(Context context, SymbolTable symbolTable)
    {
        this.context = context;
        this.symbolTable = symbolTable;
    }

	/**
	 *
	 */
	public abstract boolean needsAdditionalInterfaces();

	/**
	 *
	 */
    public void addAccessorVariable(VariableDefinitionNode node, boolean explicit)
	{
		if (accessorMap == null)
		{
			accessorMap = new LinkedHashMap();
		}

		QName qName = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getVariableName(node));

        VariableInfo variableInfo = new VariableInfo(qName, node, explicit);

        if ((node.attrs != null) && node.attrs.hasAttribute(NodeMagic.STATIC))
        {
            variableInfo.isStatic = true;
        }        

		accessorMap.put(qName, variableInfo);
	}

	/**
	 *
	 */
    public void addAccessorFunction(FunctionDefinitionNode node, boolean explicit, boolean isGetter)
	{
		if (accessorMap == null)
		{
			accessorMap = new HashMap();
		}

		QName qName = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getFunctionName(node));

		AccessorInfo info = (AccessorInfo)accessorMap.get(qName);

		//	Note: second condition indicates error (variable and getter/setter with same name), but here we just
		//	plow ahead - error will be caught and reported later by ASC
		if (info == null || !(info instanceof GetterSetterInfo))
		{
			info = new GetterSetterInfo(qName, node, explicit);
			accessorMap.put(qName, info);
		}

		if (isGetter)
		{
			((GetterSetterInfo)info).setGetterInfo(node);
		}
		else
		{
			((GetterSetterInfo)info).setSetterInfo(node);
		}
    }

    /**
     *
     */
    public void removeAccessor(QName qName)
    {
        if (accessorMap != null)
        {
            accessorMap.remove(qName);
        }
    }
    
	/**
	 *
	 */
	public abstract void removeOriginalMetaData(DefinitionNode definitionNode);

    public boolean hasAccessor(String id)
    {
    	return hasAccessor(new QName(id));
    }


    /**
	 *
	 */
	public boolean hasAccessor(QName qName)
	{
		return ((accessorMap != null) && accessorMap.containsKey(qName));
	}

    public AccessorInfo getAccessor(String id)
    {
    	return getAccessor(new QName(id));
    }
    
	/**
	 *
	 */
	public AccessorInfo getAccessor(QName qName)
	{
        AccessorInfo result = null;

        if (accessorMap != null)
        {
            result = (AccessorInfo) accessorMap.get(qName);
        }

        return result;
	}

	/**
	 *
	 */
	public Map getAccessors()
	{
		return accessorMap;
	}

	/**
	 *
	 */
	public ClassInfo getClassInfo()
	{
		return classInfo;
	}

	/**
	 *
	 */
	public String getClassName()
	{
		return className;
	}

	/**
	 *
	 */
    public Set getImports()
    {
        Set imports = new HashSet();

        if (accessorMap != null)
        {
            Iterator iterator = accessorMap.values().iterator();

            while ( iterator.hasNext() )
            {
                AccessorInfo accessorInfo = (AccessorInfo) iterator.next();

                String typeName = accessorInfo.getTypeName();

                MultiName multiName = classInfo.getMultiName(typeName);
                String[] namespace = multiName.getNamespace();
                String localPart = multiName.getLocalPart();
                int i = 0;
                QName qName = null;

                // Ignore ambiguity checks here, because they are checked by API.resolveMultiName().
                while (i < namespace.length)
                {
                    if (symbolTable.findSourceByQName(namespace[i], localPart) != null)
                    {
                        qName = multiName.getQName(i);
                        break;
                    }
                    i++;
                }

                if ((qName != null) && !qName.getLocalPart().equals(SymbolTable.NOTYPE))
                {
                    imports.add( toString(qName) );
                }
                else
                {
                    if (Trace.binding)
                    {
                        Trace.trace("GenerativeClassInfo.getImports: " + multiName + " not resolved");
                    }
                }
            }
        }

        return imports;
    }

	/**
	 *
	 */
	public void setClassInfo(ClassInfo classInfo)
	{
		this.classInfo = classInfo;
	}

	/**
	 *
	 */
	public void setClassName(String className)
	{
		this.className = className;
	}

	/**
	 *
	 */
	public abstract class AccessorInfo
	{
		private QName qName;
		private String backingPrefix;
        protected String typeName;
		private ArrayList attributes;
        private boolean isExplicit;
        public boolean isStatic;
		private DefinitionNode definitionNode;

		public AccessorInfo(QName qName,
							String typeName,
							DefinitionNode definitionNode,
							boolean isExplicit)
		{
			this.qName = qName;
			this.backingPrefix = mangledPrefix(qName.getLocalPart());
			this.typeName = typeName;
			this.definitionNode = definitionNode;
			this.isExplicit = isExplicit;

			List attributes = NodeMagic.getAttributes(definitionNode);

			if (attributes instanceof ArrayList)
            {
            	this.attributes = (ArrayList)attributes;
            }
            else
            {
            	this.attributes = new ArrayList(attributes);
            }
        }

		public String getPropertyName() { return qName.getLocalPart(); }
		public String getUserNamespace() { return qName.getNamespace(); }
		public String getBackingPrefix() { return backingPrefix; }
		public String getBackingPropertyName() { return backingPrefix + qName.getLocalPart(); }
		public String getTypeName() { return typeName; }
		public List getAttributes() { return attributes; }

		public String getAttributeString()
		{
			String result = "";
			if (attributes != null)
			{
				Iterator iter = attributes.iterator();
				while (iter.hasNext())
				{
					result += iter.next() + " ";
				}
				result = result.trim();
			}
			return result;
		}
		
        public boolean getIsExplicit()
        {
            return isExplicit;
        }

        public boolean getIsStatic()
        {
            return isStatic;
        }

		public abstract boolean getIsFunction();

		protected String getQualifier() { return getUserNamespace().length() > 0 ? getUserNamespace() + "::" : ""; }
		public String getQualifiedPropertyName() { return getQualifier() + getPropertyName(); }
		public String getQualifiedBackingPropertyName() { return getQualifier() + getBackingPropertyName(); }

		public DefinitionNode getDefinitionNode()
		{
			return definitionNode;
		}
	}

	/**
	 *
	 */
	public class VariableInfo extends AccessorInfo
	{
		private int position;
		private List metaData;

		public VariableInfo(QName qName, VariableDefinitionNode node, boolean isExplicit)
		{
			super(qName, NodeMagic.getVariableTypeName(node), node, isExplicit);
			this.position = node.pos();
			this.metaData = NodeMagic.getMetaData(node);
		}

		public int getPosition() { return position; }
		public List getMetaData() { return metaData; }

		public boolean getIsFunction() { return false; }
	}

	/**
	 *
	 */
	public class GetterSetterInfo extends AccessorInfo
	{
		private int getterPos, setterPos;
		private List getterMetaData, setterMetaData;

		public GetterSetterInfo(QName qName, FunctionDefinitionNode node, boolean isExplicit)
		{
			//	NOTE: typeName is null until we recieve getter- or setter- specific info
			super(qName, null, node, isExplicit);

			//	null until we recieve getter- or setter- specific info
			getterPos = setterPos = -1;
			getterMetaData = setterMetaData = null;
		}

		public void setGetterInfo(FunctionDefinitionNode node)
		{
			assert NodeMagic.functionIsGetter(node);

			typeName = NodeMagic.getFunctionTypeName(node);
			getterPos = node.pos();
			getterMetaData = NodeMagic.getMetaData(node);
		}

		public void setSetterInfo(FunctionDefinitionNode node)
		{
			assert NodeMagic.functionIsSetter(node);

			typeName = NodeMagic.getFunctionParamTypeName(node, 0);
			setterPos = node.pos();
			setterMetaData = NodeMagic.getMetaData(node);
		}

		public int getGetterPosition() { return getterPos; }
		public int getSetterPosition() { return setterPos; }

		public List getGetterMetaData() { return getterMetaData != null ? getterMetaData : Collections.EMPTY_LIST; }
		public List getSetterMetaData() { return setterMetaData != null ? setterMetaData : Collections.EMPTY_LIST; }

		public boolean getIsFunction() { return true; }
	}

	/**
	 * generate an improbable prefix to use when mangling backing property names
	 */
	private static String mangledPrefix(String s)
	{
		return "_" + Math.abs(s.hashCode());
	}

	/**
	 * used by templates to create mangled names on the fly e.g. in ManagedProperty for uid
	 */
	public static String mangledName(String s)
	{
		return mangledPrefix(s) + s;
	}

    private String toString(QName qName)
    {
        String result;
        String namespace = qName.getNamespace();

        if ((namespace != null) && (namespace.length() > 0))
        {
            result = namespace + "." + qName.getLocalPart();
        }
        else
        {
            result = qName.getLocalPart();
        }

        return result;
    }
}
