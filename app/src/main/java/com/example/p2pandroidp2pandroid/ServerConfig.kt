package com.example.p2pandroidp2pandroid

object ServerConfig {
    // TODO: Update to your Oracle RDS ORDS endpoint
    const val BASE_URL = "https://oracleapex.com/ords/yakiev/p2p_main/"
    
    // Endpoints
    const val USERS = "${BASE_URL}users/"
    const val CALLS = "${BASE_URL}calls/"
    const val SESSION = "${BASE_URL}session/"
    const val SDP_OFFERS = "${BASE_URL}sdp/offers/"
    const val SDP_ANSWERS = "${BASE_URL}sdp/answers/"
    const val ICE_CANDIDATES = "${BASE_URL}candidates/"
    const val HEALTH = "${BASE_URL}health/"
    
    // Test / development endpoints
    const val TEST_CONNECT = "${BASE_URL}signaling/test_connect/"
}
