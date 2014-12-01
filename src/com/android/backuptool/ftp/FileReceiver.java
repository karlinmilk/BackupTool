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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;
import java.io.*;
import java.util.Calendar;

import com.android.backuptool.FTPProcess;

import android.util.Log;

public class FileReceiver {

	class Block {
		byte[] b;

		public Block(byte[] b) {
			this.setData(b);
		}

		public void setData(byte[] b) {
			this.b = b;
		}

		public byte[] getData() {
			return b;
		}
	}

	private final Vector v = new Vector();

	public int count = 10;
	public long downlordsize = -1;
	public long delay = 50;

	public boolean isOk = false;
	public long fileSize = 0;
	private FileOutputStream downloadfile;
	private byte cbuf[] = new byte[4096];
	private InputStream ibuf = null;
	private OutputStream obuf = null;
	private int len;
	private final IDataConnector con;
	private Reply r = new Reply();
	private byte[] b;

	public FileReceiver(IDataConnector conn) {
		this.con = conn;
	}

	public void savefile(String filename, long downloadsize) {

		fileSize = downloadsize;
		long starttime;
		long endtime;
		long checkintime;
		float downloadspeed = 0;
		long downFileSize = downloadsize / 5;
		int sizeitem = 1;
		r.setRawReply(String.valueOf(downloadsize - fileSize));
		r.showDlStatus();
		try {
			downloadfile = new FileOutputStream(FTPProcess.mFTPLocalPath + "/"+ filename);
			ibuf = new BufferedInputStream(con.getInputStream());
			obuf = new BufferedOutputStream(downloadfile);
			starttime = System.currentTimeMillis();
			checkintime = starttime + 1000;

			Log.i("FileReceiver.java","download start time =" + String.valueOf(starttime));
			while (fileSize > 0) {
				// b = con.get();
				// if (b.length != 0)
				// {
				// downloadfile.write(b);
				// fileSize=fileSize-b.length;
				// }
				try {
					if ((len = ibuf.read(cbuf, 0, 4096)) > 0) {
						obuf.write(cbuf, 0, len);
						obuf.flush();
						fileSize = fileSize - len;
						//downFileSize = downFileSize - len;
					}
					/*
					 * display current download status
					 */
					if((downloadsize-fileSize)>=(downFileSize*sizeitem)){
					//if (downFileSize <= 0) {
						
						Log.i("FileReceiver.java","size item:"+sizeitem);
						FTPProcess.mMessage = FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
						FTPProcess.mMessage.getData().putString("log","Download Size(DL) :"
								+ String.valueOf(sizeitem*2) + "0%\n");
						FTPProcess.mMessage.sendToTarget();
						//downFileSize = downloadsize / 5;
						sizeitem += 1;
					}
					if (checkintime < System.currentTimeMillis()) {
						// r.setRawReply(String.valueOf(downloadsize-fileSize));
						r.showDlStatus();
						// Log.i("FileReceiver.java","isConnected="+String.valueOf(con.isConnected())+",current timemill="+String.valueOf(checkintime));
						checkintime = System.currentTimeMillis() + 1000;
						if (FTPProcess.mDisconnect == true) {
							break;
						}
					}

					// r.setRawReply(String.valueOf(downloadsize-fileSize));
					if (fileSize <= 0) {
						/*sizeitem += 1;
						FTPProcess.mMessage = FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
						FTPProcess.mMessage.getData().putString("log","Download Size(DL) :"
								+ String.valueOf(sizeitem*2) + "0%\n");
						FTPProcess.mMessage.sendToTarget();*/
						endtime = System.currentTimeMillis();
						downloadspeed = ((float) (downloadsize - fileSize) / 1024)
								/ ((float) (endtime - starttime) / 1000);
						Log.i("FileReceiver.java","download end time =" + String.valueOf(endtime)
								+ ",speed="+ String.valueOf(downloadspeed));
						
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			try {
				con.close();
				downloadfile.close();
				ibuf.close();
				obuf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			b = null;
			while (FTPProcess.mLogProtectLock) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
			}
			FTPProcess.mLogProtectLock = true;
			FTPProcess.mFileSize = downloadsize;
			FTPProcess.mThroughput = downloadspeed;
			FTPProcess.mTotalDLThroughput = FTPProcess.mTotalDLThroughput+ downloadspeed;
			FTPProcess.mEndTime = Calendar.getInstance().getTime().toString().substring(11, 19);
			// FTPProcess.mEndTime=Calendar.getInstance().getTime().toString().substring(12,19);
			FTPProcess.mMessage = FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
			FTPProcess.mMessage.getData().putString("log",
					"Download Speed:" + String.valueOf(downloadspeed) + "KB/s");
			// FTPProcess.mMessage.getData().putString("log","Download Speed:"+String.valueOf(downloadspeed)+"KB/s\n");
			FTPProcess.mMessage.sendToTarget();
		} catch (Exception ex) {

		}

	}

	public void run() { // put message from server to device log window
		byte[] b = con.get();
		if (b.length > 0) {
			this.fileSize += b.length;
			v.add(new Block(b));
			count = 10;
		}
		if (b.length == 0) {
			count--;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// FTPProcess.timerThread=new Thread(FTPProcess.timerRunnable);
				// // TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (count > 0) {
			run();
		} else {
			this.isOk = true;
		}
	}

	public synchronized boolean savelsfile() {
		BufferedReader ibuf = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		File templist = new File("/sdcard/ext_sd/parser");
		if (!templist.exists())
			templist = new File("/sdcard/parser");
		if (ibuf != null) {
			try {
				FileWriter svWriter = new FileWriter(templist);
				do {
					line = ibuf.readLine();
					if (line != null) {
						Log.i("FileReceiver.java", line);
						if (line.length() != 0) {
							svWriter.write(line + "\r\n");
							svWriter.flush();
						}
					}
				} while (line != null);
				svWriter.close();
				return true;
			} catch (Exception e) {
				Log.i("FileReceiver.java", "error happened in save_ls_file");
				e.printStackTrace();
			}
		}
		return false;
	}

	public byte[] get() {
		int length = 0;
		for (int i = 0; i < v.size(); i++) {
			length += ((Block) v.get(i)).b.length;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);

		try {
			for (int i = 0; i < v.size(); i++) {
				bos.write(((Block) v.get(i)).b);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bos.toByteArray();
	}

	protected void finalize() {
		this.con.close();
	}

}
