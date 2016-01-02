package radius.com.odysseus;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    private static final int ADMIN_INTENT = 15;
    private static final String description = "This application needs to be able to lock your device's screen.";
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mComponentName = new ComponentName(this, AdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceAdmin();
        startBluetoothService();
        finish();
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
}
