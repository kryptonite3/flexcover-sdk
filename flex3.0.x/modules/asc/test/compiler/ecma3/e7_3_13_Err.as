
/*  The contents of this file are subject to the Netscape Public License Version 1.1 (the "License");
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
 */
/**
    File Name:          7.3-13-n.js
    ECMA Section:       7.3 Comments
    Description:


    Author:             christine@netscape.com
    Date:               12 november 1997

*/
    var SECTION = "7.3-13-n";
    var VERSION = "ECMA_1";
    startTest();
    var TITLE   = "Comments";

    writeHeaderToLog( SECTION + " "+ TITLE);

    var testcases = getTestCases();
    test();

function getTestCases() {
    var array = new Array();
    var item = 0;
    array[item++] = new TestCase( SECTION,
                                  "nested comment",
                                  "error",
                                  "pass");
    /*/*array[0].actual="fail";*/*/
        return ( array );
}
function test() {


    for ( tc=0; tc < testcases.length; tc++ ) {
        testcases[tc].passed = writeTestCaseResult(
                            testcases[tc].expect,
                            testcases[tc].actual,
                            testcases[tc].description +":  "+
                            testcases[tc].actual );

        testcases[tc].reason += ( testcases[tc].passed ) ? "" : " ignored chars after line terminator of single-line comment";
    }
    stopTest();
    return ( testcases );
}
