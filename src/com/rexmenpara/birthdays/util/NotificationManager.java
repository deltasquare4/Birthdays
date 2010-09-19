package com.rexmenpara.birthdays.util;

import android.app.Notification;
import android.content.Context;

public class NotificationManager {

	private final android.app.NotificationManager nManager;
	private static NotificationManager instance = null;

	public NotificationManager() {
		Context context = ContextManager.getContext();
		nManager = (android.app.NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void cancel(int id) {
		nManager.cancel(id);
	}

	public void cancelAll() {
		nManager.cancelAll();
	}

	public void notify(int id, Notification notification) {
		nManager.notify(id, notification);
	}

	public static NotificationManager getInstance() {
		if (instance == null) {
			instance = new NotificationManager();
		}

		return instance;
	}
}
