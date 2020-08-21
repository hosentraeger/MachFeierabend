package de.fsiebecke.machfeierabend;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventLog {
    public static final String TAG = "EventLog";
    private ArrayList<Date> m_eventTimestamps;

    enum CHECKIN_STATE {
        CHECKED_IN,
        CHECKED_OUT
    };
    public EventLog ( ) {
        m_eventTimestamps = new ArrayList<Date>();
    }

    CHECKIN_STATE getCheckinState ( ) {
        return m_eventTimestamps.size() % 2 == 0 ?
                CHECKIN_STATE.CHECKED_OUT : CHECKIN_STATE.CHECKED_IN;
    }
    void logEvent ( ) {
        m_eventTimestamps.add ( Calendar.getInstance().getTime());
    }
    void logEvent ( Date d ) {
        m_eventTimestamps.add ( d);
    }
    void modifiyEvent ( int index, Date date ) {
        try {
            m_eventTimestamps.set(index, date);
        } catch ( IndexOutOfBoundsException e) {
            Log.e ( TAG, e.getMessage() );
        }
    }
    void deleteEvent ( int index ) {
        try {
            m_eventTimestamps.remove(index);
        } catch ( IndexOutOfBoundsException e) {
            Log.e ( TAG, e.getMessage() );
        }
    }
    public ArrayList <Date> getArrayList ( ) { return m_eventTimestamps;}

    /**
     * calculate total time for either worktime or break
     * @param checkin_state which state to check: CHECKED_IN or CHECKED_OUT
     * @return total time in sec
     */
    long calculateElapsedTime(CHECKIN_STATE checkin_state) {
        long retVal = 0;
        if ( m_eventTimestamps.size()== 0 ) return retVal; // no events logged so far
        // temporary add current Time to EventLog to calculate total time until now
        m_eventTimestamps.add (Calendar.getInstance().getTime());
        for ( int index = 1; index < m_eventTimestamps.size ( ); index++) {
            long duration = ( m_eventTimestamps.get(index).getTime() -
                    m_eventTimestamps.get(index-1).getTime( ) ) / 1000;
            if ( ( checkin_state == CHECKIN_STATE.CHECKED_IN ) && ( index % 2 == 1 ) ||
                    ( checkin_state == CHECKIN_STATE.CHECKED_OUT ) && ( index % 2 == 0 )
            )
                retVal += duration;
        }
        m_eventTimestamps.remove ( m_eventTimestamps.size()-1);
        return retVal;
    }

    /**
     * clears the log
     */
    void clearLog ( ) {
        m_eventTimestamps.clear();
    }

    /**
     * saves the log to shared prefs (as json string)
     */
    public void saveEventlog ( ) {
        SharedPreferences shref;
        SharedPreferences.Editor editor;
        shref = App.getApplication().getMySharedPreferences ( );
        Gson gson = new Gson();
        String json = gson.toJson(m_eventTimestamps);
        editor = shref.edit();
        editor.remove(AppConstants.PREF_NAME_EVENTLOG).apply();
        editor.putString(AppConstants.PREF_NAME_EVENTLOG, json);
        editor.commit();
    }

    /**
     * @return saved event log
     */
    private ArrayList <Date> loadEventLog ( ) {
        SharedPreferences shref;
        SharedPreferences.Editor editor;
        shref = App.getApplication().getMySharedPreferences();
        Gson gson = new Gson();
        String response=shref.getString(AppConstants.PREF_NAME_EVENTLOG, "");
        ArrayList<Date> ret =  gson.fromJson(response,
                new TypeToken<List<Date>>(){}.getType());
        if ( null == ret ) ret = new ArrayList<Date>();
        return ret;
    }

    /**
     * @return true if current event log is up-to-date
     */
    public boolean compareEventLogWithPersistedLog ( ) {
        ArrayList<Date> comparisionList = loadEventLog();
        return comparisionList.equals(m_eventTimestamps);
    }

    /**
     * replaces current event log with saved one
     */
    public void restoreEventlog ( ) {
        m_eventTimestamps = loadEventLog();
    }

    /**
     * @return true if at least one timestamp exists
     */
    public boolean isActive ( ) {
        return m_eventTimestamps.size() > 0;
    }

    /**
     * @return true if time is currently running
     */
    public boolean isRunning ( ) { return ( m_eventTimestamps.size() % 2 ) == 1; }

    /**
     * @return true if resuming after 1st break (i.e. log size = 3: checkin,checkout, checkin)
     */
    public boolean isFirstBreakLogged() {
        return m_eventTimestamps.size() == 3;
    }

    public long getSecondsSinceStart ( ) {
        if ( m_eventTimestamps.size ( ) == 0 ) return -1;
        Date currentTime = Calendar.getInstance().getTime();
        long msec = currentTime.getTime() - m_eventTimestamps.get(0).getTime ( );
        return ( msec ) / 1000;
    }
}
