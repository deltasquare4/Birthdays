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

	private static final String TAG = "Birthdays";

	private class Calendar {
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
		public static final String KEY_SYNC_EVENTS = "sync_events";
		public static final String KEY_SYNC_SOURCE = "sync_source";
		public static final String KEY_DISPLAY_ORDER = "displayOrder";
	}

	private class Event {
		public static final String KEY_ID = "_id";
		public static final String KEY_CALENDAR_ID = "calendar_id";
		public static final String KEY_TITLE = "title";
		public static final String KEY_DESCRIPTION = "description";
		public static final String KEY_BEGIN = "dtstart";
		public static final String KEY_END = "dtend";
		public static final String KEY_ALLDAY = "allDay";
		public static final String KEY_REPEAT_RULE = "rrule";
	}

	static String contentProvider;
	static Uri remindersUri;
	static Uri eventsUri;
	static Uri calendars;
	private final Context context;

	public CalendarHelper(Context context) {

		if (Build.VERSION.RELEASE.contains("2.2"))
			contentProvider = "com.android.calendar";
		else
			contentProvider = "calendar";

		remindersUri = Uri.parse(String.format("content://%s/reminders",
				contentProvider));
		eventsUri = Uri.parse(String.format("content://%s/events",
				contentProvider));
		calendars = Uri.parse(String.format("content://%s/calendars",
				contentProvider));
		this.context = context;
	}

	public Cursor getAllCalendars() {
		String[] projection = new String[] { Calendar.KEY_ID, Calendar.KEY_NAME };

		Cursor cursor = context.getContentResolver().query(calendars,
				projection, null, null, null);

		return cursor;
	}

	public String createCalendar(String name) {
		// Check if a calendar is already created
		PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(
					"com.android.providers.calendar",
					PackageManager.GET_META_DATA);

		} catch (NameNotFoundException e) {
			return null;
		}

		ContentValues calendar = new ContentValues();

		String[] ids = TimeZone.getAvailableIDs();
		String timezone = null;
		for (String id : ids) {
			if (TimeZone.getTimeZone(id).equals(TimeZone.getDefault())) {
				timezone = id;
			}
		}

		calendar.put(Calendar.KEY_NAME, name);
		calendar.put(Calendar.KEY_DISPLAYNAME, name);
		calendar.put(Calendar.KEY_ACCESS_LEVEL, 700);
		calendar.put(Calendar.KEY_TIMEZONE, timezone);
		calendar.put(Calendar.KEY_OWNER_ACCOUNT, "Birthdays");
		calendar.put(Calendar.KEY_ACTIVE, 1);
		calendar.put(Calendar.KEY_SYNC_EVENTS, 1);
		calendar.put(Calendar.KEY_COLOR, -5159922);
		calendar.put(Calendar.KEY_SYNC_ACCOUNT, "Birthdays");
		calendar
				.put(Calendar.KEY_SYNC_ACCOUNT_TYPE, "com.rexmenpara.birthdays");
		Uri result = null;
		try {
			result = context.getContentResolver().insert(calendars, calendar);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		String something = result.getLastPathSegment();

		calendar = new ContentValues();

		// Handle the fields only suppored by HTC Sense
		try {
			calendar.put(Calendar.KEY_SYNC_SOURCE, 1);
			calendar.put(Calendar.KEY_DISPLAY_ORDER, 0);
			int rowsUpdated = context.getContentResolver().update(result,
					calendar, Calendar.KEY_ID + "=" + something, null);
		} catch (Exception e) {
			// Ignore
		}

		return something;
	}

	public String createEvent(String calendarId, String name, String birthday) {
		ContentValues event = new ContentValues();

		// Process the birth date and add year and convert it into UNIX time
		String something = null;
		SimpleDateFormat srcFormatter = new SimpleDateFormat("yyyy-MM-dd");

		try {
			Date date = srcFormatter.parse(birthday);
			Date now = new Date();
			date.setYear(now.getYear());

			if (date.before(now)) {
				date.setYear(date.getYear() + 1);
			}

			long unixTime = date.getTime();
			event.put(Event.KEY_CALENDAR_ID, Integer.parseInt(calendarId));
			event.put(Event.KEY_TITLE, name);
			event.put(Event.KEY_DESCRIPTION, name + "'s Birthday");
			event.put(Event.KEY_BEGIN, unixTime);
			event.put(Event.KEY_END, unixTime);
			event.put(Event.KEY_ALLDAY, 1);
			Uri result = context.getContentResolver().insert(eventsUri, event);

			something = result.getLastPathSegment();

		} catch (ParseException e) {
			Log.e(TAG, "An error occured while parsing the birthdate.", e);
		}

		return something;
	}

	public void updateEvent(String eventId, String name, String birthday) {
		ContentValues event = new ContentValues();

		// Process the birth date and add year and convert it into UNIX time
		SimpleDateFormat srcFormatter = new SimpleDateFormat("yyyy-MM-dd");

		try {
			Date date = srcFormatter.parse(birthday);
			Date now = new Date();
			date.setYear(now.getYear());

			if (date.before(now)) {
				date.setYear(date.getYear() + 1);
			}

			long unixTime = date.getTime();
			event.put(Event.KEY_TITLE, name);
			event.put(Event.KEY_DESCRIPTION, name + "'s Birthday");
			event.put(Event.KEY_BEGIN, unixTime);
			event.put(Event.KEY_END, unixTime);
			context.getContentResolver().update(eventsUri, event,
					Event.KEY_ID + "=" + eventId, null);

		} catch (ParseException e) {
			Log.e(TAG, "An error occured while parsing the birthdate.", e);
		}
	}

	public void deleteEvent(String eventId) {
		context.getContentResolver().delete(eventsUri,
				Event.KEY_ID + "=" + eventId, null);
	}
}
