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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.rexmenpara.birthdays.models.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Calendar helper class which performs operations on user calendar
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class CalendarHelper {

	private class ACalendar {
		public static final String KEY_ID = "_id";
		public static final String KEY_NAME = "name";
		public static final String KEY_DISPLAYNAME = "displayName";
		public static final String KEY_COLOR = "color";
		public static final String KEY_ACCESS_LEVEL = "access_level";
		public static final String KEY_ACTIVE = "selected";
		public static final String KEY_TIMEZONE = "timezone";
		public static final String KEY_OWNER_ACCOUNT = "ownerAccount";
		public static final String KEY_REMINDER_TYPE = "reminder_type";
		public static final String KEY_REMINDER_DURATION = "reminder_duration";
		public static final String KEY_SYNC_ACCOUNT = "_sync_account";
		public static final String KEY_SYNC_ACCOUNT_TYPE = "_sync_account_type";
		public static final String KEY_SYNC_TIME = "_sync_time";
		public static final String KEY_URL = "url";
		public static final String KEY_SYNC_EVENTS = "sync_events";
		public static final String KEY_SYNC_SOURCE = "sync_source";
		public static final String KEY_DISPLAY_ORDER = "displayOrder";
	}

	private class AEvent {
		public static final String KEY_ID = "_id";
		public static final String KEY_CALENDAR_ID = "calendar_id";
		public static final String KEY_TITLE = "title";
		public static final String KEY_DESCRIPTION = "description";
		public static final String KEY_BEGIN = "dtstart";
		public static final String KEY_END = "dtend";
		public static final String KEY_ALLDAY = "allDay";
		public static final String KEY_REPEAT_RULE = "rrule";
		public static final String KEY_EVENT_TIME_ZONE = "eventTimezone";
		public static final String KEY_DURATION = "duration";
	}

	static String contentProvider;
	static Uri remindersUri;
	static Uri eventsUri;
	static Uri calendars;
	private final Context context;
	private String timezone = null;

	public CalendarHelper() {

		if (Build.VERSION.SDK_INT >= 8)
			contentProvider = "com.android.calendar";
		else
			contentProvider = "calendar";

		remindersUri = Uri.parse(String.format("content://%s/reminders",
				contentProvider));
		eventsUri = Uri.parse(String.format("content://%s/events",
				contentProvider));
		calendars = Uri.parse(String.format("content://%s/calendars",
				contentProvider));
		this.context = ContextManager.getContext();

		this.timezone = getCurrentTimeZoneString();
	}

	public Cursor getAllCalendars() {
		String[] projection = new String[] { ACalendar.KEY_ID,
				ACalendar.KEY_NAME };

		Cursor cursor = context.getContentResolver().query(calendars,
				projection, null, null, null);

		return cursor;
	}

	public String createCalendar(String name) {
		// Check if a calendar application exists
		PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(
					"com.android.providers.calendar",
					PackageManager.GET_META_DATA);

		} catch (NameNotFoundException e) {
			return null;
		}

		// Remove old calendars and events
		resetCalendars();

		ContentValues calendar = new ContentValues();

		calendar.put(ACalendar.KEY_NAME, name);
		calendar.put(ACalendar.KEY_DISPLAYNAME, name);
		calendar.put(ACalendar.KEY_ACCESS_LEVEL, 700);
		calendar.put(ACalendar.KEY_TIMEZONE, this.timezone);
		calendar.put(ACalendar.KEY_OWNER_ACCOUNT, "Birthdays");
		calendar.put(ACalendar.KEY_ACTIVE, 1);
		calendar.put(ACalendar.KEY_SYNC_EVENTS, 1);
		calendar.put(ACalendar.KEY_COLOR, -5159922);
		calendar.put(ACalendar.KEY_SYNC_ACCOUNT, "Birthdays");
		calendar.put(ACalendar.KEY_SYNC_ACCOUNT_TYPE, "com.google");
		Uri result = context.getContentResolver().insert(calendars, calendar);

		String something = result.getLastPathSegment();

		calendar = new ContentValues();

		// Handle the fields only supported by HTC Sense
		try {
			calendar.put(ACalendar.KEY_SYNC_SOURCE, 3);
			calendar.put(ACalendar.KEY_DISPLAY_ORDER, 0);
			calendar.put(ACalendar.KEY_SYNC_TIME, new Date().getTime());
			calendar.put(ACalendar.KEY_URL, "http://www.rakshitmenpara.com");
			int rowsUpdated = context.getContentResolver().update(result,
					calendar, null, null);
			context.getContentResolver().
			Log.d(Constants.TAG, String.valueOf(rowsUpdated));
		} catch (Exception e) {
			// Ignore
		}

		return something;
	}

	public boolean verifyCalendar(String id) {
		String[] projection = new String[] { ACalendar.KEY_ID };

		Cursor cursor = context.getContentResolver().query(calendars,
				projection, ACalendar.KEY_ID + "=" + id, null, null);

		boolean result = cursor.moveToFirst();
		cursor.close();

		return result;
	}

	public void resetCalendars() {
		// Get IDs of all the calendars created by the application
		String[] projection = new String[] { ACalendar.KEY_ID };

		Cursor cursor = context.getContentResolver().query(
				calendars,
				projection,
				ACalendar.KEY_SYNC_ACCOUNT_TYPE + "='" + Constants.PACKAGE
						+ "'", null, null);

		try {
			// Delete all the events
			while (cursor.moveToNext()) {
				int calendarId = cursor.getInt(0);

				Cursor eventCursor = context.getContentResolver().query(
						eventsUri, new String[] { AEvent.KEY_ID },
						AEvent.KEY_CALENDAR_ID + "=" + calendarId, null, null);

				while (eventCursor.moveToNext()) {
					int eventId = eventCursor.getInt(0);
					Uri tmpUri = Uri.withAppendedPath(eventsUri, String
							.valueOf(eventId));
					context.getContentResolver().delete(tmpUri, null, null);
				}
				eventCursor.close();

				// Delete all the calendars
				Uri tmpUri = Uri.withAppendedPath(calendars, String
						.valueOf(calendarId));
				context.getContentResolver().delete(tmpUri, null, null);
			}

			cursor.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String createEvent(String calendarId, String name, String birthday) {
		ContentValues event = new ContentValues();

		// Process the birth date and add year and convert it into UNIX time
		String something = null;
		try {
			Date date = DateUtility.parseDate(birthday);
			Date now = new Date();
			date.setYear(now.getYear());

			if (date.before(now)) {
				date.setYear(date.getYear() + 1);
			}

			long unixTimeStart = Date.UTC(date.getYear(), date.getMonth(), date
					.getDate(), 0, 0, 0);

			event.put(AEvent.KEY_CALENDAR_ID, Integer.parseInt(calendarId));
			event.put(AEvent.KEY_TITLE, name);
			event.put(AEvent.KEY_DESCRIPTION, name + "'s Birthday");
			event.put(AEvent.KEY_BEGIN, unixTimeStart);
			event.put(AEvent.KEY_ALLDAY, 1);
			event.put(AEvent.KEY_EVENT_TIME_ZONE, "UTC");
			event.put(AEvent.KEY_DURATION, "P1D");
			event.put(AEvent.KEY_REPEAT_RULE, "FREQ=YEARLY;WKST=MO");
			Uri result = context.getContentResolver().insert(eventsUri, event);

			something = result.getLastPathSegment();

		} catch (ParseException e) {
			Log.e(Constants.TAG,
					"An error occured while parsing the birthdate.", e);
		}

		return something;
	}

	public void updateEvent(String eventId, String calendarId, String name,
			String birthday) {
		ContentValues event = new ContentValues();

		// Process the birth date and add year and convert it into UNIX time
		try {
			Date date = DateUtility.parseDate(birthday);
			Date now = new Date();
			date.setYear(now.getYear());

			if (date.before(now)) {
				date.setYear(date.getYear() + 1);
			}

			long unixTimeStart = date.getTime();

			event.put(AEvent.KEY_CALENDAR_ID, Integer.parseInt(calendarId));
			event.put(AEvent.KEY_TITLE, name);
			event.put(AEvent.KEY_DESCRIPTION, name + "'s Birthday");
			event.put(AEvent.KEY_BEGIN, unixTimeStart);
			event.put(AEvent.KEY_ALLDAY, 1);
			event.put(AEvent.KEY_EVENT_TIME_ZONE, this.timezone);
			event.put(AEvent.KEY_DURATION, "P1D");
			event.put(AEvent.KEY_REPEAT_RULE, "FREQ=YEARLY;WKST=MO");

			Uri tmpUri = Uri.withAppendedPath(eventsUri, eventId);
			context.getContentResolver().update(tmpUri, event, null, null);

		} catch (ParseException e) {
			Log.e(Constants.TAG,
					"An error occured while parsing the birthdate.", e);
		}
	}

	public void deleteEvent(String eventId) {
		context.getContentResolver().delete(eventsUri,
				AEvent.KEY_ID + "=" + eventId, null);
	}

	public boolean verifyEvent(String id) {
		String[] projection = new String[] { ACalendar.KEY_ID };

		Cursor cursor = context.getContentResolver().query(eventsUri,
				projection, AEvent.KEY_ID + "=" + id, null, null);

		boolean result = cursor.moveToFirst();
		cursor.close();

		return result;
	}

	private String getCurrentTimeZoneString() {
		String[] ids = TimeZone.getAvailableIDs();
		for (String id : ids) {
			if (TimeZone.getTimeZone(id).equals(TimeZone.getDefault())) {
				return id;
			}
		}
		return null;
	}
}
