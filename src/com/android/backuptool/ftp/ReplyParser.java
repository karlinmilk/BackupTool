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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class ReplyParser {
    public static int parsePassivePort(String rep) 
    throws IllegalArgumentException{
	int port = -1;
	
	Pattern p = Pattern
		.compile(".*([0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},([0-9]{1,3}),([0-9]{1,3})).*");
	Matcher m = p.matcher(rep.trim());
	if (m.matches() && m.groupCount() == 3) {
	    port = Integer.parseInt(m.group(3)) + Integer.parseInt(m.group(2))
		    * 256;
	}
	return port;
    }

    public static long parseDownloadfilesize(String rep)
            throws IllegalArgumentException
    {
        long downloadsize = -1;
        //2011/04/15 fix Vincent's code - by Flora
        Pattern p = Pattern.compile(".*[(]((\\d*)\\s[Bb]ytes)[)].*");  //find the file size in reply string
        //Pattern p = Pattern.compile(".*[(]((\\d*)\\sbytes)[)]");  //find the file size in reply string
        Matcher m = p.matcher(rep.trim());
        if (m.matches() && m.groupCount() == 2)
        {
            String par = new String(m.group(2));
            downloadsize = Long.parseLong(par);
        }
        else
        {
        	p = Pattern.compile("150\\s((\\d*[.\\d*]+)\\skbytes).*"); 
        	m = p.matcher(rep.trim());
        	if (m.matches() && m.groupCount() == 2)
            {
                String par = new String(m.group(2));
                float size = Float.parseFloat(par);
                downloadsize = (long)((size * 1024) -2048);
            }
        }
        return downloadsize;
    }

}
