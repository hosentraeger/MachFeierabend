package de.fsiebecke.machfeierabend;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFragment = null;

                    switch(menuItem.getItemId()) {
                        case R.id.nav_status:
                            selectedFragment = new StatusFragment();
                            break;
                        case R.id.nav_settings:
                            selectedFragment=new SettingsFragment ( );
                            break;
                        case R.id.nav_tables:
                            selectedFragment = new TablesFragment();
                            break;
                    }
                    assert selectedFragment != null;
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment ).commit ( );
                    return true;
                }

            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container,new StatusFragment()).commit();

        if ( !App.getApplication().getEventLog().compareEventLogWithPersistedLog()
//                &&  ( App.getApplication().getEventLog().getSecondsSinceStart ( ) < 24 * 60 * 60 )
        ) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.dialog_title_data_found);
                    builder.setMessage(R.string.dialog_details_data_found);

                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getApplication().getEventLog().clearLog();
                            StatusFragment fragment = (StatusFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            fragment.refreshDisplay();
                            dialog.cancel();
                        }
                    });

                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getApplication().getEventLog().restoreEventlog();
                            StatusFragment fragment = (StatusFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            // if we started with restored data, check if we have to start the timer
                            boolean b = App.getApplication().getEventLog().isRunning();
                            App.getApplication().setTimerIsRunning(b);
                            if ( b )
                                fragment.start ( );
                            fragment.refreshDisplay();
                            dialog.cancel();
                        }
                    });
                    AlertDialog alertdialog = builder.create();
                    alertdialog.show();
                }
            }, 1000);
        }
    }



    @Override
    protected void onPause() {
        Log.d ( TAG, "onPause");
        App.getApplication().sentToBackground();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        App.getApplication().sentToForeground();
    }

    @Override
    public void onBackPressed() {
        App.getApplication().shutdown();
        super.onBackPressed();
    }
}
