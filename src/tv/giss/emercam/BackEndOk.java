package tv.giss.emercam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.KeyStore;
import java.security.SecureRandom;

import org.json.JSONObject;
import org.json.JSONException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackEndOk {

   private String answer = null;
   private String command = null;
   private String requestURL = null;
   private JSONObject params = null;
   private BackEndListener caller = null;

   // worker to get the POST data
   private class POSTWORK implements Runnable {

          private long startTime;
          private long endTime;

          @Override
          public void run() {
            try
            {
               startTime = System.currentTimeMillis();

               // Create a OkHttpClient object so we can set our timeout times.
               MediaType MEDIA_TYPE = MediaType.parse("application/json");
               OkHttpClient client = new OkHttpClient();

               try {
                 command = params.get(Constants.COMMAND).toString();
               } catch( JSONException e) {
                 Log.e(Constants.TAG, "No command in post " + e.getMessage(), e);
                 return;
               }

               RequestBody body = RequestBody.create(MEDIA_TYPE, params.toString());

               Request request = new Request.Builder()
                    .url(requestURL)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build();
        
               Response response = client.newCall(request).execute();

               // Log.v(Constants.TAG, "response status: " + response.getStatusLine().getStatusCode());
               if ( response.code() == 200 )
               {
                  answer = response.body().string();
                  endTime = System.currentTimeMillis();
                  // Log.v(Constants.TAG, "response size: " + answer.length());
                  // Log.v(Constants.TAG, "response: t=" + (endTime-startTime) + "ms");
                  // Log.v(Constants.TAG, "" + answer);
               }
               else
               {
                  answer = "BEError: Communication Error " + response.code();
               }
            }
            catch (Exception e)
            {
               Log.e(Constants.TAG, "POSTWORK : Exception: " + e.getMessage(), e);
               answer = "BEError: Communication Error : " + e.getMessage();
            }
            finally 
            {
               if ( caller != null )   
               {
                  caller.gotAnswer( command, answer );
               }
            }
          }

        }

        public void getBackEndData(String url, JSONObject params, BackEndListener caller) {
            this.requestURL = url;
            this.params = params;
            this.caller = caller;

            try
            {
               POSTWORK postwork = new POSTWORK();
               Thread Tpost = new Thread(postwork);
               Tpost.start();
               return;
            }
            catch (Exception e)
            {
               Log.e(Constants.TAG, "Exception launchind thread : " + e.getMessage(), e);
               if ( caller != null )   
               {
                  caller.gotAnswer( command, "BEError: Task Error" );
               }
               return;
            }
        }

}
