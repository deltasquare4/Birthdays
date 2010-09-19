package com.rexmenpara.birthdays;

import com.rexmenpara.birthdays.db.DBAdapter;
import com.rexmenpara.birthdays.models.Constants;
import com.rexmenpara.birthdays.util.CalendarHelper;
import com.rexmenpara.birthdays.util.ContextManager;
import com.rexmenpara.birthdays.util.NotificationManager;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.widget.Toast;

public class CalendarSyncService extends Thread {
	private Handler handler = null;
	private Runnable callback = null;

	public CalendarSyncService(Handler handler, Runnable callback) {
		this.handler = handler;
		this.callback = callback;
	}

	public void run() {
		Context context = ContextManager.getContext();
		DBAdapter db = DBAdapter.getInstance();

		// Get the custom preference
		SharedPreferences preferences = context.getSharedPreferences(
				"birthdayPrefs", Activity.MODE_PRIVATE);

		CalendarHelper helper = new CalendarHelper();

		// Notify the user of calendar syncing
		Notification notification = new Notification(R.drawable.toolbar,
				"Syncing with calendar...", System.currentTimeMillis());
		Intent intent = new Intent(context.getApplicationContext(),
				BirthdaysActivity.class);
		notification.setLatestEventInfo(context, "Birthdays",
				"Syncing Birthdays with the Calendar", PendingIntent
						.getActivity(context, 0, intent,
								PendingIntent.FLAG_CANCEL_CURRENT));
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		NotificationManager.getInstance()
				.notify(Constants.APP_ID, notification);

		String calendarId = preferences.getString("calendarId", null);

		// Create a birthday calendar if not already present
		if (!preferences.contains("calendarId")
				|| !helper.verifyCalendar(calendarId)) {
			createCalendar(preferences, helper);
			calendarId = preferences.getString("calendarId", null);
		}

		try {
			db.open();

			Cursor cursor = db.getAllEntriesForEdit();
			if (cursor != null) {
				// Sync the birthdays with calendar
				while (cursor.moveToNext()) {
					int rowIdCol = cursor.getColumnIndex(DBAdapter.KEY_ROWID);
					long rowId = cursor.getLong(rowIdCol);

					int nameCol = cursor.getColumnIndex(DBAdapter.KEY_NAME);
					String name = cursor.getString(nameCol);

					int dateCol = cursor.getColumnIndex(DBAdapter.KEY_BDAY);
					String origDateString = cursor.getString(dateCol);

					int reminderCol = cursor
							.getColumnIndex(DBAdapter.KEY_REMINDER);
					Integer reminder = cursor.getInt(reminderCol);

					int eventIdCol = cursor
							.getColumnIndex(DBAdapter.KEY_EVENTID);
					String eventId = cursor.getString(eventIdCol);

					if (reminder != 0) {
						if (eventId == null || eventId.trim().equals("")
								|| !helper.verifyEvent(eventId)) {
							// Create the event and get the associated
							// Id
							eventId = helper.createEvent(calendarId, name,
									origDateString);
						} else {
							// Update an event
							helper.updateEvent(eventId, calendarId, name,
									origDateString);
						}
					} else {
						// Remove the event and eventId
						helper.deleteEvent(eventId);
					}

					// Update the event data in the database
					db.updateEntry(rowId, eventId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}

		handler.post(callback);
	}

	private void createCalendar(SharedPreferences preferences,
			CalendarHelper helper) {
		String calId = helper.createCalendar("Birthdays");

		if (calId == null) {
			// Calendar not found. Display a toast and return.
			Toast toast = Toast
					.makeText(ContextManager.getContext(),
							"Calendar is not installed. Aborting...",
							Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("calendarId", calId);
		editor.commit();
	}
}
