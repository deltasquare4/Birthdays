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

package com.rexmenpara.birthdays.db;

import com.rexmenpara.birthdays.util.ContextManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {
	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_CONTACTID = "contact_id";
	public static final String KEY_BDAY = "bday";
	public static final String KEY_REMINDER = "reminder";
	public static final String KEY_EVENTID = "event_id";
	public static final String KEY_LASTSYNC = "last_sync";
	private static final String TAG = "DBAdapter";

	private static final String DATABASE_NAME = "birthdays";
	private static final String DATABASE_TABLE = "birthdays";
	private static final int DATABASE_VERSION = 2;

	public static final int BIRTHDAY_REMINDER_TRUE = 1;
	public static final int BIRTHDAY_REMINDER_FALSE = 0;

	private static final String DATABASE_CREATE = "CREATE TABLE "
			+ DATABASE_TABLE + " (" + KEY_ROWID
			+ " integer primary key autoincrement, " + KEY_NAME
			+ " text not null, " + KEY_CONTACTID + " integer not null, "
			+ KEY_BDAY + " text not null, " + KEY_REMINDER
			+ " integer not null, " + KEY_EVENTID + " text, " + KEY_LASTSYNC
			+ " integer);";

	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;
	private static DBAdapter dbAdapter;

	public static DBAdapter getInstance() {
		if (dbAdapter == null) {
			dbAdapter = new DBAdapter();
		}
		return dbAdapter;
	}

	private DBAdapter() {
		DBHelper = new DatabaseHelper(ContextManager.getContext());
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Opens the database
	 * 
	 * @return
	 * @throws SQLException
	 */
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Closes the database
	 */
	public void close() {
		DBHelper.close();
	}

	/**
	 * Insert an entry into the database
	 * 
	 * @param name
	 * @param contactId
	 * @param birthday
	 * @param reminder
	 * @return
	 */
	public long insertEntry(String name, Integer contactId, String birthday,
			Integer reminder) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_CONTACTID, contactId);
		initialValues.put(KEY_BDAY, birthday);
		initialValues.put(KEY_REMINDER, reminder);
		initialValues.put(KEY_LASTSYNC,
				(int) (System.currentTimeMillis() / 1000L));
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Insert an entry into the database with associated eventId
	 * 
	 * @param name
	 * @param contactId
	 * @param birthday
	 * @param reminder
	 * @param eventId
	 * @return
	 */
	public long insertEntry(String name, Integer contactId, String birthday,
			Integer reminder, String eventId) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_CONTACTID, contactId);
		initialValues.put(KEY_BDAY, birthday);
		initialValues.put(KEY_REMINDER, reminder);
		initialValues.put(KEY_EVENTID, eventId);
		initialValues.put(KEY_LASTSYNC,
				(int) (System.currentTimeMillis() / 1000L));
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Deletes a particular entry
	 * 
	 * @param rowId
	 * @return
	 */
	public boolean deleteEntry(long rowId) {
		return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Synchronizes an entry with existing ones if available. Adds if not
	 * available.
	 * 
	 * @param name
	 * @param contactId
	 * @param birthday
	 * @param reminder
	 */
	public void syncEntries(String name, Integer contactId, String birthday,
			Integer reminder) {
		Integer rowId = hasEntry(contactId);

		if (rowId != -1) {
			updateEntry(rowId, name, contactId, birthday);
		} else {
			insertEntry(name, contactId, birthday, reminder);
		}
	}

	/**
	 * Retrieves all the entries for view page
	 */
	public Cursor getAllEntriesForView() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				KEY_CONTACTID, KEY_BDAY, KEY_REMINDER }, null, null, null,
				null, null);
	}

	/**
	 * Retrieves all the entries for edit page
	 */
	public Cursor getAllEntriesForEdit() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				KEY_CONTACTID, KEY_BDAY, KEY_REMINDER, KEY_EVENTID }, null,
				null, null, null, KEY_NAME + " ASC");
	}

	/**
	 * Retrieves a particular entry
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public Cursor getEntry(long rowId) throws SQLException {
		Cursor mCursor = db.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_NAME, KEY_CONTACTID, KEY_BDAY, KEY_EVENTID,
				KEY_REMINDER }, KEY_ROWID + "=" + rowId, null, null, null,
				null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Checks if a contact entry already exists
	 * 
	 * @param contactId
	 * @return
	 */
	public Integer hasEntry(Integer contactId) {
		Integer result = -1;
		Cursor mCursor = db.query(false, DATABASE_TABLE,
				new String[] { KEY_ROWID }, KEY_CONTACTID + "=" + contactId,
				null, null, null, null, null);

		try {
			if (mCursor != null && mCursor.getCount() > 0) {
				mCursor.moveToFirst();
				result = mCursor.getInt(0);
			}
		} finally {
			mCursor.close();
		}

		return result;
	}

	public boolean getReminder(Integer contactId) {
		boolean result = true;
		Cursor mCursor = db.query(false, DATABASE_TABLE,
				new String[] { KEY_ROWID }, KEY_CONTACTID + "=" + contactId,
				null, null, null, null, null);

		try {
			if (mCursor != null && mCursor.getCount() > 0) {
				mCursor.moveToFirst();
				int resInt = mCursor.getInt(0);
				result = (resInt == 1);
			}
		} finally {
			mCursor.close();
		}

		return result;
	}

	/**
	 * Updates an entry
	 * 
	 * @param rowId
	 * @param name
	 * @param contactId
	 * @param birthday
	 * @param reminder
	 * @return
	 */
	public boolean updateEntry(long rowId, String name, Integer contactId,
			String birthday) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_CONTACTID, contactId);
		args.put(KEY_BDAY, birthday);
		args.put(KEY_LASTSYNC, (int) (System.currentTimeMillis() / 1000L));
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateEntry(long rowId, String eventId) {
		ContentValues args = new ContentValues();
		args.put(KEY_EVENTID, eventId);
		args.put(KEY_LASTSYNC, (int) (System.currentTimeMillis() / 1000L));
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Updates an entry from Edit activity
	 * 
	 * @param rowId
	 * @param name
	 * @param contactId
	 * @param birthday
	 * @param reminder
	 * @return
	 */
	public boolean updateEntryFromEdit(long rowId, String birthday,
			Integer reminder) {
		ContentValues args = new ContentValues();
		String eventId = null;
		args.put(KEY_BDAY, birthday);
		args.put(KEY_REMINDER, reminder);

		// Remove associated eventId if reminder is disabled
		if (reminder == 0) {
			args.put(KEY_EVENTID, eventId);
		}
		args.put(KEY_LASTSYNC, (int) (System.currentTimeMillis() / 1000L));
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Removes all the entries older than specified timestamp
	 * 
	 * @param timestamp
	 */
	public boolean cleanUp(Integer timestamp) {
		return db.delete(DATABASE_TABLE, KEY_LASTSYNC + "<" + timestamp, null) > 0;
	}
}
