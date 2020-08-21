/*
 * receive broadcasts for various alarms (1., 2., last alarm) and
 * for updating the notification while the app is in background
 */

package de.fsiebecke.machfeierabend;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.legacy.content.WakefulBroadcastReceiver;
//import android.content.BroadcastReceiver;

public class AlertReceiver extends WakefulBroadcastReceiver {
//public class AlertReceiver extends BroadcastReceiver {
    private static final String TAG = "AlertReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String s = intent.getAction();
        Log.d ( TAG, s );
        int notificationId = intent.getExtras().getInt(context.getResources().getString(R.string.key_notification_id));
        Log.d(TAG, "onReceive (notificationId=" + notificationId + ").");
        switch (notificationId) {
            case AppConstants.NOTIFICATION_ID_ALARM_1ST:
            case AppConstants.NOTIFICATION_ID_ALARM_2ND:
            case AppConstants.NOTIFICATION_ID_ALARM_FINAL:
                Log.d(TAG, "-> 1st, 2nd or final alarm");
                App.getApplication().handleAlarm(notificationId);
                break;
            case AppConstants.NOTIFICATION_ID_APP_UPDATE:
                Log.d(TAG, "-> app update");
                App.getApplication().handleAppAlarm();
                break;
            case AppConstants.NOTIFICATION_ID_DISMISSED:
                Log.d(TAG, "-> notification dismissed");
                App.getApplication().onDismissAlarmNotification();
                break;
            case AppConstants.NOTIFICATION_ID_APP_NOTIFICATION_DISMISSED:
                Log.d(TAG, "-> app notification dismissed");
                App.getApplication().onDismissAppNotification ( );
                break;
            case AppConstants.NOTIFICATION_ID_STOP_RINGTONE:
                Log.d(TAG, "-> stop ringtone");
                App.getApplication().stopRingtone();
                break;
        }
    }
}
