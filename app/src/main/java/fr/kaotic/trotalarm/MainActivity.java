package fr.kaotic.trotalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.math.BigDecimal;

import static android.content.ContentValues.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public static final int BLUETOOTH_PIN_CODE = 123456;
    public static final String BLUETOOTH_SERVICE_UUID = "00000000-0000-1001-8000-00805F9B34FB";
    public static final String BLUETOOTH_CHARACTERISTIC_UUID_READ = "00000000-0000-1002-8000-00805F9B34FB";
    public static final String BLUETOOTH_CHARACTERISTIC_UUID_WRITE = "00000000-0000-1003-8000-00805F9B34FB";

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mBluetoothGattReadCharacteristic;
    private BluetoothGattCharacteristic mBluetoothGattWriteCharacteristic;
    private int mConnectionState = STATE_DISCONNECTED;

    public static Handler mHandler;
    public static Handler mRefreshHandler;

    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    private BluetoothDevice selectedDevice;
    private String selectedDeviceName;
    private String selectedDeviceAddress;

    boolean mConnectionInitiated = false;
    boolean mSendData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBTPermissions();

        // Check if Bluetooth is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check if Bluetooth Low Energy is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, filter);

        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);


        final LinearLayout llDeviceInformations = findViewById(R.id.linearLayoutDeviceInformations);
        final TextView textViewState = findViewById(R.id.textViewState);
        final TextView textViewMotion = findViewById(R.id.textViewMotion);
        final TextView textViewBatteryLow = findViewById(R.id.textViewBatteryLow);
        final TextView textViewBatteryPercentage = findViewById(R.id.textViewBatteryPercentage);
        final TextView textViewActualVoltage = findViewById(R.id.textViewActualVoltage);
        final TextView textViewActualCurrent = findViewById(R.id.textViewActualCurrent);
        final TextView textViewRelaySiren = findViewById(R.id.textViewRelaySiren);
        final TextView textViewRelayPower = findViewById(R.id.textViewRelayPower);
        final TextView textViewRelayHeadlights = findViewById(R.id.textViewRelayHeadlights);
        final TextView textViewRelayFlange = findViewById(R.id.textViewRelayFlange);
        final TextView textViewAccelerometerX = findViewById(R.id.textViewAccelerometerX);
        final TextView textViewAccelerometerY = findViewById(R.id.textViewAccelerometerY);
        final TextView textViewAccelerometerZ = findViewById(R.id.textViewAccelerometerZ);
        final TextView textViewGyroscopeX = findViewById(R.id.textViewGyroscopeX);
        final TextView textViewGyroscopeY = findViewById(R.id.textViewGyroscopeY);
        final TextView textViewGyroscopeZ = findViewById(R.id.textViewGyroscopeZ);
        final Button buttonEnableAlarm = findViewById(R.id.buttonEnableAlarm);
        final Button buttonDisableAlarm = findViewById(R.id.buttonDisableAlarm);
        final Button buttonRelayControlSirenOn = findViewById(R.id.buttonRelayControlSirenOn);
        final Button buttonRelayControlSirenOff = findViewById(R.id.buttonRelayControlSirenOff);
        final Button buttonRelayControlPowerOn = findViewById(R.id.buttonRelayControlPowerOn);
        final Button buttonRelayControlPowerOff = findViewById(R.id.buttonRelayControlPowerOff);
        final Button buttonRelayControlHeadlightsOn = findViewById(R.id.buttonRelayControlHeadlightsOn);
        final Button buttonRelayControlHeadlightsOff = findViewById(R.id.buttonRelayControlHeadlightsOff);
        final Button buttonRelayControlFlangeOn = findViewById(R.id.buttonRelayControlFlangeOn);
        final Button buttonRelayControlFlangeOff = findViewById(R.id.buttonRelayControlFlangeOff);
        final Button buttonDebugBatteryProtectionsOn = findViewById(R.id.buttonDebugBatteryProtectionsOn);
        final Button buttonDebugBatteryProtectionsOff = findViewById(R.id.buttonDebugBatteryProtectionsOff);
        final Button buttonDebugTestMode = findViewById(R.id.buttonDebugTestMode);
        final Button buttonDebugNothingMode = findViewById(R.id.buttonDebugNothingMode);


        llDeviceInformations.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        mHandler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + selectedDeviceName);
                                progressBar.setVisibility(View.GONE);
                                llDeviceInformations.setVisibility(View.VISIBLE);
                                buttonConnect.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Connection failed to device");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        // Parse JSON data received from the device
                        JSONObject jsonObject = null;

                        try {
                            // Check if is string
                            if(msg.obj == null){
                                break;
                            }

                            String data = msg.obj.toString();
                            Log.e(TAG, "Data received: " + data);

                            int state = 100;
                            boolean motion = false;

                            boolean batteryLow = false;
                            int batteryPercentage = 0;
                            float actualVoltage = 0.0f;
                            float actualCurrent = 0.0f;

                            boolean relaySirenEnabled = false;
                            boolean relayPowerEnabled = false;
                            boolean relayHeadlightsEnabled = false;
                            boolean relayFlangeEnabled = false;

                            float aX = 0.0f;
                            float aY = 0.0f;
                            float aZ = 0.0f;

                            float gX = 0.0f;
                            float gY = 0.0f;
                            float gZ = 0.0f;

                            if(isJSONValid(data)){
                                jsonObject = new JSONObject(data);

                                // Get state of the device
                                state = jsonObject.optInt("s", 100);
                                motion = jsonObject.optInt("m", 0) == 1;

                                JSONObject battery = jsonObject.optJSONObject("b");
                                if(battery != null) {
                                    batteryLow = battery.optInt("l", 0) == 1;
                                    batteryPercentage = battery.getInt("p");
                                    actualVoltage = BigDecimal.valueOf(battery.getDouble("v")).floatValue();
                                    actualCurrent = BigDecimal.valueOf(battery.getDouble("c")).floatValue();
                                }

                                JSONObject relays = jsonObject.optJSONObject("r");
                                if(relays != null) {
                                    relaySirenEnabled = relays.optInt("s", 0) == 1;
                                    relayPowerEnabled = relays.optInt("p", 0) == 1;
                                    relayHeadlightsEnabled = relays.optInt("h", 0) == 1;
                                    relayFlangeEnabled = relays.optInt("f", 0) == 1;
                                }

                                JSONObject accelerometer = jsonObject.optJSONObject("a");
                                if(accelerometer != null) {
                                    aX = BigDecimal.valueOf(accelerometer.getDouble("x")).floatValue();
                                    aY = BigDecimal.valueOf(accelerometer.getDouble("y")).floatValue();
                                    aZ = BigDecimal.valueOf(accelerometer.getDouble("z")).floatValue();
                                }

                                JSONObject gyroscope = jsonObject.optJSONObject("g");
                                if(gyroscope != null) {
                                    gX = BigDecimal.valueOf(gyroscope.getDouble("x")).floatValue();
                                    gY = BigDecimal.valueOf(gyroscope.getDouble("y")).floatValue();
                                    gZ = BigDecimal.valueOf(gyroscope.getDouble("z")).floatValue();
                                }
                            }

                            String stateString = "";
                            if(state == 0){
                                stateString = "DISABLED";
                            }else if(state == 1){
                                stateString = "ARMED";
                            }else if(state == 5){
                                stateString = "TEST MODE";
                            }else if(state == 6){
                                stateString = "NOTHING MODE";
                            }else if(state == 10){
                                stateString = "FIRST TRIGGER RUNNING";
                            }else if(state == 15){
                                stateString = "FIRST TRIGGER";
                            }else if(state == 20){
                                stateString = "SECOND TRIGGER RUNNING";
                            }else if(state == 25){
                                stateString = "SECOND TRIGGER";
                            }else if(state == 30){
                                stateString = "ALARM RUNNING";
                            }else if(state == 100){
                                stateString = "SYNCHRONIZING";
                            }else{
                                stateString = "UNKNOWN";
                            }

                            textViewState.setText("State : " + stateString);
                            textViewMotion.setText("Motion : " + (motion ? "YES" : "NO"));

                            textViewBatteryLow.setText("Battery Low : " + (batteryLow ? "YES" : "NO"));
                            textViewBatteryPercentage.setText("Percentage : " + batteryPercentage + "%");
                            textViewActualVoltage.setText("Voltage : " + actualVoltage + "v");
                            textViewActualCurrent.setText("Current : " + actualCurrent + "A");

                            textViewRelaySiren.setText("Siren : " + (relaySirenEnabled ? "YES" : "NO"));
                            textViewRelayPower.setText("Power : " + (relayPowerEnabled ? "YES" : "NO"));
                            textViewRelayHeadlights.setText("Headlights : " + (relayHeadlightsEnabled ? "YES" : "NO"));
                            textViewRelayFlange.setText("Flange : " + (relayFlangeEnabled ? "YES" : "NO"));

                            textViewAccelerometerX.setText("X : " + aX);
                            textViewAccelerometerY.setText("Y : " + aY);
                            textViewAccelerometerZ.setText("Z : " + aZ);

                            textViewGyroscopeX.setText("X : " + gX);
                            textViewGyroscopeY.setText("Y : " + gY);
                            textViewGyroscopeZ.setText("Z : " + gZ);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };

        mRefreshHandler = new Handler(Looper.getMainLooper());
        mRefreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mConnectionState == STATE_CONNECTED && mSendData) {
                    if(mBluetoothGatt != null && mBluetoothGattReadCharacteristic != null){
                        if(!mConnectionInitiated){
                            mHandler.obtainMessage(CONNECTING_STATUS, 1, 1).sendToTarget();
                            mConnectionInitiated = true;
                        }
                        mBluetoothGatt.readCharacteristic(mBluetoothGattReadCharacteristic);
                    }
                }

                mRefreshHandler.postDelayed(this, 500);
            }
        }, 500);

        selectedDeviceName = "MyScooterAlarm";
        selectedDeviceAddress = "00:00:00:00:00:00";

        //selectedDeviceName = getIntent().getStringExtra("selectedDeviceName");
        if(selectedDeviceName != null){
            //selectedDeviceAddress = getIntent().getStringExtra("selectedDeviceAddress");

            toolbar.setSubtitle("Connecting to " + selectedDeviceAddress + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            if(!connectToDevice(selectedDeviceAddress)){
                Toast.makeText(this, R.string.bluetooth_device_not_found, Toast.LENGTH_LONG).show();
            }
        }

        buttonConnect.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, SelectDeviceActivity.class)));

        buttonEnableAlarm.setOnClickListener(view -> sendCommandState(1));
        buttonDisableAlarm.setOnClickListener(view -> sendCommandState(0));
        buttonRelayControlSirenOn.setOnClickListener(view -> sendCommandState(11));
        buttonRelayControlSirenOff.setOnClickListener(view -> sendCommandState(10));
        buttonRelayControlPowerOn.setOnClickListener(view -> sendCommandState(21));
        buttonRelayControlPowerOff.setOnClickListener(view -> sendCommandState(20));
        buttonRelayControlHeadlightsOn.setOnClickListener(view -> sendCommandState(31));
        buttonRelayControlHeadlightsOff.setOnClickListener(view -> sendCommandState(30));
        buttonRelayControlFlangeOn.setOnClickListener(view -> sendCommandState(41));
        buttonRelayControlFlangeOff.setOnClickListener(view -> sendCommandState(40));
        buttonDebugBatteryProtectionsOn.setOnClickListener(view -> sendCommandState(61));
        buttonDebugBatteryProtectionsOff.setOnClickListener(view -> sendCommandState(60));
        buttonDebugTestMode.setOnClickListener(view -> sendCommandState(5));
        buttonDebugNothingMode.setOnClickListener(view -> sendCommandState(6));
    }

    public boolean sendCommandState(int state){
        if(mConnectionState == STATE_CONNECTED) {
            if(mBluetoothGatt != null && mBluetoothGattWriteCharacteristic != null){
                mSendData = false;
                mHandler.postDelayed(() -> {
                    mBluetoothGattWriteCharacteristic.setValue("{\"c\":\"cs\",\"s\":" + state + "}");
                    mBluetoothGatt.writeCharacteristic(mBluetoothGattWriteCharacteristic);
                }, 1000);

                return true;
            }
        }

        return false;
    }

    public boolean connectToDevice(String address){
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothGatt != null) {
            Log.i(TAG, "BGatt isnt null, Closing.");
            try {
                mBluetoothGatt.close();
            } catch (NullPointerException e) {
                Log.d(TAG, "concurrency related null pointer exception in connect");
            }
            mBluetoothGatt = null;
        }

        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
                Log.v(TAG, "Device found, already bonded, going to connect");
                if(mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress()) != null) {
                    selectedDevice = bluetoothDevice;
                    mBluetoothGatt = selectedDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                    return true;
                }
            }
        }

        selectedDevice = mBluetoothAdapter.getRemoteDevice(address);
        if(selectedDevice == null){
            Log.e(TAG, "Device not found. Unable to connect.");
            return false;
        }

        Log.e(TAG, "Device not found. Trying to bond.");
        mBluetoothGatt = selectedDevice.connectGatt(getApplicationContext(), false, mGattCallback);
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    public boolean bondDevice(BluetoothDevice device){
        device.setPin((String.valueOf(BLUETOOTH_PIN_CODE)).getBytes(StandardCharsets.UTF_8));
        return device.createBond();
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (mBluetoothGatt != null && mBluetoothGatt.getDevice() != null && device != null) {
                if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress())) {
                    Log.e(TAG, "Bond state wrong device");
                    return; // That wasnt a device we care about!!
                }
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                try {
                    if(state == BluetoothDevice.BOND_BONDED){
                        Log.e("mPairReceiver", "BOND_BONDED");
                    }

                    if (state == BluetoothDevice.BOND_BONDING) {
                        Log.e("mPairReceiver", "BOND_BONDING");
                    }

                    if(state == BluetoothDevice.BOND_NONE){
                        Log.e("mPairReceiver", "BOND_NONE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                mBluetoothGatt = gatt;
                selectedDevice = mBluetoothGatt.getDevice();
                mConnectionState = STATE_CONNECTED;

                Log.d(TAG, "Connected to GATT server.");

                if (mBluetoothGatt == null || !mBluetoothGatt.discoverServices()) {
                    Log.w(TAG, "Discovering failed !");
                }
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                mConnectionState = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server.");
            }else {
                Log.d(TAG, "Gatt callback... strange state.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(status == BluetoothGatt.GATT_SUCCESS && mBluetoothGatt != null){
                mBluetoothGattService = mBluetoothGatt.getService(UUID.fromString(BLUETOOTH_SERVICE_UUID));

                if(mBluetoothGattService != null){
                    Log.e(TAG, "Service found.");
                    mBluetoothGattReadCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(BLUETOOTH_CHARACTERISTIC_UUID_READ));
                    if(mBluetoothGattReadCharacteristic != null){
                        Log.e(TAG, "Read Characteristic found.");

                        mBluetoothGattWriteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(BLUETOOTH_CHARACTERISTIC_UUID_WRITE));
                        if(mBluetoothGattWriteCharacteristic != null){
                            Log.e(TAG, "Write Characteristic found.");
                            mConnectionState = STATE_CONNECTED;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String data = characteristic.getStringValue(0);
            mHandler.obtainMessage(MESSAGE_READ, data).sendToTarget();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e("onCharacteristicWrite", characteristic.getStringValue(0));
            mSendData = true;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e("onCharacteristicChanged", characteristic.getStringValue(0));
        }
    };

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        mConnectionState = STATE_DISCONNECTED;
        unregisterReceiver(mPairReceiver);
    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}