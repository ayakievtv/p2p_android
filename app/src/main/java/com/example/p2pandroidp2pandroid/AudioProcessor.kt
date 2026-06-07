    package com.example.p2pandroidp2pandroid

    import android.content.Context
    import android.media.AudioManager
    import android.util.Log
    import org.webrtc.AudioTrack

    /**
     * Audio processing utilities for call quality optimization.
     * Handles echo cancellation, noise suppression, speakerphone, mute.
     */
    class AudioProcessor(private val context: Context) {
        companion object {
            private const val TAG = "AudioProcessor"
        }

        private var audioTrack: AudioTrack? = null
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        fun initialize() {
            Log.d(TAG, "Audio processor ready")
            // WebRTC handles echo cancellation and noise suppression automatically
            // via JavaAudioDeviceModule settings in WebRTCManager
        }

        fun enableSpeakerphone(enable: Boolean) {
            audioManager.isSpeakerphoneOn = enable
            Log.d(TAG, "Speakerphone: $enable")
        }

        fun setMicrophoneMute(mute: Boolean) {
            audioManager.isMicrophoneMute = mute
            Log.d(TAG, "Microphone muted: $mute")
        }

        fun setAudioTrack(track: AudioTrack) {
            audioTrack = track
            audioTrack?.setEnabled(true)
        }

        fun muteAudioTrack(mute: Boolean) {
            audioTrack?.setEnabled(!mute)
            Log.d(TAG, "Audio track muted: $mute")
        }

        fun cleanup() {
            audioTrack?.dispose()
            audioTrack = null
            Log.d(TAG, "Cleaning up audio processor")
        }
    }