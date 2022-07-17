package fr.kaotic.trotalarm;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final List<Object> deviceList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageDeviceCompatible;
        TextView textName, textAddress;
        LinearLayout linearLayout;

        public ViewHolder(View v) {
            super(v);
            imageDeviceCompatible = v.findViewById(R.id.imageViewDeviceCompatible);
            textName = v.findViewById(R.id.textViewDeviceName);
            textAddress = v.findViewById(R.id.textViewDeviceAddress);
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo);
        }
    }

    public DeviceListAdapter(Context context, List<Object> deviceList) {
        this.context = context;
        this.deviceList = deviceList;

    }

    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_info_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        ViewHolder itemHolder = (ViewHolder) holder;
        final DeviceInfoModel deviceInfoModel = (DeviceInfoModel) deviceList.get(position);

        if(deviceInfoModel.getIsValidDevice()) {
            itemHolder.imageDeviceCompatible.setImageResource(R.mipmap.ic_device_compatible);
        }else{
            itemHolder.imageDeviceCompatible.setImageResource(R.mipmap.ic_device_not_compatible);
        }

        itemHolder.textName.setText(deviceInfoModel.getDeviceName());
        itemHolder.textAddress.setText(deviceInfoModel.getDeviceHardwareAddress());

        itemHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(deviceInfoModel.getIsValidDevice()) {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.putExtra("selectedDeviceName", deviceInfoModel.getDeviceName());
                    intent.putExtra("selectedDeviceAddress", deviceInfoModel.getDeviceHardwareAddress());
                    context.startActivity(intent);
                }else{
                    Toast.makeText(context, "This device is not a K-Trotalarm system !", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}
