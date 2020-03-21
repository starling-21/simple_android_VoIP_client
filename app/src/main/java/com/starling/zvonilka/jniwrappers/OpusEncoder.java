package com.starling.zvonilka.jniwrappers;

/**
 * Created by starling on 2/12/18.
 */

public class OpusEncoder {

    public native boolean nativeInitEncoder(int samplingRate, int numberOfChannels, int frameSize);

    public native int nativeEncodeBytes(short[] in, byte[] out);

    public native boolean nativeReleaseEncoder();

    static {
        try {
            System.loadLibrary("codec");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    public void init(int sampleRate, int channels, int frameSize) {
        this.nativeInitEncoder(sampleRate, channels, frameSize);
    }

    public int encode(short[] buffer, byte[] out) {
        int encoded = this.nativeEncodeBytes(buffer, out);

        return encoded;
    }

    public void close() {
        this.nativeReleaseEncoder();
    }

}
