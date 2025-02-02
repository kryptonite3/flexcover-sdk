/**

 *  The contents of this file are subject to the Netscape Public License Version 1.1 (the "License");
 *  you may not use this file except in compliance with the License. You may obtain a copy of the
 *  License at http://www.mozilla.org/NPL/ Software distributed under the License is distributed on
 *  an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 *  the specific language governing rights and limitations under the License. The Original Code is 
 *  Mozilla Communicator client code, released March 31, 1998. The Initial Developer of the Original
 *  Code is Netscape Communications Corporation. Portions created by Netscape are 
 *  Copyright (C) 1998-1999 Netscape Communications Corporation. All Rights Reserved.
 *
 *  Contributor(s): Adobe Systems Incorporated.
 *
 *
 *  Author: Sushant Dutta (sdutta@macromedia.com)
 *  Date: 2/10/2005
 *  
 *
 *  SECTION           -- section of testing
 *                    -- for example: "Directives" or "Array"
 *  VERSION           -- either "ECMA3", "Clean AS2", or "AS3"
 *  TITLE             -- sub section
 *  BUGNUMBER         -- leave as ""
 *
 *  Running the test
 *
 *  There are three possible environments that these tests should
 *  run in
 *
 *  spidermonkey  - ECMA 3 tests only, should not need to write any
 *                  more than what we have from mozilla.org
 *
 *                  * use shell.as
 *
 *  Flash MX 2004 - existing Actionscript 2.0 environment.  Only tests
 *                  that are supported by Flash 7 will run here
 *
 *                  * use shellMX2004.as
 *
 *  ASC/ASRT      - All tests written must compile and run here
 *
 *                  * use shellASC.as
 */

package Errors {

	public var str:String = new String("");
	public var bool:Boolean = new Boolean(false);
	public var num:Number = 1000;

	public class ThreeOptArgFunctionErr2{
		
	  function returnArgumentsInner(s:String = "Str1", b:Boolean = true, n:Number = 10, ..rest) {
	  	str = s;
	  	bool = b;
	  	num = n;
	  }
	  public function returnArguments() { returnArgumentsInner("String","true",20); }
		
	}

	function returnArgumentsInner(s:String = "Str2", b:Boolean = false, n:Number = 20, ..rest) {
	
		str = s;
		bool = b;
	  	num = n;
	}
	
	
	public function returnArguments() { returnArgumentsInner("String",false,"10"); }
	
}


function returnArgumentsNoPackage(s:String = "Str4", b:Boolean = false, n:Number = 40, ..rest) {
	
		str = s;
		bool = b;
	  	num = n;
	}


import Errors.*;

var SECTION = "Definitions";       // provide a document reference (ie, ECMA section)
var VERSION = "AS3";  // Version of JavaScript or ECMA
var TITLE   = "Function Body Parameter/Result Type";       // Provide ECMA section title or a description
var BUGNUMBER = "";

startTest();                // leave this alone

/**
 * Calls to AddTestCase here. AddTestCase is a function that is defined
 * in shell.js and takes three arguments:
 * - a string representation of what is being tested
 * - the expected result
 * - the actual result
 *
 * For example, a test might look like this:
 *
 * var helloWorld = "Hello World";
 *
 * AddTestCase(
 * "var helloWorld = 'Hello World'",   // description of the test
 *  "Hello World",                     // expected result
 *  helloWorld );                      // actual result
 *
 */

var TESTOBJ = new ThreeOptArgFunctionErr2();

TESTOBJ.returnArguments();

returnArguments();

returnArgumentsNoPackage(false,true,"12");


test();       // leave this alone.  this executes the test cases and
              // displays results.