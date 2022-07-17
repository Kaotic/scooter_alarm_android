package fr.kaotic.trotalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SelectDeviceActivity extends AppCompatActivity {
    private BluetoothAdapter BTAdapter;
    List<Object> deviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        BTAdapter = BluetoothAdapter.getDefaultAdapter();

        Log.e("SelectDeviceActivity", "Starting bluetooth scan...");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        BTAdapter.startDiscovery();
    }

    private void refreshUI() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewDevice);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(this, deviceList);
        recyclerView.setAdapter(deviceListAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                boolean deviceCompatible = false;
                boolean deviceKnown = false;

                if(deviceHardwareAddress.contains("00:00:00:00")) {
                    deviceCompatible = true;
                }

                DeviceInfoModel deviceInfoModel = new DeviceInfoModel(deviceName, deviceHardwareAddress, deviceCompatible);

                for(Object d : deviceList) {
                    if(d instanceof DeviceInfoModel) {
                        DeviceInfoModel dm = (DeviceInfoModel) d;
                        if(dm.getDeviceHardwareAddress().equals(deviceHardwareAddress)) {
                            deviceKnown = true;
                        }
                    }
                }

                if(!deviceKnown) {
                    deviceList.add(deviceInfoModel);
                    refreshUI();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BTAdapter.cancelDiscovery();
        unregisterReceiver(receiver);
    }
}
