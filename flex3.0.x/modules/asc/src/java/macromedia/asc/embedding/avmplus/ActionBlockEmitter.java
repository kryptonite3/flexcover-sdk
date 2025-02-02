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

/*
 * Written by Jeff Dyer
 * Copyright (c) 1998-2003 Mountain View Compiler Company
 * All rights reserved.
 */

package macromedia.asc.embedding.avmplus;

import macromedia.abc.BytecodeBuffer;
import macromedia.asc.parser.LiteralNumberNode;
import macromedia.asc.parser.MetaDataEvaluator;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.*;
import macromedia.asc.util.ByteList;
import macromedia.asc.util.Context;
import macromedia.asc.util.IntList;
import macromedia.asc.util.Names;
import macromedia.asc.util.Namespaces;
import macromedia.asc.util.ObjectList;
import macromedia.asc.util.Qualifiers;
import macromedia.asc.util.Slots;
import macromedia.asc.util.IntegerPool;

import java.io.PrintWriter;
import java.util.*;

import static macromedia.asc.embedding.avmplus.RuntimeConstants.*;
import static macromedia.asc.embedding.avmplus.ActionBlockConstants.*;
import static macromedia.asc.embedding.avmplus.ByteCodeFactory.*;
import static macromedia.asc.parser.Tokens.*;

/**
 * ActionBlockEmitter
 * 
 * @author Jeff Dyer
 */
public class ActionBlockEmitter extends Emitter
{
    public static final int MAJOR = 46;
    public static final int MINOR = 16;

    class ActionBlock
    {
        public int minor_version;
        public int major_version;
        public int vars_count;
        public ObjectList<ByteList> vars;
        public int methods_count;
        public ObjectList<ByteList> methods;
        public int metadata_count;
        public ObjectList<ByteList> metadata;
        public int bodies_count;
        public ObjectList<ByteList> bodies;
        public int dispids_count;
        public ObjectList<ByteList> dispids;
        public int classes_count;
        public ObjectList<ByteList> classes;
        public ObjectList<ByteList> instances;
        public int interfaces_count;
        public ObjectList<ByteList> interfaces;
        public int scripts_count;
        public ObjectList<ByteList> scripts;
        public int main_index;

        private Map<ByteList, Integer> bodies_map= new HashMap<ByteList, Integer>();

        private Map<ByteList, Integer> constant_utf8_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_utf8_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_mn_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_mn_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_nss_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_nss_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_ns_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_ns_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_double_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_double_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_uint_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_uint_pool = new ObjectList<ByteList>();

        private Map<ByteList, Integer> constant_int_map= new HashMap<ByteList, Integer>();
        public ObjectList<ByteList> constant_int_pool = new ObjectList<ByteList>();
        
        /*
        class ByteListComparator implements Comparator
        {
            public int compare(Object a, Object b)
            {
                if ((a instanceof ByteList) && (b instanceof ByteList))
                    return ByteList.compare((ByteList)a,(ByteList)b);
                return -1;
            }
        }
        private Map<ByteList, Integer> constant_map= new TreeMap(new ByteListComparator());
        private Map<ByteList, Integer> bodies_map= new TreeMap(new ByteListComparator());
        */


        // ISSUE: the following fields are used as temporaries while compiling a method. It seems like
        // these could be expressed more clearly.

        public int exception_count;
        public ByteList exception_table;  // not currently being used
        public ByteList code;

        public boolean show_bytecode;

        public ActionBlock(boolean show_bytecode)
        {
            minor_version = MINOR;
            major_version = MAJOR;
            vars_count = 0;
            vars = new ObjectList<ByteList>();
            methods_count = 0;
            methods = new ObjectList<ByteList>();
            metadata_count = 0;
            metadata = new ObjectList<ByteList>();
            bodies_count = 0;
            bodies = new ObjectList<ByteList>();
            dispids_count = 0;
            dispids = new ObjectList<ByteList>();
            classes_count = 0;
            instances = new ObjectList<ByteList>();
            classes = new ObjectList<ByteList>();
            interfaces_count = 0;
            interfaces = new ObjectList<ByteList>();
            scripts_count = 0;
            scripts = new ObjectList<ByteList>();

            exception_table = allocBytes();
            exception_count = 0;
            code = null;
            this.show_bytecode = show_bytecode;
        }

        // Add a body entry if the same set of bits does not already
        // reside in it. If they do, then just return the index of the matching
        // entry.

        public int addBody(ByteList bytes)
        {
            Integer mapcheck = bodies_map.get(bytes);

            if (mapcheck != null)
            {
                return mapcheck.intValue() + 1;
            }

            // Add it to map for fast lookups
            bodies_map.put(bytes, bodies.size());

            // If not, then add it.

            bodies.push_back(bytes);
            int index = bodies.size();

//        if( show_bytecode ) printf( " -> %d",index);
//        if( show_bytecode ) defns_out << " -> " << index;
            return index;
        }

        public int addUtf8Constant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_utf8_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_utf8_map.put(bytes, constant_utf8_pool.size());

            // If not, then add it.

            constant_utf8_pool.add(bytes);
            int index = constant_utf8_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addMultiNameConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_mn_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_mn_map.put(bytes, constant_mn_pool.size());

            // If not, then add it.

            constant_mn_pool.add(bytes);
            int index = constant_mn_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addNsSetConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_nss_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_nss_map.put(bytes, constant_nss_pool.size());

            // If not, then add it.

            constant_nss_pool.add(bytes);
            int index = constant_nss_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addNsConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_ns_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_ns_map.put(bytes, constant_ns_pool.size());

            // If not, then add it.

            constant_ns_pool.add(bytes);
            int index = constant_ns_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addDoubleConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_double_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_double_map.put(bytes, constant_double_pool.size());

            // If not, then add it.

            constant_double_pool.add(bytes);
            int index = constant_double_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addUIntConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_uint_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_uint_map.put(bytes, constant_uint_pool.size());

            // If not, then add it.

            constant_uint_pool.add(bytes);
            int index = constant_uint_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }
        public int addIntConstant(ByteList bytes)
        {
            // See if this constant is already in the pool.
            Integer mapcheck = constant_int_map.get(bytes);

            if (mapcheck != null)
            {
                int constant_index = mapcheck.intValue();
                return constant_index + 1;
            }

            // Add it to map for fast lookups
            constant_int_map.put(bytes, constant_int_pool.size());

            // If not, then add it.

            constant_int_pool.add(bytes);
            int index = constant_int_pool.size();
//        if( show_bytecode ) printf( " . %d",index);
            if (show_bytecode)
            {
                bytecodeFactory.cpool_out.write(" -> " + index);
            }
            return index;

        }

        public int addBytesToTable(ObjectList<ByteList> table, ByteList bytes)
        {
            // See if this bytes is already in the table.

            int idx = 0;
            for (ByteList list : table)
            {
                int n = list.size();
                if (n == bytes.size())
                {
                    int i = 0;
                    for (; i < n; i++)
                    {
                        if (list.get(i) != bytes.get(i))
                        {
                            break;
                        }
                    }
                    if (i == n)
                    {
                        // if( show_bytecode ) code_out.write(" -> " + index;
                        return (idx + 1);
                    }
                }
                idx++;
            }

            // If not, then add it.

            table.add(bytes);
            int index = table.size();
            return index;
        }
    }

    private ActionBlock ab;  // the current action block
    private Context cx;

    private ByteCodeFactory bytecodeFactory;

    protected int max_locals;
    protected int cur_locals;
    protected int max_stack;
    protected int cur_stack;
    protected int max_scope;
    protected int cur_scope;
    protected int max_params;
    protected String scriptname;
    protected String modulename;

    class ExceptionBlock
    {
        public int try_start  = 0;                   // start of try block
        public int try_end    = 0;                   // end of try block
        public IntList fixups = new IntList();       // patch with jump to end of catch clauses
        public IntList finallyAddrs = new IntList(); // patch with jump to post-finally logic
        public int scopeIndex = -1;                  // scope index of try block
        public boolean hasFinally = false;           // set if try block has a finally
        public int cur_locals = 0;
        public int loop_index = -1;
    };
    protected ObjectList<ExceptionBlock> exceptionBlocks = new ObjectList<ExceptionBlock>();

    protected IntList if_addrs = new IntList();
    protected IntList try_addrs = new IntList();
    protected IntList else_addrs = new IntList();
    protected IntList loopbegin_addrs = new IntList();
    protected ObjectList<IntList> break_addrs = new ObjectList<IntList>();
    protected IntList break_scope_depth = new IntList();
    protected IntList break_temp_count = new IntList();
    protected ObjectList<IntList> continue_addrs = new ObjectList<IntList>();
    protected IntList continue_scope_depth = new IntList();
    protected IntList switchbegin_addrs = new IntList();
    protected ObjectList<IntList> case_addrs = new ObjectList<IntList>();
    protected IntList default_addrs = new IntList();
    protected IntList seen_default_case = new IntList();
    private boolean sets_dxns;

    int addClassName(QName name)
    {
        if (name == null || name.toString().equals("*"))
        {
            return 0;
        }
        String classname_string = name.name;

        int namespace_index = addNamespace(name.ns);

        int name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(classname_string));
        int qname_index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace_index,false));

        return qname_index;
    }

    private Map<ObjectValue, Integer> nsConstants = new HashMap<ObjectValue, Integer>();

    int addNamespace(ObjectValue ns)
    {
        if( ns == cx.anyNamespace() )
            return 0;
        else
        {
            if( nsConstants.containsKey(ns) )
            {
                return nsConstants.get(ns);
            }

            int namespace_name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(ns.name));
            int namespace_index;
            if ( ns.isPackage() )
            {
                namespace_index = ab.addNsConstant(bytecodeFactory.ConstantPackageNamespace(namespace_name_index));
            }
            else
            {
                switch( ns.getNamespaceKind() )
                {
                    case Context.NS_INTERNAL:
                        namespace_index = ab.addNsConstant(bytecodeFactory.ConstantInternalNamespace(namespace_name_index));
                        break;
                    case Context.NS_PRIVATE:
                        namespace_index = ab.addNsConstant(bytecodeFactory.ConstantPrivateNamespace(namespace_name_index));
                        break;
                    case Context.NS_PROTECTED:
                        namespace_index = ab.addNsConstant(bytecodeFactory.ConstantProtectedNamespace(namespace_name_index));
                        break;
                    case Context.NS_STATIC_PROTECTED:
                        namespace_index = ab.addNsConstant(bytecodeFactory.ConstantStaticProtectedNamespace(namespace_name_index));
                        break;
                    default:
                        namespace_index = ab.addNsConstant(bytecodeFactory.ConstantNamespace(namespace_name_index));
                        break;
                }
            }
            nsConstants.put(ns, namespace_index);
            return namespace_index;
        }
    }

    int makeNamespaceSet( ObjectList<ObjectValue> namespaces )
    {
        Set<Integer> namespace_set = new TreeSet<Integer>();
        for( ObjectValue ns : namespaces )
        {
            if( ns != null )
            {
                int ns_index = addNamespace(ns);
                namespace_set.add(IntegerPool.getNumber(ns_index));
            }
            else
            {
                cx.internalError("internal error: non object value for namespace");
            }
        }
        int ns_set_index = ab.addNsSetConstant(bytecodeFactory.ConstantNamespaceSet(namespace_set));
        return ns_set_index;
    }

    int makeMultiname(String name, ObjectList<ObjectValue> namespaces)
    {
        int name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        int namespaces_set = makeNamespaceSet(namespaces);
        int mname_index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,false));

        return mname_index;
    }

    protected void StartClass(final String filename)
    {
        if (show_instructions)
        {
            code_out.println();
            code_out.print("// ++StartClass " + filename);
        }
        showLineNumber();
    }

    protected void FinishClass(Context cx, final QName name, final QName basename, boolean is_dynamic, boolean is_final, boolean is_interface, boolean is_nullable)
    {
        ObjectList<ByteList> static_traits = new ObjectList<ByteList>();
        ObjectList<ByteList> instance_traits = new ObjectList<ByteList>();
        IntList interfaces = new IntList();
        int scope_depth  = cx.getScopes().size();
        
        ObjectValue protected_namespace = null;

        // Iterate through the members of the class and define the traits

        if( !is_interface )
        {
            // Class traits

            ObjectValue obj = cx.scope(scope_depth-2);
            Builder     bui = obj.builder;

            protected_namespace = ((ClassBuilder)bui).protected_namespace;

            FinishTraits(obj, static_traits);
        }


        {
            // Instance vars

            ObjectValue obj = cx.scope(scope_depth-1);
            InstanceBuilder bui = obj.builder instanceof InstanceBuilder ? (InstanceBuilder) obj.builder : null;

            // interfaces can implement other interfaces, must express this in abc
            {
                for (ReferenceValue ref : bui.interface_refs)
                {
                    interfaces.add(makeMultiname(ref.name,ref.getImmutableNamespaces()));
                }
            }

            FinishTraits(obj, instance_traits);            
        }

        // Make a multiname

        int qname_index = addClassName(name);
        int base_index = basename == null ? 0 : addClassName(basename);
        int iinit_info = 0;

        //if( !is_interface )
        {
            ObjectValue obj = cx.scope();
            int slot_id = obj.getSlotIndex(cx,GET_TOKEN,"$construct",cx.publicNamespace());
            slot_id = obj.getImplicitIndex(cx,slot_id,EMPTY_TOKEN);
            Slot slot = obj.getSlot(cx,slot_id);
            iinit_info = GetMethodInfo(slot.getMethodName());
        }
        /*else
        {
            iinit_info = GetMethodInfo(name+"$iinit");
        }*/

        int class_info = GetClassInfo(name);

        int flags = 0;
        if (!is_dynamic)
            flags |= CLASS_FLAG_sealed;
        if (is_final)
            flags |= CLASS_FLAG_final;
        if (is_interface)
            flags |= CLASS_FLAG_interface;
        if (!is_nullable)
            flags |= CLASS_FLAG_non_nullable;

        int protected_index = (protected_namespace != null) ? addNamespace(protected_namespace) : 0;
        if (protected_index != 0)
            flags |= CLASS_FLAG_protected;
        
        ab.addBytesToTable(ab.instances,bytecodeFactory.InstanceInfo(ab.instances.at(class_info),qname_index,base_index,flags,protected_index,
                           interfaces.size(),interfaces,
                           iinit_info /*GetMethodInfo(name+"$"+name)*/,instance_traits,class_info));

        int cinit_info = GetMethodInfo(name+"$cinit");
        ab.addBytesToTable(ab.classes,bytecodeFactory.ClassInfo(ab.classes.at(class_info),
                                                cinit_info,static_traits,class_info));

        {
            String constName = name.toString().replace('.','_');
            constName = constName.replace('/','_');
            constName = constName.replace(':','_');
            constName = constName.replace('|','_');
            constName = constName.replace('$','_');

            header_out.println("const int abcclass_" + constName + " = " + class_info + ";");
            if (class_info >= native_class_count)
                native_class_count = class_info+1;
        }

        if( show_instructions )
        {
            code_out.println();
            code_out.print("// --FinishClass " + name + " " + (basename!=null?basename.toString():""));
        }
        showLineNumber();
    }

    protected void StartProgram(final String name)
    {
        this.modulename = name;

        if (show_instructions)
        {
            code_out.println();
            code_out.print("// ++StartProgram " + modulename);
        }
    }

    public ByteList emit(ByteList bytes)
    {

        return bytecodeFactory.ActionBlock(bytes,
            ab.minor_version,
            ab.major_version,
            (ab.constant_int_pool.size() == 0 ? 0 : ab.constant_int_pool.size() + 1),
            ab.constant_int_pool,
            (ab.constant_uint_pool.size() == 0 ? 0 : ab.constant_uint_pool.size() + 1),
            ab.constant_uint_pool,
            (ab.constant_double_pool.size() == 0 ? 0 : ab.constant_double_pool.size() + 1),
            ab.constant_double_pool,
            (ab.constant_utf8_pool.size() == 0 ? 0 : ab.constant_utf8_pool.size() + 1),
            ab.constant_utf8_pool,
            (ab.constant_mn_pool.size() == 0 ? 0 : ab.constant_mn_pool.size() + 1),
            ab.constant_mn_pool,
            (ab.constant_nss_pool.size() == 0 ? 0 : ab.constant_nss_pool.size() + 1),
            ab.constant_nss_pool,
            (ab.constant_ns_pool.size() == 0 ? 0 : ab.constant_ns_pool.size() + 1),
            ab.constant_ns_pool,
            (ab.methods.size() == 0 ? 0 : ab.methods.size()),
            ab.methods,
            (ab.metadata.size() == 0 ? 0 : ab.metadata.size()),
            ab.metadata,
            (ab.classes.size() == 0 ? 0 : ab.classes.size()),
            ab.instances,
            ab.classes,
            (ab.scripts.size() == 0 ? 0 : ab.scripts.size()),
            ab.scripts,
            (ab.bodies.size() == 0 ? 0 : ab.bodies.size()),
            ab.bodies);

    }
    
    int getValueIndex(ObjectValue objValue)
    {
        int value_index = 0;
        String value = objValue.toString();
        TypeValue defaultValueType = objValue.type != null ? objValue.type.getTypeValue() : null;
        // TODO: this should probably deal with non-nullable object types
        if (defaultValueType == cx.booleanType())
        {
            // The index doesn't matter, as long as its non 0
            // there are no boolean values in any cpool, instead the value will be determined by the kind byte
            value_index = value.equals("true")?CONSTANT_True:CONSTANT_False;
        }
        else if (defaultValueType == cx.stringType())
        {
            value_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(value));
        }
        else if (defaultValueType == cx.intType())
        {
            TypeValue[] type = new TypeValue[1];
            value_index = ab.addIntConstant(bytecodeFactory.ConstantIntegerInfo((int)getValueOfNumberLiteral(value,type)));
        }
        else if (defaultValueType == cx.numberType())
        {
            TypeValue[] type = new TypeValue[1];
            value_index = ab.addDoubleConstant(bytecodeFactory.ConstantDoubleInfo(getValueOfNumberLiteral(value,type)));
        }
        else if (defaultValueType == cx.nullType())
        {
            // Similar to boolean above, the value will be determined by the kind byte (there is only 1 possible value for null)
            value_index = CONSTANT_Null;
        }
        else if (defaultValueType == cx.voidType())
        {
            value_index = 0; // will be undefined at runtime
        }
        else if ( cx.isNamespace(objValue) )
        {
            value_index = addNamespace(objValue);
        }
        return value_index;
    }

    byte getValueKind(ObjectValue objValue)
    {
        byte value_kind = 0;
        String value = objValue.toString();
        TypeValue defaultValueType = objValue.type != null ? objValue.type.getTypeValue() : null;
        if (defaultValueType == cx.booleanType())
        {
            value_kind = value.equals("true")?CONSTANT_True:CONSTANT_False;
        }
        else if (defaultValueType == cx.stringType())
        {
            value_kind = CONSTANT_Utf8;
        }
        else if (defaultValueType == cx.intType())
        {
            value_kind = CONSTANT_Integer;
        }
        else if (defaultValueType == cx.numberType())
        {
            value_kind = CONSTANT_Double;
        }
        else if (defaultValueType == cx.nullType())
        {
            value_kind = CONSTANT_Null;
        }
        else if (defaultValueType == cx.voidType())
        {
            value_kind = CONSTANT_Null; // will be undefined at runtime
        }
        else if ( cx.isNamespace(objValue) )
        {
            value_kind = CONSTANT_Namespace;
        }
        return value_kind;
    }

    protected void DefineSlotVariable(Context cx, String name, String func_name, int pos, TypeInfo type, int slotIndex)
    {
        if (emit_debug_info)
        {
            DebugSlot(name, slotIndex, debug_info.debug_linenum);
        }
    }

    protected void StartMethod()
    {
        StartMethod("", 0, 0, 0, false, 0, null);
    }

    protected void StartMethod(final String name, int param_count, int local_count, int temp_count, boolean needs_activation, int needs_arguments, String debug_name)
    {
        if (show_instructions)
        {
            code_out.println();
            // code_out.print("// ++StartMethod " + name + ", " + (param_count+1) + ", " + local_count + ", " + temp_count);
            code_out.print("// ++StartMethod " + name);
        }
        cur_stack = max_stack = 0;
        cur_scope = max_scope = 0;
        max_params = param_count+1; // implicit this
        if (!needs_activation)
        {
            cur_locals = max_locals = param_count + local_count + temp_count+1; // implicit this
        }
        else
        {
            cur_locals = max_locals = param_count + temp_count + ((needs_arguments!=0)?1:0) + 1; // implicit this
        }
        ab.code = allocBytes();
        last_in = IKIND_other;

        // If starting a new script, might redefine the same method name, like $init.
        if (name.equals("$init")) {
            method_infos_map.remove(name);
        }

        GetMethodInfo(name); // this must happen before FinishMethod gets called

        // Reset debug line number for new method
        debug_info.debug_file_dirty = true;
        
        // FLEXCOVER: this dirty flag causes the next branch coverage instrumentation to emit the source filename
        debug_info.debug_branch_file_dirty = true;
        
        debug_info.debug_linenum_dirty = true;
        debug_info.suppress_debug_method = (name.indexOf("$iinit") != -1 ||
                                            name.indexOf("$cinit") != -1);
        
        // FLEXCOVER: stash the name to be used for recording coverage within this function and set a dirty flag to
        //     indicate that a change occurred.
        //
        debug_info.debug_function = debug_name;
        debug_info.debug_function_dirty = true;
        
        sets_dxns = false;
    }

    protected int FinishMethod(Context cx, final String name, TypeInfo type, ObjectList<TypeInfo> types, ObjectValue activation,
                               int needs_arguments, int scope_depth, String debug_name, boolean is_native, boolean is_interface, String[] arg_names)
    {
        if (show_instructions)
        {
            code_out.println();
            code_out.print("// --FinishMethod " + name + " " + debug_name);
        }
        showLineNumber();

        int method_info = GetMethodInfo(name);

        ObjectList<ByteList> traits = new ObjectList<ByteList>();

        // Traits

        if( activation != null )
        {
            ObjectValue obj = activation;
            FinishTraits(obj, traits);
       }

        int flags = 0;
        flags |= needs_arguments; // needs arguments &2=arguments, &4=rest
        flags |= activation != null ? METHOD_Activation : 0; // needs activation

        int debug_name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(emit_debug_info ? debug_name : ""));
        
        if( is_native )
        {
            //debug_name_index = ab.addConstant(ConstantUtf8Info(debug_name));
            flags |= METHOD_Native;
        }
        
        if (sets_dxns)
        {
            flags |= METHOD_Setsdxns;
        }

        int return_type = type != null ? addClassName(type.getName(cx)) : 0;
        

        IntList param_types = null;
        IntList param_values = null;
        ByteList param_kinds = null;

        {
            ObjectValue obj = cx.scope();
            Slots slots = obj.slots;

            if (slots != null)
            {
                param_types = new IntList(slots.size());
                param_values = new IntList(slots.size()); // default values
                param_kinds = new ByteList(slots.size());
    
                Iterator<Slot> it = slots.iterator();
                //it.next(); // first one is this
                boolean value_required = false;
                for (int i = 1; it.hasNext() && i < max_params; ++i)
                {
                    Slot slot = it.next();
                    int type_index = addClassName(slot.getType().getName(cx));
                    param_types.add(type_index);
    
                    int value_index = 0;
                    byte value_kind = 0;
                    if (slot.getInitializerValue() != null)
                    {
                        value_required = true;
                        String value = slot.getInitializerValue().toString();
                        TypeValue defaultValueType = slot.getInitializerValue().type != null ? slot.getInitializerValue().type.getTypeValue() : null;
                        if (defaultValueType == cx.booleanType())
                        {
                            // The index doesn't matter, as long as its non 0
                            // there are no boolean values in any cpool, instead the value will be determined by the kind byte
                            value_index = value.equals("true")?CONSTANT_True:CONSTANT_False;
                            value_kind = value.equals("true")?CONSTANT_True:CONSTANT_False;
                        }
                        else if (defaultValueType == cx.stringType())
                        {
                            value_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(value));
                            value_kind = CONSTANT_Utf8;
                        }
                        else if (defaultValueType == cx.intType())
                        {
                            value_index = ab.addIntConstant(bytecodeFactory.ConstantIntegerInfo((int)getValueOfNumberLiteral(value,new TypeValue[1])));
                            value_kind = CONSTANT_Integer;
                        }
                        else if (defaultValueType == cx.numberType())
                        {
                            value_index = ab.addDoubleConstant(bytecodeFactory.ConstantDoubleInfo(getValueOfNumberLiteral(value,new TypeValue[1])));
                            value_kind = CONSTANT_Double;
                        }
                        else if (defaultValueType == cx.nullType())
                        {
                            // Similar to boolean above, the value will be determined by the kind byte (there is only 1 possible value for null)
                            value_index = CONSTANT_Null;
                            value_kind = CONSTANT_Null;
                        }
                        else if (defaultValueType == cx.voidType())
                        {
                            value_index = 0; // will be undefined at runtime
                            value_kind = 0;
                        }
                        else if (slot.getInitializerValue() instanceof NamespaceValue )
                        {
                            value_index = addNamespace(slot.getInitializerValue());
                            value_kind = CONSTANT_Namespace;
                        }
                    }
                    if (value_required)
                    {
                        param_values.add(value_index);
                        param_kinds.add(value_kind);
                    }
                }
                if (value_required)
                {
                    flags |= METHOD_HasOptional;
                }
            }
        }

        IntList param_names = null;
        if( emit_debug_info && arg_names != null )
        {
            flags |= METHOD_HasParamNames;
            param_names = new IntList(arg_names.length);
            for( int x = 0; x < arg_names.length; ++x )
            {
                param_names.add(ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(arg_names[x])));
            }
        }
        // This is modifying the ab->methods table directly and does not need to call
        // addBytesToTable.
        {
            bytecodeFactory.MethodInfo(ab.methods.at(method_info),max_params-1,return_type,param_types,
                param_values,param_kinds,param_names,debug_name_index,flags,method_info /*debug*/);
        }

        if (!is_native)
        {
            if (!is_interface)
            {
                ab.addBody(bytecodeFactory.MethodBody(allocBytes(), (short) max_stack/*+1*//*off by 1*/,
                    max_locals, scope_depth, scope_depth + max_scope,
                    ab.code.size(), ab.code, ab.exception_count, ab.exception_table, traits,
                    method_info /*debug*/));
            }
        }
        else
        {
            String constName = debug_name.replace('.','_');
            constName = constName.replace('/','_');
            constName = constName.replace(':','_');
            constName = constName.replace('|','_');
            constName = constName.replace('$','_');
            
            header_out.println("const int " + constName + " = " + method_info + ";");
            if (method_info >= native_method_count)
                native_method_count = method_info+1;
        }

        cur_locals = max_locals=0;

        ab.exception_count = 0;
        ab.exception_table.clear();

        return method_info;
    }

    protected void MakeDispatchMethod(Context cx)
    {
    }

    /*
    GetMethodInfo returns the method info that corresponds to a
    particular internal method name. It creates a new method info
    and adds it to the list if one does not exist for the given
    name.
    */

    protected ObjectList<String> method_infos = new ObjectList<String>();
    protected Map<String, Integer> method_infos_map = new HashMap<String,Integer>();

    public int GetMethodInfo(final String name)
    {
        Integer id = method_infos_map.get(name);
        if (id != null)
            return id.intValue();
        id = method_infos.size();
        method_infos_map.put(name,id);

        method_infos.add(name);
        ab.methods.add(allocBytes());
        if (method_infos.size() != ab.methods.size())
        {
            cx.internalError("internal error: internal method name and info out of sync");
        }
        return id.intValue();
    }

    protected ObjectList<String> metadata_infos = new ObjectList<String>();
    protected Map<String, Integer> metadata_infos_map = new HashMap<String,Integer>();

    public int GetMetadataInfo(final String name)
    {


        Integer id = metadata_infos_map.get(name);
        if (id != null)
            return id.intValue();
        id = metadata_infos_map.size();
        metadata_infos_map.put(name,id);


        /* unoptimized code
        int i = 0;
        for (int n = method_infos.size(); i < n; ++i)
        {
            if (name.equals(method_infos.get(i)))
            {
                return i;
            }
        }
        */


        metadata_infos.add(name);
        ab.metadata.add(ByteCodeFactory.allocBytes());
        if (metadata_infos.size() != ab.metadata.size())
        {
            cx.internalError("internal error: internal metadata name and info out of sync");
        }
        return id.intValue();
    }

    protected ObjectList<QName> class_infos = new ObjectList<QName>();

    public int GetClassInfo(final QName name)
    {
        int i;
        for( i = 0; i < class_infos.size(); ++i )
        {
            if( class_infos.get(i).equals(name) )
            {
                return i;
            }
        }
        class_infos.push_back(name);
        ab.classes.push_back(allocBytes());
        ab.instances.push_back(allocBytes());
        if( class_infos.size() != ab.classes.size() )
        {
            cx.internalError("internal error: internal class name and info out of sync");
        }
        return i;
    }

    protected ObjectList<String> package_infos = new ObjectList<String>();

    public int GetPackageInfo(final String name)
    {
        return GetPackageInfo(name, false);
    }

    public int GetPackageInfo(final String name, boolean dont_add)
    {
        int i;

        for( i = 0; i < package_infos.size(); ++i )
        {
            if( package_infos.get(i).equals(name) )
            {
                return i;
            }
        }
        if( dont_add )
        {
            return -1;
        }
        package_infos.push_back(name);
        ab.scripts.push_back(allocBytes());
        if( package_infos.size() != ab.scripts.size() )
        {
            cx.internalError("internal error: internal class name and info out of sync");
        }
        return i;
    }

    /*
    GetMethodId translates an internal name into a unique integer for
    use in the slot structure
    */

    protected ObjectList<String> global_method_names = new ObjectList<String>();

    public int GetMethodId(String name, Namespaces unused)
    {
        int i = 0;

        for (int n = global_method_names.size(); i < n; ++i)
        {
            if (name.equals(global_method_names.get(i)))
            {
                return i+1; // 0 is special
            }
        }
        global_method_names.add(name);
        return global_method_names.size();
    }

    protected String GetMethodName(int n)
    {
        if (n > 0 && n <= global_method_names.size())
        {
            return global_method_names.get(n-1);
        }
        cx.internalError("invalid method name");
        return "";
    }

/*
    public int GetQualifiedNameIndex(String name, ObjectValue qual)
    {
        int id_index = ab.addConstant(bytecodeFactory.ConstantUtf8Info(name));
        int ns_index = addNamespace(qual.name);
        int name_index = ab.addConstant(bytecodeFactory.ConstantQualifiedName((int) id_index, ns_index,false));
        return name_index;
    }
*/
    // protected int getUnaryOffset(int op_index);
    // protected int getBinaryOffset(int op_index);

    protected String getBinaryName(int op_index)
    {
        switch (op_index)
        {
            case BINARY_BinaryPlusOp_II:
                return "BinaryPlusOp_II";
            case BINARY_BinaryPlusOp:
                return "BinaryPlusOp";
            case BINARY_BinaryMinusOp:
                return "BinaryMinusOp";
            case BINARY_BinaryMinusOp_II:
                return "BinaryMinusOp_II";
            case BINARY_MultiplyOp:
                return "MultiplyOp";
            case BINARY_MultiplyOp_II:
                return "MultiplyOp_II";
            case BINARY_DivideOp:
                return "DivideOp";
            case BINARY_ModulusOp:
                return "ModulusOp";
            case BINARY_LeftShiftOp:
                return "LeftShiftOp";
            case BINARY_LeftShiftOp_II:
                return "LeftShiftOp_II";
            case BINARY_RightShiftOp:
                return "RightShiftOp";
            case BINARY_RightShiftOp_II:
                return "RightShiftOp_II";
            case BINARY_UnsignedRightShiftOp:
                return "UnsignedRightShiftOp";
            case BINARY_UnsignedRightShiftOp_II:
                return "UnsignedRightShiftOp_II";
            case BINARY_LessThanOp:
                return "LessThanOp";
            case BINARY_GreaterThanOp:
                return "GreaterThanOp";
            case BINARY_LessThanOrEqualOp:
                return "LessThanOrEqualOp";
            case BINARY_GreaterThanOrEqualOp:
                return "GreaterThanOrEqualOp";
            case BINARY_InstanceofOp:
                return "InstanceofOp";
            case BINARY_InOp:
                return "InOp";
            case BINARY_EqualsOp_II:
                return "EqualsOp_II";
            case BINARY_EqualsOp:
                return "EqualsOp";
            case BINARY_NotEqualsOp_II:
                return "NotEqualsOp_II";
            case BINARY_NotEqualsOp:
                return "NotEqualsOp";
            case BINARY_StrictEqualsOp:
                return "StrictEqualsOp";
            case BINARY_StrictEqualsOp_II:
                return "StrictEqualsOp_II";
            case BINARY_StrictNotEqualsOp:
                return "StrictNotEqualsOp";
            case BINARY_StrictNotEqualsOp_II:
                return "StrictNotEqualsOp_II";
            case BINARY_BitwiseAndOp:
                return "BitwiseAndOp";
            case BINARY_BitwiseAndOp_II:
                return "BitwiseAndOp_II";
            case BINARY_BitwiseXorOp:
                return "BitwiseXorOp";
            case BINARY_BitwiseXorOp_II:
                return "BitwiseXorOp_II";
            case BINARY_BitwiseOrOp:
                return "BitwiseOrOp";
            case BINARY_BitwiseOrOp_II:
                return "BitwiseOrOp_II";
            case BINARY_LogicalAndOp:
                return "LogicalAndOp";
            case BINARY_LogicalAndOp_II:
                return "LogicalAndOp_II";
            case BINARY_LogicalOrOp:
                return "LogicalOrOp";
            case BINARY_LogicalOrOp_II:
                return "LogicalOrOp_II";
            case BINARY_IsLateOp:
                return "IsLateOp";
            case BINARY_IsOp:
                return "IsOp";
            case BINARY_AsLateOp:
                return "AsLateOp";
            case BINARY_AsOp:
                return "AsOp";
            default:
                return "N/A";
        }
    }

       // utility used by getValueOfNumberLiteral
        final private int unHex(char c)
        {
            return Character.digit(c,16);
        }


         // Parses a string into a double format number, set ppType to numberType if the result
         //  is larger than this emitter can hold in a signed integer, or if the string is
         //  floating point format, else sets ppType to cx.intType().  Replaces getTypeOfNumberLiteral
         //  because you can't tell the type of a number until you know how large it is.
        public double getValueOfNumberLiteral(String str, TypeValue[] ppType)
        {
            double  resultSign = 1.0;
            int     len = str.length();
            int     startIndex = 0;
            double  sum = 0.0;
            int     base = 10;

            // First check if this is a base 10 float, perhaps with exponent.  Assume lexer has ensured the
            //  format is valid (i.e. no hex numbers with exponents, multiple .'s etc.)

            if( str.equals("NaN") )
            {
                ppType[0] = cx.numberType();
                return Double.NaN;
            }
            else
            if( str.equals("Infinity") )
            {
                ppType[0] = cx.numberType();
                return Double.POSITIVE_INFINITY;
            }
            else
            if( str.equals("-Infinity") )
            {
                ppType[0] = cx.numberType();
                return Double.NEGATIVE_INFINITY;
            }
            else																			 // short cut.  max integer is 2147483647, 10 digits
            if ((str.indexOf(".") > -1 || str.indexOf("e") > -1 || str.indexOf("E") > -1 || str.length() > 10) &&
                 !(str.indexOf("x") > -1 || str.indexOf("X") > -1)) 
            {

                Double d = Double.valueOf(str);

                if (d.isNaN())
                {
                    sum = Double.NaN;
                    ppType[0] = cx.numberType();
                }
                else if (d.isInfinite())
                {
                    sum = (str.charAt(0) == '-') ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                    ppType[0] = cx.numberType();
                }
                else if (d.doubleValue() == 0.0 && str.charAt(0) == '-')
                {
                    sum = d.doubleValue();
                    ppType[0] = cx.numberType(); // avoid sum == (int)sum test below because 0 == -0, and there is no integer version of negative zero
                }
                else
                {
                    sum = d.doubleValue();
                    // avmplus treats numbers > 2^29 as doubles internally. 
                    int intSum = (int)sum;
                    if (sum == intSum ) // && ((intSum << 3) >> 3) == intSum )
                    {
                        ppType[0] = cx.intType();
                    }
                    else        
                    {
                        ppType[0] = cx.numberType();
                    }
                }
                
                return sum;
            }
            if ( len > 1 && (str.charAt(0) == '-' || str.charAt(0) == '+') )
            {
                resultSign = ((str.charAt(0) == '-') ? -1.0 : 1.0);
                startIndex = 1;
            }
            if ( len > 2 )
            {
                if ( str.charAt(startIndex) == '0' )
                {
                    startIndex++;
                    // check for hex/octal number, skip over necessary chars
                    if (str.charAt(startIndex) == 'x' || str.charAt(startIndex) == 'X')
                    {
                        base = 16;
                        startIndex++;
                    }
                    else
                    {
                        // Octal support has been dropped in AS3.
                        base = 10;
                        //base = 8;
                    }
                }
            }

        // skip leading zeros
        while (startIndex < len && str.charAt(startIndex) == '0')
            startIndex++;
        String str2 = str.substring(startIndex, len);  // need to count bits of precision starting from first nonzero digit
        len -= startIndex;  //  Necessary to accurately detect when we've read more 
        startIndex = 0;     //  than 53 bits worth of mantissa data (where roundoff error occurs).

        int        newDigit = 0;
        int        end;
        boolean    roundUp = false;

        switch(base)
        {
            case 10:
                for (end=startIndex; end < len; end++)
                {
                    newDigit = str2.charAt(end) - '0';
                    sum = sum*base + newDigit;
                }
				if (sum > 2147483647) // i.e. biggest integer possible.  After that, error creeps into the above calculation.
				{
					Double d = Double.valueOf(str2);
					sum = d.doubleValue();
				}
                break;

            case 8:
                for (end=startIndex; end*3 < 52; end++) // 3 bits of precision per digit. 3bits*18 = 54
                {
                    if (end >= len)  // out of bits before 52
                        break;
                    newDigit = str2.charAt(end) - '0';
                    sum = sum*base + newDigit;
                }

                if (end < len-1) // number contains more than 53 bits of precision, may need to roundUp last digit processed.
                {
                    int bit53 = newDigit & 0x1; // i.e. the 54th bit, the first to be dropped from the mantissa.
                    int bit54 = (newDigit & 0x2) >> 1; // roundUp if bit54 is 1 and either the bit before it or any bit after it is 1

                    roundUp = false;
                    double factor = 1.0;
                    while(end++ < len)
                    {
                        newDigit = str2.charAt(end) - '0';
                        roundUp |= (newDigit != 0); // any trailing positive bit causes us to round up
                        factor *= base;
                    }
                    roundUp = (bit54 != 0) && ((bit53 != 0) || roundUp);
                    sum += (roundUp ? 1.0 : 0.0);
                    sum *= factor;
                }
                break;

            case 16:
                for (end=startIndex; end*4 < 53; end++) // 4 bits of precision per digit, 4*13=52
                {
                    if (end >= len)  // out of bits before 52
                        break;
                    newDigit = unHex(str2.charAt(end));
                    sum = sum*base + newDigit;
                }
                if (end < len) // If number contains more than 53 bits of precision, may need to roundUp last digit processed.
                {
                    int bit53 = (newDigit & 0x1);
                    newDigit  = unHex(str2.charAt(end));
                    int bit54 = (newDigit & 0x8) >> 3;  // the 54th bit is the first to be dropped from the mantissa.
                    roundUp   = (newDigit & 0x7) != 0;  // check if any bit after bit54 is set

                    double factor = base;
                    while(++end < len)
                    {
                        newDigit = unHex(str2.charAt(end));
                        roundUp |= (newDigit != 0); // any trailing positive bit causes us to round up
                        factor *= base;
                    }
                    roundUp = (bit54 != 0) && ( (bit53 != 0) || roundUp);
                    sum += (roundUp ? 1.0 : 0.0);
                    sum *= factor;
                }
                break;
        }
        // check for negative 0.  Note: double math above will not produce -0, Double.valueOf will, and 0 == -0 but is not the same number
        if (resultSign==-1 && sum == 0.0)
        {
            Double d = Double.valueOf("-0");
            ppType[0] = cx.numberType();
            return d.doubleValue();    // don't multiply by resultSign
        }

        // avmplus treats numbers > 2^29 as doubles internally. 
        int intSum = (int)sum;
        if (sum == intSum) //  && ((intSum << 3) >> 3) == intSum )
        {
            ppType[0] = cx.intType();
        }
        else        
        {
            ppType[0] = cx.numberType();
        }

        return sum * resultSign;
    }

    // debug only
    protected String getUnaryName(int op_index)
    {
        switch (op_index)
        {
            case UNARY_Put:
                return "put";
            case UNARY_Get:
                return "get";
            case UNARY_HasMoreNames:
                return "HasMoreNamesOp";
            case UNARY_NextName:
                return "NextNameOp";
            case UNARY_NextValue:
                return "NextValueOp";
            case UNARY_DeleteOp:
                return "DeleteOp";
            case UNARY_TypeofOp:
                return "TypeofOp";
            case UNARY_TypeofOp_I:
                return "TypeofOp_I";
            case UNARY_IncrementOp:
                return "IncrementOp";
            case UNARY_IncrementOp_I:
                return "IncrementOp_I";
            case UNARY_IncrementLocalOp:
                return "IncrementLocalOp";
            case UNARY_IncrementLocalOp_I:
                return "IncrementLocalOp_I";
            case UNARY_DecrementOp:
                return "DecrementOp";
            case UNARY_DecrementOp_I:
                return "DecrementOp_I";
            case UNARY_DecrementLocalOp:
                return "DecrementLocalOp";
            case UNARY_DecrementLocalOp_I:
                return "DecrementLocalOp_I";
            case UNARY_UnaryPlusOp:
                return "UnaryPlusOp";
            case UNARY_UnaryPlusOp_I:
                return "UnaryPlusOp_I";
            case UNARY_UnaryMinusOp:
                return "UnaryMinusOp";
            case UNARY_UnaryMinusOp_I:
                return "UnaryMinusOp_I";
            case UNARY_BitwiseNotOp:
                return "BitwiseNotOp";
            case UNARY_BitwiseNotOp_I:
                return "BitwiseNotOp_I";
            case UNARY_LogicalNotOp:
                return "LogicalNotOp";
            case UNARY_LogicalNotOp_B:
                return "LogicalNotOp_B";
            case UNARY_LogicalNotOp_I:
                return "LogicalNotOp_I";
            case UNARY_ToXMLString:
                return "ToXMLString";
            case UNARY_ToXMLAttrString:
                return "ToXMLAttrString";
            case UNARY_CheckFilterOp:
                return "CheckFilterOperand";
        }
        return "N/A";
    }


    protected double stringToDouble(final String str)
    {
        // C: atof() may be very different from this code.
        double  number = 0;
        double  sign = (str.charAt(0) == '-') ? -1.0 : 1.0;

        int     startIndex = (str.charAt(0) == '+' || str.charAt(0) == '-') ? 1 : 0;
        boolean isHex = str.startsWith("0x",startIndex) || str.startsWith("0X",startIndex);
        boolean isFloat = !isHex && (str.indexOf('.') != -1 || str.indexOf('e') != -1 || str.indexOf('E') != -1);
        boolean isOctal = !isHex && !isFloat && str.startsWith("0",startIndex);
        int     radix = (isHex ? 16 : (isOctal ? 8 : 10));

        try
        {
            if (radix != 10)
            {
                String  subString = (isHex ? str.substring(startIndex+2) : str.substring(startIndex));
                number = sign * Long.parseLong(subString,radix);
            }
            else
                number = Double.parseDouble(str);
        }
        catch (NumberFormatException ex1)
        {
            assert(false);
        }
        return number;
    }

    /*  If Long.parseLong() turns out not to be compliant with ECMA3/4, then we'll need to use this
    protected int stringToInt(final String s)
    {
        int len = s.length();
        int mu = 1;
        int iv = 0;

        if (s.startsWith("0x") || s.startsWith("-0x") || s.startsWith("+0x")
            || s.startsWith("0X") || s.startsWith("-0X") || s.startsWith("+0X")
        {
            for (int n = len; n > 2; --n)
            {
                if (n == 3 && s.charAt(n - 3) == '-')
                {
                    iv *= -1;
                }
                else
                {
                    if (s.charAt(n - 1) >= 'A' && s.charAt(n - 1) <= 'F')
                    {
                        iv += mu * (s.charAt(n - 1) - 'A' + 10);
                    }
                    else if (s.charAt(n - 1) >= 'a' && s.charAt(n - 1) <= 'f')
                    {
                        iv += mu * (s.charAt(n - 1) - 'a' + 10);
                    }
                    else
                    {
                        iv += mu * (s.charAt(n - 1) - '0');
                    }
                }
                mu *= 16;
            }
        }
        // check for Octal.  Assumes we would not be here if this was a floating point number ala 0.0
        else if(s.startsWith("0") || s.startsWith("-0") || s.startsWith("+0"))
        {
             for (int n = len; n > 1; --n)
            {
                if (n == 2 && s.charAt(n - 2) == '-')
                {
                    iv *= -1;
                }
                else
                {
                    if (s.charAt(n - 1) >= '0' && s.charAt(n - 1) <= '7')
                    {
                        iv += mu * (s.charAt(n - 1) - '0');
                    }
                    else
                    {
                        iv =(int) Long.parseLong(s,8);
                        mu =0;
                        assert(false); // todo: what to do with an invalid format number ?
                    }
                }
                mu *= 8;
            }
        }
        else
        {
            for (int n = len; n > 0; --n)
            {
                if (n == 1 && s.charAt(n - 1) == '-')
                {
                    iv *= -1;
                }
                else
                {
                    iv += mu * (s.charAt(n - 1) - 48);
                }   // todo: what to do with an invalid format number ?
                mu *= 10;
            }
        }
        return iv;
    }
    */


    // Temps are allocated from max_locals down to the fixed
    // locals

    protected int allocateTemp()
    {
        int temp = cur_locals++;
        if (show_instructions)
        {
            code_out.println();
            code_out.print("AllocTemp " + temp);
        }

        if (cur_locals > max_locals)
        {
            max_locals = cur_locals;
        }
        return temp-1;  // less this
    }

    protected void Kill(int t)
    {
        t++; // adjust for this

        if (show_instructions)
        {
            code_out.println();
            code_out.print("Kill " + t);
        }

        Kill(ab.code, t);
    }

    protected void freeTemp(int t)
    {
        t++; // adjust for this

        if (show_instructions)
        {
            code_out.println();
            code_out.print("FreeTemp " + t);
            if (t != cur_locals-1)
                code_out.write("  out of order");
        }
        Kill(ab.code, t);
        --cur_locals;
    }

    protected int getTempCount()
    {
        return cur_locals;
    }
    
    protected void stack(int size)
    {
        cur_stack += size;
        if (cur_stack > max_stack)
        {
            max_stack = cur_stack;
        }
    }

    protected void scope(int delta)
    {
        cur_scope += delta;
        if (cur_scope > max_scope)
        {
            max_scope = cur_scope;
        }
    }

    IntList stackDepthStack = new IntList();
    IntList scopeDepthStack = new IntList();

    protected void saveStackDepth()
    {
        stackDepthStack.add(cur_stack);
        scopeDepthStack.add(cur_scope);
    }

    protected void restoreStackDepth()
    {
        cur_stack = stackDepthStack.removeLast();

        cur_scope = scopeDepthStack.last();
        scopeDepthStack.removeLast();
    }

    /*
     * getIP
     *
     */

    protected int getIP()
    {
        return ab.code != null ? ab.code.size() : -1;
    }

    /*
     * showLineNumber
     *
     */

    protected void showLineNumber()
    {
        if (show_linenums)
        {
            int[] ln = new int[1], col = new int[1];
            String[] name = new String[1];
            getOriginAndPosition(name, ln, col);
            code_out.println();
            code_out.print("[Ln " + ln[0] + "]");
        }
    }

    public TypeValue getTypeOfNumberLiteral(LiteralNumberNode node)
    {
        TypeValue[] nuType = new TypeValue[1];

        // we can't tell if its an int or a double until we know the value
        //  (integers can be larger than will fit into a 32bit signed int)
         node.numericValue = getValueOfNumberLiteral(node.value, nuType);

        // but, if the format was scientific, force type to be Number
        if (node.value.indexOf(".eE") > -1 && !(node.value.indexOf("xX") > -1))

            node.type = cx.numberType();
        else
            node.type = nuType[0];

        return node.type;
    }

    // Virtual instruction generators

    /*
     * Break(loop_index)
     */

    protected void Break(int loop_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Break target " + loop_index);
        }

        flushDebugInfo();

        last_in = IKIND_other;
        
        int breakTempCount = break_temp_count.get(loop_index);
        for (int i=cur_locals-1; i >= breakTempCount; i--)
        {
            Kill(ab.code, i);
        }
        
        int breakScopeIndex = break_scope_depth.get(loop_index)-1;
        int reg_offset = getRegisterOffset(cx);

        int cur_scope_index = cx.getScopeDepth()-1;
        for( int i = exceptionBlocks.size(); i > 0 && exceptionBlocks.at(i-1).loop_index >= loop_index ; --i )
        {
            ExceptionBlock exceptionBlock = exceptionBlocks.at(i-1);

            while(cur_scope_index > exceptionBlock.scopeIndex )
            {
                // Pop off any scopes before calling the finally so we don't get
                // scope stack depth mismatches
                Popscope(ab.code);
                int temp_reg = cx.scope(cur_scope_index).builder.temp_reg;
                if (temp_reg != -1)
                {
                    Kill(ab.code, temp_reg + reg_offset);
                }
                --cur_scope_index;
            }

            if (exceptionBlock.hasFinally)
            {
                // Label as a kludge to keep the VM from discarding
                // these opcodes
                Label(ab.code);

                // Push the "return address" index
                int finallyIndex = exceptionBlock.finallyAddrs.size();
                Pushbyte(ab.code, finallyIndex);

                // Add a fixup to jump to the finally block
                Jump(ab.code);
                exceptionBlock.fixups.add(getIP() - 3);

                // dummy instruction to pop the "return address" index
                Label(ab.code);
                Pop();

                // Add a fixup to return here
                exceptionBlock.finallyAddrs.add(getIP());
                Label(ab.code);
            }
        }

        while(cur_scope_index > breakScopeIndex )
        {
            Popscope(ab.code);
            int temp_reg = cx.scope(cur_scope_index).builder.temp_reg;
            if (temp_reg != -1)
            {
                Kill(ab.code, temp_reg + reg_offset);
            }
            --cur_scope_index;
        }

        if (loop_index < break_addrs.size())
        {
            Jump(ab.code);  // this needs to get patched
            break_addrs.get(loop_index).add(getIP() - 3);
        } // Otherwise, it is a break and label without a loop. Do nothing

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void CaseLabel(boolean is_default)
    {
        showLineNumber();
        if (show_instructions)
        {
            if (is_default)
            {
                code_out.println();
                code_out.print("CaseLabel default");
            }
            else
            {
                code_out.println();
                code_out.print("CaseLabel");
            }
        }

        restoreStackDepth();
        saveStackDepth();

        boolean need_label = true;
        if (is_default)
        {
            if (seen_default_case.last() == 0)
            {
                default_addrs.add(getIP());
                case_addrs.last().add(getIP());
                seen_default_case.set(seen_default_case.size() - 1, 1);
            }
        }
        else
        {
            if( case_addrs.last().size() > 0 && case_addrs.last().last() == getIP()-1 )
            {
                need_label = false;
            }
            case_addrs.last().add(need_label? getIP() : getIP()-1);
        }

        if( need_label )
            Label(ab.code);
        
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void CheckType(final QName name)
    {
        if (name.toString().length() == 0)
        {
            return; // do nothing
        }

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("CheckType " + name);
        }

        flushDebugInfo();
        
        if( last_in == IKIND_coerce )
        {
            stack(0);
            code_out.println();
            code_out.write("* ERASING "+(ab.code.size()-last_ip)+" bytes");
            ab.code.remove(last_ip,ab.code.size()-last_ip);
        }

        last_ip = getIP();
        last_in = IKIND_coerce;

        String fullname = name.toString();
        if ("*".equals(fullname))
        {
            Coerce_a(ab.code);
        }
        else if ("String".equals(fullname))
        {
            Coerce_s(ab.code);
        }
        else if ("Boolean".equals(fullname))
        {
            Convert_b(ab.code);
        }
        else if ("Number".equals(fullname))
        {
            Convert_d(ab.code);
        }
        else if ("int".equals(fullname))
        {
            Convert_i(ab.code);
        }
        else if ("uint".equals(fullname))
        {
            Convert_u(ab.code);
        }
        else
        {
            int class_index;
            class_index = addClassName(name);
            Coerce(ab.code, class_index);
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Continue(loop_index)
     */

    protected void Continue(int loop_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Continue " + loop_index);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        int continueScopeIndex = continue_scope_depth.get(loop_index)-1;
        int reg_offset = getRegisterOffset(cx);

        int cur_scope_index = cx.getScopeDepth()-1;
        for( int i = exceptionBlocks.size(); i > 0 && exceptionBlocks.at(i-1).loop_index >= loop_index ; --i )
        {
            ExceptionBlock exceptionBlock = exceptionBlocks.at(i-1);

            while(cur_scope_index > exceptionBlock.scopeIndex )
            {
                // Pop off any scopes before calling the finally so we don't get
                // scope stack depth mismatches
                Popscope(ab.code);
                int temp_reg = cx.scope(cur_scope_index).builder.temp_reg;
                if (temp_reg != -1)
                {
                    Kill(ab.code, temp_reg + reg_offset);
                }
                --cur_scope_index;
            }

            if (exceptionBlock.hasFinally)
            {
                // Label as a kludge to keep the VM from discarding
                // these opcodes
                Label(ab.code);

                // Push the "return address" index
                int finallyIndex = exceptionBlock.finallyAddrs.size();
                Pushbyte(ab.code, finallyIndex);

                // Add a fixup to jump to the finally block
                Jump(ab.code);
                exceptionBlock.fixups.add(getIP() - 3);

                // dummy instruction to pop the "return address" index
                Label(ab.code);
                Pop();

                // Add a fixup to return here
                exceptionBlock.finallyAddrs.add(getIP());
                Label(ab.code);
            }
        }

        while(cur_scope_index > continueScopeIndex )
        {
            Popscope(ab.code);
            int temp_reg = cx.scope(cur_scope_index).builder.temp_reg;
            if (temp_reg != -1)
            {
                Kill(ab.code, temp_reg + reg_offset);
            }
            --cur_scope_index;
        }

        if (loop_index >= 0 && loop_index < continue_addrs.size())
        {
            Jump(ab.code); // patch with PatchContinue()
            continue_addrs.get(loop_index).add(getIP() - 3);
        } // Otherwise, it is a continue and label without a loop. Do nothing

        // ISSUE: what if the if block is not executed???

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * DebugSlot
     */
    protected void DebugSlot(String name, int slot, int linenum)
    {
        int index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        DebugLocal(ab.code, index, slot, linenum);
    }

    /*
     * DebugFile
     */
    protected String DebugFile(String name)
    {
        int index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        DebugFile(ab.code, index);
        return name;
    }

    /*
     * DebugLine
     */
    protected void DebugLine(int pos)
    {
        DebugLine(ab.code, pos);
    }

    protected void addSlotTrait(ObjectValue obj, ObjectList<ByteList> traits, String name, Qualifiers quals)
    {
        Builder bui = obj.builder;

        IntList namespaces = new IntList(quals.size());  // will be (*it)->namespaces

        int prev_var_index = -1;
        int var_index = -1;
        int flags = 0;
        Slot prev_slot = null;

        Iterator<Map.Entry<ObjectValue, Integer>> i = quals.entrySet().iterator();        
        Map.Entry<ObjectValue, Integer> qual_it = i.hasNext() ? i.next() : null;

        while( qual_it != null )
        {
            // accumulate namespaces for names that point to the same slot

            int slot_index;
            Slot slot;
            namespaces.clear();  // new multiname

            var_index = -1;

            ObjectValue ns = qual_it.getKey();
            slot_index = obj.getSlotIndex(cx,GET_TOKEN,name,ns);
            slot  = obj.getSlot(cx,slot_index);
            var_index = slot.getVarIndex()+bui.var_offset+1 /*zero is special*/;

            if( slot.declaredBy != obj ) // if it is inherited, then skip it
            {
                qual_it = i.hasNext() ? i.next() : null;
                continue;
            }
            
            while( true )
            {
                if( ns != null )
                {
                    int ns_index = addNamespace(ns);
                    namespaces.add(ns_index);
                }
                else
                {
                    cx.internalError("internal error: non object value for namespace");
                }

                qual_it = i.hasNext() ? i.next() : null;
                if( qual_it == null )
                {
                    break;
                }

                prev_var_index = var_index;
                prev_slot = slot;

                ns = qual_it.getKey();
                slot_index = obj.getSlotIndex(cx,GET_TOKEN,name,ns);
                if(slot_index <= 0)
                	continue;
                slot  = obj.getSlot(cx,slot_index);
                var_index = slot.getVarIndex()+bui.var_offset+1;  // zero is special
                flags &= slot.isFinal() ?TRAIT_FLAG_final:0/*virtual*/;
                flags &= slot.isOverride() ?TRAIT_FLAG_override:0/*new*/;

                if( var_index < 0 || var_index != prev_var_index || slot.declaredBy != prev_slot.declaredBy )
                {
                    var_index = prev_var_index;
                    slot = prev_slot;
                    break; // not an alias of the previous name
                }
            }

            int name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
            int qname_index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespaces.back(),false));

            // Using the last namespace, get the slot for the name

            ObjectValue objval = slot.getInitializerValue();
            int value_index = 0;
            byte value_kind = 0;
            int kind;
            int info;
            if( objval != null && ((objval.builder instanceof ClassBuilder) ? objval.builder : null) != null )
            {
                kind = TRAIT_Class;
                info = GetClassInfo(objval.builder.classname);
            }
//#if 0  // ISSUE: this almost works, but the compiler needs to be smarter about multiple definitions
//            else
//            if( objval && dynamic_cast<FunctionBuilder*>(objval->builder) )
//            {
//                kind = TRAIT_Function;
//                info = GetMethodInfo(objval->name);
//            }
//#endif
            else
            {
                kind = slot.isConst() ? TRAIT_Const : TRAIT_Var;

                if( objval != null )
                {
                    value_index = getValueIndex(objval);
                    value_kind = getValueKind(objval);
                }

                {
                    QName type_name = slot.getType().getName(cx); //slot->type->name;
                    info = addClassName(type_name);
                }

            }

            IntList  metaDataIndices = addMetadata(slot);
            traits.push_back(allocBytes());
            ab.addBytesToTable(traits,bytecodeFactory.TraitInfo(traits.back(),qname_index,kind, obj.canEarlyBind() ? var_index : 0,info,value_index,value_kind,metaDataIndices));
        }
    }

    protected IntList addMetadata(Slot slot)
    {
        if( slot != null )
        {
            return addMetadata(slot.getMetadata());
        }
        return null;
    }
    protected IntList addMetadata(ArrayList<MetaDataNode> metadata)
    {
        IntList metaDataIndices = null;
        if( metadata != null && metadata.size() > 0 )
        {
            metaDataIndices = new IntList(metadata.size());
            Iterator<MetaDataNode> it = metadata.iterator();
            while( it.hasNext() )
            {
                MetaDataNode entry = it.next();
                String id = entry.id;
                Value[] values = entry.values;
                int metaDataIndex = addMetadataInfo(id, values) ;
                metaDataIndices.add(metaDataIndex);
            }
        }
        return metaDataIndices;
    }

    protected int addMetadataInfo(String id, Value[] values )
    {
        String metaDataKey = id;

        int metaNameIndex = id == null ? 0 : ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(id));
        int metaValuesCount = values == null ? 0 : values.length;
        IntList metaKeys = new IntList(metaValuesCount);
        IntList metaValues = new IntList(metaValuesCount);
        for( int z = 0; z < metaValuesCount; ++z )
        {
            Value val = values[z];
            String key = null;
            String value = null;
            if( val instanceof MetaDataEvaluator.KeylessValue )
            {
                key = null;
                value = ((MetaDataEvaluator.KeylessValue)val).obj;
            }
            else if( val instanceof MetaDataEvaluator.KeyValuePair )
            {
                MetaDataEvaluator.KeyValuePair pair = (MetaDataEvaluator.KeyValuePair)val;
                key = pair.key;
                value = pair.obj;
            }
            else
            {
                value = val.toString();
            }
            if( key != null )
            {
                metaKeys.add(ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(key)));
                metaDataKey += key;
            }
            else
            {
                metaKeys.add(0);
            }
            metaDataKey += value;
            metaValues.add(ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(value)));
        }
        int metaDataIndex = GetMetadataInfo(metaDataKey);
        if ( ab.metadata.at(metaDataIndex).size() == 0 )
        {
            // Only add the bytes if they weren't added by a previous trait.  Multiple traits can refer to the
            // same metadata info.
            ab.addBytesToTable(ab.metadata, bytecodeFactory.MetadataInfo(ab.metadata.at(metaDataIndex),
                                          metaNameIndex, metaValuesCount, metaKeys, metaValues, metaDataIndex));
        }
        return metaDataIndex;
    }

    protected void addMethodTrait(int kind, ObjectValue obj, ObjectList<ByteList> traits, String name, Qualifiers quals)
    {
        if( "$construct".equals(name) )
        {
            return;  // ISSUE: experiment. don't define traits for constructors
        }
        
        IntList namespaces = new IntList(quals.size());  // will be (*it)->namespaces

        Iterator<ObjectValue> i = quals.keySet().iterator();        

        boolean isInterface = (obj.type != null && obj.type.isInterface());
     
        while (i.hasNext())
        {
            ObjectValue ns = i.next();

            int slot_kind = kind==TRAIT_Setter?SET_TOKEN:GET_TOKEN;

            int slot_index = obj.getSlotIndex(cx,slot_kind,name,ns);
            Slot slot  = obj.getSlot(cx,slot_index);
          
            if( kind == TRAIT_Method )
            {
                int implicit_index = obj.getImplicitIndex(cx,slot_index,EMPTY_TOKEN);
                slot = obj.getSlot(cx,implicit_index);
            }

            if( slot == null || slot.declaredBy != obj || slot.getMethodName().length() == 0 ) // this happens with internal accessors, that are not traits
            {
                continue;
            }  
            
            ObjectValue slot_value = slot != null? (slot.getValue() instanceof ObjectValue ? (ObjectValue) slot.getValue() : null) : null;
            ArrayList<MetaDataNode> metaData = slot.getMetadata();
            
            // If a name is interface-qualified, emit the trait only if the traits
            // are for an interface definition.  In a class, the VM will automatically
            // add the interface-qualified traits when it sees the public method definition.
            if (!isInterface && ns.isInterface())
            {
                continue;
            }

            boolean is_override = slot.isOverride();
                    
            int flags = 0; // final/virtual & 1, override/new & 2
            flags |= slot.isFinal() ?TRAIT_FLAG_final:0/*virtual*/;
            flags |= is_override?TRAIT_FLAG_override:0/*new*/;

            if( ns != null )
            {
                int ns_index = addNamespace(ns);
                namespaces.add(ns_index);
            }
            else
            {
                cx.internalError("internal error: non object value for namespace");
            }

            int method_info = -1;
            int method_id = slot.getMethodID();
            if( method_id >= 0 )
            {       // method
                method_info = GetMethodInfo(slot.getMethodName());
            }
            else
            if( slot_value != null && slot_value.method_info >= 0 )
            {       // function
                method_info = slot_value.method_info;
            }
            else
            {
                cx.internalError("internal error");
                continue;
            }

            int name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
            int qname_index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespaces.back(),false));

            IntList  metaDataIndices = addMetadata(metaData);

            if( method_info >= 0 )
            {
                traits.push_back(allocBytes());
                method_id = method_id<0?0:method_id; // ISSUE: normalize -1 to 0. refactor to make rest of compiler use 0
                ab.addBytesToTable(traits,bytecodeFactory.TraitInfo(traits.back(),qname_index,kind,obj.canEarlyBind() ? method_id : 0 ,method_info,flags, (byte)0,metaDataIndices));
            }
            else
            {
                cx.internalError("internal error: invalid method info");
            }
        }
    }

    protected void FinishProgram(Context cx, final String name, int init_info)
    {
        ObjectValue obj = cx.scope();
        ObjectList<ByteList> traits = new ObjectList<ByteList>();

        FinishTraits(obj, traits);

        int package_info = GetPackageInfo(name);

        ab.addBytesToTable(ab.scripts,bytecodeFactory.ScriptInfo(ab.scripts.at(package_info),init_info,traits,package_info));

        if (name.length() > 1)
        {
            // ignore packages "" and "$"

            String constName = name.replace('.','_');
            constName = constName.replace('/','_');
            constName = constName.replace(':','_');
            constName = constName.replace('|','_');
            constName = constName.replace('$','_');
            
            header_out.println("const int abcpackage_" + constName + " = " + package_info + ";");
            if (package_info >= native_package_count)
                native_package_count = package_info+1;
        }

        if (show_instructions)
        {
            code_out.println();
            code_out.print("// --FinishProgram " + modulename);
        }
    }

	private void FinishTraits(ObjectValue obj, ObjectList<ByteList> traits) 
	{
		Names names = obj.builder.getNames();
        
        if (names != null)
        {
        	if(Builder.removeBuilderNames)
        	//if(false)
        	{
        		// addSlotTraits/addMethodTraits ignore's the value of the Qualifier so a list of 
        		// namespaces would suffice although even better to refactor these methods to take 
        		// one namespace at a time
        		Qualifiers q = new Qualifiers();
        		for (int i = 0; (i = names.hasNext(i)) != -1; i++)
        		{
        			q.clear();
        			int slotId = names.getSlot(i);
        			
	                String name = names.getName(i);      		
	                q.put(names.getNamespace(i), 0);

        			if(slotId < 1)
        				continue;

        			Slot s = obj.getSlot(cx, slotId);

                    if (names.getNamespace(i) == ObjectValue.loopLabelNamespace || s == null)
                        continue;
                    
        			if(s.declaredBy != obj)
        				continue;
        			
        			if(s instanceof VariableSlot && names.getType(i) != Names.SET_NAMES)
        			{
 	                    addSlotTrait(obj, traits, name, q);
 	                }       			
        			
        			
        			if(s instanceof MethodSlot)
 	                {
	        			int methodKind = TRAIT_Method;
	        			if(s.isGetter())
	        				methodKind = TRAIT_Getter;
	        			if(names.getType(i) == Names.SET_NAMES)
	        				methodKind = TRAIT_Setter;        		
 	                    addMethodTrait(methodKind, obj, traits, name, q);
 	                }
        		}
        	}
        	else
        	{
	            for( Map.Entry<String, Qualifiers> n : names.entrySet(Names.VAR_NAMES))
	            {
	                // Add trait for multiname var
	                addSlotTrait(obj,traits,n.getKey(), n.getValue());
	            }
	
	            for( Map.Entry<String, Qualifiers> n : names.entrySet(Names.METHOD_NAMES))
	            {
	                addMethodTrait(TRAIT_Method,obj,traits,n.getKey(), n.getValue());
	            }
	
	            for( Map.Entry<String, Qualifiers> n : names.entrySet(Names.GET_NAMES))
	            {
	                addMethodTrait(TRAIT_Getter,obj,traits,n.getKey(), n.getValue());
	            }
	
	            for( Map.Entry<String, Qualifiers> n : names.entrySet(Names.SET_NAMES))
	            {
	                addMethodTrait(TRAIT_Setter,obj,traits,n.getKey(), n.getValue());
	            }
        	}
        }
	}

    protected void Dup()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Dup");
        }

        flushDebugInfo();
        
        last_ip = getIP();
        last_in = IKIND_push;
        Dup(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Else
     *
     */

    protected void Else()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Else");
        }

        flushDebugInfo();        

        Jump(ab.code);
        else_addrs.add(getIP() - 3);

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void GetProperty(boolean is_qualified, boolean is_super, boolean is_attr, Namespaces used_def_namespaces)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetProperty is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index=-1;
        if( is_qualified )
        {
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedLate(is_attr));
            stack(-2);
        }
        else
        {
            int namespaces_set = makeNamespaceSet(used_def_namespaces);            
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,is_attr));
            stack(-1);
        }

        if( is_super )
        {
            Getsuper(ab.code,index);
        }
        else
        {
            Getproperty(ab.code,index);
        }

        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void GetProperty(String name, boolean is_super, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetProperty "+name+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int name_index;
        if( name.equals("*") )
        {
            name_index = 0;  // AnyName
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
                
        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if( is_super )
        {
            Getsuper(ab.code,index);
        }
        else
        {
            Getproperty(ab.code,index);
        }
                
        stack(-1); // for the rt namespace

        last_in = IKIND_other;
        if( show_instructions ) code_out.print(" ["+cur_stack+"]");
    }

    // Helper method to print out namespace sets, eliminates duplicate namespaces, since
    // they will be eliminated in the namespace set in the abc file as well.
    private void printNamespaceSet(ObjectList<ObjectValue> namespaces)
    {
        Set<String> namespace_set = new TreeSet<String>();
        for( ObjectValue ns : namespaces )
        {
            String ns_name = ns == cx.publicNamespace() ? "public" : ns.name;
            switch( ns.getNamespaceKind() )
            {
                case Context.NS_PROTECTED:
                    ns_name += "(protected)";
                    break;
                case Context.NS_PRIVATE:
                    ns_name += "(private)";
                    break;
                case Context.NS_INTERNAL:
                    ns_name += "(package-internal)";
                    break;
                case Context.NS_STATIC_PROTECTED:
                    ns_name += "(static-protected)";
                    break;
            }
            namespace_set.add(ns_name);

        }
        code_out.print(namespace_set.size() + " {");
        for(Iterator<String> it = namespace_set.iterator(); it.hasNext(); )
        {
            code_out.print(" " + it.next() );
        }
        code_out.print(" }");
    }

    protected void GetProperty(final String name, Namespaces qualifiers, boolean is_qualified, boolean is_super, boolean is_attr)
    {
        showLineNumber();

        if( show_instructions )
        {
            code_out.println();
            code_out.print("GetProperty " + name + " " );
            printNamespaceSet(qualifiers);
            code_out.print(" is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();


        int index;
        
        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
        
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            Getsuper(ab.code,index);
        }
        else
        {
            Getproperty(ab.code,index);
        }


        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void SetProperty(boolean is_qualified, boolean is_super, boolean is_attr, Namespaces used_def_namespaces, boolean is_constinit)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("SetProperty is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index = -1;
        if( is_qualified )
        {
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedLate(is_attr));
            stack(-4);
        }
        else
        {
            int namespaces_set = makeNamespaceSet(used_def_namespaces);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set, is_attr));
            stack(-3);
        }

        if( is_super )
        {
            Setsuper(ab.code,index);
        }
        else
        if( is_constinit )
        {
            Initproperty(ab.code,index);
        }
        else
        {
            Setproperty(ab.code,index);
        }

        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void SetProperty(String name, boolean is_super, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("SetProperty "+name+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int name_index;
        if( name.equals("*") )
        {
            name_index = 0;  // AnyName
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
                
        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if( is_super )
        {
            Setsuper(ab.code,index);
        }
        else
        {
            Setproperty(ab.code,index);
        }
                
        stack(-3); // for base, ns, val

        last_in = IKIND_other;
        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    
    protected void SetProperty(final String name, Namespaces qualifiers, boolean is_qualified, boolean is_super, boolean is_attr,boolean is_constinit)
    {
        showLineNumber();

        if( show_instructions )
        {
            code_out.println();
            code_out.print("SetProperty " + name + " ");
            printNamespaceSet(qualifiers);
            code_out.print(" is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr+" is_constinit="+is_constinit);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index;
        
        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
        
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            Setsuper(ab.code,index);
        }
        else
        if( is_constinit )
        {
            Initproperty(ab.code,index);
        }
        else
        {
            Setproperty(ab.code,index);
        }

        stack(-2);   // base val


        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void DeleteProperty(boolean is_qualified, boolean is_super, boolean is_attr, Namespaces used_def_namespaces)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("DeleteProperty is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index = -1;
        if( is_qualified )
        {
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedLate(is_attr));
            stack(-2);
        }
        else
        {
            int namespaces_set = makeNamespaceSet(used_def_namespaces);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,is_attr));
            stack(-1);
        }

        if( is_super )
        {
            // error
        }
        else
        {
            Delproperty(ab.code,index);
        }

        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void DeleteProperty(String name, boolean is_super, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("DeleteProperty "+name+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int name_index;
        if( name.equals("*") )
        {
            name_index = 0;  // AnyName
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
                
        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if( is_super )
        {
            // error 
        }
        else
        {
            Delproperty(ab.code,index);
        }
                
        stack(-1); // for the rt namespace

        last_in = IKIND_other;
        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    
    protected void DeleteProperty(final String name, Namespaces qualifiers, boolean is_qualified, boolean is_super, boolean is_attr)
    {
        showLineNumber();

        if( show_instructions )
        {
            code_out.println();
            code_out.print("DeleteProperty " + name + " " );
            printNamespaceSet(qualifiers);
            code_out.print(" is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index;
        
        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
        
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            // error;
        }
        else
        {
            Delproperty(ab.code,index);
        }


        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void GetDescendants(boolean is_qualified, boolean is_attr, Namespaces used_def_namespaces)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetDescendants is_qualified="+is_qualified+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int index;
        if( is_qualified )
        {
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedLate(is_attr));
            stack(-2);
        }
        else
        {
            int namespaces_set = makeNamespaceSet(used_def_namespaces);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set, is_attr));
            stack(-1);
        }
            
        Descendants(ab.code,index);

        last_in = IKIND_other;
        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void GetDescendants(final String name, boolean is_super, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetDescendants "+name+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int name_index;
        if( name.equals("*") )
        {
            name_index = 0;  // AnyName
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
                
        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if( is_super )
        {
            // error 
        }
        else
        {
            Descendants(ab.code,index);
        }
                
        stack(-1); // for the rt namespace

        last_in = IKIND_other;
        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    
    protected void GetDescendants(final String name, Namespaces qualifiers, boolean is_qualified, boolean is_super, boolean is_attr)
    {
        showLineNumber();

        if( show_instructions )
        {
            code_out.println();
            code_out.print("GetDescendants " + name + " " );
            printNamespaceSet(qualifiers);
            code_out.print(" is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index;
        
        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
        
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            // error;
        }
        else
        {
            Descendants(ab.code,index);
        }


        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void DeleteDescendants(final String name, boolean is_super, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("DeleteDescendants "+name+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
                    
        last_ip = getIP();

        int name_index;
        if( name.equals("*") )
        {
            name_index = 0;  // AnyName
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
                
        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if( is_super )
        {
            // error 
        }
        else
        {
            Deletedescendants(ab.code,index);
        }
                
        stack(-1); // for the rt namespace

        last_in = IKIND_other;
        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    
    protected void DeleteDescendants(final String name, Namespaces qualifiers, boolean is_qualified, boolean is_super, boolean is_attr)
    {
        showLineNumber();

        if( show_instructions )
        {
            code_out.println();
            code_out.print("DeleteDescendants " + name + " ");
            printNamespaceSet(qualifiers);
            code_out.print(" is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int index;
        
        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }
        
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            // error;
        }
        else
        {
            Deletedescendants(ab.code,index);
        }


        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void FindProperty(final String name, Namespaces qualifiers, boolean is_strict, boolean is_qualified, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("FindProperty " + name + " " );
            printNamespaceSet(qualifiers);
            code_out.print(" is_strict="+is_strict+" is_qualified="+is_qualified+" is_attr="+is_attr);
        }

        flushDebugInfo();

        last_ip = getIP();
        last_in = IKIND_other;

        {
            int index;
            int name_index;

            if( name.equals("*") )
            {
                name_index = 0; // any name
            }
            else
            {
                name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
            }

            if( is_qualified && qualifiers.size() == 1 )
            {
                int namespace = addNamespace(qualifiers.last());
                index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
            }
            else
            {
                int namespaces_set = makeNamespaceSet(qualifiers) ;
                index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
            }

            if (is_strict)
            {
                Findpropstrict(ab.code, index); // name, needs to be a multiname if with is involved
            }
            else
            {
                Findproperty(ab.code, index); // name, needs to be a multiname if with is involved
            }
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void FindProperty(final String name, boolean is_strict, boolean is_attr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("FindProperty " + name + " is_strict="+is_strict+" is_attr="+is_attr);
        }

        flushDebugInfo();        
        stack(-1);
        last_ip = getIP();
        last_in = IKIND_other;

        int name_index;
        if( "*".equals(name) )
        {
            name_index = 0;
        }
        else
        {
            name_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }

        int index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedName(name_index,is_attr));

        if(is_strict)
        {
            Findpropstrict(ab.code,index);
        }
        else
        {
            Findproperty(ab.code,index);
        }


        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void FindProperty(boolean is_strict, boolean is_attr, boolean is_qualified, Namespaces used_def_namespaces)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("FindProperty " + " is_strict="+is_strict+" is_attr="+is_attr);
        }

        flushDebugInfo();        
        last_ip = getIP();
        last_in = IKIND_other;

        int index;
        if( is_qualified )
        {
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantRuntimeQualifiedLate(is_attr));
            stack(-2);
        }
        else
        {
            int namespaces_set = makeNamespaceSet(used_def_namespaces);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,is_attr));
            stack(-1);
        }

        if(is_strict)
        {
            Findpropstrict(ab.code,index);
        }
        else
        {
            Findproperty(ab.code,index);
        }


        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void GetGlobalScope()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetGlobalScope");
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Getglobalscope(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }        
    }
    
    /*
     * GetBaseObject(scope_index)
     *
     * ISSUE: special case when scope_index == 0,
     * so that we don't use the scope chain to get
     * the global object.
     */

    protected void GetBaseObject(int scope_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetBaseObject " + scope_index);
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Getscopeobject(ab.code, scope_index);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * GetScopeChain()
     */

    protected void GetScopeChain()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetScopeChain");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * GetScopeOnTop()
     */

    protected void GetScopeOnTop()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("GetScopeOnTop");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Exception handling support
     */

    protected void Try(boolean hasFinally)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Try");
        }

        ExceptionBlock exceptionBlock = new ExceptionBlock();
        exceptionBlock.try_start = getIP();
        exceptionBlock.hasFinally = hasFinally;
        exceptionBlock.scopeIndex = cx.getScopeDepth()-1;
        exceptionBlock.loop_index = break_addrs.size() - 1;
        exceptionBlocks.add(exceptionBlock);

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }

        flushDebugInfo();

    }

    protected void CatchClausesBegin()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("CatchClausesBegin");
        }

        ExceptionBlock exceptionBlock = exceptionBlocks.last();
        exceptionBlock.try_end = getIP();
        exceptionBlock.cur_locals = cur_locals;

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }

        flushDebugInfo();

    }

    protected void CatchClausesEnd()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("CatchClausesEnd");
        }

        ExceptionBlock exceptionBlock = exceptionBlocks.last();

        int target = getIP();

        // Patch end of try block to jump around catch clauses
        while (!exceptionBlock.fixups.isEmpty())
        {
            int jump_index = exceptionBlock.fixups.removeLast();
            int offset = target - jump_index + 1 - 4;
            if (bytecodeFactory.show_bytecode)
            {
                code_out.println();
                code_out.print("      Jump@" + (jump_index - 1) + " <- " + offset);
            }
            ab.code.set(jump_index, (byte) offset);
            ab.code.set(jump_index + 1, (byte) (offset >> 8));
            ab.code.set(jump_index + 2, (byte) (offset >> 16));
        }

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }

        flushDebugInfo();
    }

    // numFinallys is how many finally clauses you want called.  -1, means call them all
    // this is because some statements (like throw) only want the closest finally invokes,
    // whereas other statements (like return) want all the finallies invoked.  
    protected void CallFinally(int numFinallys)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("CallFinally");
        }

        flushDebugInfo();

        // Need to invoke every finally block between the return and the function
        // TODO: exceptionBlocks will only include the try/catch/finally blocks for this method, right?
        int finallysInvoked = 0;
        for( int i = exceptionBlocks.size(); i > 0; --i )
        {
            ExceptionBlock exceptionBlock = exceptionBlocks.at(i-1);

            if( cur_locals > exceptionBlock.cur_locals )
                exceptionBlock.cur_locals = cur_locals;

            if( exceptionBlock.hasFinally )
            {
                // Push the "return address" index
                int finallyIndex = exceptionBlock.finallyAddrs.size();
                Pushbyte(ab.code, finallyIndex);

                // Add a fixup to jump to the finally block
                Jump(ab.code);
                exceptionBlock.fixups.add(getIP() - 3);

                // Will never run, but will satisfy the verifiers need for matching stack depths
                Label(ab.code);
                Pop();

                // Add a fixup to return here
                exceptionBlock.finallyAddrs.add(getIP());
                Label(ab.code);

                if( ++finallysInvoked == numFinallys )
                    break;
            }
        }
        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void FinallyClauseBegin()
    {
        // reset the cur_locals count - this is because the cur_locals coming from a catch block
        // may be greater than the cur_locals at this point, and we don't want to tromp on any of the catch
        // blocks locals
        // save the real cur_locals in the exception block so it can be restored when we're done with the finally block.
        ExceptionBlock exblock = exceptionBlocks.last();
        int temp = exblock.cur_locals;
        exblock.cur_locals = cur_locals;
        cur_locals = temp;
    }

    protected void FinallyClauseEnd()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("FinallyClauseEnd");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        IntList fixups = exceptionBlocks.last().finallyAddrs;
        if (fixups.size() > 0)
        {
            int start = getIP();
            Lookupswitch(ab.code);  // rework to fit avm+ semantics
    
            // This will be fixed up with end of switch
            Int24(ab.code, 0);
            
              Int(ab.code, fixups.size() - 1);
              
            for (int i = 0, n=fixups.size(); i < n; ++i)
            {
                Int24(ab.code, fixups.get(i) - start);
            }
            
            // Fixup default to jump to here
            int offset = getIP() - start;
            ab.code.set(start+1, (byte) offset);
            ab.code.set(start+2, (byte) (offset >> 8));
            ab.code.set(start+3, (byte) (offset >> 16));
        }

        // restore the cur_locals
        cur_locals = exceptionBlocks.last().cur_locals;

        // Remove the exception block
        exceptionBlocks.removeLast();
        
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }        
    }

    protected void Catch(TypeValue type, final QName name)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Catch");
        }

        flushDebugInfo();        

        // Add a jump to the end of catch clauses
        ExceptionBlock exceptionBlock = exceptionBlocks.last();
        Jump(ab.code);
        exceptionBlock.fixups.add(getIP() - 3);

        // Add to exception_table
        Int(ab.exception_table, exceptionBlock.try_start);
        Int(ab.exception_table, exceptionBlock.try_end);
        Int(ab.exception_table, getIP());

        int class_index = addClassName(type.builder.classname);
        Int(ab.exception_table, class_index);

        // what to use instead of addClassName
        int name_index = (name != null) ? addClassName(name) : 0;
        Int(ab.exception_table, name_index);

        stack(1); // ex received on stack:
        
        ab.exception_count++;

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void Throw()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Throw");
        }

        flushDebugInfo();
        
        Throw(ab.code);

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * If
     *
     */

    protected void If(int kind, boolean alwaysBranch)
    {

        showLineNumber();
        if (show_instructions)
        {
            String kind_str="";
            switch( kind )
            {
                case IF_false:
                    kind_str = "false";
                    break;
                case IF_true:
                    kind_str = "true";
                    break;
                case IF_lt:
                    kind_str = "lt";
                    break;
                case IF_le:
                    kind_str = "le";
                    break;
                case IF_gt:
                    kind_str = "gt";
                    break;
                case IF_ge:
                    kind_str = "ge";
                    break;
                case IF_eq:
                    kind_str = "eq";
                    break;
                case IF_ne:
                    kind_str = "ne";
                    break;
                case IF_stricteq:
                    kind_str = "stricteq";
                    break;
                case IF_strictne:
                    kind_str = "strictne";
                    break;
                case IF_nlt:
                    kind_str = "nlt";
                    break;
                case IF_nle:
                    kind_str = "nle";
                    break;
                case IF_ngt:
                    kind_str = "ngt";
                    break;
                case IF_nge:
                    kind_str = "nge";
                    break;
                default:
                    //throw "invalid if kind";
                    break;
            }
            code_out.println();
            code_out.print("If "+kind_str);
        }

        flushDebugInfo();        

        switch( kind )
        {
            case IF_false:
                Iffalse(ab.code);
                break;
            case IF_true:
                Iftrue(ab.code);
                break;
            case IF_lt:
                Iflt(ab.code);
                break;
            case IF_le:
                Ifle(ab.code);
                break;
            case IF_gt:
                Ifgt(ab.code);
                break;
            case IF_ge:
                Ifge(ab.code);
                break;
            case IF_eq:
                Ifeq(ab.code);
                break;
            case IF_ne:
                Ifne(ab.code);
                break;
            case IF_stricteq:
                Ifstricteq(ab.code);
                break;
            case IF_strictne:
                Ifstrictne(ab.code);
                break;
            case IF_nlt:
                Ifnlt(ab.code);
                break;
            case IF_nle:
                Ifnle(ab.code);
                break;
            case IF_ngt:
                Ifngt(ab.code);
                break;
            case IF_nge:
                Ifnge(ab.code);
                break;
            default:
                //throw "invalid if kind";
                break;
        }

        if_addrs.add(getIP() - 3);

        saveStackDepth();

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
        
        // FLEXCOVER: record branch coverage for If conditional 
        if (!alwaysBranch)
        {
            RecordIfCoverage();
        }
    }

    /*
     * InvokeBinary
     */

    protected void InvokeBinary(int op_index)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("InvokeBinary " + getBinaryName(op_index));
        }

        flushDebugInfo();
        
        last_in = IKIND_other;

        switch (op_index)
        {

            // Integer operators

            case BINARY_BinaryPlusOp_II:
                Add_i(ab.code);
                break;
            case BINARY_BinaryPlusOp:
                Add(ab.code);
                break;
            case BINARY_BinaryMinusOp_II:
                Subtract_i(ab.code);
                break;
            case BINARY_BinaryMinusOp:
                Subtract(ab.code);
                break;
            case BINARY_MultiplyOp_II:
                Multiply_i(ab.code);
                break;
            case BINARY_MultiplyOp:
                Multiply(ab.code);
                break;
            case BINARY_DivideOp:
                Divide(ab.code);
                break;
            case BINARY_ModulusOp:
                Modulo(ab.code);
                break;
            case BINARY_LeftShiftOp_II:
            case BINARY_LeftShiftOp:
                Lshift(ab.code);
                break;
            case BINARY_RightShiftOp_II:
            case BINARY_RightShiftOp:
                Rshift(ab.code);
                break;
            case BINARY_UnsignedRightShiftOp_II:
            case BINARY_UnsignedRightShiftOp:
                Urshift(ab.code);
                break;
            case BINARY_LessThanOp:
                Lessthan(ab.code);
                break;
            case BINARY_GreaterThanOp:
                Greaterthan(ab.code);
                break;
            case BINARY_LessThanOrEqualOp:
                Lessequals(ab.code);
                break;
            case BINARY_GreaterThanOrEqualOp:
                Greaterequals(ab.code);
                break;
            case BINARY_EqualsOp_II:
            case BINARY_EqualsOp:
                Equals(ab.code);
                break;
            case BINARY_StrictEqualsOp_II:
            case BINARY_StrictEqualsOp:
                Strictequals(ab.code);
                break;
            case BINARY_NotEqualsOp_II:
            case BINARY_NotEqualsOp:
                Equals(ab.code);
                Not(ab.code);
                break;
            case BINARY_StrictNotEqualsOp_II:
            case BINARY_StrictNotEqualsOp:
                Strictequals(ab.code);
                Not(ab.code);
                break;
            case BINARY_BitwiseAndOp_II:
            case BINARY_BitwiseAndOp:
                Bitand(ab.code);
                break;
            case BINARY_BitwiseXorOp_II:
            case BINARY_BitwiseXorOp:
                Bitxor(ab.code);
                break;
            case BINARY_BitwiseOrOp_II:
            case BINARY_BitwiseOrOp:
                Bitor(ab.code);
                break;
            case BINARY_LogicalAndOp_II:
            case BINARY_LogicalAndOp:
            case BINARY_LogicalOrOp_II:
            case BINARY_LogicalOrOp:
                // these aren't used
                break;

                // Generic operators that return int

            case BINARY_InstanceofOp:
                Instanceof(ab.code);
                break;
            case BINARY_InOp:
                In(ab.code);
                break;
            case BINARY_IsLateOp:
                Istypelate(ab.code);
                break;
            case BINARY_AsLateOp:
                Astypelate(ab.code);
                break;
        }


        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * InvokeClosure
     * A closure is an object that captures the environment of some code.
     */

    protected void InvokeClosure(boolean asConstruct, int size)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("InvokeClosure " + (asConstruct ? "construct" : "call") + ", " + size);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Callfunction(ab.code, asConstruct, size);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void ConstructProperty(String name, ObjectList<ObjectValue> qualifiers, int size, boolean is_qualified, boolean is_super, boolean is_attr)
    {
        callPropertyCommon(name,qualifiers,size,is_qualified,is_super,is_attr,true/*is_new*/,false);
    }    
     
    protected void CallProperty(String name, ObjectList<ObjectValue> qualifiers, int size, boolean is_qualified, boolean is_super, boolean is_attr, boolean is_lex)
    {
        callPropertyCommon(name,qualifiers,size,is_qualified,is_super,is_attr,false/*is_new*/,is_lex);
    }    
     
    private void callPropertyCommon(String name, ObjectList<ObjectValue> qualifiers, int size, boolean is_qualified, boolean is_super, boolean is_attr, boolean is_new, boolean is_lex)
    {

        showLineNumber();
        if( show_instructions )
        {
            code_out.println();
            if( is_new )
            {
                code_out.print("ConstructProperty ");
            }
            else
            if( is_lex )
            {
                code_out.print("CallPropLex ");
            }
            else
            {
                code_out.print("CallProperty ");
            }
            code_out.print(name + " " );
            printNamespaceSet(qualifiers);
            code_out.print(" " + size + " is_qualified="+is_qualified+" is_super="+is_super+" is_attr="+is_attr);
        }

        flushDebugInfo();
        
        last_ip = getIP();

        int name_index;
        
        if( name.equals("*") )
        {
            name_index = 0; // any name
        }
        else
        {
            name_index  = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));
        }

        int index;
        if( is_qualified && qualifiers.size() == 1 )
        {
            int namespace = addNamespace(qualifiers.last());
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantQualifiedName(name_index,namespace,is_attr));
        }
        else
        {
            int namespaces_set = makeNamespaceSet(qualifiers);
            index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultiname(name_index,namespaces_set,is_attr));
        }
        
        if( is_super )
        {
            Callsuper(ab.code,index,size);
        }
        else
        if( is_new )
        {
            Constructproperty(ab.code,index,size);
        }
        else
        if( is_lex )
        {
            Callproplex(ab.code,index,size);
        }
        else
        {
            Callproperty(ab.code,index,size);
        }

        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void InvokeMethod(boolean localDispatch, int method_id, int size)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("InvokeMethod " + (localDispatch ? "local" : "global") + " " + method_id + " " + size);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        if (localDispatch)
            Callmethod(ab.code, method_id, size);
        else
            Callstatic(ab.code, method_id, size);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void InvokeSuper(boolean construct, int size)
    {

        showLineNumber();
        if( show_instructions )
        {
            code_out.println();
            code_out.print("InvokeSuper " + (construct?"construct":"call") + " " + size);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        if( construct )
        {
            Constructsuper(ab.code,size);
        }
        else
        {
            cx.internalError("internal error in InvokeSuper()");
        }


        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    protected void InvokeUnary(int operator_id, int size, int data, Namespaces used_def_namespaces)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("InvokeUnary " + getUnaryName(operator_id) + " " + size);
            if (data >= 0)
            {
                code_out.write(" " + data);
            }
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        int str_index;
        int namespaces_set;
        int index;        

        switch (operator_id)
        {
            case UNARY_TypeofOp_B:
                str_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info("boolean"));
                Pushstring(ab.code, str_index);
                break;
            case UNARY_TypeofOp_I:
            case UNARY_TypeofOp_N:
                str_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info("number"));
                Pushstring(ab.code, str_index);
                break;
            case UNARY_TypeofOp_S:
                str_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info("string"));
                Pushstring(ab.code, str_index);
                break;
            case UNARY_TypeofOp_U:
                str_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info("undefined"));
                Pushstring(ab.code, str_index);
                break;
            case UNARY_TypeofOp:
                Typeof(ab.code);
                break;
            case UNARY_IncrementOp:
                Increment(ab.code);
                break;
            case UNARY_IncrementOp_I:
                Increment_i(ab.code);
                break;
            case UNARY_IncrementLocalOp:
                Inclocal(ab.code, data);
                break;
            case UNARY_IncrementLocalOp_I:
                Inclocal_i(ab.code, data);
                break;
            case UNARY_DecrementOp:
                Decrement(ab.code);
                break;
            case UNARY_DecrementOp_I:
                Decrement_i(ab.code);
                break;
            case UNARY_DecrementLocalOp:
                Declocal(ab.code, data);
                break;
            case UNARY_DecrementLocalOp_I:
                Declocal_i(ab.code, data);
                break;
            case UNARY_UnaryPlusOp_I:
                // do nothing
                break;
            case UNARY_UnaryPlusOp:
                Convert_d(ab.code);
                break;
            case UNARY_UnaryMinusOp_I:
            case UNARY_UnaryMinusOp:
                Negate(ab.code);
                break;
            case UNARY_LogicalNotOp_B:
            case UNARY_LogicalNotOp_I:
                Not(ab.code);
                break;
            case UNARY_Put:
                namespaces_set = makeNamespaceSet(used_def_namespaces);
                index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,false));
                Setproperty(ab.code, index);
                break;
            case UNARY_Get:
                namespaces_set = makeNamespaceSet(used_def_namespaces);
                index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,false));
                Getproperty(ab.code, index);
                break;
            case UNARY_LogicalNotOp:
                Not(ab.code);
                break;
            case UNARY_HasMoreNames:
                Hasnext(ab.code);
                break;
            case UNARY_DeleteOp:
                namespaces_set = makeNamespaceSet(used_def_namespaces);
                index = ab.addMultiNameConstant(bytecodeFactory.ConstantMultinameLate(namespaces_set,false));                
                Delproperty(ab.code, index);
                break;
            case UNARY_BitwiseNotOp_I:
            case UNARY_BitwiseNotOp:
                Bitnot(ab.code);
                break;
            case UNARY_NextName:
                Nextname(ab.code);
                break;
            case UNARY_NextValue:
                Nextvalue(ab.code);
                break;
            case UNARY_VoidOp:
                // This is compiled code_out. It evaluates the expression and pushes undefined
                break;
            case UNARY_ToXMLString:
                Esc_xelem(ab.code);
                break;
            case UNARY_ToXMLAttrString:
                Esc_xattr(ab.code);
                break;
            case UNARY_CheckFilterOp:
                CheckFilter(ab.code);
                break;
            default:
                // ISSUE: error
                break;
        }
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadGlobal( index, type_id )
     */

    protected void LoadGlobal(int var_index, int type_id)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadGlobal " + var_index + ", " + typeToString(type_id));
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Getglobalscope(ab.code);
        Getslot(ab.code, var_index + 1/*1-rel*/);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadGlobal( name )
     */

    protected void LoadGlobal(String name)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadGlobal " + name);
        }

        flushDebugInfo();        

        cx.internalError("LoadGlobal(name) is deprecated");

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadRegister( reg )
     */

    protected void LoadRegister(int reg, int type_id)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadRegister " + reg + ", " + typeToString(type_id));
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;
        Getlocal(ab.code, reg);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadThis()
     */

    protected void LoadThis()
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadThis");
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;
        Getlocal(ab.code,0);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadSuper()
     */

    protected void LoadSuper()
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadSuper");
        }

        flushDebugInfo();        

        last_ip = getIP();
        cx.internalError("super expressions are not implemented");
        last_in = IKIND_push;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoadVar( index )
     */

    protected void LoadVar(int var_index)
    {

        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoadVar " + var_index);
        }

        flushDebugInfo();        

        last_ip = getIP();

        Getslot(ab.code, var_index + 1/*1 rel*/);

        last_in = IKIND_other; // ISSUE: this is broken. IKIND_push;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LabelStatement (for breaking out of a labeled block handling)
     *
     */

    protected void LabelStatementBegin()
    {
        break_addrs.add(new IntList());
        break_scope_depth.add(cx.getScopeDepth());
        break_temp_count.add(cur_locals);

        // These will never be used, but add because the loop_index for continues
        // as well as breaks were calculated with the LabelStatement incrementing the loop_index
        // so if we don't add these, we get badness later.
        continue_addrs.add(new IntList());
        continue_scope_depth.add(cx.getScopeDepth());
    }

    protected void LabelStatementEnd(int loop_index)
    {
        PatchBreak(loop_index);
        PatchContinue(loop_index);
    }


    
    /*
     * LoopBegin
     *
     */

    protected void LoopBegin()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("LoopBegin");
        }

        //flushDebugInfo();  Don't flush the debug info - let the first statement in the loop take care of that so the debugging info will be hit on each interation of the loop.
        
        break_addrs.add(new IntList());
        break_scope_depth.add(cx.getScopeDepth());
        break_temp_count.add(cur_locals);
        
        continue_addrs.add(new IntList());
        continue_scope_depth.add(cx.getScopeDepth());
        
        Jump(ab.code);
        loopbegin_addrs.add(getIP() - 3);

        Label(ab.code);
        
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * LoopEnd
     *
     */

    protected void LoopEnd(int kind, boolean alwaysBranch)
    {

        showLineNumber();
        if (show_instructions)
        {
            String kind_str="";
            switch( kind )
            {
                case IF_false:
                    kind_str = "false";
                    break;
                case IF_true:
                    kind_str = "true";
                    break;
                case IF_lt:
                    kind_str = "lt";
                    break;
                case IF_le:
                    kind_str = "le";
                    break;
                case IF_gt:
                    kind_str = "gt";
                    break;
                case IF_ge:
                    kind_str = "ge";
                    break;
                case IF_eq:
                    kind_str = "eq";
                    break;
                case IF_ne:
                    kind_str = "ne";
                    break;
                case IF_stricteq:
                    kind_str = "stricteq";
                    break;
                case IF_strictne:
                    kind_str = "strictne";
                    break;
                case IF_nlt:
                    kind_str = "nlt";
                    break;
                case IF_nle:
                    kind_str = "nle";
                    break;
                case IF_ngt:
                    kind_str = "ngt";
                    break;
                case IF_nge:
                    kind_str = "nge";
                    break;
                default:
                    //throw "invalid if kind";
                    break;
            }
            code_out.println();
            code_out.print("LoopEnd "+kind_str);
        }

        flushDebugInfo();        

        int addr = getIP();
        int offset = loopbegin_addrs.back()-addr+2-3;
        
        switch( kind )
        {
            case IF_false:
                Iffalse(ab.code,offset);
                break;
            case IF_true:
                Iftrue(ab.code,offset);
                break;
            case IF_lt:
                Iflt(ab.code,offset);
                break;
            case IF_le:
                Ifle(ab.code,offset);
                break;
            case IF_gt:
                Ifgt(ab.code,offset);
                break;
            case IF_ge:
                Ifge(ab.code,offset);
                break;
            case IF_eq:
                Ifeq(ab.code,offset);
                break;
            case IF_ne:
                Ifne(ab.code,offset);
                break;
            case IF_stricteq:
                Ifstricteq(ab.code,offset);
                break;
            case IF_strictne:
                Ifstrictne(ab.code,offset);
                break;
            case IF_nlt:
                Ifnlt(ab.code,offset);
                break;
            case IF_nle:
                Ifnle(ab.code,offset);
                break;
            case IF_ngt:
                Ifngt(ab.code,offset);
                break;
            case IF_nge:
                Ifnge(ab.code,offset);
                break;
            default:
                //throw "invalid if kind";
                break;
        }

        // FLEXCOVER: record branch coverage for LoopEnd conditional 
        if (!alwaysBranch)
        {
            RecordLoopEndCoverage();
        }

        loopbegin_addrs.removeLast();
        last_in = IKIND_other;
        
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * NewArray
     */

    protected void NewArray(int size)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewArray " + size);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Newarray(ab.code, size);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * NewClassObject
     *
     */


    protected void NewClassObject(final QName name)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewClassObject " + name);
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_other;

        {
            int info = GetClassInfo(name);
            Newclass(ab.code, info);
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * NewFunctionObject
     *
     * ---                ---
     * #                function_object
     *                    #
     */

    protected void NewFunctionObject(final String name)
    {
        showLineNumber();

        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewFunctionObject " + name);
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_other;

        {
            int method_info = GetMethodInfo(name);
            Newfunction(ab.code, method_info);
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * NewActivation
     */

    protected void NewActivation()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewActivation");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Newactivation(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * NewCatch
     */

    protected void NewCatch(int index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewCatch " + index);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Newcatch(ab.code, index);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }
    
    /*
     * NewObject
     */

    protected void NewObject(int size)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("NewObject " + size);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Newobject(ab.code, size);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PatchBreak
     */

    protected void PatchBreak(int loop_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchBreak " + loop_index);
        }

        flushDebugInfo();        

        // ASSERT(loop_index==break_addrs.size()-1);

        int target = getIP();
        IntList break_addr = break_addrs.removeLast();
        while (break_addr.size() != 0)
        {
            int break_index = break_addr.removeLast();
            int offset = target - break_index + 1 - 4;

            if (bytecodeFactory.show_bytecode)
            {
                code_out.println();
                code_out.print("      Jump@" + (break_index - 1) + " <- " + offset);
            }

            ab.code.set(break_index, (byte) (offset));
            ab.code.set(break_index + 1, (byte) (offset >> 8));
            ab.code.set(break_index + 2, (byte) (offset >> 16));
        }
        break_scope_depth.removeLast();
        break_temp_count.removeLast();
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PatchContinue
     */

    protected void PatchContinue(int loop_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchContinue " + loop_index);
        }

        flushDebugInfo();        

        // ASSERT(loop_index==continue_addrs.size()-1);

        int target = getIP();
        IntList continue_addr = continue_addrs.removeLast();
        while (continue_addr.size() != 0)
        {
            int continue_index = continue_addr.removeLast();
            int offset = target - continue_index + 1 - 4;

            if (bytecodeFactory.show_bytecode)
            {
                code_out.println();
                code_out.print("      Jump@" + (continue_index - 1) + " <- " + offset);
            }

            ab.code.set(continue_index, (byte) (offset));
            ab.code.set(continue_index + 1, (byte) (offset >> 8));
            ab.code.set(continue_index + 2, (byte) (offset >> 16));
        }
        continue_scope_depth.removeLast();
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PatchElse
     */

    protected void PatchElse(int target)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchElse " + else_addrs.last());
        }

        flushDebugInfo();        

        int else_index = else_addrs.removeLast();
        int offset = target - else_index + 1 - 4;

        if (bytecodeFactory.show_bytecode)
        {
            code_out.println();
            code_out.print("      Jump@" + (else_index - 1) + " <- " + offset);
        }

        ab.code.set(else_index, (byte) (offset));
        ab.code.set(else_index + 1, (byte) (offset >> 8));
        ab.code.set(else_index + 2, (byte) (offset >> 16));
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void PatchIf(int target)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchIf " + if_addrs.last());
        }

        flushDebugInfo();        

        restoreStackDepth();

        int if_index = if_addrs.removeLast();

        int offset = target - if_index + 1 - 4;
        if (bytecodeFactory.show_bytecode)
        {
            code_out.println();
            code_out.print("      If@" + (if_index - 1) + " <- " + offset);
        }
        ab.code.set(if_index, (byte) offset);
        ab.code.set(if_index + 1, (byte) (offset >> 8));
        ab.code.set(if_index + 2, (byte) (offset >> 16));

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PatchLoopBegin
     */

    protected void PatchLoopBegin(int target)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchLoopBegin " + target);
        }

        flushDebugInfo();        

        int loopbegin_index = loopbegin_addrs.last();
        int offset = target - loopbegin_index + 1 - 4;

        if (bytecodeFactory.show_bytecode)
        {
            code_out.println();
            code_out.print("      Jump@" + (loopbegin_index - 1) + " <- " + offset);
        }

        ab.code.set(loopbegin_index, (byte) (offset));
        ab.code.set(loopbegin_index + 1, (byte) (offset >> 8));
        ab.code.set(loopbegin_index + 2, (byte) (offset >> 16));
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PatchSwitchBegin
     */

    protected void PatchSwitchBegin(int addr)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PatchSwitchBegin " + addr);
        }

        flushDebugInfo();
        
        restoreStackDepth();

        int switchbegin_index = switchbegin_addrs.removeLast();
        int offset = addr - switchbegin_index + 1 - 4;
        if (bytecodeFactory.show_bytecode)
        {
            code_out.println();
            code_out.print("      Jump@" + (switchbegin_index - 1) + " <- " + offset);
        }
        ab.code.set(switchbegin_index, (byte) (offset));
        ab.code.set(switchbegin_index + 1, (byte) (offset >> 8));
        ab.code.set(switchbegin_index + 2, (byte) (offset >> 16));
        last_in = IKIND_other;
        seen_default_case.removeLast();
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Pop
     *
     */

    protected void Pop()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Pop");
        }

        flushDebugInfo();        

        // Check the cached instructions to see if there
        // are any optimizations possible.

        // total_bytes_erased = 0;

/*
        if (last_in == IKIND_push)
        {
            stack(-1);
            total_bytes_erased += ab.code.size() - last_ip + 1;
            if (show_instructions)
            {
                code_out.println();
                code_out.print("* ERASING " + (ab.code.size() - last_ip + 1) + " bytes, total = " + total_bytes_erased);
            }
            ab.code.remove(last_ip, ab.code.size());
        }
        else
*/
        {
            Pop(ab.code);
        }

        last_in = IKIND_other;

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushBoolean
     *
     * Push a literal boolean value onto the stack.
     *
     * ---                ---
     * #                boolean_value
     *                    #
     */

    protected void PushBoolean(boolean value)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushBoolean " + (value ? 1 : 0));
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        if (value)
        {
            Pushtrue(ab.code);
        }
        else
        {
            Pushfalse(ab.code);
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushEmpty
     */

    protected void PushEmpty()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushEmpty");
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Pushnull(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushCaseIndex(int index)
     */

    protected void PushCaseIndex(int index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushCaseIndex " + index);
        }

        flushDebugInfo();        

        Pushshort(ab.code, index);
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushNull
     */

    protected void PushNull()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushNull");
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Pushnull(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushNumber
     */

    protected void PushNumber(double val, int type_id)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();

            StringBuffer numStr = new StringBuffer();
            IL_FormatDoubleAsString(val,numStr);
            code_out.print("PushNumber " + numStr.toString() + ", " + typeToString(type_id));
        }

        flushDebugInfo();
        
        last_ip = getIP();
        last_in = IKIND_push;

        if (type_id == TYPE_int || type_id == TYPE_uint_external)
        {
            int index;
            int ival = (int)val;
            if ((byte)ival == ival)
            {
                Pushbyte(ab.code, ival);
            }
            else if ((short)ival == ival)
            {
                Pushshort(ab.code, ival);
            }
            else
            {
                index = ab.addIntConstant(bytecodeFactory.ConstantIntegerInfo(ival));
                Pushint(ab.code, index);
            }
        }
        else
        {
            if (Double.isNaN(val))
            {
                Pushnan(ab.code);
            }
            else
            {
                int index = ab.addDoubleConstant(bytecodeFactory.ConstantDoubleInfo(val));
                Pushdouble( ab.code, index);
            }
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void NewNamespace(ObjectValue ns)
    {
        showLineNumber();
        if( show_instructions )
        {
            code_out.println();
            code_out.print("NewNamespace " + ns.name);
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_other;
        addNamespace(ns);

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }

    /*
     * Pushstring
     *
     */

    protected void PushString(final String str)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushString " + str);
        }

        last_ip = getIP();
        last_in = IKIND_push;
        
        {
            int str_index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(str));
            Pushstring(ab.code, str_index);

        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Pushnamespace
     *
     */

    protected void PushNamespace(final ObjectValue ns)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushNamespace " + ns.name);
        }

        last_ip = getIP();
        last_in = IKIND_push;

        {
            int ns_index = addNamespace(ns);
            Pushnamespace(ab.code, ns_index);

        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushUndefined
     */

    protected void PushUndefined()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushUndefined");
        }

        flushDebugInfo();        

        last_ip = getIP();
        last_in = IKIND_push;

        Pushundefined(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushUndefined
     */

    protected void PushUninitialized()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushUninitialized");
        }

        flushDebugInfo();

        last_ip = getIP();
        last_in = IKIND_push;

        Pushuninitialized(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * Return
     *
     */

    protected void Return(int type_id)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Return");
        }

        flushDebugInfo();        

        if (type_id == TYPE_void)
        {
            Returnvoid(ab.code);
        }
        else
        {
            Returnvalue(ab.code);
        }
        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * StoreGlobal( index, type_id )
     */

    protected void StoreGlobal(int var_index, int type_id)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("StoreGlobal " + var_index + ", " + typeToString(type_id));
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Getglobalscope(ab.code);
        Swap(ab.code);
        Setslot(ab.code, var_index + 1);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * StoreGlobal( name )
     */

    protected void StoreGlobal(String name)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("StoreGlobal " + name);
        }

        flushDebugInfo();        

        cx.internalError("StoreGlobal(name) is deprecated");

//if (false)
//{
//        last_in = IKIND_other;
//
//        int name_index = ab.addConstant(ConstantUtf8Info(name));
//        int pref_index = ab.addConstant(ConstantPropRef(0, name_index));
//
//        Setglobal(ab.code, pref_index);
//}

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * StoreRegister( reg, type_id, varName )
     */

    protected void StoreRegister(int reg, int type, final String varName)
    {
        showLineNumber();

        if (show_instructions)
        {
            code_out.println();
            code_out.print("StoreRegister " + reg + ", " + typeToString(type));
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Setlocal(ab.code, reg);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * HasNext( objectRegister, indexRegister )
     */

    protected void HasNext(int objectRegister, int indexRegister)
    {
        showLineNumber();

        if (show_instructions)
        {
            code_out.println();
            code_out.print("HasNext " + objectRegister + ", " + indexRegister);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Hasnext2(ab.code, objectRegister, indexRegister);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }
    
    /*
     * StoreVar( index )
     */

    protected void StoreVar(int var_index)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("StoreVar " + var_index);
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Setslot(ab.code, var_index + 1);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * SwitchBegin
     *
     */

    protected void SwitchBegin()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("SwitchBegin");
        }

        seen_default_case.add(0);
        case_addrs.add(new IntList());
        break_addrs.add(new IntList());
        break_scope_depth.add(cx.getScopeDepth());
        break_temp_count.add(cur_locals);
        continue_addrs.add(new IntList());
        continue_scope_depth.add(cx.getScopeDepth());
        // Even though switches do not have continues, this is
        // to keep the loop index of nested loops synchronized
        // with this vector.

        Jump(ab.code);
        switchbegin_addrs.add(getIP() - 3);
        
        saveStackDepth();

        last_in = IKIND_other;
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * SwitchTable
     *
     * Generate code to jump to the case corresponding to
     * the index on the stack.
     */

    protected void SwitchTable()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("SwitchTable");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        int start = getIP();
        Lookupswitch(ab.code);  // rework to fit avm+ semantics

        int default_addr = default_addrs.removeLast();
        Int24(ab.code, default_addr - start);

        IntList case_addr = case_addrs.removeLast();
          Int(ab.code, case_addr.size() - 1);
          
        for (int case_index = 0, n=case_addr.size(); case_index < n; ++case_index)
        {
            Int24(ab.code, case_addr.get(case_index) - start);
        }
        
        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
        
        // We push a continue_addrs to keep the loop index stacks synchronized.
        // But switch doesn't support continue, so copy any continues up to the
        // surrounding loop.
        IntList switch_continues = continue_addrs.removeLast();
        if (continue_addrs.size() != 0)
        {
        	IntList outer_continues = continue_addrs.last();
        	outer_continues.addAll(switch_continues);
        }
        continue_scope_depth.removeLast();       
    }

    protected void ToBoolean(int type_id)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToBoolean " + typeToString(type_id));
        }

        flushDebugInfo();        

        last_in = IKIND_other;
        Convert_b(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void ToNativeBool()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToNativeBool");
        }

        flushDebugInfo();        
    }

    protected void ToInt()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToInt");
        }
        
        Convert_i(ab.code);

        flushDebugInfo();        
    }

    protected void ToUint()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToUint");
        }
        
        Convert_i(ab.code);
//        Convert_u(ab.code);

        flushDebugInfo();        
    }

    protected void ToNumber(int type_id)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToNumber " + typeToString(type_id));
        }

        flushDebugInfo();        

        last_in = IKIND_other;
        Convert_d(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * ToObject
     *
     * ---                ---
     * value            object_value
     * #                #
     */

    protected void ToObject()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToObject");
        }

        flushDebugInfo();        

        last_in = IKIND_other;

        Convert_o(ab.code);

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * ToString
     *
     * ---                ---
     * value            value
     * #                #
     */

    protected void ToString()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("ToString");
        }

        flushDebugInfo();        

        if (true)
        {
            last_in = IKIND_other;
            Convert_s(ab.code);
        }

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushScope()
     */

    protected void PushScope()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushScope");
        }

        flushDebugInfo();        

        // Get the object on top of stack onto scope stack

        Pushscope(ab.code);

        last_ip = getIP();

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushWith()
     */

    protected void PushWith()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PushWith");
        }

        flushDebugInfo();        

        // Get the object on top of stack onto scope stack

        Pushwith(ab.code);
        
        last_ip = getIP();

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PopScope()
     */

    protected void PopScope()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PopScope");
        }

        flushDebugInfo();        

        // Get the object on top of stack onto scope stack

        Popscope(ab.code);

        last_ip = getIP();

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    protected void PopWith()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("PopWith");
        }

        flushDebugInfo();

        // Get the object on top of stack onto scope stack

        Popscope(ab.code);

        last_ip = getIP();

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    /*
     * PushWith()
     */

    protected void Swap()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("Swap");
        }

        flushDebugInfo();        

        // Get the object on top of stack onto scope stack

        Swap(ab.code);
        
        last_ip = getIP();

        if (show_instructions)
        {
            code_out.write(" [" + cur_stack + "]");
        }
    }

    public void DefaultXMLNamespace()
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("DefaultXMLNamespace");
        }

        flushDebugInfo();

        Dxnslate(ab.code);
        sets_dxns = true;

        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    
    public void DefaultXMLNamespace(String name)
    {
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("DefaultXMLNamespace " + name);
        }

        flushDebugInfo();

        int index = ab.addUtf8Constant(bytecodeFactory.ConstantUtf8Info(name));

        Dxns(ab.code,index);
        sets_dxns = true;

        last_in = IKIND_other;

        if( show_instructions )
        {
            code_out.print(" [" + cur_stack + "]");
        }
    }
    

    static final int IKIND_push = 1;
    static final int IKIND_other = 2;
    static final int IKIND_coerce = 3;

    protected int last_in;
    //protected unsigned int last_ip;
    protected int last_ip;


    /* Virtual Machine instructions
     */

    protected void Bkpt(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Bkpt");
        }
        Byte(code, OP_bkpt);
    }

/*
protected void Getsuper(ByteList code,int index)
    {
        stack(0);
        if (show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getsuper "+index);
        }
        Byte(code, OP_getsuper);
        Int(code,index);
    }
*/
/*
    protected void Delsuper(ByteList code,int index)
    {
        stack(0);
        if (show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Delsuper "+index);
        }
        Byte(code, OP_delsuper);
        Int(code,index);
    }
*/

/*
protected void Setsuper(ByteList code,int index)
    {
        stack(0);
        if (show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Setsuper "+index);
        }
        Byte(code, OP_setsuper);
        Int(code,index);
    }
*/

    protected void Nop(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Nop");
        }

        Byte(code, OP_nop);
    }

    protected void Throw(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Throw");
        }

        Byte(code, OP_throw);
    }

    protected void Jump(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Jump");
        }

        Byte(code, OP_jump);
        Int24(code, 0);
    }
    
    protected void Label(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Label");
        }

        Byte(code, OP_label);
    }

    protected void Iftrue(ByteList code)
    {
        Iftrue(code, 0);
    }

    protected void Iftrue(ByteList code, int offset)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Iftrue " + offset);
        }

        Byte(code, OP_iftrue);
        Int24(code, offset);
    }

    protected void Iffalse(ByteList code)
    {
        Iffalse(code, 0);
    }

    protected void Iffalse(ByteList code, int offset)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Iffalse");
        }

        Byte(code, OP_iffalse);
        Int24(code, offset);
    }

    protected void Ifeq(ByteList code)
    {
        Ifeq(code,0);
    }
    
    protected void Ifeq(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifeq");
        }

        Byte(code, OP_ifeq);
        Int24(code, offset);
    }

    protected void Ifne(ByteList code)
    {
        Ifne(code,0);
    }
    
    protected void Ifne(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifne");
        }

        Byte(code, OP_ifne);
        Int24(code, offset);
    }

    protected void Iflt(ByteList code)
    {
        Iflt(code,0);
    }
    
    protected void Iflt(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Iflt");
        }

        Byte(code, OP_iflt);
        Int24(code, offset);
    }

    protected void Ifle(ByteList code)
    {
        Ifle(code,0);
    }
    
    protected void Ifle(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifle");
        }

        Byte(code, OP_ifle);
        Int24(code, offset);
    }

    protected void Ifgt(ByteList code)
    {
        Ifgt(code,0);
    }
    
    protected void Ifgt(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifgt");
        }

        Byte(code, OP_ifgt);
        Int24(code, offset);
    }

    protected void Ifge(ByteList code)
    {
        Ifge(code,0);
    }
    
    protected void Ifge(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifge");
        }

        Byte(code, OP_ifge);
        Int24(code, offset);
    }

    protected void Ifstricteq(ByteList code)
    {
        Ifstricteq(code,0);
    }
    
    protected void Ifstricteq(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifstricteq");
        }

        Byte(code, OP_ifstricteq);
        Int24(code, offset);
    }

    protected void Ifstrictne(ByteList code)
    {
        Ifstrictne(code,0);
    }
    
    protected void Ifstrictne(ByteList code,int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifstrictne");
        }

        Byte(code, OP_ifstrictne);
        Int24(code, offset);
    }

    protected void Ifnlt(ByteList code)
    {
        Ifnlt(code,0);
    }
    
    protected void Ifnlt(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifnlt");
        }

        Byte(code, OP_ifnlt);
        Int24(code, offset);
    }

    protected void Ifnle(ByteList code)
    {
        Ifnle(code,0);
    }
    
    protected void Ifnle(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifnle");
        }

        Byte(code, OP_ifnle);
        Int24(code, offset);
    }

    protected void Ifngt(ByteList code)
    {
        Ifngt(code,0);
    }
    
    protected void Ifngt(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifngt");
        }

        Byte(code, OP_ifngt);
        Int24(code, offset);
    }

    protected void Ifnge(ByteList code)
    {
        Ifnge(code,0);
    }
    
    protected void Ifnge(ByteList code, int offset)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Ifnge");
        }

        Byte(code, OP_ifnge);
        Int24(code, offset);
    }

    protected void Lookupswitch(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Lookupswitch");
        }

        Byte(code, OP_lookupswitch);
    }


    protected void Pushnull(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushnull");
        }

        Byte(code, OP_pushnull);
    }

    protected void Pushundefined(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushundefined");
        }

        Byte(code, OP_pushundefined);
    }

    protected void Pushuninitialized(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushuninitialized ");
        }

        Byte(code, OP_pushuninitialized);
    }

    protected void Pushstring(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushstring " + id);
        }

        Byte(code, OP_pushstring);
        Int(code, id);
    }

    protected void Pushnamespace(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushnamespace " + id);
        }

        Byte(code, OP_pushnamespace);
        Int(code, id);
    }

    protected void Pushuint(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushuint " + id);
        }

        Byte(code, OP_pushuint);
        Int(code, id);
    }

    protected void Pushdouble(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushdouble " + id);
        }

        Byte(code, OP_pushdouble);
        Int(code, id);
    }

    protected void Pushint(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushint " + id);
        }

        Byte(code, OP_pushint);
        Int(code, id);
    }
    protected void Pushbyte(ByteList code, int n)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushbyte " + n);
        }

        Byte(code, OP_pushbyte);
        Byte(code, n);
    }

    protected void Pushshort(ByteList code, int n)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushshort " + n);
        }

        Byte(code, OP_pushshort);
        Int(code, n);
    }

    protected void Pushtrue(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushtrue");
        }

        Byte(code, OP_pushtrue);
    }

    protected void Pushfalse(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushfalse");
        }

        Byte(code, OP_pushfalse);
    }

    protected void Pushnan(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushnan");
        }

        Byte(code, OP_pushnan);
    }

    protected void Pop(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pop");
        }

        Byte(code, OP_pop);
    }

    protected void Dup(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Dup");
        }

        Byte(code, OP_dup);
    }

    protected void Swap(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Swap");
        }

        Byte(code, OP_swap);
    }

    protected void Newfunction(ByteList code, int id)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newfunction " + id);
        }

        Byte(code, OP_newfunction);
        Int(code, id);  // ISSUE this is different than the avm+ spec
    }

    protected void Newclass(ByteList code, int class_info)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newclass " + class_info);
        }

        Byte(code, OP_newclass);
        Int(code, class_info);
    }

    protected void Callfunction(ByteList code, boolean isnew, int size)
    {

        if (!isnew)
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Call " + size);
            }
            stack(1 - size - 2/*implicit_args*/); // no scope chain for now
            Byte(code, OP_call);
        }
        else
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Construct " + size);
            }
            stack(1 - size - 1/*this+implicit_args*/); // no scope chain for now
            Byte(code, OP_construct);
        }
        Int(code, size);
    }

    protected void Callstatic(ByteList code, int index, int size)
    {
        stack(1 - size - 1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Callstatic " + index + " " + size);
        }

        Byte(code, OP_callstatic);
        Int(code, index);
        Int(code, size);
    }

    protected void Callmethod(ByteList code, int index, int size)
    {
        stack(1 - size - 1/*this*/);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Callmethod " + index + " " + size);
        }

        Byte(code, OP_callmethod);
        Int(code, index);
        Int(code, size);
    }

    protected void Callsuper(ByteList code, int index, int size)
    {
        stack(1-size-1/*this*/);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Callsuper " + index + " " + size);
        }

        Byte(code, OP_callsuper);
        Int(code,index);
        Int(code, size);
    }

    protected void Constructsuper(ByteList code, int size)
    {
        stack(0-size-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Constructsuper " + size);
        }

        Byte(code, OP_constructsuper);
        Int(code, size);
    }

    protected void Callproperty(ByteList code, int index, int size)
    {
        stack(1-size-1/*this*/);
        if( bytecodeFactory.show_bytecode )
        {
            code_out.write("\n      " + getIP() + ":Callproperty "+ index + " " +size);
        }

        Byte(code,OP_callproperty);
        Int(code,index);
        Int(code,size);
    }

    protected void Callproplex(ByteList code, int index, int size)
    {
        stack(1-size-1/*this*/);
        if( bytecodeFactory.show_bytecode )
        {
            code_out.write("\n      " + getIP() + ":Callproplex "+ index + " " +size);
        }

        Byte(code,OP_callproplex);
        Int(code,index);
        Int(code,size);
    }

    protected void Constructproperty(ByteList code, int index, int size)
    {
        stack(1-size-1/*this*/);
        if( bytecodeFactory.show_bytecode )
        {
            code_out.write("\n      " + getIP() + ":Constructproperty "+ index + " " +size);
        }

        Byte(code,OP_constructprop);
        Int(code,index);
        Int(code,size);
    }

    protected void Returnvoid(ByteList code)
    {
        stack(0);
        if( bytecodeFactory.show_bytecode )
        {
            code_out.print("\n      " + getIP() + ":Returnvoid");
        }

        Byte(code,OP_returnvoid);
    }

    protected void Returnvalue(ByteList code)
    {
        stack(-1);
        if( bytecodeFactory.show_bytecode )
        {
            code_out.print("\n      " + getIP() + ":Returnvalue");
        }

        Byte(code,OP_returnvalue);
    }

    protected void Newactivation(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newactivation");
        }

        Byte(code, OP_newactivation);
    }

    protected void Newcatch(ByteList code, int index)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newcatch " + index);
        }

        Byte(code, OP_newcatch);
        Int(code, index);
    }
    
    // ISSUE: this is different than the Avm+ spec

    protected void Newobject(ByteList code, int size)
    {
        stack(1 - size * 2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newobject " + size);
        }

        Byte(code, OP_newobject);
        Int(code, size);
    }

    protected void Newarray(ByteList code, int size)
    {
        stack(1 - size);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Newarray " + size);
        }

        Byte(code, OP_newarray);
        Int(code, size);
    }

    protected void Getlocal(ByteList code, int index)
    {
        stack(1);

        if (index <= 3)
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Getlocal" + index);
            }
            Byte(code, OP_getlocal0 + index);
        }
        else
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Getlocal " + index);
            }
            Byte(code, OP_getlocal);
            Int(code, index);
        }
    }

    protected void Setlocal(ByteList code, int index)
    {
        stack(-1);

        if (index <= 3)
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Setlocal" + index);
            }
            Byte(code, OP_setlocal0 + index);
        }
        else
        {
            if (bytecodeFactory.show_bytecode)
            {
                code_out.write("\n      " + getIP() + ":Setlocal " + index);
            }
            Byte(code, OP_setlocal);
            Int(code, index);
        }
    }

    protected void Getglobalscope(ByteList code)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getglobalscope");
        }

        Byte(code, OP_getglobalscope);
    }
    
    protected void Getscopeobject(ByteList code, int index)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getscopeobject " + index);
        }

        Byte(code, OP_getscopeobject);
        Byte(code, index);
    }

    protected void Getproperty(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getproperty " + index);
        }

        Byte(code, OP_getproperty);
        Int(code, index);
    }

    protected void Getsuper(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getsuper " + index);
        }

        Byte(code, OP_getsuper);
        Int(code, index);
    }
    
    protected void Descendants(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Descendants " + index);
        }

        Byte(code, OP_getdescendants);
        Int(code, index);
    }

    protected void Deletedescendants(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Deletedescendants " + index);
        }

        Byte(code, OP_deldescendants);
        Int(code, index);
    }

    protected void Delproperty(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Delproperty " + index);
        }

        Byte(code, OP_deleteproperty);
        Int(code, index);
    }

/*
    protected void Delqualsuper(ByteList code, int index)
    {
        stack(0);
        if (show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Delqualsuper " + index);
        }

        Byte(code, OP_delqualsuper);
        Int(code, index);
    }
*/

    protected void Setproperty(ByteList code, int index)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Setproperty " + index);
        }

        Byte(code, OP_setproperty);
        Int(code, index);
    }

    protected void Initproperty(ByteList code, int index)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Initproperty " + index);
        }

        Byte(code, OP_initproperty);
        Int(code, index);
    }

    protected void Setsuper(ByteList code, int property)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Setsuper " + property);
        }

        Byte(code, OP_setsuper);
        Int(code, property);
    }

    protected void Pushwith(ByteList code)
    {
        stack(-1);
        scope(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushwith");
        }

        Byte(code, OP_pushwith);
    }

    protected void Pushscope(ByteList code)
    {
        stack(-1);
        scope(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Pushscope");
        }

        Byte(code, OP_pushscope);
    }

    protected void Popscope(ByteList code)
    {
        stack(0);
        scope(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Popscope");
        }

        Byte(code, OP_popscope);
    }

    protected void Nextname(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Nextname");
        }

        Byte(code, OP_nextname);
    }

    protected void Nextvalue(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Nextvalue");
        }

        Byte(code, OP_nextvalue);
    }

    protected void Descendants(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Descendants");
        }

        Byte(code, OP_getdescendants);
    }

    protected void Hasnext(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Hasnext");
        }

        Byte(code, OP_hasnext);
    }

    protected void Hasnext2(ByteList code, int objectRegister, int indexRegister)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Hasnext2 " + objectRegister + " " + indexRegister);
        }

        Byte(code, OP_hasnext2);
        Int(code, objectRegister);
        Int(code, indexRegister);
    }
    
    protected void Convert_s(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.s");
        }

        Byte(code, OP_convert_s);
    }

    protected void Esc_xelem(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Esc.xelem");
        }

        Byte(code, OP_esc_xelem);
    }

    protected void Esc_xattr(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Esc.xattr");
        }

        Byte(code, OP_esc_xattr);
    }

    protected void CheckFilter(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":CheckFilter");
        }

        Byte(code, OP_checkfilter);
    }

    protected void Convert_i(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.i");
        }

        Byte(code, OP_convert_i);
    }

    protected void Convert_u(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.u");
        }

        Byte(code, OP_convert_u);
    }

    protected void Convert_d(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.d");
        }

        Byte(code, OP_convert_d);
    }

    protected void Convert_b(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.b");
        }

        Byte(code, OP_convert_b);
    }

    protected void Convert_o(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Convert.o");
        }

        Byte(code, OP_convert_o);
    }

    protected void Coerce(ByteList code, int name)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Coerce " + name);
        }

        Byte(code, OP_coerce);
        Int(code, name);
    }

    protected void Coerce_a(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Coerce.o");
        }

        Byte(code, OP_coerce_a);
    }

    protected void Coerce_s(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Coerce.s");
        }

        Byte(code, OP_coerce_s);
    }

    protected void Astype(ByteList code, int name)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Astype " + name);
        }

        Byte(code, OP_astype);
        Int(code, name);
    }

    protected void Astypelate(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Astypelate");
        }

        Byte(code, OP_astypelate);
    }

    protected void Negate(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Negate");
        }

        Byte(code, OP_negate);
    }

    protected void Negate_i(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Negate.i");
        }

        Byte(code, OP_negate_i);
    }

    protected void Increment(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Increment");
        }

        Byte(code, OP_increment);
    }

    protected void Increment_i(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Increment.i");
        }

        Byte(code, OP_increment_i);
    }

    protected void Inclocal(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Inclocal " + index);
        }

        Byte(code, OP_inclocal);
        Int(code, index);
    }

    protected void Inclocal_i(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Inclocal.i " + index);
        }

        Byte(code, OP_inclocal_i);
        Int(code, index);
    }

    protected void Decrement(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Decrement");
        }

        Byte(code, OP_decrement);
    }

    protected void Decrement_i(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Decrement.i");
        }

        Byte(code, OP_decrement_i);
    }

    protected void Declocal(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Declocal " + index);
        }

        Byte(code, OP_declocal);
        Int(code, index);
    }

    protected void Declocal_i(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Declocal.i " + index);
        }

        Byte(code, OP_declocal_i);
        Int(code, index);
    }

    protected void Typeof(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Typeof");
        }

        Byte(code, OP_typeof);
    }

    protected void Not(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Not");
        }

        Byte(code, OP_not);
    }

    protected void Add(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Add");
        }

        Byte(code, OP_add);
    }
    protected void Add_i(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Add.i");
        }

        Byte(code, OP_add_i);
    }

    protected void Subtract(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Subtract");
        }

        Byte(code, OP_subtract);
    }
    protected void Subtract_i(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Subtract.i");
        }

        Byte(code, OP_subtract_i);
    }

    protected void Multiply(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Multiply");
        }

        Byte(code, OP_multiply);
    }
    protected void Multiply_i(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Multiply.i");
        }

        Byte(code, OP_multiply_i);
    }

    protected void Divide(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Divide");
        }

        Byte(code, OP_divide);
    }

    protected void Modulo(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Modulo");
        }

        Byte(code, OP_modulo);
    }

    protected void Lshift(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Lshift");
        }

        Byte(code, OP_lshift);
    }

    protected void Rshift(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Rshift");
        }

        Byte(code, OP_rshift);
    }

    protected void Urshift(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Urshift");
        }

        Byte(code, OP_urshift);
    }

    protected void Bitand(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Bitand");
        }

        Byte(code, OP_bitand);
    }

    protected void Bitor(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Bitor");
        }

        Byte(code, OP_bitor);
    }

    protected void Bitxor(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Bitxor");
        }

        Byte(code, OP_bitxor);
    }

    protected void Equals(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Equals");
        }

        Byte(code, OP_equals);
    }

    protected void Strictequals(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Strictequals");
        }

        Byte(code, OP_strictequals);
    }

    protected void Lessthan(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Lessthan");
        }

        Byte(code, OP_lessthan);
    }

    protected void Lessequals(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Lessequals");
        }

        Byte(code, OP_lessequals);
    }

    protected void Greaterthan(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Greaterthan");
        }

        Byte(code, OP_greaterthan);
    }

    protected void Greaterequals(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Greaterequals");
        }

        Byte(code, OP_greaterequals);
    }

    protected void Instanceof(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Instanceof");
        }

        Byte(code, OP_instanceof);
    }

    protected void In(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":In");
        }

        Byte(code, OP_in);
    }

    protected void Istype(ByteList code, int type)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Istype " + type);
        }

        Byte(code, OP_istype);
        Int(code, type);
    }

    protected void Istypelate(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Istypelate");
        }

        Byte(code, OP_istypelate);
    }

    // New instructions

    protected void finddef(ByteList code, int mname)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Finddef " + mname);
        }

        Byte(code, OP_finddef);
        Int(code, mname);
    }

    protected void Findpropstrict(ByteList code, int index)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Findpropstrict " + index);
        }

        Byte(code, OP_findpropstrict);
        Int(code, index);
    }

    protected void Findproperty(ByteList code, int index)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Findproperty " + index);
        }

        Byte(code, OP_findproperty);
        Int(code, index);
    }

    protected void Getslot(ByteList code, int index)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Getslot " + index);
        }

        Byte(code, OP_getslot);
        Int(code, index);
    }

    protected void Setslot(ByteList code, int index)
    {
        stack(-2);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Setslot " + index);
        }

        Byte(code, OP_setslot);
        Int(code, index);
    }

    protected void Bitnot(ByteList code)
    {
        stack(0);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Bitnot");
        }

        Byte(code, OP_bitnot);
    }

    // debug info type for OP_debug
    static final int DI_BAD = 0;
    static final int DI_LOCAL = 1;

    protected void DebugLocal(ByteList code, int index, int slot, int linenum)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Debug DI_SLOT " + index + " slot "+ slot +" line "+linenum);
        }
        Byte(code, OP_debug);
        Byte(code, DI_LOCAL); // type of OP_debug is info about local
        Int(code, index);
        Byte(code, slot);
        Int(code, linenum);
    }

    protected void DebugFile(ByteList code, int index)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":DebugFile " + index);
        }

        Byte(code, OP_debugfile);
        Int(code, index);
    }

    protected void DebugLine(ByteList code, int linenum)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":DebugLine " + linenum);
        }

        Byte(code,OP_debugline);
        Int(code, linenum);
    }

    protected void Dxnslate(ByteList code)
    {
        stack(-1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Dxnslate");
        }

        Byte(code, OP_dxnslate);
    }

    protected void Dxns(ByteList code, int index)
    {
        stack(1);
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Dxns " + index);
        }

        Byte(code, OP_dxns);
        Int(code, index);
    }

    protected void Kill(ByteList code, int index)
    {
        if (bytecodeFactory.show_bytecode)
        {
            code_out.write("\n      " + getIP() + ":Kill " + index);
        }

        Byte(code, OP_kill);
        Int(code, index);
    }

    public int getRegisterOffset(Context cx)
    {
        // Return the register offset for the code being currently generated.
        // We want the innermost scope but ignoring any catch scopes, which
        // do not specify a register offset.
        for (int i = cx.getScopes().size(); --i >= 0; )
        {
            Builder builder = cx.scope(i).builder;
            if (builder.hasRegisterOffset())
            {
                return builder.reg_offset;
            }
        }
        return -1;
    }

    /*
     * Debugging support
     */
    public void setOrigin(String origin)
    {
        super.setOrigin(origin);
        if (emit_debug_info)
        {
            debug_info.debug_file = origin;
            debug_info.debug_file_dirty = true;
            debug_info.debug_branch_file_dirty = true;
        }
    }

    public void setPosition(int lnNum, int colPos, int pos)
    {
        super.setPosition(lnNum, colPos, pos);
        
        if (emit_debug_info && pos > 0)
        {
            if (debug_info.debug_linenum != lnNum)
            {
                debug_info.debug_linenum_dirty = true;
                debug_info.debug_linenum = lnNum;
            }
        }
    }

    public void clearPositionInfo()
    {
        if( emit_debug_info )
        {
            debug_info.debug_linenum_dirty = false;
        }
    }

    public void flushDebugInfo()
    {
        if (emit_debug_info)
        {
            if (debug_info.debug_linenum_dirty &&
                debug_info.debug_linenum > -1 &&
                ab.code != null &&
                !debug_info.suppress_debug_method)
            {
                String debugFileName = null;
                if (debug_info.debug_file_dirty)
                {
                	// FLEXCOVER: we pick up the debug filename for use below
                    debugFileName = DebugFile(debug_info.debug_file);
                    debug_info.debug_file_dirty = false;
                }
                
                DebugLine(debug_info.debug_linenum);
                debug_info.debug_linenum_dirty = false;
                
                // FLEXCOVER:
                //    For each line with debug information, try to record its coverage.
                //    If this is in fact enabled, and this is the first debug line in a function,
                //    then record a function branch point for the function entry.
                //
                boolean lineRecorded = 
                    RecordLineCoverage(debug_info.debug_function, debug_info.debug_linenum, debugFileName);

                if (lineRecorded && debug_info.debug_function_dirty)
                {
                    if (recordBranch(true))
                    {
                        debug_info.debug_function_dirty = false;
                    }
                }
            }
        }
    }
    
    /**
     * FLEXCOVER: record a conditional branch based on the current function name and source position.
     * @param isBranch if true, records the conditional branch as taken (+); if false, records it as not taken (-).
     */
    protected boolean recordBranch(boolean isBranch)
    {
        String debugFileName = null;
        if (debug_info.debug_branch_file_dirty)
        {
            debugFileName = debug_info.debug_file;
            debug_info.debug_branch_file_dirty = false;
        }
        return RecordBranchCoverage(debug_info.debug_function, isBranch, lnNum, colPos);
    }

    /**
     * FLEXCOVER:
     * Record branch coverage at the end of a loop by inserting extra code to record both arms of the control flow,
     * and patching the just-emitted conditional jump.  The net result is like this:
     * <pre>
     * L1:
     *     [loop body]
     *     [push conditional on stack]
     *     IfXXX L2
     *     [record - condition]
     *     Jump L3
     * L2:
     * 	   [record + condition]
     *     Jump L1
     * L3:
     * </pre>
     */
    protected void RecordLoopEndCoverage()
    {
        if (!branch_coverage)
        {
            return;
        }
        
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("RecordLoopEndCoverage");
        }

        // save the address of the just-emitted jump on the true condition that goes back to the loop start.
        int true_addr = getIP() - 3;

        // Record the false arm of the iteration condition as we are exiting the loop and stick in a jump
        // over the recording of the true arm that we'll patch later.
        recordBranch(false);
        Jump(ab.code);
        int false_addr = getIP() - 3;

        // now patch the previous conditional jump to go to the recording of the true arm
        int offset = getIP() - true_addr + 1 - 4;
        ab.code.set(true_addr, (byte) offset);
        ab.code.set(true_addr + 1, (byte) (offset >> 8));
        ab.code.set(true_addr + 2, (byte) (offset >> 16));

        // record the true arm of the branch, and jump back to the start of the loop.
        recordBranch(true);
        Jump(ab.code);
        int loop_addr = getIP() - 3;
        offset = loopbegin_addrs.back() + 3 - getIP();
        ab.code.set(loop_addr, (byte) offset);
        ab.code.set(loop_addr + 1, (byte) (offset >> 8));
        ab.code.set(loop_addr + 2, (byte) (offset >> 16));
        
        // Finally, patch the jump at the end of the recording of the false arm to come here.
        offset = getIP() - false_addr + 1 - 4;
        ab.code.set(false_addr, (byte) offset);
        ab.code.set(false_addr + 1, (byte) (offset >> 8));
        ab.code.set(false_addr + 2, (byte) (offset >> 16));
    }

    /**
     * FLEXCOVER:
     * 
     * Record branch coverage for an If() by inserting extra code to record both arms of the control flow,
     * and patching the just-emitted conditional jump.  Also fiddles with the IP sitting on the stack so that
     * the instrumented code gets patched for Else, etc.  The net result for an AS3 if() statement is:
     * 
     * <pre>
     *     [conditional]
     *     [push conditional on stack]
     *     IfXXX L1
     *     [record + condition]
     *     Jump L2
     * L1:
     * 	   [record - condition]
     *     Jump L3
     * L2:
     *     [if clause]
     *     Jump L4
     * L3:
     *     [else clause, if any]
     * L4:
     * </pre>

     */
    protected void RecordIfCoverage()
    {
        if (!branch_coverage)
        {
            return;
        }
        
        showLineNumber();
        if (show_instructions)
        {
            code_out.println();
            code_out.print("RecordIfCoverage");
        }


        // record the true arm of the branch and insert a jump over the recording of the false arm
        // that we'll patch later.
        recordBranch(true);
        Jump(ab.code);
        int true_addr = getIP() - 3;

        // now patch the previous conditional jump to go to the recording of the false arm
        int if_index = if_addrs.removeLast();

        int offset = getIP() - if_index + 1 - 4;
        ab.code.set(if_index, (byte) offset);
        ab.code.set(if_index + 1, (byte) (offset >> 8));
        ab.code.set(if_index + 2, (byte) (offset >> 16));

        // record the false arm of the branch, and create a jump that will be patched by a subsequent target
        // in place of the original conditional jump.
        recordBranch(false);
        Jump(ab.code);
        if_addrs.add(getIP() - 3);
        
        // Finally, patch the jump at the end of the recording of the true arm to come here.
        offset = getIP() - true_addr + 1 - 4;
        ab.code.set(true_addr, (byte) offset);
        ab.code.set(true_addr + 1, (byte) (offset >> 8));
        ab.code.set(true_addr + 2, (byte) (offset >> 16));
    }
    
    /**
     * Record line coverage within some function in some file.
     * @return true if coverage recording was enabled in this context.
     */
    public boolean RecordLineCoverage(String functionName, int linenum, String debugFileName)
    {
        return false;
    }

    /**
     * Record branch coverage within some function in some file, for the branch sense indicated by
     * isBranch.
     * @return true if coverage recording was enabled in this context.
     */
    public boolean RecordBranchCoverage(String functionName, boolean isBranch, int linenum, int colnum)
    {
        return false;
    }

    public void reorderMainScript()
    {
        ab.scripts.add(ab.scripts.remove(0));
    }

    public String il_str()
    {
        StringBuffer out = new StringBuffer();

        if (bytecodeFactory.show_bytecode)
        {
            out.append("constants----------------------------------------------------");
            out.append(bytecodeFactory.cpool_out.str());
            out.append("\ndefinitions--------------------------------------------------");
            out.append(bytecodeFactory.defns_out.str());
            out.append("\ncode---------------------------------------------------------");
        }

        out.append(code_out);

        return out.toString();
    }
    
    private static String cleanupString(ByteList bl)
    {
    	StringBuffer b = new StringBuffer();
    	for (int i=0, n=bl.size(); i < n; i++)
    	{
    		char c = (char) bl.get(i);
    		if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')
    			b.append(c);
    	}
    	return b.toString();
    }
    
    private String cleanupName(ByteList bl)
    {
    	BytecodeBuffer bc = new BytecodeBuffer(bl.toByteArray(false));
    	if (bc.readU8() == CONSTANT_Qname)
    	{
    		int ns_index = (int) bc.readU32();
    		ByteList nsbl = ab.constant_ns_pool.get(ns_index-1);
    		BytecodeBuffer nsbc = new BytecodeBuffer(nsbl.toByteArray(false));
    		int nskind = nsbc.readU8();
    		if (nskind == CONSTANT_Namespace || nskind == CONSTANT_PackageNamespace)
    		{
	    		int uri_index = (int) nsbc.readU32();
	    		int name_index = (int) bc.readU32();
		    	return cleanupString(ab.constant_utf8_pool.get(uri_index-1))+"_"+cleanupString(ab.constant_utf8_pool.get(name_index-1));
    		}
    	}    	
    	return "";
    }
    
    public void dumpCpoolVars()
    {
    	int i=1;
    	Set<String> done = new HashSet<String>();
    	for (ByteList bl: ab.constant_utf8_pool)
    	{
    		String s = cleanupString(bl);
    		if (s.length() > 0 && !done.contains(s))
    		{
    			done.add(s);
        		header_out.println("const int abcstr_"+s+" = "+ i + ";");
    		}
    		i++;
    	}
    	
    	i=1;
    	for (ByteList bl: ab.constant_mn_pool)
    	{
    		String s = cleanupName(bl);
    		if (s.length() > 0)
    			header_out.println("const int abcname_"+s+" = "+ i + ";");
    		i++;
    	}
    }

    public String header_str()
    {
        String out = header_out.toString();
        if (out.length() > 0)
            return out;
        return null;
    }

    public boolean show_instructions;
    public boolean show_linenums;
    public boolean show_stacknames;
    public boolean emit_debug_info;
    
    // FLEXCOVER: flag enabling instrumentation of coverage
    public boolean coverage;

    // hack to temporarily prevent branch coverage
    private boolean branch_coverage = true;

    private DebugInfo debug_info = new DebugInfo();

    private PrintWriter code_out;
    private PrintWriter header_out;
    public int native_method_count = 0;
    public int native_class_count = 0;
    public int native_package_count = 1; // start by counting the global script

    public ActionBlockEmitter(Context cx,
                              final String scriptname)
    {
        this(cx, scriptname, null, null, false, true, false, false);
    }

    public ActionBlockEmitter(Context cx,
                              final String scriptname,
                              PrintWriter code_out,
                              PrintWriter header_out,
                              boolean show_instructions,
                              boolean show_bytecode,
                              boolean show_linenums,
                              boolean emit_debug_info)
    {
        this.cx = cx;
        this.scriptname = scriptname;
        this.show_instructions = show_instructions;
        this.code_out = code_out;
        this.header_out = header_out;
        this.show_linenums = show_linenums;
        this.emit_debug_info = emit_debug_info;
        show_stacknames = false;
        bytecodeFactory = cx.getByteCodeFactory();
        bytecodeFactory.show_bytecode = show_bytecode;
        ab = new ActionBlock(show_bytecode);
//        initOpTables();
    }

}
