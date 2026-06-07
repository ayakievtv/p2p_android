package com.example.p2papp.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WebRTCManager(
    private val context: Context,
    private val peerConnectionObserver: PeerConnectionObserver
) {
    companion object {
        private const val TAG = "WebRTCManager"
        private const val AUDIO_TRACK_ID = "audio-track"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var localAudioTrack: AudioTrack? = null
    private var isSettingRemoteDescription = false
    private var pendingCandidates: MutableList<IceCandidate> = mutableListOf()

    fun initialize() {
        Log.d(TAG, "Initializing WebRTC")

        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        Log.d(TAG, "WebRTC initialized")
    }

    fun createPeerConnection(servers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(servers).apply {
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory?.createPeerConnection(
            rtcConfig,
            PeerConnectionObserverBridge(peerConnectionObserver)
        )

        Log.d(TAG, "PeerConnection created")
    }

    fun createOffer(callback: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                description?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() { callback(it.description) }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            Log.e(TAG, "setLocalDescription failed: $p0")
                        }
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "createOffer failed: $p0")
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: String, type: String, callback: () -> Unit = {}) {
        isSettingRemoteDescription = true
        val sessionDescription = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type),
            sdp
        )
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                isSettingRemoteDescription = false
                // добавляем накопленные кандидаты
                pendingCandidates.forEach { peerConnection?.addIceCandidate(it) }
                pendingCandidates.clear()
                callback()
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "setRemoteDescription failed: $p0")
                isSettingRemoteDescription = false
            }
        }, sessionDescription)
    }

    fun createAnswer(callback: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                description?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() { callback(it.description) }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            Log.e(TAG, "setLocalDescription (answer) failed: $p0")
                        }
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "createAnswer failed: $p0")
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (isSettingRemoteDescription) {
            pendingCandidates.add(candidate)
        } else {
            peerConnection?.addIceCandidate(candidate)
        }
    }

    fun getLocalDescription(): String? {
        return peerConnection?.localDescription?.description
    }

    fun hangUp() {
        executor.execute {
            try {
                peerConnection?.close()
                peerConnection = null
                localAudioTrack?.dispose()
                localAudioTrack = null
                Log.d(TAG, "PeerConnection closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during hangUp", e)
            }
        }
    }

    fun dispose() {
        hangUp()
        executor.shutdown()
        factory?.dispose()
        factory = null
    }
}