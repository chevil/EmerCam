package tv.giss.emercam;

import android.os.Bundle;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.provider.Settings;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.Patterns;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
  
public class SettingsActivity extends Activity {

  private static DESEncrypt mCrypter = new DESEncrypt( Constants.TAG.replaceAll("mer","merde") );
  private static SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getApplicationContext().getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
        //                                                  View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | 
        //                                                  View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // hide status bar
        //                                                  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | // hide nav bar
        //                                                  View.SYSTEM_UI_FLAG_FULLSCREEN | 
        //                                                  View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.settings);

        final EditText reply = (EditText) findViewById(R.id.reply);
        final EditText phone = (EditText) findViewById(R.id.phone);
        final EditText econtacts = (EditText) findViewById(R.id.econtacts);

        final Spinner cameras = (Spinner) findViewById(R.id.cameras);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.used_cameras, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cameras.setAdapter(adapter);

        final Spinner flash = (Spinner) findViewById(R.id.flash_modes);
        adapter = ArrayAdapter.createFromResource(
            this, R.array.flash_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        flash.setAdapter(adapter);

        final Spinner uninstall = (Spinner) findViewById(R.id.uninstall);
        adapter = ArrayAdapter.createFromResource(
            this, R.array.no_yes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uninstall.setAdapter(adapter);

        Button save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

              final String friends = econtacts.getText().toString(); 
              if ( friends.equals("") ) {
                 Toast.makeText(SettingsActivity.this, getString(R.string.no_contacts), Toast.LENGTH_LONG).show();
                 return;
              }
              Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
              String[] myFriends = friends.split("\n");
              for (String friend : myFriends) {
                 // Log.v( Constants.TAG, "Checking : " + friend );
                 if (!emailPattern.matcher(friend).matches()) {
                    Toast.makeText(SettingsActivity.this, getString(R.string.wrong_email_format), Toast.LENGTH_LONG).show();
                    return;
                 }
              }
              final String cameraMode = cameras.getSelectedItem().toString(); 
              final String flashMode = flash.getSelectedItem().toString(); 
              final String sreply = reply.getText().toString(); 
              if (!emailPattern.matcher(sreply).matches()) {
                 Toast.makeText(SettingsActivity.this, getString(R.string.wrong_reply_format), Toast.LENGTH_LONG).show();
                 return;
              }
              final String sphone = phone.getText().toString(); 
              final String suninstall = uninstall.getSelectedItem().toString(); 

              AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
              builder.setTitle(R.string.are_you_sure);
              builder.setMessage(getString(R.string.save_warning));
              builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
              {
                 public void onClick(DialogInterface dialog, int id) {
                    // store encrypted contacts in the preferences
                    SharedPreferences.Editor ed = mPrefs.edit();
                    ed.putString(Constants.PROPERTY_CONTACTS, mCrypter.encrypt(friends) );
                    ed.putString(Constants.PROPERTY_CAMERA_MODE, cameraMode );
                    ed.putString(Constants.PROPERTY_FLASH_MODE, flashMode );
                    ed.putBoolean(Constants.PROPERTY_CONFIGURED, true );
                    ed.putString(Constants.PROPERTY_REPLY, mCrypter.encrypt(sreply) );
                    ed.putString(Constants.PROPERTY_PHONE, mCrypter.encrypt(sphone) );
                    ed.putString(Constants.PROPERTY_UNINSTALL, suninstall );
                    ed.commit();

                    // configuration is complete, goto operational mode
                    startMainActivity();

                 }
              });
              builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
              {
                 public void onClick(DialogInterface dialog, int id) {
                    // nothing
                 }
              });
              builder.show();
            }
        });
    }

    @Override
    protected void onStop() {
       super.onStop();
    }

    private void startMainActivity() {

      if ( mPrefs.getBoolean(Constants.PROPERTY_CONFIGURED, false ) ) {

          // starts main activity that asks permissions and activate service
          try {
            Log.v( Constants.TAG, "Launching Main Activity");
            Intent startMainIntent = new Intent(Intent.ACTION_MAIN);
            startMainIntent.setClass(this, MainActivity.class);
            startMainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMainIntent);
            Log.e( Constants.TAG, "Started Main Activity" );
            finish();
          } catch ( Exception e ) {
            Log.e( Constants.TAG, "Coudn't start Main Activity : " + e.getMessage(), e );
          }

      } else {
          Toast.makeText(this, getString(R.string.app_not_configured), Toast.LENGTH_LONG).show();
      }

   }

}

 



