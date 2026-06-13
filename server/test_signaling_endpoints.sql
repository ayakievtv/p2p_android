DECLARE
    -- Пробуем сначала создать модуль, если его нет
    v_module_exists NUMBER := 0;
BEGIN
    -- 1. РЕГИСТРАЦИЯ ИЛИ ПЕРЕЗАПИСЬ МОДУЛЯ
    ORDS.DEFINE_MODULE(
        p_module_name    => 'p2p_main',
        p_base_path      => '/p2p_main/', -- Базовый URL: /ords/holayakay/p2p_main/
        p_items_per_page => 25,
        p_status         => 'PUBLISHED',
        p_comments       => 'Main module for P2P VoIP Signaling'
    );

    -- 2. СОЗДАНИЕ ШАБЛОНА И ОБРАБОТЧИКА ДЛЯ GET /signaling/test_users/
    ORDS.DEFINE_TEMPLATE(
        p_module_name => 'p2p_main',
        p_pattern     => 'signaling/test_users/',
        p_priority    => 0,
        p_etag_type   => 'HASH'
    );

    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main',
        p_pattern     => 'signaling/test_users/',
        p_method      => 'GET',
        p_source_type => 'plsql/block',
        p_mimes_allowed => NULL,
        p_comments    => 'Test endpoint: returns two hardcoded users with fake signaling tokens',
        p_source => q'[
BEGIN
    OWA_UTIL.MIME_HEADER('application/json', TRUE);
    HTP.P('{');
    HTP.P('  "users":[');
    HTP.P('    {"user_id":"aidar","name":"Aidar","signaling_token":"test_tok_aidar_a1b2c3d4e5f6","fcm_token":"fake_fcm_aidar_000000000000"},');
    HTP.P('    {"user_id":"mama","name":"Mama","signaling_token":"test_tok_mama_x9y8z7w6v5u4","fcm_token":"fake_fcm_mama_000000000000"}');
    HTP.P('  ],');
    HTP.P('  "mode":"test"');
    HTP.P('}');
END;
        ]'
    );

    -- 3. СОЗДАНИЕ ШАБЛОНА И ОБРАБОТЧИКА ДЛЯ POST /signaling/test_connect/
    ORDS.DEFINE_TEMPLATE(
        p_module_name => 'p2p_main',
        p_pattern     => 'signaling/test_connect/',
        p_priority    => 0,
        p_etag_type   => 'HASH'
    );

    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main',
        p_pattern     => 'signaling/test_connect/',
        p_method      => 'POST',
        p_source_type => 'plsql/block',
        p_mimes_allowed => 'application/json',
        p_comments    => 'Test endpoint: simulates successful WebRTC session creation between two users',
        p_source => q'[
DECLARE
    l_caller_id  VARCHAR2(50) := COALESCE(:caller_id, :userId);
    l_callee_id  VARCHAR2(50) := :callee_id;
    l_session_id VARCHAR2(50);
    l_now        TIMESTAMP;
    l_crlf       VARCHAR2(2) := CHR(13) || CHR(10);
BEGIN
    OWA_UTIL.MIME_HEADER('application/json', TRUE);

    IF l_caller_id IS NULL OR l_callee_id IS NULL THEN
        HTP.P('{"success":false,"error":"caller_id and callee_id are required"}');
        RETURN;
    END IF;

    SELECT 'test_' || DBMS_RANDOM.STRING('x', 16) INTO l_session_id FROM dual;
    l_now := CURRENT_TIMESTAMP;

    HTP.P('{');
    HTP.P('  "success":true,');
    HTP.P('  "session_id":"' || l_session_id || '",');
    HTP.P('  "caller_id":"' || l_caller_id || '",');
    HTP.P('  "callee_id":"' || l_callee_id || '",');
    HTP.P('  "status":"connected",');
    HTP.P('  "offer_sdp":"v=0' || l_crlf || 'o=- ' || TRUNC(DBMS_RANDOM.VALUE(10000,99999)) || ' 2 IN IP4 127.0.0.1' || l_crlf || 's=-' || l_crlf || 't=0 0' || l_crlf || 'm=audio 9 UDP/TLS/RTP/SAVPF 111' || l_crlf || 'a=ice-ufrag:testufrg' || l_crlf || 'a=ice-pwd:testpwd0000000000abcd' || l_crlf || 'a=fingerprint:sha-256 AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89' || l_crlf || 'a=mid:0",');
    HTP.P('  "answer_sdp":"v=0' || l_crlf || 'o=- ' || TRUNC(DBMS_RANDOM.VALUE(10000,99999)) || ' 1 IN IP4 127.0.0.1' || l_crlf || 's=-' || l_crlf || 't=0 0' || l_crlf || 'm=audio 9 UDP/TLS/RTP/SAVPF 111' || l_crlf || 'a=ice-ufrag:answufrg' || l_crlf || 'a=ice-pwd:answpwd0000000000xyz' || l_crlf || 'a=fingerprint:sha-256 FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10' || l_crlf || 'a=mid:0",');
    HTP.P('  "ice_candidates":[');
    HTP.P('    {"candidate":"candidate:1 1 UDP 2130706431 test-local 5000 typ host","sdp_mid":"0","sdp_mline_index":0},');
    HTP.P('    {"candidate":"candidate:2 1 TCP 2105342879 test-local 5001 typ host tcptype active","sdp_mid":"0","sdp_mline_index":0}');
    HTP.P('  ],');
    HTP.P('  "mode":"test",');
    HTP.P('  "created_at":"' || TO_CHAR(l_now, 'YYYY-MM-DD"T"HH24:MI:SS') || '"');
    HTP.P('}');
END;
        ]'
    );

    COMMIT;
END;