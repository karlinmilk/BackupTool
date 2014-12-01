package com.android.backuptool.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;
import com.android.backuptool.BackupService;


public class Logger extends RandomAccessFile
{
	public static final String LOG_TAG = "Logger";
	
	public Logger(File file, String mode) throws FileNotFoundException
	{
		super(file, mode);
	}
	
	public Logger(String fileName, String mode) throws FileNotFoundException
	{
		super(fileName, mode);
	}
	
	public void write(String msg)
	{
		print(msg);
	}
	
	public void time_println(String msg)
	{
		Date dt = new Date(System.currentTimeMillis());
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	String time = sdf.format(dt);
		try
		{
			this.seek(this.length());
			this.writeBytes(time + " - " + msg + "\r\n");
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger println(msg): " + e.getMessage());
		}
	}	
    
	public void full_date_println(String msg)
	{
		Date dt = new Date(System.currentTimeMillis());
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    	String time =  sdf.format(dt);
		try
		{
			this.seek(this.length());
			this.writeBytes(time + " - " + msg + "\r\n");
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger println(msg): " + e.getMessage());
		}
    }    
	
	public void println(String msg)
	{
		try
		{
			this.seek(this.length());
			this.writeBytes(msg + "\r\n");
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger println(msg): " + e.getMessage());
		}
	}
	
	public void println()
	{
		try
		{
			this.seek(this.length());
			this.writeBytes("\r\n");
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger println(): " + e.getMessage());
		}
	}
	
	public void time_print(String msg)
	{
		Date dt = new Date(System.currentTimeMillis());
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	String time = sdf.format(dt);
		try
		{
			this.writeBytes(time + " - " + msg);
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger print(msg): " + e.getMessage());
		}
	}
	
	public void print(int val)
	{
		try
		{
			this.seek(this.length());
			this.writeBytes(String.valueOf(val));
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger print(int): " + e.getMessage());
		}
	}
	
	public void print(long val)
	{
		try
		{
			this.seek(this.length());
			this.writeBytes(String.valueOf(val));
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger print(long): " + e.getMessage());
		}
	}
	
	public void print(String msg)
	{
		try
		{
			this.seek(this.length());
			this.writeBytes(msg);
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger print(msg): " + e.getMessage());
		}
	}
	
	public void appendTsLog(String msg)
	{
		try
		{
			this.writeBytes(msg);
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger (msg): " + e.getMessage());
		}
	}
	
	public void flush()
	{
		try
		{
			getFD().sync();
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "flush: " + e.getMessage());
		}	
	}
	
	public void close()
	{
		try
		{
			super.close();
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Logger close(): " + e.getMessage());
		}
	}
}
