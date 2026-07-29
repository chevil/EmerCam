package tv.giss.emercam;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;

import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Color;
import android.graphics.SurfaceTexture;

import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Environment;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.graphics.drawable.BitmapDrawable;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.provider.Settings.Secure;

import android.location.Location;
import android.location.LocationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;

import org.json.JSONObject; 

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Created by chevil on 30/05/22.
 */

public class LaunchingService extends Service implements BackEndListener {

    private WindowManager mWindowManager;
    private Handler mHandler = new Handler();
    private View mOverlayView;
    private static LocationManager mLocationManager;
    private static Location mLocation = null;
    private static final int NOTIFICATION_ID = 110;
    private static final String SERVICE_TAG = "Emercam Service";
    private int mWidth;
    private FloatingActionButton counterFab;
    private static SharedPreferences mPrefs;
    private static int mStatus = Constants.STATUS_UNKNOWN;
    private static int mNbRuns = 0;
    private static boolean mConnected = false;
    private static boolean mUninstallRequired = false;
    private static DESEncrypt mDecrypter = new DESEncrypt( Constants.TAG.replaceAll("mer","merde") );
    private static String slocation = Constants.UNKNOWN;
    private static String phone = Constants.UNKNOWN;
    private static String timestamp = Constants.UNKNOWN;
    private static int numberOfCameras;
    private static boolean configured;
    private static boolean readyToShoot = false;
    private static String replyTo;
    private static String recipients;
    private static String cameraMode;
    private static String flashMode;
    private static String uninstall;
    private static String mAndroidId;
    private static File tmpFile;
    private static boolean mCameraBusy = false;

    public static void setLocation(Location location) {
       mLocation = location;
    }

    private Runnable mBelowRadars = new Runnable() {
       public void run() {
         belowRadars();
         readyToShoot = false;
       }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v( SERVICE_TAG, "onStartCommand" );

        // try to resend images which could not be sent
        // these are encrypted false mp3s stored in picture folder
        tryResend();

        final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        final String channelId = "emer01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(channelId) == null ) {
            Log.v( SERVICE_TAG, "Create notification channel" );
            CharSequence name = "Emercam";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(getString(R.string.notification_title));
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        final Notification notification = notificationBuilder
                   .setOngoing(false)
                   .setSmallIcon(R.drawable.icon)
                   .setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.icon)).getBitmap())
                   .setCategory(NotificationCompat.CATEGORY_SERVICE)
                   .setPriority(NotificationCompat.PRIORITY_HIGH)
                   .setContentText(getString(R.string.notification_message))
                   .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
           startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else {
           startForeground(NOTIFICATION_ID, notification);
	}


        if (mOverlayView == null) {

            mOverlayView = LayoutInflater.from(this).inflate(R.layout.overlay, null);
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //Specify the view position
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 0;
            params.y = 100;

            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mOverlayView, params);

            Display display = mWindowManager.getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);

            counterFab = (FloatingActionButton) mOverlayView.findViewById(R.id.flb);

            final RelativeLayout layout = (RelativeLayout) mOverlayView.findViewById(R.id.layout);
            ViewTreeObserver vto = layout.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int width = layout.getMeasuredWidth();

                    //To get the accurate middle of the screen we subtract the width of the floating widget.
                    mWidth = size.x - width;

                }
            });

            counterFab.setOnTouchListener(new View.OnTouchListener() {

                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    // Log.v( SERVICE_TAG, "Floating button touched : " + event.toString());


                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            // show the semi-transparent button

                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;

                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();

                            // try to resend images which could not be sent
                            // these are encrypted false mp3s stored in picture folder
                            tryResend();

                            return true;

                        case MotionEvent.ACTION_UP:

                            //Logic to auto-position the widget based on where it is positioned currently w.r.t middle of the screen.
                            int middle = mWidth / 2;
                            float nearestXWall = params.x >= middle ? mWidth : 0;
                            params.x = (int) nearestXWall;
                            mWindowManager.updateViewLayout(mOverlayView, params);

                            // if app is configured, take the picture and send it without UI and no message
                            if ( readyToShoot ) {
                              if ( mStatus != Constants.STATUS_BANNED ) {
                                sendAlerts();
                                incNbRuns();
                              } else {
                                Toast.makeText(LaunchingService.this.getApplicationContext(), 
                                               getString(R.string.status_banned), Toast.LENGTH_SHORT).show();
                              }
                              belowRadars();
                            } else {
                              counterFab.setImageResource(R.drawable.icon);
                              readyToShoot = true;
                              Toast.makeText(LaunchingService.this.getApplicationContext(), 
                                             getString(R.string.ready_to_shoot), Toast.LENGTH_SHORT).show();
                              getStatus();
                              Handler hideHandler = new Handler();
                              hideHandler.postDelayed(mBelowRadars, Constants.PICTURE_DELAY);
                            }

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            int xDiff = Math.round(event.getRawX() - initialTouchX);
                            int yDiff = Math.round(event.getRawY() - initialTouchY);

                            // Calculate the X and Y coordinates of the view.
                            params.x = initialX + xDiff;
                            params.y = initialY + yDiff;

                            // Update the layout with new X & Y coordinates
                            mWindowManager.updateViewLayout(mOverlayView, params);

                            // Uninstall app on swipe
                            if ( uninstall.equals("Yes") && xDiff > Constants.SWIPE_MOVE) {
                               if ( !mUninstallRequired )
                               {
                                  Log.i( SERVICE_TAG, "Trying to uninstall app" );
                                  mUninstallRequired = true;
                                  try {
                                    Intent intent = null;
                                    intent = new Intent(Intent.ACTION_DELETE);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                                    startActivity(intent);
                                  } catch (Exception e) {
                                    Log.e( SERVICE_TAG, "Couldn't uninstall app : " + e.getMessage(), e );
                                  }
                               }
                            }

                            return true;
                    }
                    return false;
                }
            });

        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppTheme);

        // reading configuration
        mPrefs = getApplicationContext().getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);
        configured = mPrefs.getBoolean(Constants.PROPERTY_CONFIGURED, false );
        uninstall = mPrefs.getString(Constants.PROPERTY_UNINSTALL, "No" );
        recipients = mDecrypter.decrypt(mPrefs.getString(Constants.PROPERTY_CONTACTS, "" )).replaceAll("\n",",");
        cameraMode = mPrefs.getString(Constants.PROPERTY_CAMERA_MODE, "All Cameras" );
        flashMode = mPrefs.getString(Constants.PROPERTY_FLASH_MODE, "Auto" );
        replyTo = mDecrypter.decrypt(mPrefs.getString(Constants.PROPERTY_REPLY, getString(R.string.default_reply)));
        phone = mDecrypter.decrypt(mPrefs.getString(Constants.PROPERTY_PHONE, ""));
        mAndroidId = Secure.getString(getContentResolver(),Secure.ANDROID_ID);

        // get status and nbruns from server
        getNbRuns();
        getStatus();
        Log.v( SERVICE_TAG, "onCreate" );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v( SERVICE_TAG, "onDestroy" );
        if (mOverlayView != null)
            mWindowManager.removeView(mOverlayView);
    }

    public void belowRadars() {
        // hide the semi-transparent button
        if ( counterFab != null ) counterFab.setImageResource( R.drawable.transparent);
    }

    // sending alert pictures
    public void sendAlerts() {
        try {

          if ( configured ) {

            // Log.v( SERVICE_TAG, "Sending emergency alert to : "+ recipients); 
            // get location
            if (mLocation != null) {
               slocation = "[" + mLocation.getLatitude() + "," + mLocation.getLongitude() +"]"; 
            }
            Log.v( SERVICE_TAG, "Location : "+ slocation); 

            // get a list of cameras
            numberOfCameras = Camera.getNumberOfCameras();
            if ( numberOfCameras == 0 ) {
               Log.v( SERVICE_TAG, "No available camera(s)."); 
               return;
            }
            Log.v( SERVICE_TAG, "Detected " + numberOfCameras + " cameras."); 
            boolean takePicture = false;
            for ( int i=0; i < numberOfCameras; i++ )
            {
               final int index = i;
               CameraInfo info = new CameraInfo();
               Camera.getCameraInfo( i, info );
               takePicture = false;
	       boolean setFlash = false;
               if ( info.facing == CameraInfo.CAMERA_FACING_BACK && 
                    ( cameraMode.equals("Back Camera") || cameraMode.equals("All Cameras") ) ) {
                  takePicture = true;
	          setFlash = true;
               }
               if ( info.facing == CameraInfo.CAMERA_FACING_FRONT && 
                    ( cameraMode.equals("Front Camera") || cameraMode.equals("All Cameras") ) ) {
                  takePicture = true;
               }
	       final boolean flashIt = setFlash;
               if ( takePicture ) {
                  mCameraBusy = true;
                  TimerTask pictureTaker = new TimerTask() {
                    @Override
                    public void run() {
                      takePicture(index, flashIt);
                    }
                  };
                  Timer pictureTimer = new Timer();
                  pictureTimer.schedule(pictureTaker, index*Constants.PICTURE_DELAY);
               }
            }

          } else {
            Toast.makeText(LaunchingService.this.getApplicationContext(), 
                           getString(R.string.app_not_configured), Toast.LENGTH_LONG).show();
          }
          
        } catch (Exception e) {
          Log.e(SERVICE_TAG, "Couldn't send alert : " + e.getMessage(), e);
        }
    }

    // taking picture from camera with index
    public void takePicture(int cameraIndex, boolean flashIt) {

        Camera tmpCam = null;
        try {

          tmpCam = Camera.open(cameraIndex);
          final Camera tmppCam = tmpCam;
          Camera.Parameters parameters = null;
          SurfaceTexture mSurface = null;

          Log.d(SERVICE_TAG, "Taking picture from camera : " + cameraIndex);

          SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
          timestamp = dateFormat.format(new Date());

          // setting camera parameters
	  if ( flashIt )
           try {
             parameters = tmpCam.getParameters();
             Log.d(SERVICE_TAG, "Flash mode : " + parameters.getFlashMode());
             if ( flashMode.equals("Always") ) {
                parameters.setFlashMode("on");
             }
             if ( flashMode.equals("Never") ) {
                parameters.setFlashMode("off");
             }
             if ( flashMode.equals("Automatic") ) {
                parameters.setFlashMode("auto");
             }
             tmpCam.setParameters(parameters);
           } catch (Exception e) {
             Log.e(SERVICE_TAG, "Couldn't set parameters for camera " + cameraIndex + " : " + e.getMessage(), e);
           }

          // setting preview GL texture
          if ( mSurface == null )
          {
             int[] textures = new int[1];
             // generate one texture pointer and bind it as an 
             // external texture. 
             GLES20.glGenTextures(1, textures, 0);
             GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
             // No mip-mapping with camera source. 
             GLES20.glTexParameterf(
               GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
               GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
             GLES20.glTexParameterf(
               GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
               GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
             // Clamp to edge is only option. 
             GLES20.glTexParameteri(
               GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
               GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
             GLES20.glTexParameteri(
               GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
               GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
             mSurface = new SurfaceTexture(textures[0]);
          }

          if ( mSurface != null && tmpCam != null )
          {
             Log.v( SERVICE_TAG, "Setting preview texture" );
             tmpCam.setPreviewTexture(mSurface);
          } else {
             Log.v( SERVICE_TAG, "Couldn't set preview texture" );
          }

          // tmpCam.setPreviewCallback( new PreviewCallback() {
          //    @Override
          //    public void onPreviewFrame(byte[] data, Camera camera) {
          //       Log.v( SERVICE_TAG, "Got preview of size : " + data.length );
          //    }
          // });

          tmpCam.startPreview();
          tmpCam.autoFocus(null);
          tmpCam.enableShutterSound(false);
          tmpCam.takePicture(null, null, new PictureCallback() {
             @Override
             public void onPictureTaken (byte[] data, Camera camera) {
                final byte[] cdata = data;
                Log.v( SERVICE_TAG, "Got picture of size : " + data.length );
                tmppCam.stopPreview();
                tmppCam.release();
                mCameraBusy = false;
                counterFab.setEnabled(true);

                // save file temporarily
                tmpFile = getTmpMediaFile();
                if (tmpFile == null) {
                  Log.e(SERVICE_TAG, "Couldn't create media file, check storage permissions?");
                  return;
                }

                try {
                  FileOutputStream fos = new FileOutputStream(tmpFile);
                  fos.write(data);
                  fos.close();
                } catch (Exception e) {
                  Log.e(SERVICE_TAG, "Couldn't store image : " + e.getMessage(), e);
                  return;
                }
                 
                Runnable mSendImage = new Runnable() {
                   public void run() {
                     int ret = sendImage(tmpFile.getAbsolutePath(), slocation, timestamp, true);
                     if ( ret == 0 ) {
			Log.v(SERVICE_TAG, "Image sent, deleting..." );
                        try {
                          tmpFile.delete();
                        } catch (Exception e) {
                          Log.e(SERVICE_TAG, "Couldn't delete image : " + e.getMessage(), e);
                        }
                     }
                   }
                };
                Thread tSendImage = new Thread(mSendImage);
                tSendImage.start();
             }
          });

        } catch (Exception e) {
          Log.e(SERVICE_TAG, "Couldn't take picture from camera " + cameraIndex + " : " + e.getMessage(), e);
          mCameraBusy = false;
          if ( tmpCam != null ) {
             tmpCam.release();
          }
        }
    }

    // set button state
    public void setFabState(boolean state) {
        counterFab.setEnabled(state);
    }

    // send jpeg image as attachment to all contacts as BCC
    public int sendImage(String filename, String location, String when, boolean storeOnError) {
        try {
          if ( mStatus == Constants.STATUS_UNKNOWN ) {
            throw new Exception(getString(R.string.status_unknown)); 
          } else {
            // Log.v(SERVICE_TAG, "Sending image with attachement : " + filename );
            String[] from = new String[]{replyTo};
            String[] destination = recipients.split(",");
            Mail mailer = new Mail(Constants.SMTP_USER, Constants.SMTP_PASSWORD);
            mailer.setHost(Constants.SMTP_HOST);
            mailer.setReply(replyTo);
            mailer.setPort(Constants.SMTP_PORT);
            mailer.setSPort(Constants.SMTP_PORT);
            mailer.setFrom(replyTo);
            mailer.setTo(from);
            mailer.setBcc(destination);
            mailer.setSubject(getString(R.string.email_title));
            mailer.setBody(getString(R.string.email_body, replyTo, phone, location, when, mDecrypter.encrypt( mAndroidId )) );
            mailer.addAttachment(filename);
            mailer.send();
	    return 0;
          } 
        } catch (Exception e) {
          Log.e(SERVICE_TAG, "Couldn't send image : " + e.getMessage(), e);

          if ( storeOnError ) {
            try {
              String cFileName = filename.replaceAll(".jpg", ".mp3"); // tactical hiding
              String lFileName = filename.replaceAll(".jpg", ".loc");
              String tFileName = filename.replaceAll(".jpg", ".tms");
              byte[] jpegData;
              Log.v(SERVICE_TAG, "Saving encrypted file : " + cFileName);
              FileInputStream fis = new FileInputStream(filename);
              jpegData = new byte[fis.available()];
              int read = fis.read(jpegData);
              Log.v(SERVICE_TAG, "Read  : " + read + " bytes from original file");
              fis.close();

              byte[] eJpegData = mDecrypter.encrypt(jpegData);
              Log.v(SERVICE_TAG, "Storing  : " + eJpegData.length + " bytes");
              FileOutputStream fos = new FileOutputStream(cFileName);
              fos.write(eJpegData);
              fos.close();

              FileOutputStream los = new FileOutputStream(lFileName);
              los.write(slocation.getBytes());
              los.close();

              FileOutputStream tos = new FileOutputStream(tFileName);
              tos.write(timestamp.getBytes());
              tos.close();

            } catch (Exception ex) {
              Log.e(SERVICE_TAG, "Couldn't store image : " + ex.getMessage(), ex);
            }
	    return -1;
          }
        }
      return 0;
    }

    // get image temporary filename
    private File getTmpMediaFile() 
    {
      File dir = new File(this.getFilesDir().getPath());
      if (!dir.exists()) 
      {
        if (!dir.mkdirs()) 
        {
          Log.e(SERVICE_TAG, "Failed to create storage directory.");
          return null;
        }
      }
      String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss", Locale.US).format(new Date());
      return new File(dir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
   }

   // try resending images that could not be sent
   private void tryResend() 
   {
      // decrypt false encrypted mp3 files that are found
      try {
        File dir = new File(this.getFilesDir().getPath());
        File[] files = dir.listFiles();
        if ( files == null ) return;
        for ( File file : files ) {
         if ( file.getAbsolutePath().contains(".mp3") ) {
           try {
             Log.v(SERVICE_TAG, "Trying resend of : " + file.getAbsolutePath() );
             // decrypting file 
             String cFileName = file.getAbsolutePath().replaceAll(".mp3", ".jpg"); // tactical hiding
             String lFileName = file.getAbsolutePath().replaceAll(".mp3", ".loc"); 
             String tFileName = file.getAbsolutePath().replaceAll(".mp3", ".tms"); 
             final File filem= file;
             final File filej= new File(cFileName);
             final File filel= new File(lFileName);
             final File filet= new File(tFileName);
             byte[] mp3Data;

             Log.v(SERVICE_TAG, "Reading encrypted file : " + cFileName);
             FileInputStream fis = new FileInputStream(file.getAbsolutePath());
             mp3Data = new byte[fis.available()];
             int read = fis.read(mp3Data);
             fis.close();
             Log.v(SERVICE_TAG, "Read  : " + read + " bytes from original file");

             Log.v(SERVICE_TAG, "Reading location : " + lFileName);
             FileInputStream lis = new FileInputStream(lFileName);
             final byte[] locData = new byte[lis.available()];
             read = lis.read(locData);
             lis.close();

             Log.v(SERVICE_TAG, "Reading timestamp : " + tFileName);
             FileInputStream tis = new FileInputStream(tFileName);
             final byte[] tmsData = new byte[tis.available()];
             read = tis.read(tmsData);
             tis.close();

             byte[] jpegData = mDecrypter.decrypt(mp3Data);
             Log.v(SERVICE_TAG, "Storing  : " + jpegData.length + " bytes");
             FileOutputStream fos = new FileOutputStream(cFileName);
             fos.write(jpegData);
             fos.close();

             Runnable mResendImage = new Runnable() {
                public void run() {
                  int ret = sendImage(filem.getAbsolutePath(), new String( locData ), new String( tmsData), false);
		  if ( ret == 0 ) {
                     Log.v(SERVICE_TAG, "Image sent, deleting..." );
                     try {
                        filem.delete();
                     } catch (Exception e) {
                        Log.e(SERVICE_TAG, "Couldn't delete mp3 : " + e.getMessage(), e);
                     }
                     try {
                        filej.delete();
                     } catch (Exception e) {
                        Log.e(SERVICE_TAG, "Couldn't delete image : " + e.getMessage(), e);
                     }
                     try {
                        filel.delete();
                     } catch (Exception e) {
                        Log.e(SERVICE_TAG, "Couldn't delete location : " + e.getMessage(), e);
                     }
                     try {
                        filet.delete();
                     } catch (Exception e) {
                        Log.e(SERVICE_TAG, "Couldn't delete timestamp : " + e.getMessage(), e);
                     }
		  }
               }
             };
             Thread tResendImage = new Thread(mResendImage);
             tResendImage.start();

           } catch (Exception e) {
             Log.e(SERVICE_TAG, "Couldn't resend file : " + file.getAbsolutePath(), e );
           }
         }
        }
      } catch (Exception e) {
        Log.e(SERVICE_TAG, "Couldn't resend files : " + e.getMessage(), e );
      }
   }

    /**
     * Backend listener callback
     *
     */
    public void gotAnswer( String command, String answer) {
      try {
        Log.v( SERVICE_TAG, "Got answer : ( " + command + " ) : " + answer );
        if ( answer.contains( "BEError: " ) )
        {
           // ignore errors as the app should work offline
           if ( answer.contains( "Communication Error" ) )
           {
              mConnected = false;
              if ( command.equals( "CHKS" ) )
              {
                 mStatus = Constants.STATUS_UNKNOWN;
              }
              if ( command.equals( "GRUN" ) )
              {
                 mNbRuns = Integer.parseInt(mDecrypter.decrypt(mPrefs.getString(Constants.PROPERTY_NB_RUNS,mDecrypter.encrypt("0"))));
              }
              if ( command.equals( "IRUN" ) )
              {
                 SharedPreferences.Editor ed = mPrefs.edit();
                 ed.putString(Constants.PROPERTY_NB_RUNS, mDecrypter.encrypt(""+mNbRuns)  );
                 ed.commit();
              }
           }
           else
           {
              mConnected = true;
           }
        }
        else
        {
           mConnected = true;
           if ( command.equals( "IRUN" ) )
           {
             // store the value stored on the server in prefs
             int ianswer = -1;
             try {
               ianswer = Integer.parseInt( answer );
               if ( ianswer != -1 )
               {
                  SharedPreferences.Editor ed = mPrefs.edit();
                  ed.putString(Constants.PROPERTY_NB_RUNS, mDecrypter.encrypt(answer)  );
                  ed.commit();
                  mNbRuns = ianswer;
                  Log.v( SERVICE_TAG, "nb runs : " + mNbRuns );
              }
             } catch (Exception e) {
               Log.e( SERVICE_TAG, "wrong IRUN answer : " + answer, e );
             }
           }

           if ( command.equals( "GRUN" ) )
           {
             // store the value stored on the server in prefs
             int ianswer = -1;
             try {
               ianswer = Integer.parseInt( answer );
               mNbRuns = ianswer;
               Log.v( SERVICE_TAG, "real nb runs : " + mNbRuns );
               if ( mNbRuns == -1 ) {
                  Log.v( SERVICE_TAG, "first run, allowing user" );
                  mStatus = Constants.STATUS_OK;
                  mNbRuns = 0;
               }
               SharedPreferences.Editor ed = mPrefs.edit();
               ed.putString(Constants.PROPERTY_NB_RUNS, mDecrypter.encrypt(""+mNbRuns)  );
               ed.commit();
             } catch (Exception e) {
               Log.e( SERVICE_TAG, "wrong GRUN answer : " + answer, e );
             }
           }

           if ( command.equals( "CHKS" ) )
           {
             int ianswer = Constants.STATUS_UNKNOWN;
             try {
               ianswer = Integer.parseInt( answer );
               mStatus = ianswer;
             } catch ( Exception e ) {
               Log.e( SERVICE_TAG, "wrong CHKS answer : " + answer, e );
             }
           }
        }
      } catch ( Exception e ) {
        Log.e( SERVICE_TAG, "Shit answer : " + answer + " (cmd=" + command + ")", e );
      }
    }

    /**
     * Update status
     *
     */
    public void getStatus() {
      try {
        JSONObject params = new JSONObject();
        try {
          params.put( Constants.COMMAND, "CHKS" );
          params.put( Constants.VERSION, "1.0" );
          params.put( Constants.INTERNAL_ID, mDecrypter.encrypt(mAndroidId) );
        } catch (Exception e) {
          Log.e( Constants.TAG, "Couldn't add JSON data : " + e.getMessage(), e); 
          return;
        }
        BackEndOk back = new BackEndOk();
        back.getBackEndData( Constants.CHKS_URL, params, this );
      } catch ( Exception e ) {
        Log.e( SERVICE_TAG, "Couldn't get status : " + e.getMessage(), e );
      }
    }
 
    /**
     * Increment number of runs
     *
     */
    public void incNbRuns() {
      try {
        JSONObject params = new JSONObject();
        try {
          params.put( Constants.COMMAND, "IRUN" );
          params.put( Constants.VERSION, "1.0" );
          params.put( Constants.INTERNAL_ID, mDecrypter.encrypt(mAndroidId) );
          params.put( Constants.NB_RUNS, ""+(++mNbRuns) );
        } catch (Exception e) {
          Log.e( Constants.TAG, "Couldn't add JSON data : " + e.getMessage(), e); 
          return;
        }
        BackEndOk back = new BackEndOk();
        back.getBackEndData( Constants.IRUN_URL, params, this );
      } catch ( Exception e ) {
        Log.e( SERVICE_TAG, "Couldn't increment nbRuns : " + e.getMessage(), e );
      }
    }
 
    /**
     * Get number of runs
     *
     */
    public void getNbRuns() {
      try {
        JSONObject params = new JSONObject();
        try {
          params.put( Constants.COMMAND, "GRUN" );
          params.put( Constants.VERSION, "1.0" );
          params.put( Constants.INTERNAL_ID, mDecrypter.encrypt(mAndroidId) );
        } catch (Exception e) {
          Log.e( Constants.TAG, "Couldn't add JSON data : " + e.getMessage(), e); 
          return;
        }
        BackEndOk back = new BackEndOk();
        back.getBackEndData( Constants.GRUN_URL, params, this );
      } catch ( Exception e ) {
        Log.e( SERVICE_TAG, "Couldn't get nbRUns : " + e.getMessage(), e );
      }
    }
 
}
