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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * An activity that lets the user configure the application.
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class AppPrefActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		// Get the custom preference
		Preference calendarName = (Preference) findPreference("calendarName");
		calendarName
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						SharedPreferences sharedPreferences = getSharedPreferences(
								"birthdayPrefs", Activity.MODE_PRIVATE);
						SharedPreferences.Editor editor = sharedPreferences
								.edit();
						editor.putString("calendarName", newValue.toString());
						editor.commit();
						return true;
					}

				});
	}

	public static void actionPreferences(Context context) {
		try {
			Intent intent = new Intent(context, AppPrefActivity.class);
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
