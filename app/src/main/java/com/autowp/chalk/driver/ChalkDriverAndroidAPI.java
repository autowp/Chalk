package com.autowp.chalk.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

/**
 * Created by Dmitry on 21.08.2015.
 */
public class ChalkDriverAndroidAPI extends ChalkDriverAbstract {
    private final UsbManager mUsbManager;
    private UsbDevice mUsbDevice = null;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mInEnpoint;
    private UsbEndpoint mOutEnpoint;
    private UsbDeviceConnection mConnection;

    public static boolean isCompatibleDevice(UsbDevice usbDevice) {
        final int productId = usbDevice.getProductId();
        return (usbDevice.getVendorId() == VENDOR_ID)
            && (productId == ST_PRODUCT_ID || productId == MT_PRODUCT_ID);
    }

    public ChalkDriverAndroidAPI(final UsbManager usbManager) throws ChalkException {
        if (usbManager == null) {
            throw new ChalkException("UsbManager is null");
        }
        mUsbManager = usbManager;
    }

    public void connect() throws ChalkException {
        try {
            if (mUsbDevice == null) {
                throw new ChalkException("USB device is null");
            }

            if (mUsbDevice.getInterfaceCount() <= ChalkDriverAndroidAPI.INTERFACE) {
                throw new ChalkException("No interface found");
            }
            mUsbInterface = mUsbDevice.getInterface(ChalkDriverAndroidAPI.INTERFACE);

            mInEnpoint = mUsbInterface.getEndpoint(ChalkDriverAndroidAPI.ENDPOINT_IN);
            if (mInEnpoint.getDirection() != UsbConstants.USB_DIR_IN) {
                throw new ChalkException("No in endpoint");
            }
            mOutEnpoint = mUsbInterface.getEndpoint(ChalkDriverAndroidAPI.ENDPOINT_OUT);
            if (mOutEnpoint.getDirection() != UsbConstants.USB_DIR_OUT) {
                throw new ChalkException("No out endpoint");
            }

            mConnection = mUsbManager.openDevice(mUsbDevice);
            mConnection.claimInterface(mUsbInterface, true);

        } catch (Exception e) {

            mUsbDevice = null;
            mUsbInterface = null;
            mInEnpoint = null;
            mOutEnpoint = null;
            mConnection = null;

            throw e;
        }
    }

    public boolean isConnected() {
        return mConnection != null && mUsbDevice != null && mUsbInterface != null;
    }

    public void disconnect()
    {
        if (mConnection != null && mUsbInterface != null) {
            mConnection.releaseInterface(mUsbInterface);
        }

        mUsbDevice = null;
        mUsbInterface = null;
        mInEnpoint = null;
        mOutEnpoint = null;
        mConnection = null;
    }

    public void setUsbDevice(final UsbDevice usbDevice) {
        mUsbDevice = usbDevice;
    }

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }


    private synchronized void exchange(final byte[] out, final byte[] in) throws ChalkException {
        //Log.i(TAG, ">" + bytesToHex(out));
        int sendStatus = mConnection.bulkTransfer(mOutEnpoint, out, out.length, 200);
        if (sendStatus != out.length) {
            throw new ChalkException(String.format("Command send failure. Sent %d of %d bytes", sendStatus, out.length));
        }

        int readStatus = mConnection.bulkTransfer(mInEnpoint, in, in.length, 200);
        if (readStatus != in.length) {
            throw new ChalkException(String.format("Response failure. Received %d of %d bytes", readStatus, in.length));
        }
        //Log.i(TAG, "<" + bytesToHex(in));
    }

    public void sendTouchconfig(final boolean flipH, final boolean flipV) throws ChalkException {
        byte data = 0x00;
        if (flipV) { data |= 0x01; }
        if (flipH) { data |= 0x02; }
        byte[] sendBuf = { (EEPROM_TOUCHCONFIG << 2)|0x03, data };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
    }

    private BrightnessResponse parseResponse(final byte[] in) {

        BrightnessResponse response = new BrightnessResponse();

        response.setBacklightEnabled((in[0] & RESPONSE_BACKLIGHT_STATUS) != 0x00);
        response.setAutoBacklightEnabled((in[0] & RESPONSE_AUTOBRIGHTNESS_STATUS) != 0x00);
        response.setBacklightLevel((byte) (in[0] & RESPONSE_BACKLIGHT_LEVEL));
        response.setAmbientLightLevel(in[1]);

        return response;
    }

    public BrightnessResponse readStatus() throws ChalkException {
        byte[] sendBuf = { 0x00, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse brightnessDec() throws ChalkException {
        byte[] sendBuf = { COMMAND_BRIGHTNESS_DEC, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse brightnessInc() throws ChalkException {
        byte[] sendBuf = { COMMAND_BRIGHTNESS_INC, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse setBrightnessToMax() throws ChalkException {
        byte[] sendBuf = { COMMAND_BRIGHTNESS_TO_MAX, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse setBrightnessToMin() throws ChalkException {
        byte[] sendBuf = { COMMAND_BRIGHTNESS_TO_MIN, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse toggleBacklight() throws ChalkException {
        byte[] sendBuf = { COMMAND_BACKLIGHT_TOGGLE, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse toggleAutoBacklight() throws ChalkException {
        byte[] sendBuf = { COMMAND_AUTOBRIGHTNESS_TOGGLE, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    public BrightnessResponse setBacklightLevel(final byte level) throws ChalkException {
        byte[] sendBuf = {COMMAND_SET_BRIGHTNESS, level};
        byte[] recvBuf = {0x00, 0x00};

        exchange(sendBuf, recvBuf);
        return parseResponse(recvBuf);
    }

    private String bytesToHex(final byte[] bytes) {
        String result = "";
        for (byte aByte : bytes) {
            result += String.format("%02X", aByte);
        }
        return result;
    }

    public EEPROMResponse readEEPROM() throws ChalkException {
        byte[] sendBuf = { (byte)0xFF, 0x00 };
        byte[] recvBuf = { 0x00, 0x00 };

        byte[] eeprom = new byte[ChalkDriverAndroidAPI.EEPROM_SIZE];

        for (int i=0; i< ChalkDriverAndroidAPI.EEPROM_SIZE; i++) {
            sendBuf[1] = (byte)i;
            recvBuf[0] = 0x00;
            recvBuf[1] = 0x00;
            exchange(sendBuf, recvBuf);
            if (recvBuf[0] == i) {
                eeprom[i] = recvBuf[1];
            } else {
                eeprom[i] = (byte)0xFF;
            }
        }

        // Get FW version
        //byte fwVersion = eeprom[ChalkDriverAndroidAPI.EEPROM_FW_VERSION];

        System.out.println(bytesToHex(eeprom));

        byte LCDType = eeprom[EEPROM_LCD];

        boolean flipH = (eeprom[EEPROM_TOUCHCONFIG] & EEPROM_TOUCHCONFIG_FLIPH) != 0x00;
        boolean flipV = (eeprom[EEPROM_TOUCHCONFIG] & EEPROM_TOUCHCONFIG_FLIPV) != 0x00;

        System.out.println("flipH " + flipH);
        System.out.println("flipV " + flipV);

        return new EEPROMResponse(LCDType, flipH, flipV);
    }
}
