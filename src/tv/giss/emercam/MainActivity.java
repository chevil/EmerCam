package tv.giss.emercam;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager ;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.Manifest;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Timer;
import java.util.TimerTask;
  
public class MainActivity extends Activity {

    private static final int DRAW_OVER_OTHER_APP_PERMISSION = 123;
    private static final int ALL_OTHER_APP_PERMISSIONS = 126;
    private static LocationManager mLocationManager;
    private static Location mLocation = null;
    private static Timer mTimer = null;
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
        //                                                  View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | 
        //                                                  View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // hide status bar
        //                                                  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | // hide nav bar
        //                                                  View.SYSTEM_UI_FLAG_FULLSCREEN | 
        //                                                  View.SYSTEM_UI_FLAG_IMMERSIVE);

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        askForSystemOverlayPermission();

	// set a SCREEN_ON receiver
        // IntentFilter filter = new IntentFilter();
        // filter.addAction(Intent.ACTION_SCREEN_ON);
	// OnEventReceiver onEventReceiver = new OnEventReceiver();
	// onEventReceiver.setStarted(true);
        // registerReceiver(onEventReceiver, filter);
	//

    }

    /** update location **/
    public void updateLocation() {

     Location nlocation;

      // firt check the location of the GPS ( only works outdoors )
      nlocation = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
      if ( nlocation != null )
      {
         mLocation = nlocation;
      }
      else
      {
         nlocation = mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
         if ( nlocation != null )
         {  
            mLocation = nlocation;
         }
      }

      if ( mLocation != null ) {
         LaunchingService.setLocation( mLocation );
         // Log.i( Constants.TAG, "Got location : [" + mLocation.getLongitude() + "," + mLocation.getLatitude() + "]");
      }
    }

    class UpdateLocationTask extends TimerTask {
       public void run() {
           updateLocation();
           mTimer.schedule(new UpdateLocationTask(), 0, 10000);
       }
    }

    private void askForSystemOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            Log.e( Constants.TAG, "Platform : " + Build.VERSION.SDK_INT);
            // ask for all permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
               askForAllPermissions33();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
               askForAllPermissions28();
            } else {
               askForAllPermissions();
            }
        }
    }

    private void askForAllPermissions33() {
        // dynamically ask for permissions
        if ( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_DELETE_PACKAGES) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION,
                                 Manifest.permission.CAMERA,
                                 Manifest.permission.RECEIVE_BOOT_COMPLETED,
                                 Manifest.permission.FOREGROUND_SERVICE,
                                 Manifest.permission.POST_NOTIFICATIONS,
                                 Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                 Manifest.permission.REQUEST_DELETE_PACKAGES,
                                 Manifest.permission.INTERNET}, ALL_OTHER_APP_PERMISSIONS);
        } else {
            startFloatingServiceAndExit();
        }
    }

    private void askForAllPermissions28() {
        // dynamically ask for permissions
        if ( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_DELETE_PACKAGES) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION,
                                 Manifest.permission.CAMERA,
                                 Manifest.permission.RECEIVE_BOOT_COMPLETED,
                                 Manifest.permission.FOREGROUND_SERVICE,
                                 Manifest.permission.REQUEST_DELETE_PACKAGES,
                                 Manifest.permission.INTERNET}, ALL_OTHER_APP_PERMISSIONS);
        } else {
            startFloatingServiceAndExit();
        }
    }

    private void askForAllPermissions() {
        // dynamically ask for permissions
        if ( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) ||
             (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION,
                                 Manifest.permission.CAMERA,
                                 Manifest.permission.RECEIVE_BOOT_COMPLETED,
                                 Manifest.permission.INTERNET}, ALL_OTHER_APP_PERMISSIONS);
        } else {
            startFloatingServiceAndExit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DRAW_OVER_OTHER_APP_PERMISSION:
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 if (!Settings.canDrawOverlays(this)) {
                    cannotRun();
                    return;
                 }
              }
              Log.e( Constants.TAG, "Platform : " + Build.VERSION.SDK_INT);
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                 askForAllPermissions33();
              } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                 askForAllPermissions28();
              } else {
                 askForAllPermissions();
              }
              break;
        }
    } 

    // Get permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.e( Constants.TAG, "Permission result for : " + requestCode + " : " + grantResults.length + " result(s)");
        if ( grantResults.length < 3 ) return;
        switch (requestCode) {
            case ALL_OTHER_APP_PERMISSIONS: {
                if (grantResults.length > 0 )
                {
                    for ( int ip=0; ip<grantResults.length; ip++ )
                    {
                      Log.d( Constants.TAG, "Permission : " + permissions[ip] + " got : " + grantResults[ip] );
                      if ( grantResults[ip] != PackageManager.PERMISSION_GRANTED ) {
                        cannotProceed();
                        return;
                      }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    String packageName = getPackageName();
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivity(intent);
                    }
                }

                startFloatingServiceAndExit();
            }
        }
    }

    private void cannotRun() {
        Toast.makeText(this, getString(R.string.cannot_run), Toast.LENGTH_LONG).show();
        this.finish();
    }

    private void cannotProceed() {
        Toast.makeText(this, getString(R.string.cannot_proceed), Toast.LENGTH_LONG).show();
        this.finish();
    }

    private void startFloatingServiceAndExit() {

        // we have permissions now
        mTimer = new Timer(true);
        mTimer.schedule(new UpdateLocationTask(), 0, 1000);

        // disable app and hide icon
        try {
          PackageManager p = getPackageManager();
          Log.i(Constants.TAG, "Disabling :" + getPackageName() + ".SettingsActivity"  );
          p.setComponentEnabledSetting(new ComponentName(getPackageName(), getPackageName()+".SettingsActivity"),
                                       PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                       PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
          Log.e(Constants.TAG, "Couldn't disable app :" + e.getMessage(), e );
        }

        startService(new Intent(MainActivity.this, LaunchingService.class));
        finish();
    }

}
