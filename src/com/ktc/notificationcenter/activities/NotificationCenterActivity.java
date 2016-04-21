package com.ktc.notificationcenter.activities;

import java.util.HashSet;
import java.util.Vector;
import com.haarman.listviewanimations.ArrayAdapter;
import com.ktc.notificationcenter.R;
import com.ktc.notificationcenter.services.NotificationMonitor;
import com.ktc.notificationcenter.utils.DiyTimeFormatterUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class NotificationCenterActivity extends Activity {
	static final String TAG = "MAINACTIVITY";
	private static final String TAG_PRE = "[" + NotificationCenterActivity.class.getSimpleName() + "] ";
	private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
	private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
	private final HashSet<ComponentName> mEnabledListeners = new HashSet<ComponentName>();
	private boolean isEnabledNLS = false;
	private ListView mListView;
	private MyAdapter adapter;
	TextView count;
	boolean isListViewFocused = false;
	boolean inAnimation = false;
	Vector<StatusBarNotification> mVector;
	BroadcastReceiver myReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_center);
		autoGetAccess();
		initData();
	}

	private void initData() {
		// TODO Auto-generated method stub
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.Notification_stated_changed");
		// 注册
		registerReceiver(myReceiver, filter);
		count = (TextView) findViewById(R.id.tv_counts);
		mVector = new Vector<StatusBarNotification>();
		mListView = (ListView) findViewById(R.id.lv);
		adapter = new MyAdapter();
		/*
		 * SwingRightInAnimationAdapter swingRightInAnimationAdapter = new
		 * SwingRightInAnimationAdapter( new SwipeDismissAdapter(adapter,
		 * this)); swingRightInAnimationAdapter.setListView(mListView);
		 * mListView.setAdapter(swingRightInAnimationAdapter);
		 * adapter.addAll(getItems());
		 */

		adapter = new MyAdapter();
		mListView.setAdapter(adapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				// 点击条目跳转
				PendingIntent pendingIntent = mVector.get(position).getNotification().contentIntent;
				if (pendingIntent != null) {
					try {
						pendingIntent.send();
					} catch (CanceledException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					PackageManager pm = getPackageManager();
					Intent intent = pm.getLaunchIntentForPackage(mVector.get(position).getPackageName());
					startActivity(intent);
				}
			}
		});
		mListView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				isListViewFocused = hasFocus;
			}
		});

	}

	public void autoGetAccess() {
		ContentResolver contentResolver = getContentResolver();
		/**
		 * 1.获取之前的具有权限的列表
		 */
		final String flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
		if (flat != null && !"".equals(flat)) {
			final String[] names = flat.split(":");
			for (int i = 0; i < names.length; i++) {
				final ComponentName cn = ComponentName.unflattenFromString(names[i]);
				if (cn != null) {
					mEnabledListeners.add(cn);
				}
			}
		}
		ComponentName myCN = new ComponentName("com.ktc.notificationcenter",
				"com.ktc.notificationcenter.NotificationMonitor");
		mEnabledListeners.add(myCN);
		/**
		 * 2.将最新的列表写入
		 */
		StringBuilder sb = null;
		for (ComponentName cn : mEnabledListeners) {
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append(':');
			}
			sb.append(cn.flattenToString());
		}
		Settings.Secure.putString(contentResolver, "enabled_notification_listeners", sb != null ? sb.toString() : "");
	}

	protected void listCurrentNotification() {
		mVector = NotificationMonitor.getCurrentNotifications();
		if (mVector == null) {
			count.setText("您一共有" + 0 + "条通知。");
		} else {
			count.setText("您一共有" + mVector.size() + "条通知。");
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		super.onResume();
		isEnabledNLS = isEnabled();
		logNLS("isEnabledNLS = " + isEnabledNLS);
		if (!isEnabledNLS) {
			showConfirmDialog();
		} else {
			listCurrentNotification();
		}
	}

	private boolean isEnabled() {
		String pkgName = getPackageName();
		final String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
		if (!TextUtils.isEmpty(flat)) {
			final String[] names = flat.split(":");
			for (int i = 0; i < names.length; i++) {
				final ComponentName cn = ComponentName.unflattenFromString(names[i]);
				if (cn != null) {
					if (TextUtils.equals(pkgName, cn.getPackageName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void cancelNotification(Context context, boolean isCancelAll) {
		Intent intent = new Intent();
		intent.setAction(NotificationMonitor.ACTION_NLS_CONTROL);
		if (isCancelAll) {
			intent.putExtra("command", "cancel_all");
		} else {
			intent.putExtra("command", "cancel_last");
		}
		context.sendBroadcast(intent);
	}

	private void cancelNotificationAtPosition(Context context, final int position) {
		TranslateAnimation tr = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, //
				0, //
				TranslateAnimation.RELATIVE_TO_SELF, //
				-1, //
				TranslateAnimation.RELATIVE_TO_SELF, //
				0, //
				TranslateAnimation.RELATIVE_TO_SELF, //
				0//
		);
		AlphaAnimation al = new AlphaAnimation(1, 0);
		AnimationSet animationSet = new AnimationSet(true);
		animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
		animationSet.addAnimation(tr);
		animationSet.addAnimation(al);
		animationSet.setDuration(500);
		animationSet.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				inAnimation = true;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setAction(NotificationMonitor.ACTION_NLS_CONTROL);
				intent.putExtra("command", "cancel_position");
				intent.putExtra("position", position);
				sendBroadcast(intent);
				inAnimation = false;
			}
		});
		// 注意： getChildat方法是相对第一个可见的item的 位置的元素
		mListView.getChildAt(position - mListView.getFirstVisiblePosition()).startAnimation(animationSet);

	}

	private void clearAllNotifications() {
		if (isEnabledNLS) {
			cancelNotification(this, true);
		} else {
			/**
			 * @TODO: 添加没有通知权限的逻辑代码
			 */
			showConfirmDialog();
		}
	}

	private void openNotificationAccess() {
		startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
	}

	private void showConfirmDialog() {
		new AlertDialog.Builder(this).setMessage("请开启通知访问").setTitle("Notification Access")
				.setIconAttribute(android.R.attr.alertDialogIcon).setCancelable(true)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						openNotificationAccess();
					}
				}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// do nothing
					}
				}).create().show();
	}

	private void logNLS(Object object) {
		Log.i(TAG, TAG_PRE + object);
	}

	private class MyAdapter extends ArrayAdapter<Integer> {
		@Override
		public int getCount() {
			if (mVector == null) {
				return 0;
			} else {
				return mVector.size();
			}
		}

		@Override
		public Integer getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@SuppressLint("NewApi")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			ViewHolder holder;
			StatusBarNotification currentStatusBarNotification;
			if (convertView == null) {
				v = View.inflate(getApplicationContext(), R.layout.notification_item, null);
				holder = new ViewHolder();
				holder.iv_icon = (ImageView) v.findViewById(R.id.iv_icon);
				holder.tv_title = (TextView) v.findViewById(R.id.tv_title);
				holder.tv_content = (TextView) v.findViewById(R.id.tv_content);
				holder.tv_time = (TextView) v.findViewById(R.id.tv_time);
				/*
				 * holder.iv_small_icon = (ImageView)
				 * v.findViewById(R.id.iv_small_icon);
				 */
				holder.tv_status = (TextView) v.findViewById(R.id.tv_status);
				v.setTag(holder);
			} else {
				v = convertView;
				holder = (ViewHolder) convertView.getTag();
			}
			currentStatusBarNotification = mVector.get(position);
			Bundle extras = currentStatusBarNotification.getNotification().extras;
			String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
			Bitmap notificationLargeIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
			Bitmap notificationSmallIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_SMALL_ICON));
			CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
			// CharSequence notificationSubText =
			// extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
			long when = currentStatusBarNotification.getPostTime();
			if (notificationLargeIcon != null) {
				holder.iv_icon.setImageBitmap(notificationLargeIcon);
			} else {
				PackageManager pm = getPackageManager();
				Drawable applicationIcon;
				try {
					applicationIcon = pm.getApplicationIcon(currentStatusBarNotification.getPackageName());
					holder.iv_icon.setImageDrawable(applicationIcon);
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (notificationTitle != null) {
				holder.tv_title.setText(notificationTitle);
			}
			if (notificationText != null) {
				holder.tv_content.setText(notificationText);
			}
			/*
			 * if (notificationSmallIcon != null) {
			 * holder.iv_small_icon.setImageBitmap(notificationSmallIcon); }
			 */
			if (currentStatusBarNotification.getNotification().flags == Notification.FLAG_NO_CLEAR) {
				holder.tv_status.setText("不被清除");
			} else if (currentStatusBarNotification.getNotification().flags == Notification.FLAG_ONGOING_EVENT) {
				holder.tv_status.setText("运行中");
			} else if (currentStatusBarNotification.getNotification().flags == Notification.FLAG_AUTO_CANCEL) {
				holder.tv_status.setText("常规通知");
			} else if (currentStatusBarNotification.getNotification().flags == Notification.FLAG_INSISTENT) {
				holder.tv_status.setText("持续型通知");
			} else if (currentStatusBarNotification.getNotification().flags == Notification.FLAG_HIGH_PRIORITY) {
				holder.tv_status.setText("高优先级");
			} else {
				holder.tv_status.setText("");
			}
			holder.tv_time.setText(DiyTimeFormatterUtil.getStandardDate(when));
			return v;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(myReceiver);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (isListViewFocused && (!inAnimation)) {
				int selectedItemPosition = mListView.getSelectedItemPosition();
				Log.i(TAG, "~~~" + selectedItemPosition + "~~~");
				cancelNotificationAtPosition(getApplicationContext(), selectedItemPosition);
				return true;
			}
			return super.onKeyDown(keyCode, event);
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	public void clearAll(View v) {
		clearAllNotifications();
	}

	public static class ViewHolder {
		ImageView iv_icon;
		TextView tv_title;
		TextView tv_content;
		TextView tv_time;
		TextView tv_status;
		/* ImageView iv_small_icon; */
	}

	public class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			listCurrentNotification();
		}
	}
}
