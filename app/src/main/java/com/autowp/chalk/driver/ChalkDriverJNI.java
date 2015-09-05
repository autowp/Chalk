package com.autowp.chalk.driver;

/**
 * Created by dima on 30.08.15.
 */
public class ChalkDriverJNI extends ChalkDriverAbstract {
    static {
        System.loadLibrary("chalk-jni");
    }

    @Override
    public native void connect() throws ChalkException;

    @Override
    public native boolean isConnected();

    @Override
    public native void disconnect();

    @Override
    public native void sendTouchconfig(boolean flipH, boolean flipV) throws ChalkException;

    @Override
    public native BrightnessResponse readStatus() throws ChalkException;

    @Override
    public native BrightnessResponse brightnessDec() throws ChalkException;

    @Override
    public native BrightnessResponse brightnessInc() throws ChalkException;

    @Override
    public native BrightnessResponse setBrightnessToMax() throws ChalkException;

    @Override
    public native BrightnessResponse setBrightnessToMin() throws ChalkException;

    @Override
    public native BrightnessResponse toggleBacklight() throws ChalkException;

    @Override
    public native BrightnessResponse toggleAutoBacklight() throws ChalkException;

    @Override
    public native BrightnessResponse setBacklightLevel(byte level) throws ChalkException;

    @Override
    public native EEPROMResponse readEEPROM() throws ChalkException;

    public native void allowRoot();
}
