package com.android.backuptool;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import android.util.Log;

public class SFTPClient implements Runnable{
	public static final String LOG_TAG = "SFTPClient";
	public int TEST_METHOD=0;
	public ChannelSftp channelSftp=null;
	private boolean createNewDirectory;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		switch (TEST_METHOD) {
			case SFTPProcess.COMMAND_CONNECT:
				Log.i(LOG_TAG, "enter COMMAND_CONNECT");
				try {
					channelSftp = createSession();
					SFTPProcess.mConnected=true;
					SFTPProcess.mHandler.obtainMessage(
							SFTPProcess.START_UPLOAD).sendToTarget();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e(LOG_TAG, "create session fail:"+e.getMessage());
					SFTPProcess.mErrorMsg = SFTPProcess.mErrorHandler
							.obtainMessage(SFTPProcess.Status_MSG);
					SFTPProcess.mErrorMsg.sendToTarget();						
				}
				break;
			case SFTPProcess.COMMAND_UPLOAD:
				Log.i(LOG_TAG, "enter COMMAND_UPLOAD");
				if(uploadFiles(channelSftp))
				{
					SFTPProcess.mMessage=SFTPProcess.mMessageHandler.obtainMessage(SFTPProcess.UPLOAD_FINISH);
					SFTPProcess.mMessage.sendToTarget();
				}
				else
				{
					Log.i(LOG_TAG,"FTPProcess.Upload exception happened-");
				}
				break;
			case SFTPProcess.COMMAND_DISCONNECT:
				Log.i(LOG_TAG, "enter COMMAND_DISCONNECT");
				if(!destroySession(channelSftp))
				{
					//disconnect again
					destroySession(channelSftp);
				}
						
				
		}
	}
	
	
	public ChannelSftp createSession() throws Exception{   
        JSch jsch = new JSch();   
        Session session = null;   
        Channel channel = null;   
        ChannelSftp c = null;   
        try {   
            session = jsch.getSession(SFTPProcess.mFTPUser, SFTPProcess.mFTPAddress, SFTPProcess.mFTPPort);   
            session.setPassword(SFTPProcess.mFTPPassword);   
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
            Log.e(LOG_TAG,"Unable to connect to SFTP server: "+e.toString());   
            throw e;   
        }   
	}
	
	//Destroy SFTP Session   
    public boolean destroySession(ChannelSftp c){   
        try {   
        		if(c!=null)
        		{
        			c.quit();   
        			Log.i(LOG_TAG,"SFTP disconnected...");
        			SFTPProcess.mConnected=false;
        			SFTPProcess.mMessageHandler.obtainMessage(SFTPProcess.FTP_DISCONNECTED).sendToTarget();
        			return true;
        		}
        		else
        		{
        			Log.i(LOG_TAG,"c is null");
        			return false;
        		}
        } catch (Exception exc) {   
        		Log.e(LOG_TAG,"Unable to disconnect from SFTP server: " + exc.toString());
        		return false;
        }    
    }   
    
    public boolean uploadFiles(ChannelSftp c){   
        createNewDirectory = true;   
        if(SFTPProcess.mFTPULFile!=null && !SFTPProcess.mFTPULFile.equalsIgnoreCase("")){    
                try{   
       
                	if(c!=null)
                	{
	                    c.cd(SFTPProcess.mFTPULDefaultPath); 
	                   // String defDir = c.pwd();
	                    //Log.i(LOG_TAG,"defDir:"+defDir);
	                    //Vector files = c.ls(defDir);  
	                    Vector files = c.ls(SFTPProcess.mFTPULDefaultPath);   
	                    for (int i=0; i<files.size(); i++) {   
	                        com.jcraft.jsch.ChannelSftp.LsEntry lsEntry = (com.jcraft.jsch.ChannelSftp.LsEntry) files.get(i);
	                        Log.i(LOG_TAG,"file:"+lsEntry.getFilename());
	                        if (!lsEntry.getFilename().equals(".") && !lsEntry.getFilename().equals("..")) {   
	                            if (lsEntry.getFilename().equalsIgnoreCase(SFTPProcess.mFTPULWorkingDir))   
	                                createNewDirectory = false;   
	                        }   
	                    }   
	                    if(createNewDirectory)   
	                    {
		                    String[] folders = SFTPProcess.mFTPULWorkingDir.split( "/" );
		                    for ( String folder : folders ) {
		                    	Log.i(LOG_TAG,"folder:"+folder);
		                        if ( folder.length() > 0 ) {
		                            try {
		                                c.cd( folder );
		                            }
		                            catch ( SftpException e ) {
		                            	Log.i(LOG_TAG,"not found. Creat a folder.");
		                                c.mkdir( folder );
		                                c.cd( folder );
		                            }
		                        }
		                    }
	                    }
	                    else
	                    	 c.cd(SFTPProcess.mFTPULWorkingDir);  
	                    /*
	                    if(createNewDirectory)   
	                        c.mkdir(SFTPProcess.mFTPULWorkingDir);   
	                    c.cd(SFTPProcess.mFTPULWorkingDir);   
	           */
	                    try {   
	                        File f = new File(SFTPProcess.mFTPULFile);   
	                        c.put(new FileInputStream(f), f.getName()); 
	                        Log.i(LOG_TAG,"Upload file sucessfully...");
	                        return true;
	                    } catch (Exception e) {   
	                    	Log.e(LOG_TAG,"Storing remote file failed: "+e.toString()); 
	                    	e.printStackTrace();
	                    	
	                    }  
                	}
                	else
                	{
                		//connect fail
                		SFTPProcess.mErrorMsg = SFTPProcess.mErrorHandler
    							.obtainMessage(SFTPProcess.Status_MSG);
    					SFTPProcess.mErrorMsg.sendToTarget();
    					return false;
                	}

                } catch (Exception e) {   
                	Log.e(LOG_TAG,"Error in Upload Attachments Module. Error Description : " + e.toString());   
                    e.printStackTrace();   
                   
                }   
                //upload fail
                SFTPProcess.mErrorMsg = SFTPProcess.mErrorHandler
        				.obtainMessage(SFTPProcess.Upload_MSG);
        		SFTPProcess.mErrorMsg.sendToTarget();
               
        } 
        else
        {
        	Log.i(LOG_TAG,"Upload file not set");
        	//upload file not set->disconnect
        	destroySession(channelSftp);
        }
        return false;
       
    }   


}
