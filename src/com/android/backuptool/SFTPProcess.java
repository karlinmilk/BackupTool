package com.android.backuptool;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.backuptool.ftp.FtpClient;
import com.android.backuptool.utils.BackupValue;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SFTPProcess {
	public static final String LOG_TAG = "SFTPProcess";
	
	public static final int COMMAND_CONNECT = 1;
	public static final int COMMAND_DISCONNECT = 2;
	public static final int COMMAND_UPLOAD = 3;
	
	public static final int Status_MSG = 0;
	public static final int Upload_MSG = 1;
	public static final int START_UPLOAD = 2;
	public static final int UPLOAD_FINISH = 3;
	public static final int FTP_DISCONNECTED = 4;
	
	public static boolean mConnected = false;
	
	// SFTP Settings
	public static String mFTPAddress;
	public static String mFTPUser;
	public static String mFTPPassword;
	public static String mFTPULDefaultPath;
	public static String mFTPULWorkingDir; //每次都不同,從BackupService去更改
	public static String mFTPULFile;
	public static int mFTPPort=22;
	
	public static Thread mSFTPThread;
	public static SFTPClient mSFTPClient = new SFTPClient();
	public static Handler mErrorHandler;
	public static Message mErrorMsg;
	public static Handler mMessageHandler;
	public static Message mMessage;
	
	/*
	private boolean createNewDirectory;   
    private int ftpPort;   
    private String ftpHost;   
    private String ftpUserName;   
    private String ftpPassword;    
    private String ftpRemoteDefaultDirectory;   
    private String workingDirectory;   
    private String workingFile;   
    */
	
    public static final Handler mHandler = new Handler() {
		public synchronized void handleMessage(Message msg) {
			if (msg.what == SFTPProcess.START_UPLOAD) {
			
				Log.i(BackupValue.FTP_LOG,"mConnected:"+mConnected);
				if(SFTPProcess.mConnected)
				{
					mSFTPClient.TEST_METHOD = COMMAND_UPLOAD;
					mSFTPThread = new Thread(mSFTPClient);
					mSFTPThread.start();
				}
			}
		}
		
	};

	/*
    public SFTPProcess(String ftpHost, String ftpUserName, String ftpPassword, String workingFile, String ftpRemoteDefaultDirectory, String workingDirectory) {   
        this.ftpPort = 22;   
        this.workingFile = workingFile;   
        this.ftpHost = ftpHost;   
        this.ftpUserName = ftpUserName;   
        this.ftpPassword = ftpPassword;   
        this.ftpRemoteDefaultDirectory = ftpRemoteDefaultDirectory;   
        this.workingDirectory = workingDirectory;   
    }   

    //Establish SFTP Session   
    public ChannelSftp createSession() throws Exception{   
            JSch jsch = new JSch();   
            Session session = null;   
            Channel channel = null;   
            ChannelSftp c = null;   
            try {   
                session = jsch.getSession(this.ftpUserName, this.ftpHost, this.ftpPort);   
                session.setPassword(this.ftpPassword);   
                java.util.Properties config = new java.util.Properties();   
                config.put("StrictHostKeyChecking", "no");   
                session.setConfig(config);   
                session.connect();   
                channel = session.openChannel("sftp");   
                channel.connect();   
                c = (ChannelSftp)channel; 
                Log.i(LOG_TAG,"SFTP connected...");
                return c;   
            } catch (Exception e) {   
                Log.e(LOG_TAG,"Unable to connect to FTP server. "+e.toString());   
                throw e;   
            }   
    }
    
    //Destroy SFTP Session   
    public void destroySession(ChannelSftp c){   
        try {   
            c.quit();   
            Log.i(LOG_TAG,"SFTP disconnected...");
        } catch (Exception exc) {   
        	Log.e(LOG_TAG,"Unable to disconnect from FTP server. " + exc.toString());   
        }    
    }   
       
    //Upload Attachment   
    public void uploadFiles(){   
        createNewDirectory = true;   
        if(workingFile!=null){   
            if (!workingFile.equalsIgnoreCase("")){   
                try{   
                    ChannelSftp c = createSession();         
                    c.cd(ftpRemoteDefaultDirectory); 
                    
                    Vector files = c.ls(ftpRemoteDefaultDirectory);   
                    for (int i=0; i<files.size(); i++) {   
                        com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) files.get(i);
                        Log.i(LOG_TAG,"file:"+lsEntry.getFilename());
                        if (!lsEntry.getFilename().equals(".") && !lsEntry.getFilename().equals("..")) {   
                            if (lsEntry.getFilename().equalsIgnoreCase(workingDirectory))   
                                createNewDirectory = false;   
                        }   
                    }   
                    if(createNewDirectory)   
                        c.mkdir(workingDirectory);   
                    c.cd(workingDirectory);   
           
                    try {   
                        File f = new File(workingFile);   
                        c.put(new FileInputStream(f), f.getName()); 
                        Log.i(LOG_TAG,"Upload file sucessfully...");
                    } catch (Exception e) {   
                    	Log.e(LOG_TAG,"Storing remote file failed. "+e.toString());   
                        throw e;   
                    }      
                    destroySession(c);   
                } catch (Exception e) {   
                	Log.e(LOG_TAG,"Error in Upload Attachments Module. Error Description : " + e.toString());   
                    e.printStackTrace();   
                }   
            }   
        }   
    }   
	 */

	public void startProcess()
	{

		mSFTPClient.TEST_METHOD = COMMAND_CONNECT;
		mSFTPThread = new Thread(mSFTPClient);
		mSFTPThread.start();
		
	}
	
	public void startUpload()
	{
		mSFTPClient.TEST_METHOD = COMMAND_UPLOAD;
		mSFTPThread = new Thread(mSFTPClient);
		mSFTPThread.start();
	}
	
	public void startDisconnect()
	{
		mSFTPClient.TEST_METHOD = COMMAND_DISCONNECT;
		mSFTPThread = new Thread(mSFTPClient);
		mSFTPThread.start();
	}
}
