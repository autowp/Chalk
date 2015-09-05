package com.autowp.chalk.driver;

/**
 * Created by Dmitry on 23.08.2015.
 */
public class BrightnessResponse {

    private boolean mBacklightEnabled;
    private boolean mAutoBacklightEnabled;
    private byte mBacklightLevel;
    private byte mAmbientLightLevel;

    public BrightnessResponse() {

    }

    public BrightnessResponse(final boolean backlightEnabled, final boolean autoBacklightEnabled, final byte backlightLevel, final byte ambientLightLevel) {
        mBacklightEnabled = backlightEnabled;
        mAutoBacklightEnabled = autoBacklightEnabled;
        mBacklightLevel = backlightLevel;
        mAmbientLightLevel = ambientLightLevel;
    }

    public boolean isBacklightEnabled() {
        return mBacklightEnabled;
    }

    public void setBacklightEnabled(boolean backlightEnabled) {
        mBacklightEnabled = backlightEnabled;
    }

    public boolean isAutoBacklightEnabled() {
        return mAutoBacklightEnabled;
    }

    public void setAutoBacklightEnabled(boolean autoBacklightEnabled) {
        mAutoBacklightEnabled = autoBacklightEnabled;
    }

    public byte getBacklightLevel() {
        return mBacklightLevel;
    }

    public void setBacklightLevel(byte backlightLevel) {
        mBacklightLevel = backlightLevel;
    }

    public byte getAmbientLightLevel() {
        return mAmbientLightLevel;
    }

    public void setAmbientLightLevel(byte ambientLightLevel) {
        mAmbientLightLevel = ambientLightLevel;
    }
}
