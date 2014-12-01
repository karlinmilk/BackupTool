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

import com.android.backuptool.FTPProcess;

public class Timer  implements Runnable{
	/** Rate at which timer is checked */
	protected int m_rate = 100;
	
	/** Length of timeout */
	private int m_length;

	/** Time elapsed */
	private int m_elapsed;
   private long fileSize;
	/**
	  * Creates a timer of a specified length
	  * @param	length	Length of time before timeout occurs
	  */
	public Timer ( int length )
	{
		// Assign to member variable
		m_length = length;

		// Set time elapsed
		m_elapsed = 0;
		fileSize=-1;
	}

	
	/** Resets the timer back to zero */
	public synchronized void reset()
	{
		m_elapsed = 0;
	}

	/** Performs timer specific code */
	public void run()
	{
		// Keep looping
		for (;;)
		{
			// Put the timer to sleep
			try
			{ 
				Thread.sleep(m_rate);
			}
			catch (InterruptedException ioe) 
			{
				continue;
			}
			
		if(FTPProcess.mCurrentSize==fileSize)
		{
					// Use 'synchronized' to prevent conflicts
					synchronized ( this )
					{
						// Increment time remaining
						m_elapsed += m_rate;
		
						// Check to see if the time has been exceeded
						if (m_elapsed > m_length)
						{
							// Trigger a timeout
							timeout();
							break;
						}
					}
		}
		else
		{
			fileSize=FTPProcess.mCurrentSize;
			m_elapsed = 0;
		}

		}
	}

	// Override this to provide custom functionality
	public void timeout()
	{
		FTPProcess.mFTPNote="Timeout";
	}
}
