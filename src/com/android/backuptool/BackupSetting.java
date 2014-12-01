package com.android.backuptool;

import com.android.backuptool.utils.BackupValue;
import com.android.backuptool.utils.SettingPreferenceActivity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class BackupSetting extends SettingPreferenceActivity  {
	public static final String LOG_TAG = "BackupTool";
	public static final String VERSION = "0.1.0.6";

	//BackupTool enable status
	public static boolean mEnable=false;
	
	public static NotificationManager mNotiManager;
	public static Notification mNotify;
	private final int                    NOTIFICATION_ID         = 1938;
	
	public static EditTextPreference mFtpAddrEdit;
	public static EditTextPreference mFtpUserEdit;
	public static EditTextPreference mFtpPasswordEdit;
	public static EditTextPreference mFtpULPathEdit;
	public static EditTextPreference mPeriodEdit;
	public static EditTextPreference mTimeoutEdit;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG,"on create BackupTool:"+VERSION);
        addPreferencesFromResource(R.layout.main);   
        
        mFtpAddrEdit = (EditTextPreference) findPreference(BackupValue.FTP_ADDR);
        mFtpUserEdit = (EditTextPreference) findPreference(BackupValue.FTP_USERNAME);
        mFtpPasswordEdit = (EditTextPreference) findPreference(BackupValue.FTP_PASSWORD);
        mFtpULPathEdit = (EditTextPreference) findPreference(BackupValue.FTP_UL_PATH);
        mPeriodEdit = (EditTextPreference) findPreference(BackupValue.BACKUP_PERIOD);
        mTimeoutEdit = (EditTextPreference) findPreference(BackupValue.TIMEOUT);
        
        mFtpAddrEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        mFtpUserEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        mFtpPasswordEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        mFtpULPathEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        mPeriodEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        mTimeoutEdit.setOnPreferenceChangeListener(mPreferenceChangeListener);
        
        mNotiManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    
    protected OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			SharedPreferences sp = getSharedPreferences("BackupTool",
					MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
			Editor ed = sp.edit();
			if(preference==mFtpAddrEdit)
			{
				if(newValue==null || newValue.toString().length()<=0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.FTP_ADDR, newValue.toString());
			}
			else if(preference==mFtpUserEdit)
			{
				if (newValue == null || newValue.toString().length() <= 0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.FTP_USERNAME, newValue.toString());
			}
			else if(preference==mFtpPasswordEdit)
			{
				if (newValue == null || newValue.toString().length() <= 0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.FTP_PASSWORD, newValue.toString());
			}
			else if(preference==mFtpULPathEdit)
			{
				if (newValue == null || newValue.toString().length() <= 0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.FTP_UL_PATH, newValue.toString());
			}
			else if(preference==mPeriodEdit)
			{
				if (newValue == null || newValue.toString().length() <= 0
						|| Integer.parseInt(newValue.toString()) <= 0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.BACKUP_PERIOD, newValue.toString());
			}
			else if(preference==mTimeoutEdit)
			{
				if (newValue == null || newValue.toString().length() <= 0
						|| Integer.parseInt(newValue.toString()) <= 0)
					return false;
				preference.setSummary(newValue.toString());
				ed.putString(BackupValue.TIMEOUT, newValue.toString());
			}
			return true;
		}
		
	};





	@Override
	protected void onSaveSettings() {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG,"Save Settings");
		SharedPreferences sp = getSharedPreferences("BackupTool",
				MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
		Editor ed = sp.edit();
		ed.putString(BackupValue.FTP_ADDR, mFtpAddrEdit.getText());
		ed.putString(BackupValue.FTP_USERNAME, mFtpUserEdit.getText());
		ed.putString(BackupValue.FTP_PASSWORD, mFtpPasswordEdit.getText());
		ed.putString(BackupValue.FTP_UL_PATH, mFtpULPathEdit.getText());
		ed.putString(BackupValue.BACKUP_PERIOD, mPeriodEdit.getText());
		ed.putString(BackupValue.TIMEOUT, mTimeoutEdit.getText());
		ed.putBoolean(BackupValue.TOOL_ENABLE_KEY, mEnable);
		ed.commit();
	}


	@Override
	protected void onLoadSettings() {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG,"Load Settings");		
		SharedPreferences sp = getSharedPreferences("BackupTool",
				MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
		mFtpAddrEdit.setText(sp.getString(BackupValue.FTP_ADDR, BackupValue.DEFAULT_FTP_ADDR));
		mFtpAddrEdit.setSummary(mFtpAddrEdit.getText());
		
		mFtpUserEdit.setText(sp.getString(BackupValue.FTP_USERNAME, BackupValue.DEFAULT_FTP_USERNAME));
		mFtpUserEdit.setSummary(mFtpUserEdit.getText());
		
		mFtpPasswordEdit.setText(sp.getString(BackupValue.FTP_PASSWORD, BackupValue.DEFAULT_FTP_PASSWORD));
		mFtpPasswordEdit.setSummary(mFtpPasswordEdit.getText());
		
		mFtpULPathEdit.setText(sp.getString(BackupValue.FTP_UL_PATH, BackupValue.DEFAULT_FTP_UL_PATH));
		mFtpULPathEdit.setSummary(mFtpULPathEdit.getText());
		
		mPeriodEdit.setText(sp.getString(BackupValue.BACKUP_PERIOD, BackupValue.DEFAULT_BACKUP_PERIOD));
		mPeriodEdit.setSummary(mPeriodEdit.getText());
		
		mTimeoutEdit.setText(sp.getString(BackupValue.TIMEOUT, BackupValue.DEFAULT_TIMEOUT));
		mTimeoutEdit.setSummary(mTimeoutEdit.getText());
		
		mEnable=sp.getBoolean(BackupValue.TOOL_ENABLE_KEY, false);
		Log.i(LOG_TAG,"mRuning:"+mEnable);
		setButtonEnabled(!mEnable);
	}


	@Override
	protected void onClearSettings() {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG,"Clear Settings");
		SharedPreferences sp = getSharedPreferences("BackupTool",
				MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
		Editor ed = sp.edit();
		ed.clear();
		ed.commit();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backup_setting, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       super.onOptionsItemSelected(item);
       switch(item.getItemId())
       {
       		case R.id.setting_start:
       			onSaveSettings();
       			startBackupListen();
       			break;
       		case R.id.setting_stop:
       			stopBackupListen();
       			onSaveSettings();
       			break;
       		case R.id.setting_about:
       			AboutDialog("Version :" + VERSION);
       			break;
       }
       return true;
	}
    
    private void AboutDialog(String StrVersion) {
		// TODO Auto-generated method stub
		new AlertDialog.Builder(this).setTitle("About").setMessage(StrVersion)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int btn) {

					}
				}).setCancelable(false).show();
	}
    
    private void startBackupListen(){
    	/*
		Intent intent = new Intent();
		intent.setClassName(BackupSetting.this,BackupService.class.getName());
		startService(intent);
		*/
		setButtonEnabled(false);
		mEnable=true;
		onSaveSettings();
		setNotify(R.drawable.icon_s,"Status:Enable",true,null);
	}
	
	private void stopBackupListen(){
		
		Intent intent = new Intent();
		intent.setClassName(BackupSetting.this,BackupService.class.getName());
		stopService(intent);
		
		setButtonEnabled(true);
		mEnable=false;
		BackupReceiver.cancelAlarm(this,BackupReceiver.BACKUP_RUN_ALARM,BackupReceiver.ALARM_BACKUP);
		setNotify(R.drawable.icon_s,"Status:Disable",false,null);
	}
	
	
	private void setButtonEnabled(boolean state){
		mFtpAddrEdit.setEnabled(state);
		mFtpUserEdit.setEnabled(state);
		mFtpPasswordEdit.setEnabled(state);
		mFtpULPathEdit.setEnabled(state);
		mPeriodEdit.setEnabled(state);
		mTimeoutEdit.setEnabled(state);
	}
	
	public void setNotify(int icon, String msg, boolean visible, PendingIntent pi)
	{
		if (!visible && mNotify == null)
            return;

        if (mNotiManager == null)
            return;
        
        if (visible)
        {
            if (mNotify == null)
            {
            	mNotify = new Notification();
            	mNotify.when = 0;
            }

            if (icon != -1)
            	mNotify.icon = icon;
            mNotify.defaults &= ~Notification.DEFAULT_SOUND;
            mNotify.flags = Notification.FLAG_ONGOING_EVENT;
            mNotify.tickerText = "BackupTool";

            if (pi == null)
            {
                Intent intent = new Intent(this, BackupSetting.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            }

            mNotify.setLatestEventInfo(getApplicationContext(), "BackupTool", msg, pi);
            
        }
        if (visible)
            mNotiManager.notify(NOTIFICATION_ID, mNotify);
        else
            mNotiManager.cancel(NOTIFICATION_ID);
	}
	
    
}
