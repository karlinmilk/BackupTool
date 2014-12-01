package com.android.backuptool.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public abstract class SettingPreferenceActivity extends PreferenceActivity {
	public static final String LOG_TAG = "BackupTool";
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	protected void onPause()
	{
		Log.i(LOG_TAG,"onPause");
		// do save values to settings
		super.onPause();
		
		onSaveSettings();
	}
	
	protected void onResume()
	{
		Log.i(LOG_TAG,"onResume");
		super.onResume();
		
		// update values from settings
		onLoadSettings();
	}
	
	public void onDestroy() {
		Log.i(LOG_TAG,"onDestory");
		super.onDestroy();
		onClearSettings();
	}
	
	protected abstract void onSaveSettings();
	protected abstract void onLoadSettings();
	protected abstract void onClearSettings();
	
	protected void WarningDialog(String message)
    {
    	new AlertDialog.Builder(this)
			.setTitle("Warning")
			.setMessage(message)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int btn)
					{
			    	}
				})
			.setCancelable(false)
		.show();
    }
	
	protected void InfoDialog(String message)
    {
    	new AlertDialog.Builder(this)
			.setTitle("Information")
			.setMessage(message)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int btn)
					{
			    	}
				})
			.setCancelable(false)
		.show();
    }


}
