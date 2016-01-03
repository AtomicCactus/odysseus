package radius.com.odysseus;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class BluetoothService extends Service implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = BluetoothService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 123;

    private static final int RSSI_MAX = -70;
    private static final int LOCK_TIMEOUT = 2000;

    private BluetoothAdapter bluetoothAdapter;
    private boolean running = true;
    private long lastInRangeTimestamp = -1;

    private LinkedList<Integer> rssiHistory = new LinkedList<Integer>();

    private Runnable lockRunnable = new Runnable() {
        @Override
        public void run() {
            while(running) {

                if (lastInRangeTimestamp > 0 && System.currentTimeMillis() - lastInRangeTimestamp < LOCK_TIMEOUT) {

                    // Uncomment to disable the whole device.
                    //KeyguardManager myKM = (KeyguardManager) getBaseContext().getSystemService(Context.KEYGUARD_SERVICE);
                    //if(!myKM.inKeyguardRestrictedInputMode()) {
                        //DevicePolicyManager devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
                        //devicePolicyManager.lockNow();
                    //}

                    if (!isActivityRunning()) {
                        Intent dialogIntent = new Intent(BluetoothService.this, FullscreenActivity.class);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(dialogIntent);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    sendBroadcast(new Intent(FullscreenActivity.CLOSE_INTENT));
                }
            }
        }
    };

    public boolean isActivityRunning() {
        ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (FullscreenActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()))
                return true;
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter.startLeScan(this);

        // Monitor screen off events.
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new UserPresentBroadcastReceiver(), screenStateFilter);
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

    private BluetoothDevice bleDevice = null;
    private BluetoothGatt gatt = null;


    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "New BLE connection state: " + newState);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        gatt.readRemoteRssi();
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            rssiHistory.add(rssi);
            if (rssiHistory.size() < 10) {
                return;
            } else {
                rssiHistory.removeFirst();
            }
            int average = 0;
            for (Integer value : rssiHistory) {
                average += value;
            }
            average = average / rssiHistory.size();
            Log.i(TAG, "RSSI (avg): " + average);
            if (average > RSSI_MAX) {
                lastInRangeTimestamp = System.currentTimeMillis();
            }
        }
    };

    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        if (device.getAddress().equals("98:4F:EE:04:7B:50")) {
            Log.i(TAG, "Device: " + device.getName() + ", RSSI: " + rssi + ", Address: " + device.getAddress());
            if (bleDevice == null) {
                bleDevice = device;

                bluetoothAdapter.stopLeScan(this);

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        gatt = bleDevice.connectGatt(getApplicationContext(), true, callback);
                    }
                });
            }
        }
    }
}
