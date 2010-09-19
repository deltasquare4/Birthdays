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

/**
 * Utility to parse dates. Need this because date formats in the database vary
 * based on Vendors.
 * 
 * @author Rakshit Menpara (http://www.rakshitmenpara.com)
 * 
 */
public class DateUtility {
	private class DateFormat {
		public static final String GENERIC = "\\d{4}-\\d{2}-\\d{2}";
		public static final String FULL_TS_SMALL = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";
		public static final String FULL_TS_MS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
		public static final String FULL_TS_TIMEZONE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{2}:\\d{2}";
		public static final String EPOCH_MS = "\\d{12,13}";
		public static final String EPOCH_NEG = "-\\d{0,13}";
	}

	public static Date parseDate(String dateString) throws ParseException {
		Date date = null;

		if (dateString.matches(DateFormat.GENERIC)) {
			SimpleDateFormat srcFormatter = new SimpleDateFormat("yyyy-MM-dd");
			date = srcFormatter.parse(dateString);
		} else if (dateString.matches(DateFormat.FULL_TS_SMALL)) {
			SimpleDateFormat srcFormatter = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss'Z'");
			date = srcFormatter.parse(dateString);
		} else if (dateString.matches(DateFormat.FULL_TS_MS)) {
			SimpleDateFormat srcFormatter = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			date = srcFormatter.parse(dateString);
		} else if (dateString.matches(DateFormat.FULL_TS_TIMEZONE)) {
			SimpleDateFormat srcFormatter = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
			date = srcFormatter.parse(dateString);
		} else if (dateString.matches(DateFormat.EPOCH_MS)
				|| dateString.matches(DateFormat.EPOCH_NEG)) {
			date = new Date(Long.parseLong(dateString));
		} else {
			// Pattern is not known. Generate an exception and report the new
			// pattern
			throw new ParseException("Unknown date type - " + dateString, 0);
		}
		return date;
	}
}
