package com.rexmenpara.birthdays.util;

import android.content.Context;

public class ContextManager {
	private static Context _context = null;

	public static Context getContext() {
		return _context;
	}

	public static void setContext(Context context) {
		_context = context;
	}

}
