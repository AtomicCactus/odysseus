package radius.com.odysseus;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.firebase.client.Firebase;
import com.loopj.android.http.*;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    // Package name / ADID
    private static final String DATA_REWARD_URL_FORMAT = "http://aus-api.cloudmi.datami.com/dev/goapi/action/registration/radius.com.odysseus/%s";
    private static final int ADMIN_INTENT = 15;
    private static final String ADID = "ADID";
    private static final String PREFS = "Prefs";
    private static final String description = "This application needs to be able to lock your device's screen.";
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mComponentName = new ComponentName(this, AdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Firebase.setAndroidContext(this);
        if (!prefs.contains(ADID)) {
            fetchAdvertisingId();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceAdmin();
        startBluetoothService();
        fetchAndUploadApplicationData();

        // Show a full-screen activity.
        Intent fullScreenIntent = new Intent(getApplicationContext(), FullscreenActivity.class);
        startActivity(fullScreenIntent);

        finish();
    }

    private class AppInfo {
        protected String name;
        protected String icon;
        protected boolean enabled;
    }

    private void fetchAndUploadApplicationData() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        // Create Firebase payload.
        final List<Map<String, Object>> payload = new LinkedList<Map<String, Object>>();
        for (ApplicationInfo app : apps) {
            String label = (String)pm.getApplicationLabel(app);
            if (label.equals("Odysseus")) {
                continue;
            }
            String byteString = "";
            try {
                Drawable icon = pm.getApplicationIcon(app);
                Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bytes = stream.toByteArray();
                byteString = Base64.encodeToString(bytes, Base64.DEFAULT);
            } catch (Exception e) {

            }
            Map<String, Object> info = new HashMap<String, Object>();
            info.put("name", label);
            info.put("icon", byteString);
            info.put("enabled", true);
            payload.add(info);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Update Firebase.
                Firebase firebaseRef = new Firebase("https://intense-inferno-4940.firebaseIO.com");
                firebaseRef.removeValue();
                firebaseRef.setValue(payload);
                Log.i(TAG, "Uploaded " + payload.size() + " app descriptors");
            }
        }).start();
    }

    private void getDeviceAdmin() {
        if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,description);
            startActivityForResult(intent, ADMIN_INTENT);
        }
    }

    private void startBluetoothService() {
        Intent intent = new Intent(MainActivity.this, BluetoothService.class);
        MainActivity.this.startService(intent);
    }

    public void onLockButtonClick(View view) {
        mDevicePolicyManager.lockNow();
    }

    private void fetchAdvertisingId() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] params) {
                String adid = "";
                try{
                    AdvertisingIdClient.AdInfo adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    adid = adInfo.getId();
                }catch(Exception e){
                    e.printStackTrace();
                }
                return adid;
            }
            protected void onPostExecute(String result) {
                //Store the adId in preferences
                SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ADID, result);
                editor.commit();
                //sendSms(result);
                rewardUserForRegistration();
            };
        }.execute(null, null, null);
    }

    private void sendSms(String adId) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:+17023816992"));
        smsIntent.putExtra("sms_body", adId);
        startActivity(smsIntent);
    }

    // Rewards the user for installing the app.
    private void rewardUserForRegistration() {
        AsyncHttpClient client = new AsyncHttpClient();
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String adid = prefs.getString(ADID, "");
        String url = String.format(DATA_REWARD_URL_FORMAT, adid);
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i(TAG, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                if (errorResponse == null) {
                    return;
                }
                Log.i(TAG, new String(errorResponse));
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }
}
