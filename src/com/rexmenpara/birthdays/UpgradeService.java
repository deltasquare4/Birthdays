package com.rexmenpara.birthdays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

public final class UpgradeService {

	private static final int V1_5_R5 = 8;
	private static final int V1_5 = 5;
	private static final int V1_1 = 4;
	private static final int V1_0 = 1;

	/**
	 * Perform upgrade from one version to the next. Needs to be called on the
	 * UI thread so it can display a progress bar and then show users a change
	 * log.
	 * 
	 * @param from
	 * @param to
	 */
	public void performUpgrade(final Context context, final int from) {

		if (from < V1_5_R5) {
			upgradeFromV15Calendar(context);
		}

		showChangeLog(context, from);
	}

	/**
	 * Return a change log string. Releases occur often enough that we don't
	 * expect change sets to be localized.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public void showChangeLog(Context context, int from) {
		if (!(context instanceof Activity) || from == 0)
			return;

		StringBuilder changeLog = new StringBuilder();

		if (from <= V1_5_R5)
			newVersionString(
					changeLog,
					"1.5.R4 (September 19, 2010)",
					new String[] {
							"Fixed crashs on Droid X.",
							"Fixed the bug where birthday events were a day early",
							"Better calendar cleanup", });
		if (from >= V1_5_R5 && from < V1_5)
			newVersionString(
					changeLog,
					"1.5 (September 7, 2010)",
					new String[] {
							"Fixed the bug where birthday events were a day early",
							"A few minor bugfixes and enhancements", });
		if (from >= V1_5 && from < V1_1)
			newVersionString(
					changeLog,
					"1.1.beta",
					new String[] { "Fixed calendar sync issue on non-HTC devices", });
		if (from >= V1_1 && from < V1_0)
			newVersionString(changeLog, "1.0.beta",
					new String[] { "Initial release", });
		if (changeLog.length() == 0)
			return;

		changeLog.append("Enjoy!</body></html>");
		String changeLogHtml = "<html><body style='color: white'>" + changeLog;

		WebView webView = new WebView(context);
		webView.loadData(changeLogHtml, "text/html", "utf-8");
		webView.setBackgroundColor(0);

		new AlertDialog.Builder(context).setTitle("Latest Changes").setView(
				webView).setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(android.R.string.ok, null).show();
	}

	/**
	 * Helper for adding a single version to the changelog
	 * 
	 * @param changeLog
	 * @param version
	 * @param changes
	 */
	@SuppressWarnings("nls")
	private void newVersionString(StringBuilder changeLog, String version,
			String[] changes) {
		changeLog.append(
				"<font style='text-align: center; color=#ffaa00'><b>Version ")
				.append(version).append(":</b></font><br><ul>");
		for (String change : changes)
			changeLog.append("<li>").append(change).append("</li>\n");
		changeLog.append("</ul>");
	}

	private void upgradeFromV15Calendar(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(
				"birthdayPrefs", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove("calendarId");
		editor.commit();
	}
}