package com.ktc.notificationcenter.services;

import java.util.Collections;
import java.util.Vector;

import com.ktc.notificationcenter.comparator.MyVectorComparator;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

public class NotificationMonitor extends NotificationListenerService {
	private static final String TAG = "SevenNLS";
	private static final String TAG_PRE = "[" + NotificationMonitor.class.getSimpleName() + "] ";
	private static final int EVENT_UPDATE_CURRENT_NOS = 0;
	public static final String ACTION_NLS_CONTROL = "com.seven.notificationlistenerdemo.NLSCONTROL";
	public static Vector<StatusBarNotification> mCurrentNotifications = new Vector<StatusBarNotification>();
	public static int mCurrentNotificationsCounts = 0;
	public static StatusBarNotification mPostedNotification;
	public static StatusBarNotification mRemovedNotification;
	public static StatusBarNotification[] activeNos;
	private CancelNotificationReceiver mReceiver = new CancelNotificationReceiver();
	// String a;

	private Handler mMonitorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_UPDATE_CURRENT_NOS:
				updateCurrentNotifications();
				break;
			default:
				break;
			}
		}
	};
	class CancelNotificationReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action;
			if (intent != null && intent.getAction() != null) {
				action = intent.getAction();
				if (action.equals(ACTION_NLS_CONTROL)) {
					String command = intent.getStringExtra("command");
					if (TextUtils.equals(command, "cancel_last")) {
						if (mCurrentNotifications != null && mCurrentNotificationsCounts >= 1) {
							StatusBarNotification sbnn = getCurrentNotifications().get(mCurrentNotificationsCounts - 1);
							cancelNotification(sbnn.getPackageName(), sbnn.getTag(), sbnn.getId());
						}
					} else if (TextUtils.equals(command, "cancel_all")) {
						cancelAllNotifications();
					}else if (TextUtils.equals(command, "cancel_position")) {
						int position = intent.getIntExtra("position", -1);
//						Log.i(TAG, "~~~"+position+"~~~");
						if (position>=0) {
							StatusBarNotification sbAtPosition=getCurrentNotifications().get(position);
							cancelNotification(sbAtPosition.getPackageName(), sbAtPosition.getTag(), sbAtPosition.getId());
						}
					}
				}
			}
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		logNLS("onCreate...");
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_NLS_CONTROL);
		registerReceiver(mReceiver, filter);
		mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// a.equals("b");
		logNLS("onBind...");
		return super.onBind(intent);
	}

	@SuppressLint("NewApi")
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		logNLS("onNotificationPosted...");
		logNLS("have " + mCurrentNotificationsCounts + " active notifications");
		mPostedNotification = sbn;
		updateCurrentNotifications();
	/*	Bundle extras = sbn.getNotification().extras;
		String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
		Bitmap notificationLargeIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
		Bitmap notificationSmallIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_SMALL_ICON));
		CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
		CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
		Log.i("SevenNLS", "notificationTitle:" + notificationTitle);
		Log.i("SevenNLS", "notificationText:" + notificationText);
		Log.i("SevenNLS", "notificationSubText:" + notificationSubText);
		Log.i("SevenNLS", "notificationLargeIcon is null:" + (notificationLargeIcon == null));
		Log.i("SevenNLS", "notificationSmallIcon is null:" + (notificationSmallIcon == null));*/
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		logNLS("removed...");
		logNLS("have " + mCurrentNotificationsCounts + " active notifications");
		mRemovedNotification = sbn;
		updateCurrentNotifications();
	}

	private void updateCurrentNotifications() {
		try {
			 activeNos = getActiveNotifications();
		} catch (Exception e) {
			logNLS("Should not be here!!");
			e.printStackTrace();
		}
		onChanged();
	}

	public static Vector<StatusBarNotification> getCurrentNotifications() {
		if (mCurrentNotifications!=null) {
			mCurrentNotifications.removeAllElements();
		}
		for (int i = 0; i < activeNos.length; i++) {
			mCurrentNotifications.add(activeNos[i]);
		}
		mCurrentNotificationsCounts = activeNos.length;
		Collections.sort(mCurrentNotifications, new MyVectorComparator());
		if (mCurrentNotifications.size() == 0) {
			logNLS("mCurrentNotifications size is ZERO!!");
			return null;
		}
		return mCurrentNotifications;
	}

	private static void logNLS(Object object) {
//		Log.i(TAG, TAG_PRE + object);
	}

	public void onChanged() {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.Notification_stated_changed");
		sendBroadcast(intent);
	}
}
