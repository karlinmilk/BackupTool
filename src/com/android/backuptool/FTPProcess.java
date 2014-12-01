package com.android.backuptool;

import java.io.File;
import java.util.ArrayList;

import com.android.backuptool.ftp.FtpClient;
import com.android.backuptool.utils.BackupValue;
import com.android.backuptool.utils.Logger;

import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FTPProcess {
	public static final int EVENT_START_CYCLE = 1;
	public static final int EVENT_START_TEST = 2;
	public static final int EVENT_FTP_DISCONNECT = 3;
	public static final int EVENT_TEST_FAILED = 4;
	public static final int EVENT_TEST_END = 5;
	public static final int EVENT_TEST_LIFE = 6;
	public static final int EVENT_ADD_LOG = 10;
	public static final int TIMEOUT_START_VALUE = 0x03000000;
	public static final int CYCLEINTERVAL_TIMER = TIMEOUT_START_VALUE + 1;
	public static final int DATAOUT_TIMER = TIMEOUT_START_VALUE + 2;
	public static final int FTP_ALARM_START_VALUE = 500;
	public static final int ALARM_IDLE_TIME = FTP_ALARM_START_VALUE + 1;

	public static final int COMMAND_CONNECT = 1;
	public static final int COMMAND_DISCONNECT = 2;
	public static final int COMMAND_UPLOAD = 3;
	public static final int COMMAND_SETTINGS = 4;
	public static final int COMMAND_DUAL_TEST = 5;

	public static final String FTP_IDLE_ALARM = "com.android.TestSuite.FTP.FTP_IDLE_ALARM";

	public static int mCycle=1;
	public static int mTotalLoop;
	public static int mULPass;
	public static int mDLPass;

	// FTP Settings
	public static String mFTPAddress;
	public static String mFTPUser;
	public static String mFTPPassword;
	public static String mFTPULPath;
	public static String mFTPULFile;
	public static String mFTPDLPath;
	public static String mFTPDLFile;
	public static String mFTPLocalPath;
	public static String mFTPLogMethod;
	public static long mFTPBuffer=64 * 1024;
	public static long mFTPTimeout=60*1000;
	public static String mFTPClientMethod;
	public static int mTestMethod = 1; //Upload

	//
	public static boolean mConnected = false;
	public static boolean mListOK = true;
	public static boolean mDisconnect = false;
	public static boolean mLogProtectLock = false;
	public static boolean mDLProtectLock = false;
	public static boolean mULProtectLock = false;
	public static boolean mDualThreadLock = false;

	public static Long mDownloadFileSize;
	public static long mCurrentSize = 0;

	/*
	 * log information
	 */
	public static String mStartTime;
	public static String mEndTime;
	public static String mDualStartTime;
	public static String mDualEndTime;
	public static String mFTPNote = "";
	public static long mFileSize;
	public static float mThroughput;
	public static float mTotalULThroughput;
	public static float mTotalDLThroughput;
	public static boolean mAnotherThreadDone;
	public static boolean mAnotherThreadWaiting;
	public static boolean mAnotherThreadFinish;
	public static Thread mFTPThread;
	public static Thread mFTPDualThread;

	public static FtpClient mFTPClient = new FtpClient();

	public static FtpClient mFTPDualClient = new FtpClient();
	public static Message mMessage;
	public static Message mDLMessage;
	public static Message mULMessage;
	public static Message mErrorMsg;
	public static final int FLIP_MSG = 1;
	public static final int Status_MSG = 2;
	public static final int Upload_MSG = 3;
	public static final int START_UPLOAD = 4;
	public static final int UPLOAD_FINISH = 5;
	public static final int FTP_DISCONNECTED = 6;
	public static Logger mFTPDetailLogger;
	
	public static Handler mErrorHandler;
	public static Handler mULHandler;
	public static Handler mMessageHandler;
	public static Handler mDLHandler;
	
	static ArrayList<String> mFailReportTime;
	public static boolean mTest;
	public static boolean mDaulDisconnect = false;
	private TelephonyManager mTelephonyManager;
	private WifiManager mWifiManager;
	private static File Datafile;
	private IntentFilter mFTPIntentFilter;
	
	public static boolean mFTPState ;

	public static final int FTP_RESULT_PASS	 	   = 0x00000000;
	public static final int FTP_RESULT_FAIL	 	   = 0x00000001;
	public static final int FTP_DL_FINISHED     = 0x00000002;
	public static final int FTP_UL_FINISHED	   = 0x00000004;
	
	public static final Handler mHandler = new Handler() {
		public synchronized void handleMessage(Message msg) {
			if (msg.what == FTPProcess.START_UPLOAD) {
			
				Log.i(BackupValue.FTP_LOG,"mConnected:"+mConnected);
				if(FTPProcess.mConnected)
				{
					mFTPClient.TEST_METHOD = COMMAND_UPLOAD;
					mFTPThread = new Thread(mFTPClient);
					mFTPThread.start();
				}
			}
		}
		
	};
	
	public void startProcess()
	{
		mDisconnect = false;
		mDaulDisconnect = false;
		mListOK = true;
		mFTPClient.TEST_METHOD = COMMAND_CONNECT;
		mFTPThread = new Thread(mFTPClient);
		mFTPThread.start();
		
	}
	
	public void startUpload()
	{
		mFTPClient.TEST_METHOD = COMMAND_UPLOAD;
		mFTPThread = new Thread(mFTPClient);
		mFTPThread.start();
	}
	
	public void startDisconnect()
	{
		mFTPClient.TEST_METHOD = COMMAND_DISCONNECT;
		mFTPThread = new Thread(mFTPClient);
		mFTPThread.start();
	}
	

}
