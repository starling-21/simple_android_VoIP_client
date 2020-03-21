# Simple android VoIP/SIP client. Audio streaming.

Simple is better than complex!
This is VoIP client prototype. I was searching a lot for open source projects, but found just a few working solutions for SIP signalling and voice transferring (very huge or fully native written apps). Then
Core classes of the app have explaining comments, so don't be afraid of dive into the project. 

## Built With

* [Android JAIN Sip RI](https://mvnrepository.com/artifact/javax.sip/android-jain-sip-ri) - SIP library for client-server communication

* [Opus Interactive Audio Codec](http://opus-codec.org/) - used for audio encoding/decoding. Opus is narrowband configured here (sample rate - 8 kHz), but can be extended for medium and wide band, all settings (frame rate, frame size and codec buffer size) in two classes.

* [jlibrtp as internal package](https://sourceforge.net/projects/jlibrtp/) - Used for transmitting and receiving RTP packets. Basically you initialize it with 2 DatagramSockets (one for sending RTP data and one for receiving RTCP data). I beleive it handles the RTP timestamps by itself, but you have to make sure you're payload is already formatted by the RFC reccommendations.

### Prerequisites

* (NDK) The Native Development Kit. Project contains OPUS library compiled just for arm64-v8a platform. It can be recompiled to support other platforms.

* (JNI) to be familiar with Java Native Interface. Audio codec included as external C-library via JNI calls.
There is jniwrappers java classes in separate package, which loads opus library, initialize it and does encoding / decoding calls.

* (SIP-server) I used asterisk for that. Opus can be compiled from sources and configured over there.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* It would be a pleasure to know if I helped to someone by sharing this project. 

starling1715@gmail.com 

