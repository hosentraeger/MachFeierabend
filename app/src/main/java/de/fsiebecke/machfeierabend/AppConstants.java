package de.fsiebecke.machfeierabend;

public class AppConstants {
    public static final String CHANNEL_1_ID = "app-channel";
    public static final String CHANNEL_2_ID = "alarm-channel";
    public static final String INTENT_ACTION_ALARM = "alarm";
    public static final String INTENT_ACTION_UPDATE_NOTIFICATION = "update";
    public static final String INTENT_ACTION_STOP_RINGTONE = "stopringtone";
    public static final int NOTIFICATION_ID_APP_UPDATE = 0;
    public static final int NOTIFICATION_ID_ALARM_1ST = 1;
    public static final int NOTIFICATION_ID_ALARM_2ND = 2;
    public static final int NOTIFICATION_ID_ALARM_FINAL = 3;
    public static final int NOTIFICATION_ID_STOP_RINGTONE = 10;
    public static final int NOTIFICATION_ID_DISMISSED = 11;

    public static final String PREF_NAME_WORKTIME = "preference_worktime";
    public static final String PREF_NAME_STANDARD_BREAK = "preference_standard_break";
    public static final String PREF_NAME_LEAD_TIME_1ST_ALARM = "lead_time_1st_alarm";
    public static final String PREF_NAME_LEAD_TIME_2ND_ALARM = "lead_time_2nd_alarm";
    public static final String PREF_NAME_LEAD_TIME_FINAL_ALARM = "lead_time_final_alarm";
    public static final String PREF_NAME_SOUND_URI_1ST_ALARM = "sound_uri_1st_alarm";
    public static final String PREF_NAME_SOUND_URI_2ND_ALARM = "sound_uri_2nd_alarm";
    public static final String PREF_NAME_SOUND_URI_FINAL_ALARM = "sound_uri_final_alarm";
    public static final String PREF_NAME_ENABLE_1ST_ALARM = "switch_preference_1st_alarm";
    public static final String PREF_NAME_ENABLE_2ND_ALARM = "switch_preference_2nd_alarm";
    public static final String PREF_NAME_ENABLE_FINAL_ALARM = "switch_preference_final_alarm";
    public static final String PREF_NAME_EVENTLOG = "Eventlog";
}
