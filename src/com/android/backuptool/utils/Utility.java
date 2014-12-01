package com.android.backuptool.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.util.Log;

public class Utility {
	
	private final static String MEM_FILE_PATH = "/proc/meminfo";
	private final static String MEMORY_TOTAL = "MemTotal";
	private final static String MEMORY_FREE = "MemFree";
	private final static String CACHED = "Cached";
	
	public static long getTimeInMillis ()
	{
		return	System.currentTimeMillis();
	}
	
	public static String convertMillisToDate (long time, String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		Calendar mCalendar = Calendar.getInstance ();
		
		mCalendar.setTimeInMillis(time);

		return	formatter.format(mCalendar.getTime());
	}
	public static String convertMillisToHours (int time)
	{
		int sec, min, hour;
		sec=time/1000;
		hour = sec / 3600;
		sec -= hour * 3600;
		min = sec / 60;
		sec -= min * 60;
		String format=ID2id(hour)+":"+ID2id(min)+":"+ID2id(sec);
		return	format;
	}
	private static String ID2id(int time) {
		// TODO Auto-generated method stub
		return ("" + ((time > 9) ? time : ("0" + time)));
	}
	
	public static String convertMillisToDate (long time)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		
		Calendar mCalendar = Calendar.getInstance ();
		
		mCalendar.setTimeInMillis(time);

		return	formatter.format(mCalendar.getTime());
	}
	
	public static long convertDateToMillis (String dateString, String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		Date date = null;
		
		try {
			date = formatter.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}

		return	date.getTime();
	}
	
	public static String getTime ()
	{
		Calendar mCalendar = Calendar.getInstance ();

		return	ID2id (mCalendar.get (Calendar.HOUR_OF_DAY)) + "." +
			ID2id (mCalendar.get (Calendar.MINUTE)) + "." +
			ID2id (mCalendar.get (Calendar.SECOND));
	}
	public static String getElapsedTime(long startTime, long currentTime)
	{
		String elapsedTime = "";
		Log.i("MTUtility","start time="+startTime+", end time="+currentTime);
		long timeMillis = currentTime - startTime;
		long time = timeMillis / 1000;
		Log.i("MTUtility","total timemillis :"+timeMillis+", time: "+time);
		String seconds = Integer.toString((int)(time % 60));
		String minutes = Integer.toString((int)((time % 3600) / 60));
		String hours = Integer.toString((int)(time / 3600));
		Log.i("MTUtility","time = "+hours+": "+minutes+": "+seconds);
		for (int i = 0; i < 2; i++) 
		{
			if (seconds.length() < 2) 
				seconds = "0" + seconds;
			if (minutes.length() < 2) 
				minutes = "0" + minutes;
			if (hours.length() < 2)
				hours = "0" + hours;
		}
		
		if(!hours.equals("00"))
			elapsedTime = hours + " hr ";
		if(!minutes.equals("00"))
			elapsedTime = elapsedTime + minutes + " min ";
		if(!seconds.equals("00"))
			elapsedTime = elapsedTime + seconds + " sec";
		if(elapsedTime=="")
			elapsedTime = elapsedTime + "0 sec";
		return elapsedTime;
	}
	
	public static String getOrdinalNumber(String counter)
    {
    	int currentCounter = Integer.parseInt(counter.substring(
    			counter.length()-1, counter.length()));
    		
    	if(currentCounter == 1)
    		return counter + "st.";
    	else if(currentCounter == 2)
    		return counter + "nd.";
    	else if(currentCounter == 3)
    		return counter + "rd.";
    	else
    		return counter + "th.";
    }
    
	public static String getOrdinalNumber(int counter)
    {
    	String counterString = String.valueOf(counter);
    	
    	int currentCounter = Integer.parseInt(counterString.substring(
    			counterString.length()-1, counterString.length()));
    		
    	if(currentCounter == 1)
    		return counter + "st.";
    	else if(currentCounter == 2)
    		return counter + "nd.";
    	else if(currentCounter == 3)
    		return counter + "rd.";
    	else
    		return counter + "th.";
    }
	
	public static long getInitialMemorySize()
	{
		long totalMemory = 0;
		String memFile = readAttrFile(MEM_FILE_PATH);
		String[] memFileParse = memFile.split("\n");
		
		if(memFileParse.length>0)
		{
			for(int i = 0; i < memFileParse.length; i++)
			{
				if(memFileParse[i].contains(MEMORY_TOTAL))
				{
					String tMem = memFileParse[i].substring(
						memFileParse[i].indexOf(":")+1, memFileParse[i].length()-2).trim();
					totalMemory = (long) Integer.parseInt(tMem);
					
					Log.d ("mytest", "	totalMemory: " + totalMemory);
				}
			}
		}
		return totalMemory;
	}
	
	public static long getMemFree()
	{
		long currentFreeMemory = 0;
		String memFile = readAttrFile(MEM_FILE_PATH);
		String[] memFileParse = memFile.split("\n");
		
		if(memFileParse.length>0)
		{
			for(int i = 0; i < memFileParse.length; i++)
			{
				if(memFileParse[i].contains(MEMORY_FREE))
				{
					String cMem = memFileParse[i].substring(
						memFileParse[i].indexOf(":")+1, memFileParse[i].length()-2).trim();
					currentFreeMemory = (long) Integer.parseInt(cMem);
					break;
				}
			}
		}
		
		return currentFreeMemory;
	}
	
	public static long getCached()
	{
		long cached = 0;
		String memFile = readAttrFile(MEM_FILE_PATH);
		String[] memFileParse = memFile.split("\n");
		
		if(memFileParse.length>0)
		{
			for(int i = 0; i < memFileParse.length; i++)
			{
				if(memFileParse[i].contains(CACHED))
				{
					String cMem = memFileParse[i].substring(
						memFileParse[i].indexOf(":")+1, memFileParse[i].length()-2).trim();
					cached = (long) Integer.parseInt(cMem);
					break;
				}
			}
		}
		
		return cached;
	}
	
	public static float calculateMemoryLeak(long initMemFree, long memFree, long cached)
	{
		Log.i ("mytest", "	initMem: " + initMemFree);
		Log.i ("mytest", "	currentMem: " + memFree);
		Log.i ("mytest", "	cached: " + cached);
		
		return ((float)(initMemFree - memFree - cached)/(float)initMemFree)*100;
	}

	
	public static String readAttrFile (String filename)
	{
		String ret = "";

		try
		{
			BufferedReader reader = new BufferedReader (new FileReader (filename));

			try
			{
				String line = "";
				while((line = reader.readLine()) != null)
					ret = ret + line + "\n";
			}
			finally
			{
				reader.close ();
			}
		}
		catch (IOException e)
		{
			ret = new String ();
		}
		return ret;
	}
	
    /**
     * Creates the beep MediaPlayer in advance so that the sound can be triggered with the least
     * latency possible.
     */
    public static void playBeepSound(Context context, int id) 
    {
    	MediaPlayer mp = MediaPlayer.create(context, id);
	    mp.start();
    }
    
    public static ArrayList<String> regularExpressionGrab(String content, String regexpress)
	{
		ArrayList<String> result = new ArrayList<String>();
		Pattern p = Pattern.compile(regexpress);
		Matcher m = p.matcher(content);
		while (m.find()) {
			result.add(m.group(1).trim());
		}
		
		
		return result;
	}
  public static void WarningDialog(Context context, String message)
  {
    	new AlertDialog.Builder(context)
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
  public static boolean copyStreams(InputStream is, FileOutputStream fos) 
	{
     boolean success = true;
     int BUFSIZE=1024;
     BufferedOutputStream os = null;
     try {
         byte data[] = new byte[BUFSIZE];
         int count;
         os = new BufferedOutputStream(fos, BUFSIZE);
         while ((count = is.read(data, 0, BUFSIZE)) != -1) {
             os.write(data, 0, count);
         }
         os.flush();
     } catch (IOException e) {
         Log.e("test", "Exception while copying: " + e);
         success = false;
     } finally {
         try {
             if (os != null) {
                 os.close();
             }

             if (is != null) is.close();

         } catch (IOException e2) {
        	 Log.e("test", "Exception while closing the stream: " + e2);
             success = false;
         }
     }

     return success;
 }
}
