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

import java.io.UnsupportedEncodingException;

public class Encoding {
    /*
    public static byte[] u8(byte[] b){
	String s = new String(b, "");
	
    }
    */
    
    public static String b5u8(String s) {
	return Encoding.changeEncoding(s, "big5", "utf-8");
    }

    public static String u8b5(String s) {
	return Encoding.changeEncoding(s, "utf-8", "big5");
    }

    public static String u8(String s) {
	return Encoding.changeEncoding(s, "utf-8");
    }

    public static String b5(String s) {
	return Encoding.changeEncoding(s, "big5");
    }
    public static String gbku8(String s) {
    	return Encoding.changeEncoding(s,"gbk", "big5");
    }
    public static String changeEncoding(String s, String target) {
	String result = null;
	try {
	    byte[] b = s.getBytes();
	    result = new String(b, target);
	} catch (UnsupportedEncodingException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return result;
    }

    public static String changeEncoding(String s, String source, String target) {
	String result = null;
	try {
	    byte[] b = s.getBytes(source);
	    result = new String(b, target);
	} catch (UnsupportedEncodingException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return result;
    }
}
