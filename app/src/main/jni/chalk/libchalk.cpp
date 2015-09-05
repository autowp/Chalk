/*
 * ChalkboardLCD.cpp
 *
 *  Created on: Aug 26, 2015
 *      Author: dima
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "libusb/libusb.h"
#include "libchalk.h"
#include <android/log.h>
#include <sys/stat.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"CHALKJNI",__VA_ARGS__)

const char* ChalkboardLCD::TYPESTR_UNKNOWN = "Unknown";
const char* ChalkboardLCD::TYPESTR_7INCH_OPENFRAME = "7\" open-frame";
const char* ChalkboardLCD::TYPESTR_7INCH_BLACKFRAME = "7\" black-frame";
const char* ChalkboardLCD::TYPESTR_10INCH = "10\"";
const char* ChalkboardLCD::TYPESTR_DUALLVDS_FULLHD = "dualLVDS/FullHD+";

/*void printHex(const unsigned char in[], const size_t insz) {
    const char * hex = "0123456789ABCDEF";
    char pout[3];
    for(size_t i=0; i < insz; i++) {

        pout[0] = hex[(in[i]>>4) & 0xF];
        pout[1] = hex[ in[i]     & 0xF];
        pout[2] = ':';
        printf("%s", pout);
    }

    printf("\n");
}*/

ChalkboardLCD::ChalkboardLCD() {

	backlightEnabled = false;
	autoBacklightEnabled = false;
	backlightLevel = 0;
	ambientLightLevel = 0;

	flipH = false;
	flipV = false;
	LCDType = 0;
	version = 0;

	dev = NULL;

	libusb_init(NULL);
}

ChalkboardLCD::~ChalkboardLCD() {
	libusb_exit(NULL);
}

libchalk_error ChalkboardLCD::connect(const bool chmodDevice) {

	int chmod(const char *path, mode_t mode);

    struct libusb_device **devs;
    struct libusb_device *found = NULL;
    struct libusb_device *idev;
    struct libusb_device_handle *handle = NULL;
    size_t i = 0;

    if (libusb_get_device_list(NULL, &devs) < 0) {
        return LIBCHALK_ERROR_OTHER;
    }

    while ((idev = devs[i++]) != NULL) {
        struct libusb_device_descriptor desc;
        int r = libusb_get_device_descriptor(idev, &desc);
        if (r == LIBUSB_SUCCESS) {
            if (desc.idVendor == VENDOR_ID) {
                if (desc.idProduct == ST_PRODUCT_ID || desc.idProduct == MT_PRODUCT_ID) {
                    found = idev;
                    break;
                }
            }
        }
    }

    libusb_free_device_list(devs, 1);

    if (!found) {
        return LIBCHALK_ERROR_NO_DEVICE;
    }

    int r = libusb_open(found, &dev);
    if (r != LIBUSB_SUCCESS) {
        dev = NULL;

        if (r == LIBUSB_ERROR_ACCESS) {
            return LIBCHALK_ERROR_ACCESS;
        } else if (r == LIBUSB_ERROR_NO_DEVICE) {
            return LIBCHALK_ERROR_NO_DEVICE;
        } else {
            return LIBCHALK_ERROR_OTHER;
        }

    }


	if (dev == NULL) {
        LOGI("libusb_open_device_with_vid_pid: device not found");
		return LIBCHALK_ERROR_NO_DEVICE;
	}

	#ifdef __linux__
        int isActive = libusb_kernel_driver_active(dev, INTERFACE);
        if (isActive == 1) {
            int detachResult = libusb_detach_kernel_driver(dev, INTERFACE);
            if (detachResult != 0) {
                LOGI("libusb_detach_kernel_driver: %d", detachResult);
            }
        } else if (isActive != 0) {
            LOGI("libusb_kernel_driver_active: %d", isActive);
        }
	#endif

    int result = libusb_set_configuration(dev, 1);
	if (result != LIBUSB_SUCCESS) {
        LOGI("libusb_set_configuration: %d", result);
	}

	int claimResult = libusb_claim_interface(dev, INTERFACE);
	if (claimResult != LIBUSB_SUCCESS) {
		LOGI("libusb_claim_interface: %d", claimResult);

		libusb_close(dev);
		dev = NULL;

		if (claimResult == LIBUSB_ERROR_ACCESS) {
			return LIBCHALK_ERROR_ACCESS;
		}

		return LIBCHALK_ERROR_OTHER;
	}

	readEEPROM();

	readStatus();

    LOGI("connected succeesful");

	return LIBCHALK_SUCCESS;
}

bool ChalkboardLCD::disconnect() {
	if (isConnected()) {
		libusb_release_interface(dev, INTERFACE);
		#ifdef __linux__
			libusb_attach_kernel_driver(dev, INTERFACE);
		#endif
		libusb_close(dev);
		dev = NULL;
	}

	return true;
}

bool ChalkboardLCD::isConnected() {
	return dev != NULL;
}

void ChalkboardLCD::printError(const int error) {
	switch (error) {
		case LIBUSB_ERROR_BUSY:
			printf("Busy\n");
			break;
		case LIBUSB_ERROR_IO:
			printf("I/O error\n");
			break;
		case LIBUSB_ERROR_NO_MEM:
			printf("Memory allocation failure\n");
			break;
		case LIBUSB_ERROR_ACCESS:
			printf("Insufficient permissions\n");
			break;
		case LIBUSB_ERROR_NO_DEVICE:
			printf("device has been disconnected\n");
			break;
		case LIBUSB_ERROR_TIMEOUT:
			printf("transfer timed out\n");
			break;
		case LIBUSB_ERROR_PIPE:
			printf("endpoint halted\n");
			break;
		case LIBUSB_ERROR_OVERFLOW:
			printf("device offered more data, see Packets and overflows\n");
			break;
		case LIBUSB_ERROR_NOT_FOUND:
			printf("not found\n");
			break;
		default:
			printf("Other error %d\n", error);
	}
}

void ChalkboardLCD::exchange(unsigned char *out, const int outLen, unsigned char *in, const int inLen) {
	int len = 0;
	int sendStatus = libusb_bulk_transfer(dev, ENDPOINT_OUT, out, outLen, &len, TIMEOUT);
	if (sendStatus != LIBUSB_SUCCESS) {
        LOGI("libusb_bulk_transfer: %d", sendStatus);
		return;
	}
	if (len < outLen) {
		//printf("Transfered %d of %d", len, outLen);
		return;
	}

	int receiveStatus = libusb_bulk_transfer(dev, ENDPOINT_IN, in, inLen, &len, TIMEOUT);
	if (receiveStatus != LIBUSB_SUCCESS) {
        LOGI("libusb_bulk_transfer: %d", receiveStatus);
		return;
	}
	if (len < inLen) {
		//printf("Transfered %d of %d", len, outLen);
        LOGI("exchange: %d of %d", len, outLen);
		return;
	}
}

void ChalkboardLCD::readStatus() {
	unsigned char sendBuf[] = { 0x00, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::brightnessDec() {
	unsigned char sendBuf[] = { COMMAND_BRIGHTNESS_DEC, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::brightnessInc() {
	unsigned char sendBuf[] = { COMMAND_BRIGHTNESS_INC, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::setBrightnessToMax() {
	unsigned char sendBuf[] = { COMMAND_BRIGHTNESS_TO_MAX, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::setBrightnessToMin() {
	unsigned char sendBuf[] = { COMMAND_BRIGHTNESS_TO_MIN, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::toggleBacklight() {
	unsigned char sendBuf[] = { COMMAND_BACKLIGHT_TOGGLE, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::setBacklight(bool value) {
	if (backlightEnabled != value) {
		toggleBacklight();
	}
}

bool ChalkboardLCD::getBacklight() {
	return backlightEnabled;
}

void ChalkboardLCD::toggleAutoBacklight() {
	unsigned char sendBuf[] = { COMMAND_AUTOBACKLIGHT_TOGGLE, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::setAutoBacklight(bool value) {
	if (autoBacklightEnabled != value) {
		toggleAutoBacklight();
	}
}

bool ChalkboardLCD::getAutoBacklight() {
	return autoBacklightEnabled;
}

void ChalkboardLCD::setBacklightLevel(const unsigned char level) {
	unsigned char sendBuf[] = {COMMAND_SET_BRIGHTNESS, level};
	unsigned char recvBuf[] = {0x00, 0x00};

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
	parseResponse(recvBuf);
}

void ChalkboardLCD::parseResponse(const unsigned char *in) {

	backlightEnabled = (in[0] & RESPONSE_BACKLIGHT_STATUS) != 0x00;
	autoBacklightEnabled = (in[0] & RESPONSE_AUTOBRIGHTNESS_STATUS) != 0x00;
	backlightLevel = in[0] & RESPONSE_BACKLIGHT_LEVEL;
	ambientLightLevel = in[1];
}

void ChalkboardLCD::readEEPROM() {
	unsigned char sendBuf[] = { 0xFF, 0x00 };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	unsigned char eeprom[EEPROM_SIZE];

	for (int i=0; i<EEPROM_SIZE; i++) {
		sendBuf[1] = i;
		recvBuf[0] = 0x00;
		recvBuf[1] = 0x00;
		exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
		if (recvBuf[0] == i) {
			eeprom[i] = recvBuf[1];
		} else {
			eeprom[i] = 0xFF;
		}
	}

	//printHex(eeprom, sizeof(eeprom));

	// Get FW version
	//byte fwVersion = eeprom[Chalk.EEPROM_FW_VERSION];

	version = eeprom[EEPROM_FW_VERSION];
	LCDType = eeprom[EEPROM_LCD];

	flipH = (eeprom[EEPROM_TOUCHCONFIG] & EEPROM_TOUCHCONFIG_FLIPH) != 0x00;
	flipV = (eeprom[EEPROM_TOUCHCONFIG] & EEPROM_TOUCHCONFIG_FLIPH) != 0x00;
}

unsigned char ChalkboardLCD::getVersion() {
	return version;
}

bool ChalkboardLCD::getFlipH() {
	return flipH;
}

void ChalkboardLCD::setFlipH(bool value) {
	if (value != flipH) {
		flipH = value;
		sendTouchconfig();
	}
}

bool ChalkboardLCD::getFlipV() {
	return flipV;
}

void ChalkboardLCD::setFlipV(bool value) {
	if (value != flipV) {
		flipV = value;
		sendTouchconfig();
	}
}

void ChalkboardLCD::sendTouchconfig() {
	unsigned char data = 0x00;
	if (flipV) { data |= 0x01; }
	if (flipH) { data |= 0x02; }
	unsigned char sendBuf[] = { (EEPROM_TOUCHCONFIG << 2)|0x03, data };
	unsigned char recvBuf[] = { 0x00, 0x00 };

	exchange(sendBuf, sizeof(sendBuf), recvBuf, sizeof(recvBuf));
}

unsigned char ChalkboardLCD::getLCDType() {
	return LCDType;
}

const char* ChalkboardLCD::getLCDTypeString() {
	const char* result = NULL;
	switch (LCDType) {
		case TYPE_7INCH_OPENFRAME:
			result = TYPESTR_7INCH_OPENFRAME;
			break;
		case TYPE_7INCH_BLACKFRAME:
			result = TYPESTR_7INCH_BLACKFRAME;
			break;
		case TYPE_10INCH:
			result = TYPESTR_10INCH;
			break;
		case TYPE_DUALLVDS_FULLHD:
			result = TYPESTR_DUALLVDS_FULLHD;
			break;
		default:
			result = TYPESTR_UNKNOWN;
			break;
	}

	return result;
}

unsigned char ChalkboardLCD::getBacklightLevel() {
	return backlightLevel;
}

unsigned char ChalkboardLCD::getAmbientLightLevel() {
	return ambientLightLevel;
}
