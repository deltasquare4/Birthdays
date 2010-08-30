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

import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.rexmenpara.birthdays.R;
import com.rexmenpara.birthdays.models.BirthdayBean;

public class BirthdayArrayAdapter extends ArrayAdapter<BirthdayBean> {

	public static final int MODE_VIEW = 0;
	public static final int MODE_EDIT = 1;
	private int currentMode = MODE_VIEW;

	private Context context = null;

	public BirthdayArrayAdapter(Context context, int viewResourceId,
			BirthdayBean[] beans) {
		super(context, viewResourceId, beans);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		View view = null;
		BirthdayBean bean = getItem(position);

		if (convertView == null) {
			view = inflater.inflate(R.layout.view_list_item, parent, false);
		} else {
			view = convertView;
		}

		// Set RowId in the UI
		TextView rowId_text = (TextView) view.findViewById(R.id.rowId);
		if (rowId_text != null) {
			rowId_text.setText(String.valueOf(bean.getRowId()));
		}

		// Set Photo in the UI
		ImageView mPhotoView = (ImageView) view.findViewById(R.id.photo_id);
		Bitmap bitmap = loadContactPhoto(this.context.getContentResolver(),
				bean.getContactId());
		if (bitmap != null) {
			mPhotoView.setImageBitmap(bitmap);
		} else {
			mPhotoView.setImageResource(R.drawable.android);
		}

		// Set Name in the UI
		TextView name_text = (TextView) view.findViewById(R.id.txtName);
		if (name_text != null) {
			name_text.setText(bean.getName());
		}

		// Set birthdate in the UI
		TextView birthday = (TextView) view.findViewById(R.id.txtBirthday);
		birthday.setText(bean.getBirthday());

		// Set Remaining Days in the UI
		TextView birthDayText = (TextView) view
				.findViewById(R.id.txtBirthdayText);
		if (birthDayText != null) {
			int daysRemaining = bean.getDaysRemaining();

			String birthdayString = "";

			if (bean.getAge() > 0 && bean.getAge() < 100) {
				if (daysRemaining == 0) {
					birthdayString += "Turned " + bean.getAge() + " ";
				} else {
					birthdayString += "Turning " + bean.getAge() + " ";
				}
			}
			if (daysRemaining == 0) {
				birthdayString += "Today";
			} else if (daysRemaining == 1) {
				birthdayString += "Tomorrow";
			} else if (daysRemaining == 999) {
				birthdayString = "Invalid Birthday";
			} else {
				birthdayString += "After " + daysRemaining + " Days";
			}
			birthDayText.setText(birthdayString);
		}

		// Set Reminder checkbox value in the UI
		CheckBox chkReminder = (CheckBox) view.findViewById(R.id.chkReminder);
		chkReminder.setChecked(bean.isReminder());

		if (currentMode == MODE_EDIT) {
			chkReminder.setVisibility(View.VISIBLE);
		} else {
			chkReminder.setVisibility(View.GONE);
		}

		// Set the sync image
		ImageView calImage = (ImageView) view.findViewById(R.id.syncIcon);
		if (bean.isReminder()) {
			calImage.setVisibility(View.VISIBLE);
		} else {
			calImage.setVisibility(View.INVISIBLE);
		}

		return view;
	}

	/**
	 * @return the photo URI
	 */
	private Bitmap loadContactPhoto(ContentResolver cr, long id) {
		Uri uri = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts
				.openContactPhotoInputStream(cr, uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}

	public void setMode(int mode) {
		this.currentMode = mode;
	}
}
