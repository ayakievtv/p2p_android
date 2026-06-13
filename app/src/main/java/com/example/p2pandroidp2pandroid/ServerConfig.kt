package com.example.p2pandroidp2pandroid

object ServerConfig {

    // ------------------------------------------------------------
    // BASE
    // ------------------------------------------------------------

    const val BASE_URL =
        "https://oracleapex.com/ords/yakiev/p2p_main/"

    // ------------------------------------------------------------
    // CORE SIGNALING FLOW (AUTO MATCHING)
    // ------------------------------------------------------------

    // единственная точка входа для Android
    const val MATCH =
        "${BASE_URL}signaling/match/"

    const val SESSION =
        "${BASE_URL}signaling/session/"

    // ------------------------------------------------------------
    // WEBRTC SIGNALING
    // ------------------------------------------------------------

    const val SDP_OFFERS =
        "${BASE_URL}webrtc/sdp/offers/"
//
    const val SDP_ANSWERS =
        "${BASE_URL}webrtc/sdp/answers/"
//
    const val ICE_CANDIDATES =
        "${BASE_URL}webrtc/ice/"

    // ------------------------------------------------------------
    // HEALTH CHECK
    // ------------------------------------------------------------

    const val HEALTH =
        "${BASE_URL}health/"

}