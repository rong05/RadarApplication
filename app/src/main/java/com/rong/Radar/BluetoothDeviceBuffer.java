package com.rong.Radar;

public class BluetoothDeviceBuffer {
     final String mAddress;
     final String mName;

    public BluetoothDeviceBuffer(String address, String name) {
        this.mAddress = address;
        this.mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getName() {
        return mName;
    }
}
