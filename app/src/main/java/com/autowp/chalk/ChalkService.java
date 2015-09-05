package com.autowp.chalk;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;

import com.autowp.chalk.driver.BrightnessResponse;
import com.autowp.chalk.driver.ChalkDriverAbstract;
import com.autowp.chalk.driver.ChalkDriverAndroidAPI;
import com.autowp.chalk.driver.ChalkException;
import com.autowp.chalk.driver.EEPROMResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class ChalkService extends Service {
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //private static final int NOTIFICATION_ID = 1;

    public static final String PREFERENCES_AUTOSTART = "autostart";
    private static final String PREFERENCES_REMEMBER = "save";
    //private static final String PREFERENCES_INTEGRATION = "integration";

    public static final boolean PREFERENCES_AUTOSTART_DEFAULT = true;
    private static final boolean PREFERENCES_REMEMBER_DEFAULT = true;
    //private static final boolean PREFERENCES_INTEGRATION_DEFAULT = true;

    private static final String PREFERENCES_BACKLIGHT_ENABLED = "backlightEnabled";
    private static final String PREFERENCES_AUTOBACKLIGHT_ENABLED = "autoBacklightEnabled";
    private static final String PREFERENCES_BACKLIGHT_LEVEL = "backlightLevel";
    private static final String PREFERENCES_FLIPV = "flipV";
    private static final String PREFERENCES_FLIPH = "flipH";
    private ChalkDriverAndroidAPI mChalk;

    private final IBinder mBinder = new LocalBinder();

    private boolean mBacklightEnabled;
    private boolean mAutoBacklightEnabled;
    private byte mBacklightLevel;
    private byte mAmbientLightLevel;
    private boolean mFlipH;
    private boolean mFlipV;
    private byte mLCDType;

    private String mErrorMessage;

    private SharedPreferences mSettings;

    private UsbManager mUsbManager;
    private UsbDevice mCompatibleDevice;

    public class LocalBinder extends Binder {
        ChalkService getService() {
            return ChalkService.this;
        }
    }

    public interface OnStatusChangedListener {
        void handleStatusChanged();
        void handleDeviceFoundChanged(UsbDevice usbDevice);
        void handleIsConnectedChanged(boolean isConnected);
        void handleHasPermissionChanged(boolean hasPermission);
    }

    public interface OnDataChangedListener {
        void handleBacklightLevelChanged(byte level);
        void handleAmbientLightLevelChanged(byte level);
        void handleBacklightEnabledChanged(boolean enabled);
        void handleAutoBrightnessEnabledChanged(boolean enabled);
        void handleFlipHChanged(boolean value);
        void handleFlipVChanged(boolean value);
    }

    private List<OnStatusChangedListener> mStatusChangedListeners =
            new ArrayList<>();

    private List<OnDataChangedListener> mDataChangedListeners =
            new ArrayList<>();

    public ChalkService() throws ChalkException {

    }

    public void findCompatibleDevice() {
        System.out.println("findCompatibleDevice");
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        for (UsbDevice usbDevice : deviceList.values()) {
            if (ChalkDriverAbstract.isCompatibleDevice(usbDevice)) {
                mCompatibleDevice = usbDevice;

                System.out.println("Compatible device found");

                triggerDeviceFoundChanged();
                connect();
                //updateNotification();
                triggerStatusChanged();
                return;
            }
        }

        System.out.println("Compatible device not found");
    }

    public UsbDevice getCompatibleDevice() {
        return mCompatibleDevice;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        System.out.println("onBind");
        return mBinder;
    }

    public void addEventListener(final OnStatusChangedListener listener) {
        mStatusChangedListeners.add(listener);
    }

    public void removeEventListener(final OnStatusChangedListener listener){
        mStatusChangedListeners.remove(listener);
    }

    public void addEventListener(final OnDataChangedListener listener) {
        mDataChangedListeners.add(listener);
    }

    public void removeEventListener(final OnDataChangedListener listener){
        mDataChangedListeners.remove(listener);
    }

    private String bytesToHex(final byte[] bytes) {
        String result = "";
        for (byte aByte : bytes) {
            result += String.format("%02X", aByte);
        }
        return result;
    }

    public void disconnect() {
        System.out.println("disconnect");
        mChalk.disconnect();
        triggerIsConnectedChanged();
    }

    public String getStatusString() {
        String text;
        if (mChalk.isConnected()) {
            text = String.format(getString(R.string.status_device_connected), getLCDTypeString());
        } else {
            text = getString(R.string.status_service_started);
        }

        if (mErrorMessage != null) {
            text += "\n" + String.format(getString(R.string.error), mErrorMessage);
        }

        return text;
    }

    /*@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateNotification() {

        String text = getStatusString();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(text)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .build();

        //startForeground(NOTIFICATION_ID, notification);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //stopForeground(true);
        //startForeground(NOTIFICATION_ID, notification);

        mNotifyMgr.cancel(NOTIFICATION_ID);
        mNotifyMgr.notify(NOTIFICATION_ID, notification);
    }*/

    public void onCreate() {
        super.onCreate();

        mSettings = getDefaultSharedPreferences(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        try {
            mChalk = new ChalkDriverAndroidAPI(mUsbManager);
        } catch (ChalkException e) {
            e.printStackTrace();
        }

        /*Notification notification = new Notification();

        startForeground(NOTIFICATION_ID, notification);
        updateNotification();*/
        triggerStatusChanged();
    }

    /*public void onDestroy() {
        stopForeground(true);

        super.onDestroy();
    }*/

    public boolean isConnected() {
        System.out.println("isConnected() " + mChalk.isConnected());
        return mChalk.isConnected();
    }

    public void connect() {
        System.out.println("connect");
        if (isConnected()) {
            return;
        }

        if (mCompatibleDevice == null) {
            return;
        }

        mErrorMessage = null;
        //updateNotification();
        triggerStatusChanged();

        if (!mUsbManager.hasPermission(mCompatibleDevice)) {

            triggerHasPermissionChanged(false);

            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(mCompatibleDevice, mPermissionIntent);

        } else {

            triggerHasPermissionChanged(true);

            try {
                mChalk.setUsbDevice(mCompatibleDevice);
                mChalk.connect();

                EEPROMResponse response = mChalk.readEEPROM();
                mLCDType = response.getLCDType();

                _setFlipH(response.isFlipH());
                _setFlipV(response.isFlipV());

                triggerIsConnectedChanged();

                parseResponse(mChalk.readStatus(), false);

                //updateNotification();
                triggerStatusChanged();

                if (mSettings.getBoolean(PREFERENCES_REMEMBER, PREFERENCES_REMEMBER_DEFAULT)) {
                    if (mSettings.contains(PREFERENCES_BACKLIGHT_ENABLED)) {
                        boolean backlightEnabled = mSettings.getBoolean(PREFERENCES_BACKLIGHT_ENABLED, true);
                        if (mBacklightEnabled != backlightEnabled) {
                            mBacklightEnabled = backlightEnabled;
                            toggleBacklight();
                            triggerBacklightEnabledChanged();
                        }
                    }
                    if (mSettings.contains(PREFERENCES_AUTOBACKLIGHT_ENABLED)) {
                        boolean autoBacklightEnabled = mSettings.getBoolean(PREFERENCES_AUTOBACKLIGHT_ENABLED, true);
                        if (mAutoBacklightEnabled != autoBacklightEnabled) {
                            mAutoBacklightEnabled = autoBacklightEnabled;
                            toggleAutoBacklight();
                            triggerAutoBacklightEnabledChanged();
                        }
                    }
                    if (mSettings.contains(PREFERENCES_BACKLIGHT_LEVEL)) {
                        byte backlightLevel = (byte) mSettings.getInt(PREFERENCES_BACKLIGHT_LEVEL, 0x12);
                        if (mBacklightLevel != backlightLevel) {
                            mBacklightLevel = backlightLevel;
                            sendBacklightLevel();
                            triggerCurrentBacklightLevelChanged();
                        }
                    }
                }
            } catch (ChalkException e) {

                mErrorMessage = e.getMessage();

                //updateNotification();
                triggerStatusChanged();

            }
        }
    }

    private void connected() throws ChalkException {
        EEPROMResponse response = mChalk.readEEPROM();
        mLCDType = response.getLCDType();
        mFlipH = response.isFlipH();
        mFlipV = response.isFlipV();

        triggerIsConnectedChanged();

        parseResponse(mChalk.readStatus(), false);

        //updateNotification();
        triggerStatusChanged();

        if (mSettings.getBoolean(PREFERENCES_REMEMBER, PREFERENCES_REMEMBER_DEFAULT)) {
            if (mSettings.contains(PREFERENCES_BACKLIGHT_ENABLED)) {
                boolean backlightEnabled = mSettings.getBoolean(PREFERENCES_BACKLIGHT_ENABLED, true);
                if (mBacklightEnabled != backlightEnabled) {
                    mBacklightEnabled = backlightEnabled;
                    toggleBacklight();
                    triggerBacklightEnabledChanged();
                }
            }
            if (mSettings.contains(PREFERENCES_AUTOBACKLIGHT_ENABLED)) {
                boolean autoBacklightEnabled = mSettings.getBoolean(PREFERENCES_AUTOBACKLIGHT_ENABLED, true);
                if (mAutoBacklightEnabled != autoBacklightEnabled) {
                    mAutoBacklightEnabled = autoBacklightEnabled;
                    toggleAutoBacklight();
                    triggerAutoBacklightEnabledChanged();
                }
            }
            if (mSettings.contains(PREFERENCES_BACKLIGHT_LEVEL)) {
                byte backlightLevel = (byte) mSettings.getInt(PREFERENCES_BACKLIGHT_LEVEL, 0x12);
                if (mBacklightLevel != backlightLevel) {
                    mBacklightLevel = backlightLevel;
                    sendBacklightLevel();
                    triggerCurrentBacklightLevelChanged();
                }
            }
        }
    }

    private void triggerStatusChanged() {
        for (OnStatusChangedListener mStatusChangedListener : mStatusChangedListeners) {
            mStatusChangedListener.handleStatusChanged();
        }
    }

    private void triggerIsConnectedChanged() {
        for (OnStatusChangedListener mStatusChangedListener : mStatusChangedListeners) {
            mStatusChangedListener.handleIsConnectedChanged(isConnected());
        }
    }

    private void triggerHasPermissionChanged(boolean hasPermission) {
        for (OnStatusChangedListener mStatusChangedListener : mStatusChangedListeners) {
            mStatusChangedListener.handleHasPermissionChanged(hasPermission);
        }
    }

    private void triggerDeviceFoundChanged() {
        for (OnStatusChangedListener mStatusChangedListener : mStatusChangedListeners) {
            mStatusChangedListener.handleDeviceFoundChanged(mCompatibleDevice);
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        System.out.println("onStartCommand");
        String action = "";
        if (intent == null) {
            System.out.println(intent.getAction());
            action = intent.getAction();
            if (action == null) {
                action = "";
            }
        }

        switch (action) {
            case "usbAttached": {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (ChalkDriverAbstract.isCompatibleDevice(device)) {
                    mCompatibleDevice = device;
                    triggerDeviceFoundChanged();
                    //updateNotification();
                    triggerStatusChanged();
                    connect();
                }
                break;
            }
            case "usbDetached": {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                UsbDevice currentUsbDevice = mChalk.getUsbDevice();
                if (currentUsbDevice != null && device.getDeviceId() == currentUsbDevice.getDeviceId()) {
                    disconnect();
                }

                if (mCompatibleDevice != null && device.getDeviceId() == mCompatibleDevice.getDeviceId()) {
                    mCompatibleDevice = null;
                    triggerDeviceFoundChanged();
                    findCompatibleDevice();
                    //updateNotification();
                    triggerStatusChanged();
                }
                break;
            }
            case "usbPermission": {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        triggerHasPermissionChanged(true);
                        if (device != null) {
                            mChalk.setUsbDevice(device);
                            connect();
                        }
                    } else {
                        triggerHasPermissionChanged(false);
                        System.out.println("permission denied for device " + device.getDeviceName());
                    }
                }
            }
            default: {
                findCompatibleDevice();
                break;
            }
        }

        return 0;
    }

    public boolean hasPermission() {
        if (mChalk.isConnected()) {
            return true;
        } else if (mCompatibleDevice != null) {
            return mUsbManager.hasPermission(mCompatibleDevice);
        } else {
            return false;
        }
    }

    public void brightnessDec() throws ChalkException {
        parseResponse(mChalk.brightnessDec());
    }

    public void brightnessInc() throws ChalkException {
        parseResponse(mChalk.brightnessInc());
    }

    public void setBrightnessToMax() throws ChalkException {
        parseResponse(mChalk.setBrightnessToMax());
    }

    public void setBrightnessToMin() throws ChalkException {
        parseResponse(mChalk.setBrightnessToMin());
    }

    public void setBacklightEnabled(final boolean value) throws ChalkException {
        if (value != mBacklightEnabled) {
            mBacklightEnabled = value;

            toggleBacklight();
        }
    }

    private void toggleBacklight() throws ChalkException {
        parseResponse(mChalk.toggleBacklight());
    }

    public void setAutoBacklightEnabled(final boolean value) throws ChalkException {
        if (value != mAutoBacklightEnabled) {
            mAutoBacklightEnabled = value;

            toggleAutoBacklight();
        }
    }

    private void toggleAutoBacklight() throws ChalkException {
        parseResponse(mChalk.toggleAutoBacklight());
    }

    public void setBacklightLevel(final byte level) throws ChalkException {
        if (mBacklightLevel != level) {
            mBacklightLevel = level;

            sendBacklightLevel();
        }
    }

    private void sendBacklightLevel() throws ChalkException {
        parseResponse(mChalk.setBacklightLevel(mBacklightLevel));
    }

    private void parseResponse(final BrightnessResponse response) {
        parseResponse(response, true);
    }

    private void parseResponse(final BrightnessResponse response, final boolean save) {
        boolean somethingChanged = false;

        if (mBacklightEnabled != response.isBacklightEnabled()) {
            mBacklightEnabled = response.isBacklightEnabled();
            triggerBacklightEnabledChanged();
            somethingChanged = true;
        }

        if (mAutoBacklightEnabled != response.isAutoBacklightEnabled()) {
            mAutoBacklightEnabled = response.isAutoBacklightEnabled();
            triggerAutoBacklightEnabledChanged();
            somethingChanged = true;
        }

        if (mBacklightLevel != response.getBacklightLevel()) {
            mBacklightLevel = response.getBacklightLevel();
            triggerCurrentBacklightLevelChanged();
            somethingChanged = true;
        }

        if (mAmbientLightLevel != response.getAmbientLightLevel()) {
            mAmbientLightLevel = response.getAmbientLightLevel();
            triggerAmbientLightLevelChanged();
        }

        if (save && somethingChanged && mSettings.getBoolean(PREFERENCES_REMEMBER, PREFERENCES_REMEMBER_DEFAULT)) {
            mSettings.edit()
                    .putBoolean(PREFERENCES_BACKLIGHT_ENABLED, mBacklightEnabled)
                    .putBoolean(PREFERENCES_AUTOBACKLIGHT_ENABLED, mAutoBacklightEnabled)
                    .putInt(PREFERENCES_BACKLIGHT_LEVEL, mBacklightLevel)
                    .apply();
        }
    }

    private void triggerBacklightEnabledChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleBacklightEnabledChanged(mBacklightEnabled);
        }
    }

    private void triggerAutoBacklightEnabledChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleAutoBrightnessEnabledChanged(mAutoBacklightEnabled);
        }
    }

    private void triggerCurrentBacklightLevelChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleBacklightLevelChanged(mBacklightLevel);
        }
    }

    private void triggerAmbientLightLevelChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleAmbientLightLevelChanged(mAmbientLightLevel);
        }
    }

    private void triggerFlipHChangedChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleFlipHChanged(mFlipH);
        }
    }

    private void triggerFlipVChangedChanged() {
        for (OnDataChangedListener mDataChangedListener : mDataChangedListeners) {
            mDataChangedListener.handleFlipVChanged(mFlipH);
        }
    }

    private void _setFlipH(final boolean value) {
        if (mFlipH != value) {
            mFlipH = value;
            triggerFlipHChangedChanged();
        }
    }

    private void _setFlipV(final boolean value) {
        if (mFlipV != value) {
            mFlipV = value;
            triggerFlipVChangedChanged();
        }
    }

    public void setFlipH(final boolean value) throws ChalkException {
        if (mFlipH != value) {
            _setFlipH(value);
            sendTouchconfig();
        }
    }

    public void setFlipV(final boolean value) throws ChalkException {
        if (mFlipV != value) {
            _setFlipV(value);
            sendTouchconfig();
        }
    }

    public boolean isFlipV() {
        return mFlipV;
    }

    public boolean isFlipH() {
        return mFlipH;
    }

    private void sendTouchconfig() throws ChalkException {
        mChalk.sendTouchconfig(mFlipH, mFlipV);

        if (mSettings.getBoolean(PREFERENCES_REMEMBER, PREFERENCES_REMEMBER_DEFAULT)) {
            mSettings.edit()
                    .putBoolean(PREFERENCES_FLIPH, mFlipH)
                    .putBoolean(PREFERENCES_FLIPV, mFlipV)
                    .apply();
        }
    }

    public boolean isBacklightEnabled() {
        return mBacklightEnabled;
    }

    public boolean isAutoBacklightEnabled() {
        return mAutoBacklightEnabled;
    }

    public byte getCurrentBacklightLevel() {
        return mBacklightLevel;
    }

    public byte getAmbientLightLevel() {
        return mAmbientLightLevel;
    }

    public String getLCDTypeString() {
        return ChalkDriverAbstract.getLCDTypeString(mLCDType);
    }
}
