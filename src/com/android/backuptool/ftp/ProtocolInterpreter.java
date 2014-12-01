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

import android.util.Log;



public class ProtocolInterpreter {

	protected String host;

	protected IConnector piConn;

	private int port = 21;
	
	public void setEncoding(String serv, String cli) {
		piConn.setEncoding(serv, cli);
	}
	
	public void connectserver(String host) {
		this.connectserver(host, 21);
	}

	public void connectserver(String host, int port) {
		this.host = host;
		try{
		piConn = new Connector(host, port);
		}
		catch (Exception e)
		{
			piConn=null;
			Log.w("ProtocolInterpreter.java", e);
			Log.i("ProtocolInterpreter.java","set piConn = Null");
		}
	}

	public void executeCommand(ICommand cmd) {
		piConn.put(cmd);
		try {
			Thread.sleep(80);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finalize() {
		piConn.close();
	}

	private IConnector getConnector() {
		return piConn;
	}

	public IDataConnector getDataConnector(long port){
		return new DataConnector(host, port);
	}

	public String getResponse() {
		// if(conn==null) throw
		return piConn.get();
	}

}
