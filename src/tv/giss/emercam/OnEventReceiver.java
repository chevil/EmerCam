package tv.giss.emercam;

import tv.giss.emercam.MainActivity;
import tv.giss.emercam.LaunchingService;
import tv.giss.emercam.Constants;

import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.*;
import android.content.*;

public class OnEventReceiver extends BroadcastReceiver {
 
    static boolean isStarted = false;

	public void setStarted(boolean value) {
           isStarted = value;
        }

	// Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
	@Override
	public void onReceive(Context context, Intent intent) {
	  final String action = intent.getAction();
          Log.v( Constants.TAG, "Screen on event");
	  if ( isStarted ) {
             Log.v( Constants.TAG, "Service already started");
             return;
          } else {
             isStarted = true;
          }
          try {
            Log.v( Constants.TAG, "Launching Main Activity");
            Intent startMainIntent = new Intent(Intent.ACTION_MAIN);
            startMainIntent.setClass(context, MainActivity.class);
            startMainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMainIntent);
            Log.e( Constants.TAG, "Started Main Activity" );
          } catch ( Exception e ) {
            Log.e( Constants.TAG, "Coudn't start Main Activity : " + e.getMessage(), e );
          }
	}
}
