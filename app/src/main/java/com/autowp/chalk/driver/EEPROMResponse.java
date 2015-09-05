package com.autowp.chalk.driver;

/**
 * Created by Dmitry on 23.08.2015.
 */
public class EEPROMResponse {
    private byte mLCDType;
    private boolean mFlipH;

    public EEPROMResponse(final byte LCDType, final boolean flipH, final boolean flipV) {
        setLCDType(LCDType);
        setFlipH(flipH);
        setFlipV(flipV);
    }

    private boolean mFlipV;

    public byte getLCDType() {
        return mLCDType;
    }

    public void setLCDType(byte LCDType) {
        mLCDType = LCDType;
    }

    public boolean isFlipH() {
        return mFlipH;
    }

    public void setFlipH(boolean flipH) {
        mFlipH = flipH;
    }

    public boolean isFlipV() {
        return mFlipV;
    }

    public void setFlipV(boolean flipV) {
        mFlipV = flipV;
    }
}
