package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileLogger
{
	private static final String LOG_FILE = "/var/log/keysrv/keysrv.log";
	
	private static FileHandler fh;
	private static Logger logger;

	private static void openLogFile ()
	{
		logger = Logger.getLogger ("keysrv");
		logger.setUseParentHandlers(false);

		try
		{
			fh = new FileHandler (LOG_FILE, true);
			logger.addHandler (fh);
			logger.setLevel (Level.ALL);
			SimpleFormatter formatter = new SimpleFormatter ();
			fh.setFormatter (formatter);
		}
		catch (SecurityException e)
		{
			e.printStackTrace ();
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
	}
	
	private static void closeLogFile ()
	{
		logger.removeHandler (fh);
		fh.close ();
	}
	
	public static String stackTraceToString (Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		
		return sw.toString ();
	}

	public static void logException (Exception e)
	{
		openLogFile ();
		
		logger.log (Level.SEVERE, "\n" + stackTraceToString (e));
		
		closeLogFile ();
	}
	
	public static void logMessage (String msg)
	{
		openLogFile ();
		
		logger.log (Level.INFO, msg);
		
		closeLogFile ();
	}
}
