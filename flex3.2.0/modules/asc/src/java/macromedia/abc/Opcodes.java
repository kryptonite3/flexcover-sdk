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

package macromedia.abc;

public interface Opcodes
{
	String[] opNames =
	{
    "OP_0x00",
    "bkpt",
    "nop",
    "throw",
    "getsuper",
    "setsuper",
    "dxns",
    "dxnslate",
    "kill",
    "label",
    "OP_0x0A",
    "OP_0x0B",
    "ifnlt",
    "ifnle",
    "ifngt",
    "ifnge",
    "jump",
    "iftrue",
    "iffalse",
    "ifeq",
    "ifne",
    "iflt",
    "ifle",
    "ifgt",
    "ifge",
    "ifstricteq",
    "ifstrictne",
    "lookupswitch",
    "pushwith",
    "popscope",
    "nextname",
    "hasnext",
    "pushnull",
    "pushundefined",
    "OP_0x22",
    "nextvalue",
    "pushbyte",
    "pushshort",
    "pushtrue",
    "pushfalse",
    "pushnan",
    "pop",
    "dup",
    "swap",
    "pushstring",
    "pushint",
    "pushuint",
    "pushdouble",
    "pushscope",
    "pushnamespace",
    "hasnext2",
    "pushdecimal",
    "pushdnan",
    "OP_0x35",
    "OP_0x36",
    "OP_0x37",
    "OP_0x38",
    "OP_0x39",
    "OP_0x3A",
    "OP_0x3B",
    "OP_0x3C",
    "OP_0x3D",
    "OP_0x3E",
    "OP_0x3F",
    "newfunction",
    "call",
    "construct",
    "callmethod",
    "callstatic",
    "callsuper",
    "callproperty",
    "returnvoid",
    "returnvalue",
    "constructsuper",
    "constructprop",
    "callsuperid",
    "callproplex",
    "callinterface",
    "callsupervoid",
    "callpropvoid",
    "OP_0x50",
    "OP_0x51",
    "OP_0x52",
    "applytype",
    "OP_0x54",
    "newobject",
    "newarray",
    "newactivation",
    "newclass",
    "getdescendants",
    "newcatch",
    "OP_0x5B",
    "OP_0x5C",
    "findpropstrict",
    "findproperty",
    "finddef",
    "getlex",
    "setproperty",
    "getlocal",
    "setlocal",
    "getglobalscope",
    "getscopeobject",
    "getproperty",
    "OP_0x67",
    "initproperty",
    "OP_0x69",
    "deleteproperty",
    "OP_0x6B",
    "getslot",
    "setslot",
    "getglobalslot",
    "setglobalslot",
    "convert_s",
    "esc_xelem",
    "esc_xattr",
    "convert_i",
    "convert_u",
    "convert_d",
    "convert_b",
    "convert_o",
    "checkfilter",
    "convert_m",
    "convert_m_p",
    "OP_0x7B",
    "OP_0x7C",
    "OP_0x7D",
    "OP_0x7E",
    "OP_0x7F",
    "coerce",
    "coerce_b",
    "coerce_a",
    "coerce_i",
    "coerce_d",
    "coerce_s",
    "astype",
    "astypelate",
    "coerce_u",
    "coerce_o",
    "OP_0x8A",
    "OP_0x8B",
    "OP_0x8C",
    "OP_0x8D",
    "OP_0x8E",
    "negate_p",
    "negate",
    "increment",
    "inclocal",
    "decrement",
    "declocal",
    "typeof",
    "not",
    "bitnot",
    "OP_0x98",
    "OP_0x99",
    "concat",
    "add_d",
    "increment_p",
    "inclocal_p",
    "decrement_p",
    "declocal_p",
    "add",
    "subtract",
    "multiply",
    "divide",
    "modulo",
    "lshift",
    "rshift",
    "urshift",
    "bitand",
    "bitor",
    "bitxor",
    "equals",
    "strictequals",
    "lessthan",
    "lessequals",
    "greaterthan",
    "greaterequals",
    "instanceof",
    "istype",
    "istypelate",
    "in",
    "add_p",
    "subtract_p",
    "multiply_p",
    "divide_p",
    "modulo_p",
    "OP_0xBA",
    "OP_0xBB",
    "OP_0xBC",
    "OP_0xBD",
    "OP_0xBE",
    "OP_0xBF",
    "increment_i",
    "decrement_i",
    "inclocal_i",
    "declocal_i",
    "negate_i",
    "add_i",
    "subtract_i",
    "multiply_i",
    "OP_0xC8",
    "OP_0xC9",
    "OP_0xCA",
    "OP_0xCB",
    "OP_0xCC",
    "OP_0xCD",
    "OP_0xCE",
    "OP_0xCF",
    "getlocal0",
    "getlocal1",
    "getlocal2",
    "getlocal3",
    "setlocal0",
    "setlocal1",
    "setlocal2",
    "setlocal3",
    "OP_0xD8",
    "OP_0xD9",
    "OP_0xDA",
    "OP_0xDB",
    "OP_0xDC",
    "OP_0xDD",
    "OP_0xDE",
    "OP_0xDF",
    "OP_0xE0",
    "OP_0xE1",
    "OP_0xE2",
    "OP_0xE3",
    "OP_0xE4",
    "OP_0xE5",
    "OP_0xE6",
    "OP_0xE7",
    "OP_0xE8",
    "OP_0xE9",
    "OP_0xEA",
    "OP_0xEB",
    "OP_0xEC",
    "OP_0xED",
    "abs_jump",
    "debug",
    "debugline",
    "debugfile",
    "bkptline",
    "timestamp",
    "OP_0xF4",
    "verifypass",
    "alloc",
    "mark",
    "wb",
    "prologue",
    "sendenter",
    "doubletoatom",
    "sweep",
    "codegenop",
    "verifyop",
    "decode"
	};
}
