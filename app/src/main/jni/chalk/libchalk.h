/*
 * ChalkboardLCD.h
 *
 *  Created on: Aug 26, 2015
 *      Author: dima
 */

#ifndef LIBCHALK_H_
#define LIBCHALK_H_

#include "libusb/libusb.h"

enum libchalk_error {
	LIBCHALK_SUCCESS = 0,
	LIBCHALK_ERROR_ACCESS = -1,
	LIBCHALK_ERROR_NO_DEVICE = -2,
	LIBCHALK_ERROR_OTHER = -99
};

class ChalkboardLCD {
public:
	ChalkboardLCD();
	virtual ~ChalkboardLCD();

	libchalk_error connect(const bool chmodDevice);
	bool disconnect();
	bool isConnected();

	void printError(const int);

	void readStatus();
	void brightnessDec();
	void brightnessInc();
	void setBrightnessToMax();
	void setBrightnessToMin();
	void setBacklight(bool);
	bool getBacklight();
	void setAutoBacklight(bool);
	bool getAutoBacklight();
	void setBacklightLevel(const unsigned char level);
	unsigned char getBacklightLevel();
	unsigned char getAmbientLightLevel();
	void toggleBacklight();
	void toggleAutoBacklight();

	void setFlipH(bool);
	void setFlipV(bool);
	bool getFlipH();
	bool getFlipV();
	void sendTouchconfig();

	unsigned char getVersion();
	unsigned char getLCDType();
	const char* getLCDTypeString();

private:
	static const uint16_t VENDOR_ID = 0x04D8;
	static const uint16_t ST_PRODUCT_ID = 0xF723; // for 7" and 10" single-touch firmware
	static const uint16_t MT_PRODUCT_ID = 0xF724; // for 7" and 10" multi-touch firmware

	static const int INTERFACE = 1;
	static const unsigned char ENDPOINT_OUT = 0x02;
	static const unsigned char ENDPOINT_IN = 0x82;
	static const int TIMEOUT = 500;

	static const int EEPROM_SIZE =        6;       // Size of EEPROM (bytes)
    static const int EEPROM_FW_VERSION =  0x00;    // Firmware version
    static const int EEPROM_BL_STATUS =   0x01;    // Backlight status (B/L level and AutoOn status)
    static const int EEPROM_LCD =         0x02;    // LCD type (1 - open-frame 7", 2 - black-frame 7", 3 - 10", 4 - dualLVDS/FullHD+)
    static const int EEPROM_TOUCHCONFIG = 0x03;    // Touchscreen config (bit 0 - flip V, bit 1 - flip H)
    static const int EEPROM_TOUCHCONFIG_FLIPH = 0x02;
    static const int EEPROM_TOUCHCONFIG_FLIPV = 0x01;

    static const int COMMAND_SIZE = 2;
    static const int RESPONSE_SIZE = 2;

    static const unsigned char COMMAND_AUTOBACKLIGHT_TOGGLE = 0x02;
    static const unsigned char COMMAND_BRIGHTNESS_TO_MAX = 0x04;
    static const unsigned char COMMAND_BRIGHTNESS_TO_MIN = 0x08;
    static const unsigned char COMMAND_BACKLIGHT_TOGGLE = 0x10;
    static const unsigned char COMMAND_SET_BRIGHTNESS = 0x20;
    static const unsigned char COMMAND_BRIGHTNESS_DEC = 0x40;
    static const unsigned char COMMAND_BRIGHTNESS_INC = 0x80;

    static const unsigned char RESPONSE_AUTOBRIGHTNESS_STATUS = 0x40;
    static const unsigned char RESPONSE_BACKLIGHT_STATUS = 0x80;
    static const unsigned char RESPONSE_BACKLIGHT_LEVEL = 0x3F;
    static const unsigned char RESPONSE_AMBIENTLIGHT_LEVEL = 0xFF;

    static const unsigned char TYPE_UNKNOWN = 0x00;
    static const unsigned char TYPE_7INCH_OPENFRAME = 0x01;
    static const unsigned char TYPE_7INCH_BLACKFRAME = 0x02;
    static const unsigned char TYPE_10INCH = 0x03;
    static const unsigned char TYPE_DUALLVDS_FULLHD = 0x04;

    static const char* TYPESTR_UNKNOWN;
    static const char* TYPESTR_7INCH_OPENFRAME;
    static const char* TYPESTR_7INCH_BLACKFRAME;
    static const char* TYPESTR_10INCH;
    static const char* TYPESTR_DUALLVDS_FULLHD;


	libusb_device_handle *dev;

	bool backlightEnabled;
	bool autoBacklightEnabled;
	unsigned char backlightLevel;
	unsigned char ambientLightLevel;

	bool flipH;
	bool flipV;
	unsigned char LCDType;
	unsigned char version;

	void exchange(unsigned char *out, const int outLen, unsigned char *in, const int inLen);
	void parseResponse(const unsigned char *in);
	void readEEPROM();
};

#endif /* LIBCHALK_H_ */
