package com.wearcast.app.ui.cast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wearcast.app.R;
import com.wearcast.app.cast.DlnaDevice;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(DlnaDevice device);
    }

    private final List<DlnaDevice> devices = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void addDevice(DlnaDevice device) {
        if (devices.contains(device)) return;
        devices.add(device);
        notifyItemInserted(devices.size() - 1);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DlnaDevice device = devices.get(position);
        holder.name.setText(device.friendlyName);
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_device_name);
        }
    }
}
