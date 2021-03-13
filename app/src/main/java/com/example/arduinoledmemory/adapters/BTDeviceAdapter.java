package com.example.arduinoledmemory.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arduinoledmemory.MyApplication;
import com.example.arduinoledmemory.R;
import com.example.arduinoledmemory.objects.DiscoveredBluetoothDevice;
import com.example.arduinoledmemory.viewmodels.DevicesLiveData;

import java.util.ArrayList;

public class BTDeviceAdapter extends RecyclerView.Adapter<BTDeviceAdapter.ViewHolder> {

    private Context context;
    private BTDevicePickListener listener;
    ArrayList<DiscoveredBluetoothDevice> devices;

    public BTDeviceAdapter(@NonNull final Context context, @NonNull final ArrayList<DiscoveredBluetoothDevice> devices, @NonNull final DevicesLiveData devicesLiveData) {
        setHasStableIds(true);
        this.context = context;
        this.listener = listener;
        this.devices = devices;

        devicesLiveData.observe((LifecycleOwner)this.context, newDevices -> {
            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                    new DeviceDiffCallback(this.devices, newDevices), false);
            this.devices = newDevices;
            result.dispatchUpdatesTo(this);
        });
    }

    public void setBTDevicePickListener(final BTDevicePickListener listener) {
        this.listener = listener;
    }

    public interface BTDevicePickListener {
        void onBTDevicePicked(@NonNull final DiscoveredBluetoothDevice device);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bt_device_row, parent, false);
        return new ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final DiscoveredBluetoothDevice device = devices.get(position);

        if(device != null) {

            // set name
            final String deviceName = device.getName();
            if (!TextUtils.isEmpty(deviceName))
                holder.deviceName.setText(deviceName);
            else
                holder.deviceName.setText("");

            // set mac address
            holder.deviceAddress.setText(device.getAddress());

            // set compatibility
            int compatibilityColor;
            String compatibilityText;
            switch (device.getGameCompatibility()) {
                case COMPATIBLE:
                    compatibilityColor = ContextCompat.getColor(context, R.color.COMPATIBILITY_COMPATIBLE);
                    compatibilityText = MyApplication.resources.getString(R.string.COMPATIBILITY_COMPATIBLE);
                    break;
                case INCOMPATIBLE:
                    compatibilityColor = ContextCompat.getColor(context, R.color.COMPATIBILITY_INCOMPATIBLE);
                    compatibilityText = MyApplication.resources.getString(R.string.COMPATIBILITY_INCOMPATIBLE);
                    break;
                default:
                    compatibilityColor = ContextCompat.getColor(context, R.color.COMPATIBILITY_UNKNOWN);
                    compatibilityText = MyApplication.resources.getString(R.string.COMPATIBILITY_UNKNOWN);
                    break;
            }
            holder.deviceCompatibility.setTextColor(compatibilityColor);
            holder.deviceCompatibility.setText(compatibilityText);

            // set rssi image according to rssi strength level
            final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
            holder.rssi.setImageLevel(rssiPercent);
            if(rssiPercent <= MyApplication.resources.getInteger(R.integer.RSSI_POOR))
                holder.rssi.setColorFilter(ContextCompat.getColor(context, R.color.RSSI_POOR), android.graphics.PorterDuff.Mode.SRC_IN);
            else if(rssiPercent <= MyApplication.resources.getInteger(R.integer.RSSI_WEAK))
                holder.rssi.setColorFilter(ContextCompat.getColor(context, R.color.RSSI_WEAK), android.graphics.PorterDuff.Mode.SRC_IN);
            else if(rssiPercent <= MyApplication.resources.getInteger(R.integer.RSSI_FAIR))
                holder.rssi.setColorFilter(ContextCompat.getColor(context, R.color.RSSI_FAIR), android.graphics.PorterDuff.Mode.SRC_IN);
            else if(rssiPercent <= MyApplication.resources.getInteger(R.integer.RSSI_GOOD))
                holder.rssi.setColorFilter(ContextCompat.getColor(context, R.color.RSSI_GOOD), android.graphics.PorterDuff.Mode.SRC_IN);
            else
                holder.rssi.setColorFilter(ContextCompat.getColor(context, R.color.RSSI_EXCELLENT), android.graphics.PorterDuff.Mode.SRC_IN);

            // setup click listener on root view
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener == null)
                        return;
                    listener.onBTDevicePicked(device);
                }
            });
        }
    }

    @Override
    public long getItemId(final int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final TextView deviceAddress;
        private final TextView deviceName;
        private final TextView deviceCompatibility;
        private final ImageView rssi;

        private ViewHolder(@NonNull final View view) {
            super(view);
            rootView = view.findViewById(R.id.bt_device_row_rootView);
            deviceName = view.findViewById(R.id.bt_device_row_name);
            deviceAddress = view.findViewById(R.id.bt_device_row_mac);
            deviceCompatibility = view.findViewById(R.id.bt_device_row_compatible);
            rssi = view.findViewById(R.id.bt_device_row_rssi);
        }
    }
}