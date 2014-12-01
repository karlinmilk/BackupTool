/*
* High Tech Computer Proprietary Rights Acknowledgment
*
*Copyright (c) 2008 High Tech Computer Corporation
*
*All Rights Reserved. 
*
*The information contained in this work is the exclusive property of 
*High Tech Computer Corporation ("HTC").  Only the user who is legally
*authorized by HTC ("Authorized User") has right to employ this work 
*within the scope of this statement.  Nevertheless, the Authorized User
*shall not use this work for any purpose other than the purpose agreed 
*by HTC.  Any and all addition or modification to this work shall be 
*unconditionally granted back to HTC and such addition or modification 
*shall be solely owned by HTC.  No right is granted under this statement, 
*including but not limited to, distribution, reproduction, and transmission, 
*except as otherwise provided in this statement.  Any other usage of this 
*work shall be subject to the further written consent of HTC.
 */

package com.android.backuptool.ftp;

public class RepresentationType implements ITransferParameterCommand {
    private final static String typeASCII = "A";
    private final static String formNonPrint = "N";
    private final static String typeImage = "I";
    private String cmd = "TYPE ";

    private RepresentationType() {
    }

    public static RepresentationType getImageType() {
	RepresentationType r = new RepresentationType();
	r.cmd += typeImage;
	return r;
    }

    public static RepresentationType getASCIIWithNonPrintForm() {
	RepresentationType r = new RepresentationType();
	r.cmd += typeASCII + " " + formNonPrint;
	return r;
    }

    public String toString() {
	// TODO Auto-generated method stub
	return cmd;
    }

}