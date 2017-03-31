/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.janvanura.gate_bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class DeviceControlActivity extends AppCompatActivity implements ChangeKeyFragment.NoticeDialogListener, ResetKeyFragment.NoticeDialogListener {

    private final static String TAG = GattAttributes.NAME;
    public static final String SECURE_KEY_TAG = "secureKey";

    private TextView mConnectionState;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mConnected = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private String mSecureKey = GattAttributes.SECURE_KEY;

    private MenuItem mMenuItemResetKey;
    private Button mBtnOpen, mBtnClose, mBtnConnect, mBtnDisconnect;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mBluetoothAdapter.isEnabled()){
                mBluetoothLeService.connect(GattAttributes.MAC_ADDRESS);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                enableDisableButtons();
            }
            else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.connecting);
                animateConnecting();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                enableDisableButtons();
            } else if (BluetoothLeService.ACTION_GATT_WRITE.equals(action)) {
                // write
            } else if (BluetoothLeService.ACTION_GATT_NOTHING_FOUND.equals(action)) {
                updateConnectionState(R.string.disconnected);
                enableDisableButtons();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String answer = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                resolveDataAvailable(answer);
            }
        }
    };


    private void resolveDataAvailable(String answer) {

        if (answer.equals("ok:m:1")) {
            Toast.makeText(DeviceControlActivity.this, R.string.flesh_opening, Toast.LENGTH_SHORT).show();
        } else if (answer.equals("ok:m:0")) {
            Toast.makeText(DeviceControlActivity.this, R.string.flesh_closing, Toast.LENGTH_SHORT).show();
        } else if (answer.contains("ok:c")) {
            String key = answer.split(":")[2];
            changeSecureKey(key);
            Toast.makeText(DeviceControlActivity.this, R.string.flesh_reset_key, Toast.LENGTH_SHORT).show();
        } else if (answer.equals("err:secure")) {
            Toast.makeText(DeviceControlActivity.this, R.string.flesh_err_secure, Toast.LENGTH_SHORT).show();
        } else if (answer.equals("err:master")) {
            Toast.makeText(DeviceControlActivity.this, R.string.flesh_err_master, Toast.LENGTH_SHORT).show();
        }
    }

    private void enableDisableButtons() {

        if (mConnected) {
            mBtnConnect.setVisibility(View.GONE);
            mBtnDisconnect.setVisibility(View.VISIBLE);
            if (mMenuItemResetKey != null) mMenuItemResetKey.setEnabled(true);
        } else {
            mBtnConnect.setVisibility(View.VISIBLE);
            mBtnDisconnect.setVisibility(View.GONE);
            if (mMenuItemResetKey != null) mMenuItemResetKey.setEnabled(false);
        }
        mBtnOpen.setEnabled(mConnected);
        mBtnClose.setEnabled(mConnected);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_with_toolbar);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Sets up UI references.
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        // Load secure key from storage
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        mSecureKey = sharedPreferences.getString(SECURE_KEY_TAG, GattAttributes.SECURE_KEY);

        // Bind service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Init buttons
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnClose = (Button) findViewById(R.id.btn_close);

        enableDisableButtons();

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.connect(GattAttributes.MAC_ADDRESS);
            }
        });

        mBtnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.disconnect();
            }
        });

        mBtnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected){
                    mBluetoothLeService.writeCharacteristic(concatStrings(GattAttributes.CMD_MOTION, mSecureKey, GattAttributes.VALUE_OPEN));
                    Log.d(TAG, "Writing data: " + GattAttributes.VALUE_OPEN);
                }
            }
        });

        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected) {
                    mBluetoothLeService.writeCharacteristic(concatStrings(GattAttributes.CMD_MOTION, mSecureKey, GattAttributes.VALUE_CLOSE));
                    Log.d(TAG, "Writing data: " + GattAttributes.VALUE_CLOSE);
                }
            }
        });
    }

    private String concatStrings(String cmd, String key, String value) {
        return cmd + ":" + key + ":" + value;
    }


    @Override
    public void onChangeKey(String key) {

        if (key.isEmpty()) {
            Toast.makeText(this, R.string.flesh_change_key_empty, Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = getSupportFragmentManager();
            ChangeKeyFragment changeKeyFragment = new ChangeKeyFragment();
            changeKeyFragment.show(fragmentManager, "change");
            return;
        }
        changeSecureKey(key);
        Toast.makeText(this, R.string.flesh_change_key, Toast.LENGTH_SHORT).show();
    }

    private void changeSecureKey(String key) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SECURE_KEY_TAG, key);
        editor.apply();
        mSecureKey = key;
    }


    @Override
    public void onResetKey(String masterKey, String secureKey) {

        if (secureKey.isEmpty()) {
            Toast.makeText(this, R.string.flesh_change_key_empty, Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = getSupportFragmentManager();
            ResetKeyFragment resetKeyFragment = new ResetKeyFragment();
            resetKeyFragment.show(fragmentManager, "reset");
            return;
        }

        mBluetoothLeService.writeCharacteristic(concatStrings(GattAttributes.CMD_CHANGE, masterKey, secureKey));
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (mBluetoothLeService != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothLeService.connect(GattAttributes.MAC_ADDRESS);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }



    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
                mConnectionState.clearAnimation();
            }
        });
    }


    private void animateConnecting() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(DeviceControlActivity.this, android.R.anim.fade_out);
                animation.setRepeatCount(Animation.INFINITE);
                animation.setDuration(1000);
                mConnectionState.startAnimation(animation);
            }
        });
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_NOTHING_FOUND);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gatt_services, menu);
        mMenuItemResetKey = menu.findItem(R.id.menu_reset_key);
        if (mConnected) {
            mMenuItemResetKey.setEnabled(true);
        } else {
            mMenuItemResetKey.setEnabled(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (item.getItemId()) {
            case R.id.menu_change_key:
                ChangeKeyFragment changeKeyFragment = new ChangeKeyFragment();
                changeKeyFragment.show(fragmentManager, "change");
                break;
            case R.id.menu_reset_key:
                ResetKeyFragment resetKeyFragment = new ResetKeyFragment();
                resetKeyFragment.show(fragmentManager, "reset");
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
