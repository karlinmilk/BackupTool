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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Calendar;

import com.android.backuptool.BackupService;
import com.android.backuptool.FTPProcess;
import com.android.backuptool.utils.BackupValue;

import android.util.Log;

public class FtpClient extends ProtocolInterpreter implements Runnable {

	public boolean isAutoReply = false;
	public Calendar ca;
	public static boolean isOk;
	public static boolean passloop;
	public Reply reply = new Reply();
	int dualmodeFlag = -1;
	public int TEST_METHOD=0;
	public void connect(String host) {
		connectserver(host);
		reply.setRawReply(new String(super.getResponse()));
		if (this.isAutoReply)
			this.showReply();
	}

	public void connect(String host, int port) {
		connectserver(host, port);
		reply.setRawReply(new String(super.getResponse()));
		if (this.isAutoReply)
			this.showReply();
	}

	
	public void run() {

		switch (TEST_METHOD) {
		case FTPProcess.COMMAND_CONNECT:
			Log.i("FtpClient.java", "enter COMMAND_CONNECT");
			ca = Calendar.getInstance();

			try {
				connect(FTPProcess.mFTPAddress);

				this.setEncoding("utf-8", "utf-8");
				isOk = this.login(FTPProcess.mFTPUser, FTPProcess.mFTPPassword);
				Log.i("FtpClient.java","login isOk:"+isOk+",mConnected:"+FTPProcess.mConnected);
				
				
				if (isOk) {					
					if (this.pwd()) {
						
						FTPProcess.mHandler.obtainMessage(
								FTPProcess.START_UPLOAD).sendToTarget();
					}
				}
				
				if(!FTPProcess.mConnected)
				{
					FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
							.obtainMessage(FTPProcess.Status_MSG);
					FTPProcess.mErrorMsg.sendToTarget();
				}
				
			} catch (Exception e) {
				
				/*
				if (FTPProcess.mTestMethod == 3)
					FTPProcess.mDaulDisconnect = true;
				*/				
				FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
						.obtainMessage(FTPProcess.Status_MSG);
				FTPProcess.mErrorMsg.sendToTarget();
				
				Log.i("FtpClient.java", "FTPProcess.Connect exception happened-",e);
			}

			break;
		case FTPProcess.COMMAND_DISCONNECT:
			try {
				if(this.logout())
				{
					FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FTP_DISCONNECTED).sendToTarget();
				}
			} catch (Exception e) {
				Log.i("FtpClient.java",
						"FTPProcess.Disconnect exception happened-",e);
			}

			break;			
		
		case FTPProcess.COMMAND_UPLOAD: {
			Log.i("FtpClient.java", "enter COMMAND_UPLOAD");
			try {
				 StartUpload();
				
			} catch (Exception e) {
				Log.i("FtpClient.java",
						"FTPProcess.Upload exception happened-",e);
				FTPProcess.mAnotherThreadFinish = false;
				FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
						.obtainMessage(FTPProcess.Status_MSG);
				FTPProcess.mErrorMsg.sendToTarget();
				break; 
			}

			FTPProcess.mAnotherThreadWaiting = false;
			Log.i("FtpClient.java", "Upload File:" + passloop);
			if(passloop)
			{
				FTPProcess.mMessage=FTPProcess.mMessageHandler.obtainMessage(FTPProcess.UPLOAD_FINISH);
				FTPProcess.mMessage.sendToTarget();
			}
				
			
		}
			break;
		
			
		}
	}

	public void StartUpload(){
		passloop = false;
		this.reply.setRawReply("0");
		this.reply.showUlStatus();
		ca = Calendar.getInstance();
		FTPProcess.mStartTime = ca.getTime().toString().substring(11, 19);
		reply.setRawReply(ca.getTime().toString() + "  Start Upload:");
			
		this.reply.showReply();
		FTPProcess.mAnotherThreadDone = true;
		if (FTPProcess.mDisconnect == true) 
		{			
			Log.i("FtpClient.java","FTP is disconnected");
			FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
					.obtainMessage(FTPProcess.Status_MSG);
			FTPProcess.mErrorMsg.sendToTarget();
		}
		else 
		{
			if (FTPProcess.mTestMethod == 1)
			{
				FTPProcess.mAnotherThreadDone = false;
				passloop = false;
				FTPProcess.mThroughput=0;
		
				if (FTPProcess.mFTPULFile == null) {
					Log.i("FtpClient.java","Upload file name not set");
					reply.setRawReply("Upload file name not set");
					this.reply.showReply();
				} else {
					ca = Calendar.getInstance();
					FTPProcess.mStartTime = ca.getTime().toString()
							.substring(11, 19);

					Log.i("FtpClient.java","upload file:"+FTPProcess.mFTPLocalPath + "/"
							+ FTPProcess.mFTPULFile);
					isOk = this.stor(FTPProcess.mFTPLocalPath + "/"
							+ FTPProcess.mFTPULFile);
					FTPProcess.mFTPState=true;
					Log.i("FtpClient.java","UL isok:"+isOk);
					
					if (isOk && !FTPProcess.mDisconnect) {
						if (FTPProcess.mEndTime == null)
							FTPProcess.mEndTime = FTPProcess.mStartTime;
						
						Log.i("FtpClient.java",FTPProcess.mCycle
								+ ";   " + FTPProcess.mStartTime + ";   "
								+ FTPProcess.mEndTime + ";   " + "UL" + ";   "
								+ FTPProcess.mFTPULFile + ";   "
								+ FTPProcess.mFileSize + ";   "
								+ FTPProcess.mThroughput + ";   "
								+ FTPProcess.mFTPNote + ";   " + "\r\n");
						//FTPProcess.mULPass++;
						passloop = isOk;
					} else {
						reply.setRawReply("UL fail");
						this.reply.showReply();
						Log.i("FtpClient.java", "UL fail, mDisconnect:"+FTPProcess.mDisconnect);
						FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
								.obtainMessage(FTPProcess.Upload_MSG);
						FTPProcess.mErrorMsg.sendToTarget();
					}
				}
				
			}
		}
	}	
	
	public boolean cdup() {
		this.executeCommand(new ChangeToParentDirectory());
		return this.checkReplyCode(250);
	}

	public boolean checkReplyCode(int code) {
		if (code == reply.getReplyCode())
			return true;
		return false;
	}

	public boolean cwd(String path) {
		this.executeCommand(new ChangeWorkingDirectory(path));
		return this.checkReplyCode(250);
	}

	public boolean dele(String file) {
		this.executeCommand(new Delete(file));
		return this.checkReplyCode(250);
	}

	@Override
	public void executeCommand(ICommand s) {
		Log.i("FtpClient.java", "Request: " + s.toString());
		super.executeCommand(s);

		reply.setRawReply(getsvreply());
		if (this.isAutoReply)
			this.showReply();
	}

	public String getsvreply() // get reply from server with check flag
	{
		boolean message = false;
		String serverreply = null;
		int timeoutcount = 0;
		// long replyTimeout=0;
		FTPProcess.mFTPNote = "";
		Log.i("FtpClient.java", "mDis:"+FTPProcess.mDisconnect+", dual :"+FTPProcess.mDaulDisconnect);
		if (FTPProcess.mDisconnect&& FTPProcess.mDaulDisconnect) {
			return new String("Disconnect!\r\n");
		}
		while (!message && (!FTPProcess.mDisconnect && !FTPProcess.mDaulDisconnect)) {
			serverreply = getResponse();
			Log.i("FtpClient.java", "server reply:"+serverreply);
			if (serverreply.length() > 0) {
				message = true;
			} else {
				if (timeoutcount > 300) {
					message = true;
					return new String(
							"426 Connection closed; transfer aborted.");
				}
				try {
					timeoutcount++;
					Log.i("FtpClient.java", "getsvreply running");
					Thread.sleep(100);
				} catch (Exception ex) {

				}
			}
		}
		if(serverreply!=null)
			return new String(serverreply);
		else 
			return new String("");
	}

	public void executeCommandNoReply(ICommand s) {
		super.executeCommand(s);
		// reply.setRawReply("");
	}

	public boolean login(String id, String passwd) {
		this.executeCommand(new UserName(id));
		if (this.checkReplyCode(331)) {
			this.executeCommand(new Password(passwd));
			if (this.checkReplyCode(230)) {
				// auto set type i
				FTPProcess.mConnected = true;
				// cleanRawBuffer();
				// this.typeI();
				return true;
			}
			return false;
		} else if (this.checkReplyCode(230)) {
			FTPProcess.mConnected = true;
			// cleanRawBuffer();
			// this.typeI();
			return true;
		} else {
			FTPProcess.mConnected = false;
			return false;
		}

	}

	

	public boolean loginAsAnonymous() {
		return login("anonymous", "");
	}

	public boolean logout() {
		this.executeCommand(new Logout());

		FTPProcess.mConnected = false;
		return this.checkReplyCode(221);
	}

	public boolean ls() {
		return ls(null);
	}

	public boolean ls(String path) {
		FTPProcess.mListOK = false;
		int port = pasv();
		// check pasv
		if (!this.checkReplyCode(227)) {
			return false;
		}
		typeA();
		List ls = null;
		if (path == null || "".equals(path)) {
			ls = new List();
		} else {
			ls = new List(path);
		}
		this.executeCommandNoReply(ls);

		IDataConnector dcon = this.getDataConnector(port);
		FileReceiver rec = new FileReceiver(dcon);

		// check connection

		this.reply.setRawReply(getsvreply());

		if (this.checkReplyCode(150) || this.checkReplyCode(125)) {
			if (this.isAutoReply) {
				this.reply.showReply();
			}
			
			isOk = rec.savelsfile();
			dcon.close(); // close the data channel connection
			if (this.checkReplyCode(150)) {
				this.reply.setRawReply(getsvreply());
				if (this.isAutoReply) {
					this.reply.showReply();
				}
			}

			FTPProcess.mListOK = true;
			return true;
			

		}
		try {
			dcon.close();
		} catch (Exception e) {
			FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
					.obtainMessage(FTPProcess.Status_MSG);
			FTPProcess.mErrorMsg.sendToTarget();
			Log.i("FtpClient.java", "FTPProcess.Connect exception happened");
		}
		FTPProcess.mListOK = true;
		return false;
	}

	public boolean mkd(String dir) {
		this.executeCommand(new MakeDirectory(dir));
		return this.checkReplyCode(257);
	}

	public boolean move(String fr, String to) {
		this.executeCommand(new RenameFrom(fr));
		if (this.checkReplyCode(350)) {
			this.executeCommand(new RenameTo(to));
			return this.checkReplyCode(250);
		}
		return false;
	}

	public int pasv() {
		Log.i("pasv()", "enter pasv()");
		int port = -1;
		int repeat = 0;
		String pasvreply = null;
		executeCommand(new Passive());
		while (port == -1) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			port = this.reply.parsePassivePort();
			if (port == -1) {
				pasvreply = getResponse();
				Log.i("pasv()", "pasvreply :" + pasvreply);
				if (pasvreply.length() > 0) {
					reply.setRawReply(new String(pasvreply));
				}
			}
			repeat++;
			if (FTPProcess.mDisconnect == true || repeat == 5) {
				break;
			}
		}
		return port;
	}

	public boolean pwd() {
		this.executeCommand(new PrintWorkingDirectory());
		String pwd = reply.getRawReply();
		String[] splitpwd;
		String path = null;
		if (pwd != null) {
			try {
				splitpwd = pwd.split("\"");
				if (splitpwd[1] != null && splitpwd[1].length() > 1) {
					path = splitpwd[1] + "/";
				} else if (splitpwd[1] != null) {
					path = splitpwd[1];
				} else {
					Log.i("FtpClient.java", "error in pwd() happened");
				}
				Log.i("FtpClient.java", splitpwd[1] + "," + path);
				return this.checkReplyCode(257);
			} catch (Exception e) {
				ca = Calendar.getInstance();
				FTPProcess.mEndTime = ca.getTime().toString().substring(11, 19);

				FTPProcess.mErrorMsg = FTPProcess.mErrorHandler
						.obtainMessage(FTPProcess.Status_MSG);
				FTPProcess.mErrorMsg.sendToTarget();
				Log.i("FtpClient.java", "error in pwd() happened");
				return false;
			}
		}
		return false;
	}

	public boolean retr(String file) { // download file from server
		int port = pasv();

		// check pasv
		if (!this.checkReplyCode(227)) {
			return false;
		}

		this.typeI();
		this.executeCommandNoReply(new Retrieve(file));
		Log.i("FtpClient.java", "DL port=" + String.valueOf(port));
		IDataConnector dcon = this.getDataConnector(port);
		FileReceiver rec = new FileReceiver(dcon);

		if (FTPProcess.mDisconnect) {
			try {
				dcon.close();
				rec = null;
				System.gc();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		// check connection

		this.reply.setRawReply(getsvreply());

		if (this.checkReplyCode(150) || this.checkReplyCode(125)) {
			// System.out.println(new String(b) + " " + b.length);
			if (this.isAutoReply) {
				this.reply.showReply();
			}
			// long downloadsize=1049902;
			long downloadsize = this.reply.parseDownloadfilesize();
			FTPProcess.mDownloadFileSize = downloadsize;
			// long downloadsize=FTPProcess.mDownloadFileSize;
			Log.i("FtpClient.java",
					"prepare to download:" + FTPProcess.mFTPDLFile + ","
							+ String.valueOf(FTPProcess.mDownloadFileSize));
			rec.savefile(FTPProcess.mFTPDLFile, downloadsize);
			dcon.close();
			rec = null;
			System.gc();
			File dlfile = new File(FTPProcess.mFTPLocalPath + "/"
					+ FTPProcess.mFTPDLFile);
			Log.i("FtpClient.java", "download file: " + dlfile.getName()
					+ ", size:" + dlfile.length());
			this.reply.setRawReply(getsvreply());
			if (this.isAutoReply) {
				this.reply.showReply();
			}

			if (FTPProcess.mDisconnect) {
				try {
					dcon.close();
					rec = null;
					System.gc();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			} else if (dlfile.length() < downloadsize)
				return false;
			return true;
		}
		return false;
	}

	public boolean rmd(String dir) {
		this.executeCommand(new RemoveDirectory(dir));
		return this.checkReplyCode(250);
	}

	public void showReply() {
		if (this.reply.getRawReply().trim().length() != 0)
			this.reply.showReply();
	}

	public boolean stor(File file) {
		// Upload file to server
		int port = pasv();

		// check pasv
		if (!this.checkReplyCode(227)) {
			return false;
		}

		this.typeI();

		this.executeCommandNoReply(new Store(FTPProcess.mFTPULPath
				+ file.getName()));

		Log.i("FtpClient.java", "UL port=" + String.valueOf(port));
		IDataConnector dcon = this.getDataConnector(port);
		FileDeliverer deliver = new FileDeliverer(dcon, file);

		// check connection
		
		this.reply.setRawReply(getsvreply());
		while(!this.checkReplyCode(150)&&!this.checkReplyCode(125) && !FTPProcess.mDisconnect && !FTPProcess.mDaulDisconnect)
        {
            Log.i("FtpClient.java", "It's not 150.");
			this.reply.setRawReply(getsvreply());
        }
        if( FTPProcess.mDisconnect )
        {
            try
            {
                dcon.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return false;
        }
        
        if (this.isAutoReply)
        {
            this.reply.showReply();
        }
        deliver.run();
        
        this.reply.setRawReply(getsvreply());
            
            if (this.checkReplyCode(226)) {
                if (this.isAutoReply)
                {
                    this.reply.showReply();
                }
            return true;
            }
        return false;
	
	}

	public boolean stor(String file) {
		return stor(new File(file));
	}

	public boolean typeI() {
		this.executeCommand(RepresentationType.getImageType());
		return this.checkReplyCode(200);
	}

	public boolean typeA() {
		this.executeCommand(RepresentationType.getASCIIWithNonPrintForm());
		return this.checkReplyCode(200);
	}
	
}
