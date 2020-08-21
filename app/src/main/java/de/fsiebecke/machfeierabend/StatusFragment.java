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
import android.widget.Toast;
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
             App.getApplication().cancelAlarmNotifications();
             m_muteButton.setVisibility(View.INVISIBLE);
            }
        });

        updateButtonStates ( );

        m_startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"start clicked." );
                App.getApplication().getEventLog().logEvent();
                if ( App.getApplication().getEventLog().isFirstBreakLogged ( ) ) {
                    Toast.makeText(getActivity(), R.string.info_individual_break_calculated,
                            Toast.LENGTH_LONG).show();
                }
                start ( );
                refreshDisplay();
            }
        });

        m_pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"pause clicked." );
                App.getApplication().cancelAlarms ( );
                App.getApplication().getEventLog().logEvent();
                App.getApplication().setTimerIsRunning(false);
                refreshDisplay();
            }
        });

        m_stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"stop clicked." );
                // TODO: see issue #16
                /*
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.dialog_title_data_found);
                    builder.setMessage(R.string.dialog_details_data_found);

                    builder.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getApplication().getEventLog().restoreEventlog();
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertdialog = builder.create();
                    alertdialog.show();
                 */
                App.getApplication().cancelAlarms();
                App.getApplication().setTimerIsRunning(false);
                App.getApplication().getEventLog().clearLog();
                App.getApplication().getEventLog().saveEventlog();
                App.getApplication().stopRingtone();
                updateButtonStates();
                m_timeLeft = App.getApplication().getRemainingWorktime();
                m_info.setText(R.string.info_app_usage);
                m_muteButton.setVisibility(View.INVISIBLE);
                refreshDisplay();
            }
        });

        // create a timer that counts down our "remaining worktime" every second
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
                                m_info.setText(R.string.info_app_usage);
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

    /**
     * starts the timer
     */
    public void start() {
        App.getApplication().startAlarms();
        App.getApplication().setTimerIsRunning(true);
    }

    /**
     * re-calculates remaining time, displays it
     */
    public void refreshDisplay ( ) {
        m_timeLeft = App.getApplication().getRemainingWorktime();
        if ( m_timeLeft < 0 )
            m_timeLeft = 0;
        setRemainingTime ( );
        updateButtonStates();
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume." );
        refreshDisplay();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause." );
        super.onPause();
    }

    /**
     * updates textview which shows remaining time
     */
    void setRemainingTime ( ) {
        String remainingWorktime = "--.--.--";
        long s = m_timeLeft;
        if ( s > 0 )  {
            long h = s / 3600;
            s -= 3600 * h;
            long m = s / 60;
            s -= m * 60;
            remainingWorktime = String.format("%02d:%02d:%02d", h, m, s);
        };
        m_textViewRemainingTime.setText(remainingWorktime);
    }

    /**
     * treat image button
     * @param b the button to show or hide
     * @param bid it's id
     * @param enabled show or hide
     */
    private void enableImageButton ( ImageButton b, int bid, boolean enabled ) {
        b.setEnabled(enabled);
        b.setClickable(enabled);
        b.setAlpha ( enabled? 255: 75);
    }

    /**
     * decides what buttons should be enabled or disabled
     * called from the three button handlers
     */
    private void updateButtonStates ( ) {
        if ( App.getApplication().getTimerIsRunning()) {
            enableImageButton(m_startButton, R.id.startButton,false);
            enableImageButton(m_pauseButton, R.id.pauseButton, true);
            enableImageButton(m_stopButton, R.id.stopButton,true);
        } else {
            enableImageButton(m_startButton, R.id.startButton,true);
            enableImageButton(m_pauseButton, R.id.pauseButton, false);
            if ( App.getApplication().getEventLog().isActive( ) )
                enableImageButton(m_stopButton,R.id.stopButton, true);
            else
                enableImageButton(m_stopButton,R.id.stopButton, false);
        }
    }
}
