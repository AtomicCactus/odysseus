package radius.com.odysseus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.att.m2x.java.M2XClient;
import com.att.m2x.java.M2XDevice;
import com.att.m2x.java.M2XResponse;
import com.att.m2x.java.M2XStream;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.ParseException;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * Created by yuri on 1/2/16.
 */
public class UserPresentBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = UserPresentBroadcastReceiver.class.getSimpleName();
    private static final String M2X_KEY = "cfa2ed227b29a1397eeed0c77cdb831d";
    private static final String M2X_DEVICE = "737b1aefddb922bea14ad8d0ce3b29ea";
    private static final String M2X_STREAM = "cell_usage";
    private static final String M2X_URL = String.format("http://api-m2x.att.com/v2/devices/%s/streams/%s/values", M2X_DEVICE, M2X_STREAM);
    private static final String DEVICE_ON_JSON = "{\"values\":[{\"value\":0,\"timestamp\":\"%s\"},{\"value\":1,\"timestamp\":\"%s\"}]}";
    private static final String DEVICE_OFF_JSON = "{\"values\":[{\"value\":1,\"timestamp\":\"%s\"},{\"value\":0,\"timestamp\":\"%s\"}]}";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'z'", Locale.US);

    @Override
    public void onReceive(final Context context, final Intent intent) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("gmt"));
        Date date1 = new Date(System.currentTimeMillis() - 100);
        Date date2 = new Date();
        /* Sent when the user is present after
         * device wakes up (e.g when the keyguard is gone)
         * */
        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            String json = String.format(DEVICE_ON_JSON, DATE_FORMAT.format(date1), DATE_FORMAT.format(date2));
            postValue(context, json);
        }
        /* Device is shutting down. This is broadcast when the device
         * is being shut down (completely turned off, not sleeping)
         * */
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            String json = String.format(DEVICE_OFF_JSON, DATE_FORMAT.format(date1), DATE_FORMAT.format(date2));
            postValue(context, json);
        }
    }

    private void postValue(Context context, String json) {
        AsyncHttpClient client = new AsyncHttpClient();
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        Header apiKey = new BasicHeader("X-M2X-KEY", M2X_KEY);
        Header headers[] = {apiKey};
        client.post(context, M2X_URL, headers, entity, ContentType.APPLICATION_JSON.toString(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i(TAG, new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, new String(responseBody));
            }
        });
    }
}
