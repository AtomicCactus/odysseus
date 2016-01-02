package radius.com.odysseus;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by yuri on 12/21/15.
 */
public class BluetoothService extends Service implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = BluetoothService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 123;

    private static final int RSSI_MAX = -70;
    private static final int LOCK_TIMEOUT = 3000;

    private BluetoothAdapter bluetoothAdapter;
    private boolean running = true;
    private long lastInRangeTimestamp = -1;

    private Runnable lockRunnable = new Runnable() {
        @Override
        public void run() {
            while(running) {

                if (lastInRangeTimestamp > 0 && System.currentTimeMillis() - lastInRangeTimestamp < LOCK_TIMEOUT) {
                    KeyguardManager myKM = (KeyguardManager) getBaseContext().getSystemService(Context.KEYGUARD_SERVICE);
                    //if(!myKM.inKeyguardRestrictedInputMode()) {
                        DevicePolicyManager devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
                        devicePolicyManager.lockNow();
                    //}
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter.startLeScan(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.stopLeScan(this);
    }

    @Override
    public int onStartCommand(Intent paramIntent, int flags, int startId) {

        //The intent to launch when the user clicks the expanded notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Create the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setTicker("Odysseus service started.")
                .setContentTitle("Odysseus service started.")
                .setContentText("Odysseus service is running.")
                .setWhen(System.currentTimeMillis()).setAutoCancel(false)
                .setOngoing(true).setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendIntent);
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);

        // Start the lock runnable.
        new Thread(lockRunnable).start();

        // Remain in background.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

        if (device.getName() != null &&
           (device.getName().equals("disable") || device.getName().equals("enable"))) {
            Log.i(TAG, "Device: " + device.getName() + ", RSSI: " + rssi);
            if (rssi > RSSI_MAX) {
                lastInRangeTimestamp = System.currentTimeMillis();
            }
        }
    }
}
