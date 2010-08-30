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

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rexmenpara.birthdays.db.DBAdapter;
import com.rexmenpara.birthdays.models.BirthdayBean;
import com.rexmenpara.birthdays.util.BirthdayArrayAdapter;
import com.rexmenpara.birthdays.util.BirthdayComparator;
import com.rexmenpara.birthdays.util.CalendarHelper;

/**
 * The default activity of this application. Presents the user with a contact
 * list with birthdays. TODO: Move database and calendar sync code to separate
 * class
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class BirthdaysActivity extends ListActivity {

	private static final String TAG = "Birthdays";
	private static final int APP_ID = 29871;

	private final Handler mHandler = new Handler();
	private DBAdapter db = null;
	private ProgressDialog dialog = null;
	private NotificationManager mManager;
	private int currentMode = BirthdayArrayAdapter.MODE_VIEW;

	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			updateResultsInUi();
		}
	};

	final Runnable mSyncCalendar = new Runnable() {
		public void run() {
			calendarSynced();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Create a DBAdapter instance
		db = new DBAdapter(this);

		// Detect the first run and ask the user to sync
		SharedPreferences pref = getSharedPreferences(TAG, Context.MODE_PRIVATE);
		int versionCode = pref.getInt("versionCode", -1);

		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(
					"com.rexmenpara.birthdays", PackageManager.GET_META_DATA);

		} catch (NameNotFoundException e) {
			// Ignore it
		}

		int localVersionCode = pInfo.versionCode;

		if (versionCode == -1) { // TODO: For users who installed the
			// application for the first time
			showFirstExecDialog(
					"Thank you for installing Birthdays!\n\nDo you want to load all the birthdays now?",
					pref, localVersionCode);
		} else if (versionCode != localVersionCode) {
			showFirstExecDialog(
					"Thank you for updating Birthdays!\n\nIt is recommended that you sync your contacts again.",
					pref, localVersionCode);
		} else {
			loadData();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		loadData();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		TextView rowId = (TextView) v.findViewById(R.id.rowId);
		CheckBox reminder = (CheckBox) v.findViewById(R.id.chkReminder);
		TextView birthday = (TextView) v.findViewById(R.id.txtBirthday);
		if (this.currentMode == BirthdayArrayAdapter.MODE_VIEW) {
			Bundle args = new Bundle();
			args.putString("birthday", String.valueOf(birthday.getText()));
			args.putBoolean("reminder", reminder.isChecked());
			args.putString("rowId", String.valueOf(rowId.getText()));

			// Display a window where a user can edit birthdays and reminders
			showEditDialog(args);
		} else {
			// Check/uncheck the checkbox associated with the item
			reminder.toggle();

			// Hide the sync icon
			ImageView imgView = (ImageView) v.findViewById(R.id.syncIcon);
			if (reminder.isChecked()) {
				imgView.setVisibility(View.VISIBLE);
			} else {
				imgView.setVisibility(View.INVISIBLE);
			}

			// Save the state
			db.open();
			db.updateEntryFromEdit(Long.parseLong(String.valueOf(rowId
					.getText())), String.valueOf(birthday.getText()), reminder
					.isChecked() ? 1 : 0);
			db.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_page, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (this.currentMode == BirthdayArrayAdapter.MODE_EDIT) {
			menu.setGroupVisible(R.id.menu_grp_main, false);
			menu.setGroupVisible(R.id.menu_grp_edit, true);
		} else {
			menu.setGroupVisible(R.id.menu_grp_main, true);
			menu.setGroupVisible(R.id.menu_grp_edit, false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.sync:
			syncBirthdays();
			return true;
		case R.id.batch_edit:
			onEnterBatchMode();
			return true;
		case R.id.batch_exit:
			onExitBatchMode();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			String birthday = data.getStringExtra("birthday");
			boolean reminder = data.getBooleanExtra("reminder", true);
			Long rowId = data.getLongExtra("rowId", -1);

			db.open();
			db.updateEntryFromEdit(rowId, birthday, reminder ? 1 : 0);
			db.close();

			loadData();
		}
	}

	private void showFirstExecDialog(String message,
			final SharedPreferences pref, final int versionCode) {

		// Show a dialog to automatically sync birthdays
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false).setPositiveButton(
				"Sync Now", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Write current versionCode to the
						// preferences
						SharedPreferences.Editor editor = pref.edit();
						editor.putInt("versionCode", versionCode);
						editor.commit();

						// Synchronize with contacts
						syncBirthdays();
					}
				}).setNegativeButton("I'll Do It Later",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	private void showEditDialog(Bundle args) {
		// Get the arguments passed
		String birthday = args.getString("birthday");
		boolean reminder = args.getBoolean("reminder");
		String rowId = args.getString("rowId");

		// Show the dialog
		Intent dialogIntent = new Intent(this, EditBirthdayDialog.class);
		dialogIntent.putExtra("birthday", birthday);
		dialogIntent.putExtra("reminder", reminder);
		dialogIntent.putExtra("rowId", Long.parseLong(rowId));
		startActivityForResult(dialogIntent, 0);
	}

	private void onEnterBatchMode() {
		BirthdayArrayAdapter adapter = (BirthdayArrayAdapter) getListAdapter();
		adapter.setMode(BirthdayArrayAdapter.MODE_EDIT);
		adapter.notifyDataSetChanged();

		// Set edit mode as current mode
		this.currentMode = BirthdayArrayAdapter.MODE_EDIT;
	}

	private void onExitBatchMode() {
		BirthdayArrayAdapter adapter = (BirthdayArrayAdapter) getListAdapter();
		adapter.setMode(BirthdayArrayAdapter.MODE_VIEW);
		adapter.notifyDataSetChanged();

		// Set view mode as current mode
		this.currentMode = BirthdayArrayAdapter.MODE_VIEW;
	}

	private void syncBirthdays() {

		// Create sync dialog
		dialog = ProgressDialog.show(this, "", "Synchronizing birthdays...",
				true);

		Thread t = new Thread() {
			public void run() {
				// Get Contacts with birthday field
				Cursor cursor = BirthdaysActivity.this.getContentResolver()
						.query(
								ContactsContract.Data.CONTENT_URI,
								new String[] { Event.CONTACT_ID,
										Event.PHOTO_ID, Event.DISPLAY_NAME,
										Event.START_DATE },
								Event.MIMETYPE + "='" + Event.CONTENT_ITEM_TYPE
										+ "' AND " + Event.TYPE + "="
										+ Event.TYPE_BIRTHDAY, null,
								Event.DISPLAY_NAME);

				try {
					if (cursor.getCount() > 0) {
						db.open();
						Integer timestamp = (int) (System.currentTimeMillis() / 1000L);

						while (cursor.moveToNext()) {
							// Get contactID
							int contactIdCol = cursor
									.getColumnIndex(Event.CONTACT_ID);
							Integer contactId = cursor.getInt(contactIdCol);

							int nameCol = cursor
									.getColumnIndex(Event.DISPLAY_NAME);
							String name = cursor.getString(nameCol);

							int dateCol = cursor
									.getColumnIndex(Event.START_DATE);
							String origDateString = cursor.getString(dateCol);

							// Enter the birthday data in the database
							db.syncEntries(name, contactId, origDateString,
									DBAdapter.BIRTHDAY_REMINDER_TRUE);

						}

						db.cleanUp(timestamp);
					}
				} catch (Exception e) {

				} finally {
					db.close();
				}

				mHandler.post(mUpdateResults);
			}
		};

		t.start();
	}

	private void updateResultsInUi() {

		dialog.dismiss();

		loadData();

		// Sync birthdays with calendar
		syncCalendar();
	}

	private void loadData() {

		// Get the list as cursor
		db.open();
		Cursor mainDataCursor = db.getAllEntriesForView();
		ArrayList<BirthdayBean> mainDataList = new ArrayList<BirthdayBean>();

		// Read the cursor into an ArrayList object
		while (mainDataCursor.moveToNext()) {
			BirthdayBean bean = new BirthdayBean();

			int rowIdCol = mainDataCursor.getColumnIndex(DBAdapter.KEY_ROWID);
			bean.setRowId(mainDataCursor.getLong(rowIdCol));
			int nameCol = mainDataCursor.getColumnIndex(DBAdapter.KEY_NAME);
			bean.setName(mainDataCursor.getString(nameCol));
			int contactIdCol = mainDataCursor
					.getColumnIndex(DBAdapter.KEY_CONTACTID);
			bean.setContactId(mainDataCursor.getLong(contactIdCol));
			int birthdayCol = mainDataCursor.getColumnIndex(DBAdapter.KEY_BDAY);
			bean.setBirthday(mainDataCursor.getString(birthdayCol));
			int reminderCol = mainDataCursor
					.getColumnIndex(DBAdapter.KEY_REMINDER);
			bean.setReminder(mainDataCursor.getInt(reminderCol) == 1);
			mainDataList.add(bean);
		}

		// Close the cursor
		mainDataCursor.close();
		BirthdayBean[] mainDataArray = null;

		// Sort the ArrayList
		try {
			mainDataArray = new BirthdayBean[mainDataList.size()];
			Arrays.sort(mainDataList.toArray(mainDataArray),
					new BirthdayComparator());
		} catch (Exception e) {
			// TODO: Exception due to incorrect data
			Log.e(TAG, "An error occured while comparing the birthdates.", e);
		}

		// Initialize ArrayAdapter using the ArrayList
		ListAdapter adapter = new BirthdayArrayAdapter(this,
				R.layout.view_list_item, mainDataArray);

		// Bind to our new adapter.
		setListAdapter(adapter);
		db.close();
	}

	private void syncCalendar() {

		Thread t = new Thread() {
			public void run() {
				// Get the custom preference
				SharedPreferences preferences = getSharedPreferences(
						"birthdayPrefs", Activity.MODE_PRIVATE);

				CalendarHelper helper = new CalendarHelper(
						BirthdaysActivity.this);

				// Notify the user of calendar syncing
				Notification notification = new Notification(
						R.drawable.toolbar, "Syncing with calendar...", System
								.currentTimeMillis());
				Intent intent = new Intent(getApplicationContext(),
						BirthdaysActivity.class);
				notification.setLatestEventInfo(BirthdaysActivity.this,
						"Birthdays", "Syncing Birthdays with the Calendar",
						PendingIntent.getActivity(BirthdaysActivity.this
								.getBaseContext(), 0, intent,
								PendingIntent.FLAG_CANCEL_CURRENT));
				notification.flags = Notification.FLAG_ONGOING_EVENT;
				mManager.notify(APP_ID, notification);

				// Create a birthday calendar if not already present
				if (!preferences.contains("calendarId")) {
					String calId = helper.createCalendar("Birthdays");

					if (calId == null) {
						// Calendar not found. Display a toast and return.
						Toast toast = Toast.makeText(BirthdaysActivity.this,
								"Calendar is not installed. Aborting...",
								Toast.LENGTH_LONG);
						toast.show();
						return;
					}

					SharedPreferences.Editor editor = preferences.edit();
					editor.putString("calendarId", calId);
					editor.commit();
				}

				try {
					db.open();

					Cursor cursor = db.getAllEntriesForEdit();
					if (cursor != null) {
						while (cursor.moveToNext()) {
							int rowIdCol = cursor
									.getColumnIndex(DBAdapter.KEY_ROWID);
							long rowId = cursor.getLong(rowIdCol);

							int nameCol = cursor
									.getColumnIndex(DBAdapter.KEY_NAME);
							String name = cursor.getString(nameCol);

							int dateCol = cursor
									.getColumnIndex(DBAdapter.KEY_BDAY);
							String origDateString = cursor.getString(dateCol);

							int reminderCol = cursor
									.getColumnIndex(DBAdapter.KEY_REMINDER);
							Integer reminder = cursor.getInt(reminderCol);

							int eventIdCol = cursor
									.getColumnIndex(DBAdapter.KEY_EVENTID);
							String eventId = cursor.getString(eventIdCol);

							// Sync the birthdays with calendar
							String calendarId = preferences.getString(
									"calendarId", null);

							if (reminder != 0) {
								if (eventId == null
										|| eventId.trim().equals("")) {
									// Create the event and get the associated
									// Id
									eventId = helper.createEvent(calendarId,
											name, origDateString);
								} else {
									// Update an event
									helper.updateEvent(eventId, name,
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

				} finally {
					db.close();
				}

				mHandler.post(mSyncCalendar);
			}
		};

		t.start();
	}

	private void calendarSynced() {
		mManager.cancel(APP_ID);

		// Notify the user of calendar syncing
		Notification notification = new Notification(R.drawable.toolbar,
				"Calendar Sync complete.", System.currentTimeMillis());
		Intent intent = new Intent(getApplicationContext(),
				BirthdaysActivity.class);
		notification.setLatestEventInfo(BirthdaysActivity.this, "Birthdays",
				"Calendar synchronization complete", PendingIntent.getActivity(
						BirthdaysActivity.this.getBaseContext(), 0, intent,
						PendingIntent.FLAG_CANCEL_CURRENT));
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mManager.notify(APP_ID, notification);
	}
}
