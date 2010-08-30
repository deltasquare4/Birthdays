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

package com.rexmenpara.birthdays.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BirthdayBean {
	private long rowId = -1;
	private String name = null;
	private long contactId = -1;
	private String birthday = null;
	private int daysRemaining = 999;
	private int age = 0;
	private boolean reminder = true;

	public long getRowId() {
		return rowId;
	}

	public void setRowId(long rowId) {
		this.rowId = rowId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getContactId() {
		return contactId;
	}

	public void setContactId(long contactId) {
		this.contactId = contactId;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		this.birthday = birthday;

		// Set daysRemaining
		SimpleDateFormat srcFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			// Format the date to omit the year
			Date now = new Date();
			now.setHours(0);
			now.setMinutes(0);
			now.setSeconds(0);

			Date date = null;
			try {
				date = srcFormatter.parse(birthday);
			} catch (ParseException e) {
				String[] split = birthday.split("-");
				if (split.length == 4) {
					birthday = "0100-" + split[2] + "-" + split[3];
					date = srcFormatter.parse(birthday);
				} else {
					throw e;
				}
			}

			int year = date.getYear();
			date.setYear(now.getYear());

			if (date.before(now)) {
				date.setYear(date.getYear() + 1);
			}

			// Set age
			setAge(date.getYear() - year);

			long diff = date.getTime() - now.getTime();
			int diffInDays = (int) (diff / (1000 * 60 * 60 * 24));

			setDaysRemaining(diffInDays);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getDaysRemaining() {
		return daysRemaining;
	}

	public void setDaysRemaining(int daysRemaining) {
		this.daysRemaining = daysRemaining;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isReminder() {
		return reminder;
	}

	public void setReminder(boolean reminder) {
		this.reminder = reminder;
	}
}
