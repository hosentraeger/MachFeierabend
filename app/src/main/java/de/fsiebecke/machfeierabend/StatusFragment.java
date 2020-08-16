package de.fsiebecke.machfeierabend;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StatusFragment extends Fragment {

    private static final String TAG = "StatusFragment";
    private long m_timeLeft = 0;
    private TextView m_textViewRemainingTime;
    private ImageButton m_startButton;
    private ImageButton m_pauseButton;
    private ImageButton m_stopButton;
    private ImageButton m_muteButton;
    private TextView m_info;
    Context m_ctx;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView." );
        View v = inflater.inflate(R.layout.fragment_status, container, false);
        m_ctx =v.getContext();
        m_textViewRemainingTime = (TextView)v.findViewById(R.id.editRemainingTime);
        m_startButton = (ImageButton) v.findViewById(R.id.startButton);
        m_pauseButton = (ImageButton) v.findViewById(R.id.pauseButton);
        m_stopButton = (ImageButton) v.findViewById(R.id.stopButton);
        m_muteButton = (ImageButton) v.findViewById(R.id.muteButton);
        m_info = ( TextView) v.findViewById(R.id.editStatusFragmentComment);
        m_muteButton.setVisibility(View.INVISIBLE);
        m_muteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
             App.getApplication().stopRingtone();
             m_muteButton.setVisibility(View.INVISIBLE);
            }
        });

        updateButtonStates ( );

        m_startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"start clicked." );
                App.getApplication().startAlarms();
                App.getApplication().getEventLog().logEvent();
                App.getApplication().setTimerIsRunning(true);
                m_timeLeft = App.getApplication().getRemainingWorktime();
                setRemainingTime ( );
                updateButtonStates();
            }
        });

        m_pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"pause clicked." );
                App.getApplication().getEventLog().logEvent();
                App.getApplication().setTimerIsRunning(false);
                updateButtonStates();
                App.getApplication().cancelAlarms ( );
            }
        });

        m_stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"stop clicked." );
                App.getApplication().cancelAlarms();
                App.getApplication().setTimerIsRunning(false);
                App.getApplication().getEventLog().clearLog();
                App.getApplication().stopRingtone();
                updateButtonStates();
                m_timeLeft = App.getApplication().getRemainingWorktime();
                m_info.setText("");
                m_muteButton.setVisibility(View.INVISIBLE);
                setRemainingTime ( );
            }
        });

        final Handler refreshHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if ( App.getApplication().getTimerIsRunning() ) {
                    if ( m_timeLeft > 0 ) {
                        m_timeLeft--;
                        setRemainingTime ( );
                        if ( App.getApplication().isRingtonePlaying() ) m_muteButton.setVisibility(View.VISIBLE);
                        else m_muteButton.setVisibility(View.INVISIBLE);
                        switch ( App.getApplication().getCurrentAlarmStage() )  {
                            case ALARM_STAGE_1ST:
                                m_info.setText(R.string.alarm_notification_text_1st);
                                break;
                            case ALARM_STAGE_2ND:
                                m_info.setText(R.string.alarm_notification_text_2nd);
                                break;
                            case ALARM_STAGE_FINAL:
                                m_info.setText(R.string.alarm_notification_text_final);
                                break;
                            default:
                                m_info.setText ( "" );
                        }
                    } else {
                        m_info.setText(R.string.alarm_notification_text_failed);
                    }
                }
                refreshHandler.postDelayed(this, 1 * 1000);
            }
        };
        refreshHandler.postDelayed(runnable, 1 * 1000);

        return v;
    }

    void setRemainingTime ( ) {
        long s = m_timeLeft;
        long h = s / 3600;
        s -= 3600 * h;
        long m = s / 60;
        s -= m * 60;
        String remainingWorktime = String.format ( "%02d:%02d:%02d", h, m, s );
        m_textViewRemainingTime.setText(remainingWorktime);
    }
    @Override
    public void onResume() {
        Log.d(TAG,"onResume." );
        m_timeLeft = App.getApplication().getRemainingWorktime();
        if ( m_timeLeft < 0 )
            m_timeLeft = 0;

        setRemainingTime ( );
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause." );
        super.onPause();
    }

    private void enableImageButton ( ImageButton b, int bid, boolean enabled ) {
        b.setEnabled(enabled);
        b.setClickable(enabled);
        b.setAlpha ( enabled? 255: 75);
    }

    private void updateButtonStates ( ) {
        if ( App.getApplication().getTimerIsRunning()) {
            enableImageButton(m_startButton, R.id.startButton,false);
            enableImageButton(m_pauseButton, R.id.pauseButton, true);
            enableImageButton(m_stopButton, R.id.stopButton,true);
        } else {
            enableImageButton(m_startButton, R.id.startButton,true);
            enableImageButton(m_pauseButton, R.id.pauseButton, false);
            if ( App.getApplication().getEventLog().calculateTime(
                    EventLog.CHECKIN_STATE.CHECKED_IN) > 0 )
                enableImageButton(m_stopButton,R.id.stopButton, true);
            else
                enableImageButton(m_stopButton,R.id.stopButton, false);
        }
    }
}
