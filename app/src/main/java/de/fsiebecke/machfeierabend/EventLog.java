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
     * @param checkin_state
     * @return total time in sec
     */
    long calculateTime ( CHECKIN_STATE checkin_state) {
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

    void clearLog ( ) {
        m_eventTimestamps.clear();
    }

    public void saveEventlog ( ) {
        SharedPreferences shref;
        SharedPreferences.Editor editor;
        shref = App.getApplication().getMySharedPreferences ( );
        Gson gson = new Gson();
        String json = gson.toJson(m_eventTimestamps);
        editor = shref.edit();
        editor.remove(AppConstants.PREF_NAME_EVENTLOG).commit();
        editor.putString(AppConstants.PREF_NAME_EVENTLOG, json);
        editor.commit();
    }

    public void restoreEventlog ( ) {
        SharedPreferences shref;
        SharedPreferences.Editor editor;
        shref = App.getApplication().getMySharedPreferences();
        Gson gson = new Gson();
        String response=shref.getString(AppConstants.PREF_NAME_EVENTLOG, "");
        ArrayList<Date> lstArrayList = gson.fromJson(response,
                new TypeToken<List<Date>>(){}.getType());
    }
}
