package fr.kaotic.trotalarm;

import androidx.annotation.NonNull;

public class DeviceInfoModel {
    private String deviceName, deviceHardwareAddress;
    private Boolean isValidDevice;


    public DeviceInfoModel(){

    }

    public DeviceInfoModel(String deviceName, String deviceHardwareAddress, Boolean isValidDevice){
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
        this.isValidDevice = isValidDevice;
    }

    public String getDeviceName(){
        return deviceName;
    }

    public String getDeviceHardwareAddress(){
        return deviceHardwareAddress;
    }

    public Boolean getIsValidDevice(){
        return isValidDevice;
    }

    @NonNull
    @Override
    public String toString() {
        return deviceName;
    }
}
