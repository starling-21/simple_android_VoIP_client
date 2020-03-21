package com.starling.zvonilka.jniwrappers;

/**
 * Created by starling on 2/12/18.
 */

public class OpusDecoder {

    public native boolean nativeInitDecoder(int samplingRate, int numberOfChannels, int frameSize);

    public native int nativeDecodeBytes(byte[] in, short[] out);

    public native boolean nativeReleaseDecoder();

    static {
        try {
            System.loadLibrary("codec");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }



    public void init(int sampleRate, int channels, int frameSize) {
        this.nativeInitDecoder(sampleRate, channels, frameSize);
    }

    public int decode(byte[] encodedBuffer, short[] buffer) {
        int decoded = this.nativeDecodeBytes(encodedBuffer, buffer);

        return decoded;
    }

    public void close() {
        this.nativeReleaseDecoder();
    }

}
