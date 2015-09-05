#include <jni.h>
#include "chalk/libchalk.cpp"
#include <android/log.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"CHALKJNI",__VA_ARGS__)

extern "C" {

ChalkboardLCD *lcd;

jclass brightnessResponseClassId;
jmethodID brightnessResponseConstructorId;

jclass eepromResponseClassId;
jmethodID eepromResponseConstructorId;

bool rootIsAllowed = false;


jint throwChalk(JNIEnv *env, const char *message) {
    jclass exClass = env->FindClass("com/autowp/chalk/driver/ChalkException");

    return env->ThrowNew(exClass, message);
}

jint throwChalkAccess(JNIEnv *env, const char *message) {
    jclass exClass = env->FindClass("com/autowp/chalk/driver/ChalkAccessException");

    return env->ThrowNew(exClass, message);
}

jint throwChalkNoDevice(JNIEnv *env, const char *message) {
    jclass exClass = env->FindClass("com/autowp/chalk/driver/ChalkNoDeviceException");

    return env->ThrowNew(exClass, message);
}

jobject assembleBrightnessResponse(JNIEnv * env) {
    brightnessResponseClassId = env->FindClass("com/autowp/chalk/driver/BrightnessResponse");
    if (brightnessResponseClassId == 0) {
        throwChalk(env, "Find Class Failed.");
    }

    brightnessResponseConstructorId = env->GetMethodID(brightnessResponseClassId, "<init>",
                                                       "(ZZBB)V");
    if (brightnessResponseConstructorId == 0) {
        throwChalk(env, "Find method Failed.");
    }
    return env->NewObject(brightnessResponseClassId, brightnessResponseConstructorId,
                          lcd->getBacklight(), lcd->getAutoBacklight(), lcd->getBacklightLevel(),
                          lcd->getAmbientLightLevel());
}


JNIEXPORT void JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_connect(JNIEnv * env , jobject instance ) {

    LOGI("connect");

    if (rootIsAllowed) {
        LOGI("connect with root");
    }

    lcd = new ChalkboardLCD();
    libchalk_error connectResult = lcd->connect(rootIsAllowed);
    if ( connectResult != LIBCHALK_SUCCESS ) {
        if ( connectResult == LIBCHALK_ERROR_ACCESS ) {
            throwChalkAccess(env, "Insufficient permission" ) ;
        } else if (connectResult == LIBCHALK_ERROR_NO_DEVICE) {
            throwChalkNoDevice(env, "Device not found");
        } else {
            throwChalk(env, "Connection error");
        }
    }
}

JNIEXPORT jboolean

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_isConnected(JNIEnv *env, jobject instance) {

    return (lcd != nullptr) && lcd->isConnected();

}

JNIEXPORT void JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_disconnect(JNIEnv *env, jobject instance) {

    delete lcd;
    lcd = nullptr;

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_readStatus(JNIEnv *env, jobject instance) {

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_brightnessDec(JNIEnv *env, jobject instance) {

    lcd->brightnessDec();

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_brightnessInc(JNIEnv *env, jobject instance) {

    lcd->brightnessInc();

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_setBrightnessToMax(JNIEnv *env, jobject instance) {

    lcd->setBrightnessToMax();

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_setBrightnessToMin(JNIEnv *env, jobject instance) {

    lcd->setBrightnessToMin();

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_toggleBacklight(JNIEnv *env, jobject instance) {

    lcd->toggleBacklight();

    return assembleBrightnessResponse(env);
}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_toggleAutoBacklight(JNIEnv *env, jobject instance) {

    lcd->toggleAutoBacklight();

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_setBacklightLevel(JNIEnv *env, jobject instance,
                                                              jbyte level) {

    lcd->setBacklightLevel(level);

    return assembleBrightnessResponse(env);

}

JNIEXPORT jobject

JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_readEEPROM(JNIEnv *env, jobject instance) {

    eepromResponseClassId = env->FindClass("com/autowp/chalk/driver/EEPROMResponse");
    if (eepromResponseClassId == 0) {
        throwChalk(env, "Find Class Failed.");
    }

    eepromResponseConstructorId = env->GetMethodID(eepromResponseClassId, "<init>", "(BZZ)V");
    if (eepromResponseConstructorId == 0) {
        throwChalk(env, "Find method Failed.");
    }

    return env->NewObject(eepromResponseClassId, eepromResponseConstructorId, lcd->getLCDType(),
                          lcd->getFlipH(), lcd->getFlipV());

}

JNIEXPORT void JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_sendTouchconfig(JNIEnv *env, jobject instance, jboolean flipH, jboolean flipV) {

    lcd->setFlipH(flipH);
    lcd->setFlipV(flipV);
    lcd->sendTouchconfig();

}

JNIEXPORT void JNICALL
Java_com_autowp_chalk_driver_ChalkDriverJNI_allowRoot(JNIEnv *env, jobject instance) {

    rootIsAllowed = true;

}

}