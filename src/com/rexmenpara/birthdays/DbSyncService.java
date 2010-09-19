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

package com.rexmenpara.birthdays;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;

import com.rexmenpara.birthdays.db.DBAdapter;
import com.rexmenpara.birthdays.util.ContextManager;

/**
 * Synchronizes birthdays database with android contacts
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class DbSyncService extends Thread {

	private Handler handler = null;
	private Runnable callback = null;

	public DbSyncService(Handler handler, Runnable callback) {
		this.handler = handler;
		this.callback = callback;
	}

	public void run() {
		Context context = ContextManager.getContext();
		DBAdapter db = DBAdapter.getInstance();

		// Get Contacts with birthday field
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Data.CONTENT_URI,
				new String[] { Event.CONTACT_ID, Event.PHOTO_ID,
						Event.DISPLAY_NAME, Event.START_DATE },
				Event.MIMETYPE + "='" + Event.CONTENT_ITEM_TYPE + "' AND "
						+ Event.TYPE + "=" + Event.TYPE_BIRTHDAY, null,
				Event.DISPLAY_NAME);

		if (cursor.getCount() > 0) {
			db.open();
			Integer timestamp = (int) (System.currentTimeMillis() / 1000L);

			while (cursor.moveToNext()) {
				// Get contactID
				int contactIdCol = cursor.getColumnIndex(Event.CONTACT_ID);
				Integer contactId = cursor.getInt(contactIdCol);

				int nameCol = cursor.getColumnIndex(Event.DISPLAY_NAME);
				String name = cursor.getString(nameCol);

				int dateCol = cursor.getColumnIndex(Event.START_DATE);
				String origDateString = cursor.getString(dateCol);

				// Enter the birthday data in the database
				db.syncEntries(name, contactId, origDateString,
						DBAdapter.BIRTHDAY_REMINDER_TRUE);

			}
			db.cleanUp(timestamp);
		}
		db.close();

		handler.post(callback);
	}

}
