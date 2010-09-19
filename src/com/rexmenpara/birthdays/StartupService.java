package com.rexmenpara.birthdays;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.rexmenpara.birthdays.models.Constants;
import com.rexmenpara.birthdays.util.ErrorReportHandler;
import com.rexmenpara.birthdays.util.Preferences;

public class StartupService {
	public synchronized void onStartupApplication(final Activity mainActivity) {
		// Register Exception Handler
		// Don't register again if already registered
		UncaughtExceptionHandler currentHandler = Thread
				.getDefaultUncaughtExceptionHandler();
		ErrorReportHandler newHandler = new ErrorReportHandler(currentHandler,
				mainActivity);
		if (!(currentHandler instanceof ErrorReportHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(newHandler);
		}

		// Submit pending error reports
		mainActivity.runOnUiThread(newHandler);

		// read current version
		int latestSetVersion = Preferences.getCurrentVersion();
		int version = 0;
		try {

			PackageManager pm = mainActivity.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE,
					PackageManager.GET_META_DATA);
			version = pi.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(Constants.TAG, "Application is not properly installed", e);
		}

		// invoke upgrade service
		boolean justUpgraded = latestSetVersion != version;
		UpgradeService upgradeService = new UpgradeService();
		if (justUpgraded && version > 0) {
			upgradeService.performUpgrade(mainActivity, latestSetVersion);
			Preferences.setCurrentVersion(version);
		}
	}
}
