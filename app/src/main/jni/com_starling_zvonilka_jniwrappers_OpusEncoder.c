#include <com_starling_zvonilka_jniwrappers_OpusEncoder.h>
#include <string.h>
#include <android/log.h>
//#include <opus/opus.h>
#include <opus-1.2.1/include/opus.h>
#include <stdio.h>


char logMsg[255];
OpusEncoder *enc;

opus_int32 SAMPLING_RATE;
int CHANNELS;
int APPLICATION_TYPE = OPUS_APPLICATION_VOIP;
int FRAME_SIZE;
const int MAX_PAYLOAD_BYTES = 1400;


    JNIEXPORT jboolean JNICALL Java_com_starling_zvonilka_jniwrappers_OpusEncoder_nativeInitEncoder (JNIEnv *env, jobject obj,
    jint samplingRate, jint numberOfChannels, jint frameSize)
    {
        FRAME_SIZE = frameSize;
        SAMPLING_RATE = samplingRate;
        CHANNELS = numberOfChannels;

        int error;
        int size;

        size = opus_encoder_get_size(1);
        enc = malloc(size);
        error = opus_encoder_init(enc, SAMPLING_RATE, CHANNELS, APPLICATION_TYPE);


        if (error == OPUS_OK) {
            snprintf(logMsg, sizeof(logMsg), "%s", "OK");
        } else {
            snprintf(logMsg, sizeof(logMsg), "%s", "NOT Initialized");
        }
        __android_log_write(ANDROID_LOG_DEBUG, "CODEC INITIALIZATION STATE: ", logMsg);

        return error;
    }

    JNIEXPORT jint JNICALL Java_com_starling_zvonilka_jniwrappers_OpusEncoder_nativeEncodeBytes (JNIEnv *env, jobject obj,
    jshortArray in, jbyteArray out)
    {
        jint inputArraySize = (*env)->GetArrayLength(env, in);
        jint outputArraySize = (*env)->GetArrayLength(env, out);

        jshort* audioSignal = (*env)->GetShortArrayElements(env, in, 0);

        unsigned char *data = (unsigned char*)calloc(MAX_PAYLOAD_BYTES,sizeof(unsigned char));
        int dataArraySize = opus_encode(enc, audioSignal, FRAME_SIZE, data, MAX_PAYLOAD_BYTES);

/*        //testing output
        sprintf(logMsg, "nativeEncodeBytes, dataArraySize: %d", dataArraySize);
        __android_log_write(ANDROID_LOG_DEBUG, "Native Code:", logMsg);

        //testing output 2
        sprintf(logMsg, "nativeEncodeBytes, inputArraySize: %d", inputArraySize);
        __android_log_write(ANDROID_LOG_DEBUG, "Native Code:", logMsg);

        //testing output 3
        sprintf(logMsg, "nativeEncodeBytes, outputArraySize: %d", outputArraySize);
        __android_log_write(ANDROID_LOG_DEBUG, "Native Code:", logMsg);
*/
        if (dataArraySize >=0)
        {
            if (dataArraySize <= outputArraySize)
            {
                (*env)->SetByteArrayRegion(env,out,0,dataArraySize,data);
            }
            else
            {
                sprintf(logMsg, "Output array of size: %d to small for storing encoded data.", outputArraySize);
                __android_log_write(ANDROID_LOG_DEBUG, "Native Code:", logMsg);

                return -1;
            }
        }

        (*env)->ReleaseShortArrayElements(env,in,audioSignal,JNI_ABORT);

        return dataArraySize;
    }

    JNIEXPORT jboolean JNICALL Java_com_starling_zvonilka_jniwrappers_OpusEncoder_nativeReleaseEncoder (JNIEnv *env, jobject obj)
    {
        free(enc);

        return 1;
    }
