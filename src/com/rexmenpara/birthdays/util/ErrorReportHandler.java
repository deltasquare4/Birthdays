/*
 * Copyright 2010 Rakshit Menpara (http://www.rakshitmenpara.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rexmenpara.birthdays.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * An application-level exception handler which catches, records and reports the
 * exceptions thrown by the application in its lifetime.
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class ErrorReportHandler implements UncaughtExceptionHandler, Runnable {

	private UncaughtExceptionHandler mDefaultUEH;
	private Activity mApp;

	public ErrorReportHandler(UncaughtExceptionHandler currentHandler,
			Activity aApp) {
		mDefaultUEH = currentHandler;
		mApp = aApp;
	}

	public void uncaughtException(Thread t, Throwable e) {
		submit(e);
		// do not forget to pass this exception through up the chain
		mDefaultUEH.uncaughtException(t, e);
	}

	public void run() {
		sendDebugReportToAuthor();
	}

	public void submit(Throwable e) {
		String theErrReport = generateDebugReport(e);
		saveDebugReport(theErrReport);

		NotificationManager.getInstance().cancelAll();

		// try to send file contents via email (need to do so via the UI thread)
		mApp.runOnUiThread(this);
	}

	public static void collectData(String data) {
		StringBuilder sb = new StringBuilder();
		sb.append("----------------------------------\n");
		sb.append(data);
		sb.append("----------------------------------\n\n");

		try {
			ErrorReportHandler handler = (ErrorReportHandler) Thread
					.getDefaultUncaughtExceptionHandler();
			handler.saveDebugReport(data);
		} catch (Exception e) {
			// Ignore
		}
	}

	private String generateDebugReport(Throwable e) {
		String report = "";

		report += "-------------------------------\n\n";
		report += "--------- Device ---------\n\n";
		report += "Brand: " + Build.BRAND + "\n";
		report += "Device: " + Build.DEVICE + "\n";
		report += "Model: " + Build.MODEL + "\n";
		report += "Id: " + Build.ID + "\n";
		report += "Product: " + Build.PRODUCT + "\n";
		report += "-------------------------------\n\n";
		report += "--------- Firmware ---------\n\n";
		report += "SDK: " + Build.VERSION.SDK + "\n";
		report += "Release: " + Build.VERSION.RELEASE + "\n";
		report += "Incremental: " + Build.VERSION.INCREMENTAL + "\n";
		report += "-------------------------------\n\n";
		report += "--------- Exception ---------\n\n";
		report += "Message: " + e.getMessage() + "\n";

		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stacktrace = sw.toString();

		report += "StackTrace: " + stacktrace + "\n";
		report += "-------------------------------\n\n";
		report += "--------- Cause ---------\n\n";
		Throwable cause = e.getCause();
		if (cause != null) {
			sw = new StringWriter();
			cause.printStackTrace(new PrintWriter(sw));
			stacktrace = sw.toString();

			report += "Message: " + cause.getMessage() + "\n";
			report += "StackTrace: " + cause.getStackTrace() + "\n";
			report += "-------------------------------\n\n";
		}
		return report;
	}

	private void saveDebugReport(String theErrReport) {

		try {
			FileOutputStream trace = mApp.openFileOutput("stack.trace",
					Context.MODE_PRIVATE);
			trace.write(theErrReport.getBytes());
			trace.close();
		} catch (IOException ioe) {
			// Ignore the error
		}
	}

	private void sendDebugReportToAuthor() {
		try {
			FileInputStream trace = mApp.openFileInput("stack.trace");
			if (trace != null) {
				byte[] buffer = new byte[512];
				StringBuffer stacktrace = new StringBuffer();

				// Read the file
				while (trace.read(buffer, 0, buffer.length) != -1) {
					stacktrace.append(new String(buffer));
				}

				trace.close();

				// create an email intent to send to yourself
				final Intent emailIntent = new Intent(
						android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
						new String[] { "contact@rakshitmenpara.com" });
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						"Birthdays Error Report");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						stacktrace.toString());

				// start the email activity - note you need to start it with a
				// chooser
				mApp.startActivity(Intent.createChooser(emailIntent,
						"Send error report..."));
				mApp.deleteFile("stack.trace");

			}
		} catch (IOException ios) {
			// Ignore the error
			ios.printStackTrace();
		}

	}
}
