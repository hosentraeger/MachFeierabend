package de.fsiebecke.machfeierabend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {
    public static final String TAG = "ScreenReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d ( TAG, "onReceive" );
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            App.getApplication().onScreenOff();
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            App.getApplication().onScreenOn();
        }
    }
}
