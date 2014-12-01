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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

public class NetUtil {
    public static void getLocalIP() {
	Vector addrs = new Vector();
	Pattern p = Pattern
		.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
	try {
	    Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
	    while (netInterfaces.hasMoreElements()) {
		NetworkInterface netInterface = (NetworkInterface) netInterfaces
			.nextElement();
		Enumeration netInterfaceAddrs = netInterface.getInetAddresses();
		while (netInterfaceAddrs.hasMoreElements()) {
		    InetAddress addr = (InetAddress) netInterfaceAddrs
			    .nextElement();
		    String theAddr = addr.getHostAddress();
		    if (p.matcher(theAddr).matches()) {
			if ("127.0.0.1".equals(theAddr)
				|| theAddr.startsWith("192.168")) {
			    continue;
			}
			//System.out.println(theAddr);
			addrs.add(theAddr);
		    }
		}
	    }
	} catch (SocketException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void main(String[] args) {
	NetUtil.getLocalIP();
    }
}
