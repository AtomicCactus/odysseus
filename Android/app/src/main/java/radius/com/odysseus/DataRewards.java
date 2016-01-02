package radius.com.odysseus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

/**
 * Created by yuri on 1/2/16.
 */
public class DataRewards {

    private static final String ADID = "ADID";

    public void fetchAdvertisingId(Context context) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] params) {
                String adid = "";
                try{
                    AdvertisingIdClient.AdInfo adInfo =
                            AdvertisingIdClient.getAdvertisingIdInfo(context);
                    adid = adInfo.getId();
                }catch(Exception e){
                    e.printStackTrace();
                }
                return adid;
            }
            protected void onPostExecute(String result) {
                //Store the adId in preferences
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(ADID, result);
                editor.commit();
            };
        }.execute(null,null,null);
    }
}
