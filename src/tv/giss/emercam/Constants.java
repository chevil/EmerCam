package tv.giss.emercam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.graphics.Typeface;

public class Constants {

   public static final String TAG="Emercam";
   public static final String PREFS_FILE_NAME="click.mp3";

   public static final String PROPERTY_CONFIGURED="configured";
   public static final String PROPERTY_CONTACTS="contacts";
   public static final String PROPERTY_REPLY="alert_reply";
   public static final String PROPERTY_PHONE="alert_phone";
   public static final String PROPERTY_CAMERA_MODE="camera_mode";
   public static final String PROPERTY_FLASH_MODE="flash_mode";
   public static final String PROPERTY_UNINSTALL="uninstall";
   public static final String PROPERTY_NB_RUNS="nbruns";
   public static final String PROPERTY_STATUS="status";
   public static final String UNKNOWN="Unknown";

   public static final int LOCATION_MAX_TRIES=3;
   public static final int PICTURE_DELAY=10000; // 10 seconds
   public static final int SWIPE_MOVE=20; // 20 pixels
   public static final int CONNECT_TIMEOUT=50000;
   public static final int READ_TIMEOUT=50000;
   public static final int TIMEOUT=50000;

   public static final String SMTP_HOST = "smtp.gmail.com";
   public static final String SMTP_PORT = "465"; // 465 for SSL and 587 for TLS
   public static final String SMTP_USER = "gissnetwork@gmail.com";
   public static final String SMTP_PASSWORD = "tidyoagfcvkxnenw"; // Emercam app passwaord
   public static final String DEFAULT_REPLY = "gissnetwork@gmail.com";

   // back-end
   public static final String BE_URL="https://giss.tv/emercam/api/";
   public static final String IRUN_URL=BE_URL+"irun.php";
   public static final String GRUN_URL=BE_URL+"grun.php";
   public static final String CHKS_URL=BE_URL+"chks.php";
   public static final int STATUS_UNKNOWN=-1;
   public static final int STATUS_OK=0;
   public static final int STATUS_BANNED=1;
   public static final String COMMAND="cmd";
   public static final String VERSION="ver";
   public static final String INTERNAL_ID="intId";
   public static final String NB_RUNS="nbRuns";

}
