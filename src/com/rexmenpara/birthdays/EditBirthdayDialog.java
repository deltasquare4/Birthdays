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

import java.text.ParseException;
import java.util.Date;

import com.rexmenpara.birthdays.util.DateUtility;
import com.rexmenpara.birthdays.util.ErrorReportHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;

public class EditBirthdayDialog extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			// Blur the activity behind dialog
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
					WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

			setContentView(R.layout.view_edit_dialog);

			// Get the arguments passed
			String birthday = getIntent().getStringExtra("birthday");
			boolean reminder = getIntent().getBooleanExtra("reminder", true);
			long rowId = getIntent().getLongExtra("rowId", -1);

			DatePicker picker = (DatePicker) this
					.findViewById(R.id.birthdayPicker);

			Date date = DateUtility.parseDate(birthday);

			int year = date.getYear();

			if (year <= 0) {
				// Show the ignore year checkbox
				CheckBox chkIgnoreYear = (CheckBox) this
						.findViewById(R.id.chkIgnoreYear);
				chkIgnoreYear.setVisibility(View.VISIBLE);
				chkIgnoreYear.setChecked(true);
				year = new Date().getYear();
			}

			year = 1900 + year;
			
			// Month starts from 0 for DatePicker
			picker.updateDate(year, date.getMonth(), date.getDate());

			CheckBox chkReminder = (CheckBox) this
					.findViewById(R.id.chkReminder);
			chkReminder.setChecked(reminder);

			Button btnSet = (Button) findViewById(R.id.btnOk);
			btnSet.setOnClickListener(new BtnListener(true, rowId));
			Button btnCancel = (Button) findViewById(R.id.btnCancel);
			btnCancel.setOnClickListener(new BtnListener(false, rowId));
		} catch (ParseException e) {
			ErrorReportHandler.collectData(e.getMessage());
		}
	}

	private class BtnListener implements android.view.View.OnClickListener {
		private boolean save = false;
		private long id = -1;

		public BtnListener(boolean save, long id) {
			this.save = save;
			this.id = id;
		}

		public void onClick(View v) {
			if (save) {
				// Get the data from UI
				DatePicker picker = (DatePicker) EditBirthdayDialog.this
						.findViewById(R.id.birthdayPicker);
				CheckBox chkReminder = (CheckBox) EditBirthdayDialog.this
						.findViewById(R.id.chkReminder);
				CheckBox chkIgnoreYear = (CheckBox) EditBirthdayDialog.this
						.findViewById(R.id.chkIgnoreYear);

				// Set year to "0000" in case it's not available
				String year = chkIgnoreYear.isChecked() ? "0000" : String
						.valueOf(picker.getYear());
				String month = picker.getMonth() > 8 ? (1 + picker.getMonth())
						+ "" : "0" + (1 + picker.getMonth());
				String day = picker.getDayOfMonth() > 8 ? picker
						.getDayOfMonth()
						+ "" : "0" + picker.getDayOfMonth();
				String dateString = year + "-" + month + "-" + day;

				// Put the data in an intent
				String nullStr = null;
				Intent resultIntent = new Intent(nullStr);
				resultIntent.putExtra("rowId", id);
				resultIntent.putExtra("birthday", dateString);
				resultIntent.putExtra("reminder", chkReminder.isChecked());

				setResult(Activity.RESULT_OK, resultIntent);
				EditBirthdayDialog.this.finish();
			}

			EditBirthdayDialog.this.finish();
		}
	}
}
