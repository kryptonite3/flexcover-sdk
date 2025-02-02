/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Mozilla Communicator client code, released March
 * 31, 1998.
 *
 * The Initial Developer of the Original Code is Netscape Communications
 * Corporation. Portions created by Netscape are
 * Copyright (C) 1998 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): Adobe Systems Incorporated
 * Rob Ginda rginda@netscape.com
 *
 *
 * Modified 2/7/2005 by Sushant Dutta (sdutta@macromedia.com)
 *                 "eval" function had to be removed and so the actual values
 *                 had to be modified.   
 */


test();

function test()
{
    enterFunc ("test");

    printStatus ("Non-character escapes in identifiers negative test.");
    printBugNumber (23607);

    //reportCompare ("error", ("\u0020 = 5"),
    //               "Non-character escapes in identifiers negative test.");
                   
    reportCompare ("error", ("\u0020" = 5),
                   "Non-character escapes in identifiers negative test.");

    exitFunc ("test");
}
