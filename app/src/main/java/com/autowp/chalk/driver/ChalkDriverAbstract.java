package com.autowp.chalk.driver;

import android.hardware.usb.UsbDevice;

/**
 * Created by dima on 30.08.15.
 */
abstract public class ChalkDriverAbstract {
    public static final int VENDOR_ID = 0x04D8;
    public static final int ST_PRODUCT_ID = 0xF723; // single touch
    public static final int MT_PRODUCT_ID = 0xF724; // multi touch

    public static final int INTERFACE = 1;
    public static final int ENDPOINT_IN = 0;
    public static final int ENDPOINT_OUT = 1;

    public static final int EEPROM_SIZE =        6;       // Size of EEPROM (bytes)
    public static final int EEPROM_FW_VERSION =  0x00;    // Firmware version
    public static final int EEPROM_BL_STATUS =   0x01;    // Backlight status (B/L level and AutoOn status)
    public static final int EEPROM_LCD =         0x02;    // LCD type (1 - open-frame 7", 2 - black-frame 7", 3 - 10", 4 - dualLVDS/FullHD+)
    public static final int EEPROM_TOUCHCONFIG = 0x03;    // Touchscreen config (bit 0 - flip V, bit 1 - flip H)
    public static final int EEPROM_TOUCHCONFIG_FLIPH = 0x02;
    public static final int EEPROM_TOUCHCONFIG_FLIPV = 0x01;

    public static final int COMMAND_SIZE = 2;
    public static final int RESPONSE_SIZE = 2;

    public static final byte COMMAND_AUTOBRIGHTNESS_TOGGLE = 0x02;
    public static final byte COMMAND_BRIGHTNESS_TO_MAX = 0x04;
    public static final byte COMMAND_BRIGHTNESS_TO_MIN = 0x08;
    public static final byte COMMAND_BACKLIGHT_TOGGLE = 0x10;
    public static final byte COMMAND_SET_BRIGHTNESS = 0x20;
    public static final byte COMMAND_BRIGHTNESS_DEC = 0x40;
    public static final byte COMMAND_BRIGHTNESS_INC = (byte) 0x80;

    public static final byte RESPONSE_AUTOBRIGHTNESS_STATUS = 0x40;
    public static final byte RESPONSE_BACKLIGHT_STATUS = (byte) 0x80;
    public static final byte RESPONSE_BACKLIGHT_LEVEL = 0x3F;
    public static final byte RESPONSE_AMBIENTLIGHT_LEVEL = (byte) 0xFF;

    public abstract void connect() throws ChalkException;

    public abstract boolean isConnected();

    public abstract void disconnect();

    public abstract void sendTouchconfig(final boolean flipH, final boolean flipV) throws ChalkException;

    public abstract BrightnessResponse readStatus() throws ChalkException;

    public abstract BrightnessResponse brightnessDec() throws ChalkException;

    public abstract BrightnessResponse brightnessInc() throws ChalkException;

    public abstract BrightnessResponse setBrightnessToMax() throws ChalkException;

    public abstract BrightnessResponse setBrightnessToMin() throws ChalkException;

    public abstract BrightnessResponse toggleBacklight() throws ChalkException;

    public abstract BrightnessResponse toggleAutoBacklight() throws ChalkException;

    public abstract BrightnessResponse setBacklightLevel(final byte level) throws ChalkException;

    public abstract EEPROMResponse readEEPROM() throws ChalkException;

    public static String getLCDTypeString(final byte type) {
        String result = "Unknown";
        switch (type) {
            case 0x01:
                result = "7\" open-frame";
                break;
            case 0x02:
                result = "7\" black-frame";
                break;
            case 0x03:
                result = "10\"";
                break;
            case 0x04:
                result = "dualLVDS/FullHD+";
                break;
        }

        return result;
    }

    public static boolean isCompatibleDevice(UsbDevice usbDevice) {
        final int productId = usbDevice.getProductId();
        return (usbDevice.getVendorId() == VENDOR_ID)
                && (productId == ST_PRODUCT_ID || productId == MT_PRODUCT_ID);
    }
}
