package com.ktc.notificationcenter.comparator;

import java.util.Comparator;
import android.service.notification.StatusBarNotification;

public class MyVectorComparator implements Comparator<StatusBarNotification> {

	@Override
	public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
		if (lhs.getPostTime() > rhs.getPostTime()) {
			return -1;
		} else if (lhs.getPostTime() < rhs.getPostTime()) {
			return 1;
		} else {
			return (lhs.hashCode() - rhs.hashCode());
		}
	}
}
