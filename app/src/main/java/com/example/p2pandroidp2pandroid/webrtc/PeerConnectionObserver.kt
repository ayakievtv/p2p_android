package com.example.p2pandroidp2pandroid.webrtc

import org.webrtc.*

/**
 * Кастомный интерфейс для обработки событий PeerConnection
 */
interface PeerConnectionObserver {
    fun onIceCandidate(candidate: IceCandidate)
    fun onIceCandidatesRemoved(candidates: Array<IceCandidate>)
    fun onAddStream(stream: MediaStream)
    fun onRemoveStream(stream: MediaStream)
    fun onDataChannel(channel: DataChannel)
    fun onConnectionChange(newState: PeerConnection.PeerConnectionState)
    fun onIceConnectionChange(newState: PeerConnection.IceConnectionState)
    fun onIceGatheringChange(newState: PeerConnection.IceGatheringState)
    fun onSignalingChange(newState: PeerConnection.SignalingState)
    fun onError(error: String)
}

/**
 * Адаптер с пустыми методами для опциональных переопределений
 */
abstract class PeerConnectionObserverAdapter : PeerConnectionObserver {
    override fun onIceCandidate(candidate: IceCandidate) {}
    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
    override fun onAddStream(stream: MediaStream) {}
    override fun onRemoveStream(stream: MediaStream) {}
    override fun onDataChannel(channel: DataChannel) {}
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {}
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
    override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
    override fun onError(error: String) {}
}

/**
 * Мост между PeerConnection.Observer (WebRTC) и PeerConnectionObserver (наш интерфейс).
 * Передаётся напрямую в factory.createPeerConnection()
 */
class PeerConnectionObserverBridge(
    private val observer: PeerConnectionObserver
) : PeerConnection.Observer {

    override fun onSignalingChange(newState: PeerConnection.SignalingState) {
        observer.onSignalingChange(newState)
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
        observer.onIceConnectionChange(newState)
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {}

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
        observer.onIceGatheringChange(newState)
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        observer.onIceCandidate(candidate)
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
        observer.onIceCandidatesRemoved(candidates)
    }

    override fun onAddStream(stream: MediaStream) {
        observer.onAddStream(stream)
    }

    override fun onRemoveStream(stream: MediaStream) {
        observer.onRemoveStream(stream)
    }

    override fun onDataChannel(channel: DataChannel) {
        observer.onDataChannel(channel)
    }

    override fun onRenegotiationNeeded() {}

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
        observer.onConnectionChange(newState)
    }

    override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {}

    override fun onTrack(transceiver: RtpTransceiver) {}
}