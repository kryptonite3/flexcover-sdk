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
 *  Author: Vera Fleischer (vfleisch@macromedia.com)
 *  Date: 08/18/2004
 *
 *  Modifications: (Name   :Date)
 *
 *  This is an error case and should not compile
 */
 
package FullyQualifiedNames {
	
	public class C implements OtherPackage.IClickable {
		public function whoAmI():String{}; 
	}
	
	
}

trace( "FAILED: package must be imported first" );
