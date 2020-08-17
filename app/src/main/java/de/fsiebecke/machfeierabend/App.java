/*
  app class
  provides:
  - creation of notification channels
  - holds AlarmManager instance
  - holds NotificationManager instance
  - holds SharedPreferences
  - holds EventManger (list of checkin/checkout-timestamps)
  - response to Alarms (Alarm if time is up + trigger to update background-notification)
  - calculation of time left
 */

package de.fsiebecke.machfeierabend;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatCallback;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.Objects;
import java.util.Scanner;

public class App extends Application {
    public static final String TAG = "App";
    private AlarmManager m_alarmManager = null;
    private NotificationManager m_notificationManager = null;
    private static App s_instance = null;
    private EventLog m_eventLog;
    private boolean m_timerIsRunning = false;
    private SharedPreferences m_sharedPreferences;
    private Ringtone m_ringtone = null;
    private ALARM_STAGE m_currentAlarmStage = ALARM_STAGE.ALARM_STAGE_NONE;
    private boolean m_isInBackground = false;

    enum ALARM_STAGE {
        ALARM_STAGE_NONE,
        ALARM_STAGE_1ST,
        ALARM_STAGE_2ND,
        ALARM_STAGE_FINAL
    }

    public App() {
        s_instance = this;
        m_eventLog = new EventLog();
        Log.d(TAG, "constructor done." );
    }

    public void setTimerIsRunning(boolean timerIsRunning) {
        this.m_timerIsRunning = timerIsRunning;
    }
    public boolean getTimerIsRunning() {
        return m_timerIsRunning;
    }
    public ALARM_STAGE getCurrentAlarmStage() { return m_currentAlarmStage; }
    public static App getApplication() { return s_instance; }
    public EventLog getEventLog() {
        return m_eventLog;
    }
    public SharedPreferences getMySharedPreferences() {
        return m_sharedPreferences;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // this service has only one job:
        // remove notifications and timers if user "swiped out" the app
        startService(new Intent(getBaseContext(), ClearFromRecentService.class));

        m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        createNotificationChannels();

        m_alarmManager = (AlarmManager) Objects.requireNonNull(
                getSystemService(Context.ALARM_SERVICE));

        m_notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // register screen on/off broadcast events
        // INITIALIZE RECEIVER
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
    }

    /**
     * create two notification channels
     *   app-channel, used for showing an ongoing notification if the app is in background
     *   alarm-channel, used for max. three alarms to inform user
     */
    private void createNotificationChannels() {
        Log.d(TAG, "createNotificationChannels" );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            NotificationChannel channel1 = new NotificationChannel(
                    AppConstants.CHANNEL_1_ID,
                    "App",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel1.setSound(null, null);
            channel1.setShowBadge(false);
            manager.createNotificationChannel(channel1);

            NotificationChannel channel2 = new NotificationChannel(
                    AppConstants.CHANNEL_2_ID,
                    "Alarm",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel2.setDescription("alarm");
            channel2.setShowBadge(false);
            channel2.setSound(null, null);
            manager.createNotificationChannel(channel2);

        }
    }

    /**
     * called from AlertReceiver if alarm broadcast was received
     * this method creates a notification to show the user that the time is soon up
     */
    public void handleAlarm(int notificationId) {
        Log.d(TAG, "handleAlarm (notificationId:" + notificationId + ")." );

        Resources res = getResources();
        String contentText = "";
        String uriSoundString = "";
        Uri uriSound = null;

        // remove "app-in-background"-notification
        cancelAppNotification ( );
        cancelUpdateNotificationAlarm();

        switch (notificationId) {
            case AppConstants.NOTIFICATION_ID_ALARM_1ST:
                contentText = res.getString(R.string.alarm_notification_text_1st);
                uriSoundString = m_sharedPreferences.getString(
                        AppConstants.PREF_NAME_SOUND_URI_1ST_ALARM,
                        "");
                notificationId = AppConstants.NOTIFICATION_ID_ALARM_1ST;
                m_currentAlarmStage = ALARM_STAGE.ALARM_STAGE_1ST;
            break;
            case AppConstants.NOTIFICATION_ID_ALARM_2ND:
                contentText = res.getString(R.string.alarm_notification_text_2nd);
                uriSoundString = m_sharedPreferences.getString(
                        AppConstants.PREF_NAME_SOUND_URI_2ND_ALARM,
                        "");
                notificationId = AppConstants.NOTIFICATION_ID_ALARM_2ND;
                m_currentAlarmStage = ALARM_STAGE.ALARM_STAGE_2ND;
                break;
            case AppConstants.NOTIFICATION_ID_ALARM_FINAL:
                contentText = res.getString(R.string.alarm_notification_text_final);
                uriSoundString = m_sharedPreferences.getString(
                        AppConstants.PREF_NAME_SOUND_URI_FINAL_ALARM,
                        "");
                notificationId = AppConstants.NOTIFICATION_ID_ALARM_FINAL;
                m_currentAlarmStage = ALARM_STAGE.ALARM_STAGE_FINAL;
                break;
        }

        assert uriSoundString != null;
        if (!uriSoundString.equals("")) {
            uriSound = Uri.parse(uriSoundString);
        } else {
            uriSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId,
                new Intent(this, MainActivity.class)
                        .putExtra(getResources().getString(
                                R.string.key_notification_id), notificationId), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, AppConstants.CHANNEL_2_ID)
                        .setSmallIcon(R.drawable.ic_baseline_timer_24)
                        .setContentTitle(res.getString(R.string.alarm_notification_title))
                        .setContentText(contentText)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDeleteIntent(createOnDismissedIntent(this ))
                        .setContentIntent(contentIntent)
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        m_notificationManager.notify(notificationId, mBuilder.build());
        //           showToast(context);
        vibrate(this);
        stopRingtone();
        m_ringtone = RingtoneManager.getRingtone(this, uriSound);
        setStopRingtoneAlarm ( );
        m_ringtone.play();
    }

    /**
     * checks if a ringtone is currently playing, after an alarm was raised
     * @return true if playing ringtone
     */
    public boolean isRingtonePlaying() {
        if (null != m_ringtone)
            return m_ringtone.isPlaying();
        else return false;
    }

    /**
     * stops ringtone playback immediately
     */
    public void stopRingtone() {
        Log.d(TAG, "stopRingtone." );
        if (null != m_ringtone && m_ringtone.isPlaying())
            m_ringtone.stop();

    }

    /**
     * called from AlertReceiver if update broadcast was received
     * create or update a notification that is shown if the app is in background
     */
    public void updateAppNotification() {
        Log.d(TAG, "updateAppNotification" );
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                AppConstants.NOTIFICATION_ID_APP_UPDATE,
                new Intent(this, MainActivity.class)
                        .putExtra ( getResources().getString(
                        R.string.key_notification_id), AppConstants.NOTIFICATION_ID_APP_UPDATE),
                0);


        long s = getRemainingWorktime();
        long h = s / 3600;
        s -= 3600 * h;
        long m = s / 60;
        s -= m * 60;
        Resources res = getResources();
        String remainingWorktime = res.getString(R.string.notification_format, h, m);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, AppConstants.CHANNEL_1_ID)
                        .setSmallIcon(R.drawable.ic_baseline_timer_24)
                        .setContentTitle(res.getString(R.string.notification_title))
                        .setContentText(remainingWorktime)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setOngoing(true);
        m_notificationManager.notify(AppConstants.NOTIFICATION_ID_APP_UPDATE, mBuilder.build());

    }

    /**
     *
     */
    public void handleAppAlarm () {
        // update max remaining work time in app notification
        updateAppNotification ( );
        // reschedule alarm in 30s
        setUpdateNotificationAlarm(30 * 1000);
    }

    /**
     * cancels all alarm notifications
     */
    public void cancelAlarmNotifications ( ) {
        m_notificationManager.cancel(AppConstants.NOTIFICATION_ID_ALARM_1ST);
        m_notificationManager.cancel(AppConstants.NOTIFICATION_ID_ALARM_2ND);
        m_notificationManager.cancel(AppConstants.NOTIFICATION_ID_ALARM_FINAL);
    }

    /**
     * cancels the app-in-background-notification
     * called if app comes to front
     */
    public void cancelAppNotification() {
        Log.d(TAG, "cancelAppNotification" );
        m_notificationManager.cancel(AppConstants.NOTIFICATION_ID_APP_UPDATE);
    }

    /**
     * set an alarm which will trigger an update to the ongoing notification while the
     * app is in background (handled in AlertReceiver)
     *
     * @param ms time until alarm is fired
     */
    public void setUpdateNotificationAlarm(long ms) {
        Log.d(TAG, "setUpdateNotificationAlarm(ms=" + ms + ")." );
        Intent intent = new Intent(this, AlertReceiver.class).setAction(
                AppConstants.INTENT_ACTION_UPDATE_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                AppConstants.NOTIFICATION_ID_APP_UPDATE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        m_alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().getTimeInMillis() + ms,
                pendingIntent);
    }

    /**
     * cancels the alarm that triggers the app-in-background-notification
     */
    public void cancelUpdateNotificationAlarm() {
        Log.d(TAG, "cancelUpdateNotificationAlarm" );
        Intent intent = new Intent(this, AlertReceiver.class).setAction(
                AppConstants.INTENT_ACTION_UPDATE_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                AppConstants.NOTIFICATION_ID_APP_UPDATE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        m_alarmManager.cancel(pendingIntent);
    }

    /**
     * vibrate phone for two seconds
     * @param context a context
     */
    private void vibrate(Context context) {
        // Vibrate the mobile phone
        Log.d(TAG, "vibrate." );
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(2000);
    }

    /*
        private void showToast ( Context context) {
            Toast.makeText(context, "Don't panic but your time is up!!!!.",
                    Toast.LENGTH_LONG).show();
        }
    */

    /**
     * retrieves a preference as long value. Default value applies
     * @param prefName preference's name
     * @param defaultValueResId default value
     * @return preference's value or default value
     */
    public long getTimePreferenceAsLongWithDefaultFromRes(String prefName, int defaultValueResId) {
        long ret = 0;
        int hourOFDay = 0;
        int minute = 0;
        String defaultValue = getResources().getString(defaultValueResId);
        String valueAsString = m_sharedPreferences.getString(prefName, defaultValue);
        Scanner scanner = new Scanner(valueAsString);
        scanner.useDelimiter(":");
        hourOFDay = scanner.nextInt();
        minute = scanner.nextInt();
        return (hourOFDay * 3600 + minute * 60);
    }

    /**
     * calculates maximal remaining worktime
     * gets max worktime from preferences/default
     * calculates already elapsed time
     * adds a default break if no break was taken so far
     * @return maximal remaining worktime in seconds
     */
    public long getRemainingWorktime() {
        long maxWorktime = getTimePreferenceAsLongWithDefaultFromRes(
                AppConstants.PREF_NAME_WORKTIME,
                R.string.max_worktime_default_value);
        long defaultBreak = getTimePreferenceAsLongWithDefaultFromRes(
                AppConstants.PREF_NAME_STANDARD_BREAK,
                R.string.standard_break_default_value);

        long alreadyWorked = getEventLog().calculateTime(EventLog.CHECKIN_STATE.CHECKED_IN);
        long breakTime = getEventLog().calculateTime(EventLog.CHECKIN_STATE.CHECKED_OUT);
        if (breakTime == 0) maxWorktime += defaultBreak;
        maxWorktime -= alreadyWorked;
        Log.d(TAG, "getRemainingWorktime, result=" + maxWorktime + "." );
        return maxWorktime;
    }

    /**
     * get alarm lead time from preferences
     *
     * @param stage Alarm stage to query
     * @return lead time in seconds
     */
    public long getAlarmLeadTime(ALARM_STAGE stage) {
        long leadTime = 0;
        switch (stage) {
            case ALARM_STAGE_1ST:
                leadTime = getTimePreferenceAsLongWithDefaultFromRes(
                        AppConstants.PREF_NAME_LEAD_TIME_1ST_ALARM,
                        R.string.default_lead_time_1st_alarm);
                break;
            case ALARM_STAGE_2ND:
                leadTime = getTimePreferenceAsLongWithDefaultFromRes(
                        AppConstants.PREF_NAME_LEAD_TIME_2ND_ALARM,
                        R.string.default_lead_time_2nd_alarm);
                break;
            case ALARM_STAGE_FINAL:
                leadTime = getTimePreferenceAsLongWithDefaultFromRes(
                        AppConstants.PREF_NAME_LEAD_TIME_FINAL_ALARM,
                        R.string.default_lead_time_final_alarm);
                break;
            default:
                leadTime = -1;
        }
        Log.d(TAG, "getAlarmLeadTime (stage=" + stage + "). result=" + leadTime + "." );

        return leadTime;
    }

    /**
     * check if time left is near maximum
     *
     * @return alarm stage
     */
    public ALARM_STAGE getAlarmStage() {
        long timeLeft = getRemainingWorktime();
        if (timeLeft <= getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_FINAL))
            return ALARM_STAGE.ALARM_STAGE_FINAL;
        if (timeLeft <= getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_2ND))
            return ALARM_STAGE.ALARM_STAGE_2ND;
        if (timeLeft <= getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_1ST))
            return ALARM_STAGE.ALARM_STAGE_1ST;

        return ALARM_STAGE.ALARM_STAGE_NONE;
    }

    /**
     * starts an alarm
     * alarm will be raised even if device is in sleep mode
     *
     * @param c Time at which alarm will be raised
     * @param notificationId id to distinguish alarm stage, use NOTIFICATION_ID_ALARM_1ST
     *                       to NOTIFICATION_ID_ALARM_FINAL
     */
    private void startAlarm(Calendar c, int notificationId) {
        Log.d(TAG, "startAlarm for notificationID " + notificationId + " at " + c.getTime().toString() );
        Intent intent = new Intent(this, AlertReceiver.class);
        intent.setAction(AppConstants.INTENT_ACTION_ALARM);
        intent.putExtra(getResources().getString(R.string.key_notification_id),
                notificationId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //MARSHMALLOW OR ABOVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            m_alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    c.getTimeInMillis(), pendingIntent);
        }
        //LOLLIPOP 21 OR ABOVE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                    c.getTimeInMillis(), pendingIntent);
            m_alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        }
        //KITKAT 19 OR ABOVE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            m_alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                    c.getTimeInMillis(), pendingIntent);
        }
        //FOR BELOW KITKAT ALL DEVICES
        else {
            m_alarmManager.set(AlarmManager.RTC_WAKEUP,
                    c.getTimeInMillis(), pendingIntent);
        }
    }

    /**
     * cancel alarm with given ID
     * @param requestCode notificationID to cancel, use NOTIFICATION_ID_ALARM_1ST to
     *      * NOTIFICATION_ID_ALARM_FINAL
     */
    private void cancelAlarm(int requestCode) {
        Log.d(TAG, "cancelAlarm (requestCode=" + requestCode + ").");
        Intent intent = new Intent(this, AlertReceiver.class);
        intent.setAction(AppConstants.INTENT_ACTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        m_alarmManager.cancel(pendingIntent);
    }

    /**
     * start all enabled and not yet expired alarms
     */
    public void startAlarms() {
        Log.d(TAG, "startAlarms." );
        Calendar c = Calendar.getInstance();
        c.setTime(Calendar.getInstance().getTime());
        long timeLeft = getRemainingWorktime();
        long secondsUntilAlarm = timeLeft - getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_1ST );
        long minutesUntilAlarm = secondsUntilAlarm / 60;
        secondsUntilAlarm -= minutesUntilAlarm * 60;
        c.add(Calendar.MINUTE,
                (int) minutesUntilAlarm );
        c.add ( Calendar.SECOND,
                (int)secondsUntilAlarm);
        if ( minutesUntilAlarm > 0  && m_sharedPreferences.getBoolean(AppConstants.PREF_NAME_ENABLE_1ST_ALARM, false))
            startAlarm(c, AppConstants.NOTIFICATION_ID_ALARM_1ST);

        c.setTime(Calendar.getInstance().getTime());
        secondsUntilAlarm = timeLeft - getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_2ND );
        minutesUntilAlarm = secondsUntilAlarm / 60;
        secondsUntilAlarm -= minutesUntilAlarm * 60;
        c.add(Calendar.MINUTE,
                (int) minutesUntilAlarm);
        c.add ( Calendar.SECOND,
                (int)secondsUntilAlarm);
        if ( minutesUntilAlarm > 0  && m_sharedPreferences.getBoolean(AppConstants.PREF_NAME_ENABLE_2ND_ALARM, false))
            startAlarm(c, AppConstants.NOTIFICATION_ID_ALARM_2ND);

        c.setTime(Calendar.getInstance().getTime());
        secondsUntilAlarm = timeLeft - getAlarmLeadTime(ALARM_STAGE.ALARM_STAGE_FINAL );
        minutesUntilAlarm = secondsUntilAlarm / 60;
        secondsUntilAlarm -= minutesUntilAlarm * 60;
        c.add(Calendar.MINUTE,
                (int) minutesUntilAlarm);
        c.add ( Calendar.SECOND,
                (int)secondsUntilAlarm);
        if ( minutesUntilAlarm > 0  && m_sharedPreferences.getBoolean(AppConstants.PREF_NAME_ENABLE_FINAL_ALARM, false))
            startAlarm(c, AppConstants.NOTIFICATION_ID_ALARM_FINAL );
    }

    /**
     * cancel all (max-worktime-exceeded-)alarms
     */
    public void cancelAlarms() {
        Log.d(TAG, "cancelAlarms." );
        cancelAlarm(AppConstants.NOTIFICATION_ID_ALARM_1ST);
        cancelAlarm(AppConstants.NOTIFICATION_ID_ALARM_2ND);
        cancelAlarm(AppConstants.NOTIFICATION_ID_ALARM_FINAL);
        m_currentAlarmStage = ALARM_STAGE.ALARM_STAGE_NONE;
    }

    /**
     * set alarm which cancels ringtone playback after 30sec
     */
    public void setStopRingtoneAlarm() {
        Log.d(TAG, "setStopRingtoneAlarm." );
        Intent intent = new Intent(this, AlertReceiver.class).setAction(
                AppConstants.INTENT_ACTION_STOP_RINGTONE);
        intent.putExtra(getResources().getString(R.string.key_notification_id),
                AppConstants.NOTIFICATION_ID_STOP_RINGTONE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                AppConstants.NOTIFICATION_ID_STOP_RINGTONE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar c = Calendar.getInstance();
        c.add ( Calendar.SECOND, 30 );

        m_alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                c.getTimeInMillis(),
                pendingIntent);
    }

    /**
     * cancel stop ringtone alarm
     */
    public void cancelStopRingtoneAlarm() {
        cancelAlarm(AppConstants.NOTIFICATION_ID_STOP_RINGTONE);
    }

    /**
     * prepares an intent which notifies the app if user swiped out an alarm notification
     * @param context a context
     * @return prepared intent
     */
    private PendingIntent createOnDismissedIntent(Context context) {
        Intent intent = new Intent(context, AlertReceiver.class);
        intent.putExtra(getResources().getString(R.string.key_notification_id),
                AppConstants.NOTIFICATION_ID_DISMISSED);

        return PendingIntent.getBroadcast(context.getApplicationContext(),
                AppConstants.NOTIFICATION_ID_DISMISSED, intent, 0);
    }

    /**
     * called from SettingsFragment if a preference was changed
     * alarms have to be recalculated
     */
    public void preferenceChanged ( ) {
        Log.d(TAG, "preferenceChanged." );
        if (m_timerIsRunning) {
            cancelAlarms();
            startAlarms();
        }
    }

    /**
     * shutdown app: clear all alarms, notifications...
     */
    public void shutdown ( ) {
        // cancel all alarms
        cancelUpdateNotificationAlarm();
        cancelAlarms(); // max-worktime-exceeded-alarms
        cancelStopRingtoneAlarm();
        // remove all notifications
        cancelAppNotification();
        cancelAlarmNotifications();
        // save an empty log
        getEventLog().clearLog();
        getEventLog().saveEventlog();
    }


    /**
     * prepare background operation
     */
    public void goBackground ( ) {
        updateAppNotification ( );
        setUpdateNotificationAlarm(0);
        m_eventLog.saveEventlog();
    }

    /**
     * prepare foreground operation
     */
    public void goForeground ( ) {
        cancelUpdateNotificationAlarm();
        cancelAppNotification();
        m_eventLog.restoreEventlog();
    }

    /**
     * app was sent to background
     */
    public void sentToBackground ( ) {
        m_isInBackground = true;
        goBackground();
    }

    /**
     * app was sent to foreground
     */
    public void sentToForeground ( ) {
        m_isInBackground = false;
        stopRingtone();
        goForeground();
    }

    /**
     * alarm notification was swiped out
     */
    public void onDismissAlarmNotification ( ) {
        stopRingtone();
        if (m_isInBackground) { // if app is in background, restore background operation
            goBackground();
        }
    }

    /**
     * action if screen was set off
     * called from ScreenReceiver
     */
    public void onScreenOff ( ) {
        Log.d ( TAG, "onScreenOff" );
        stopRingtone();
    }
    /**
     * action if screen was set on
     * called from ScreenReceiver
     */
    public void onScreenOn ( ) {
        Log.d ( TAG, "onScreenOn" );
    }

}
