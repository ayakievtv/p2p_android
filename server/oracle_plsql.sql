-- =============================================================================
-- P2P Calling Server - Oracle PL/SQL Implementation
-- =============================================================================

SET SERVEROUTPUT ON
SET VERIFY OFF
SET TIMING OFF

WHENEVER SQLError ON ROLLBACK
WHENEVER SQLerror ON ROLLBACK

-- -----------------------------------------------------------------------------
-- 1. CREATE TABLES
-- -----------------------------------------------------------------------------

CREATE TABLE app_users (
    user_id VARCHAR2(50) PRIMARY KEY,
    fcm_token VARCHAR2(1000),
    name VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_users_fcm ON app_users(fcm_token);

CREATE TABLE call_sessions (
    session_id VARCHAR2(50) PRIMARY KEY,
    caller_id VARCHAR2(50) NOT NULL,
    callee_id VARCHAR2(50) NOT NULL,
    status VARCHAR2(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE call_sessions ADD CONSTRAINT fk_caller FOREIGN KEY (caller_id) REFERENCES app_users(user_id);
ALTER TABLE call_sessions ADD CONSTRAINT fk_callee FOREIGN KEY (callee_id) REFERENCES app_users(user_id);

CREATE INDEX idx_call_sessions_status ON call_sessions(status);
CREATE INDEX idx_call_sessions_caller ON call_sessions(caller_id);
CREATE INDEX idx_call_sessions_callee ON call_sessions(callee_id);

CREATE TABLE ice_candidates (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id VARCHAR2(50) NOT NULL,
    candidate CLOB,
    sdp_mid VARCHAR2(50),
    sdp_manced NUMBER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ice_candidates ADD CONSTRAINT fk_ice_session FOREIGN KEY (session_id) REFERENCES call_sessions(session_id) ON DELETE CASCADE;
CREATE INDEX idx_ice_session ON ice_candidates(session_id);

CREATE TABLE sdp_offers (
    session_id VARCHAR2(50) PRIMARY KEY,
    offer_sdp CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sdp_answers (
    session_id VARCHAR2(50) PRIMARY KEY,
    answer_sdp CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 2. PL/SQL PACKAGE
-- -----------------------------------------------------------------------------

CREATE OR REPLACE PACKAGE p2p_api_pkg AS

    -- User management
    PROCEDURE register_user(p_user_id IN VARCHAR2, p_fcm_token IN VARCHAR2 DEFAULT NULL, p_name IN VARCHAR2 DEFAULT NULL, p_result OUT VARCHAR2);
    FUNCTION get_user_fcm_token(p_user_id IN VARCHAR2) RETURN VARCHAR2;
    FUNCTION get_user_name(p_user_id IN VARCHAR2) RETURN VARCHAR2;
    FUNCTION get_user_json(p_user_id IN VARCHAR2) RETURN CLOB;
  
    -- Call management
    PROCEDURE initiate_call(p_caller_id IN VARCHAR2, p_callee_id IN VARCHAR2, p_session_id OUT VARCHAR2, p_result OUT VARCHAR2);
    PROCEDURE update_session_status(p_session_id IN VARCHAR2, p_status IN VARCHAR2, p_result OUT VARCHAR2);
    FUNCTION get_session_json(p_session_id IN VARCHAR2) RETURN CLOB;
  
    -- SDP Offer
    PROCEDURE save_offer(p_session_id IN VARCHAR2, p_sdp IN CLOB, p_result OUT VARCHAR2);
    FUNCTION get_offer(p_session_id IN VARCHAR2) RETURN CLOB;
    FUNCTION get_offer_json(p_session_id IN VARCHAR2) RETURN CLOB;
  
    -- SDP Answer
    PROCEDURE save_answer(p_session_id IN VARCHAR2, p_sdp IN CLOB, p_result OUT VARCHAR2);
    FUNCTION get_answer(p_session_id IN VARCHAR2) RETURN CLOB;
    FUNCTION get_answer_json(p_session_id IN VARCHAR2) RETURN CLOB;
  
    -- ICE Candidates
    PROCEDURE add_candidate(p_session_id IN VARCHAR2, p_candidate IN VARCHAR2, p_sdp_mid IN VARCHAR2, p_sdp_manced IN NUMBER, p_result OUT VARCHAR2);
    FUNCTION get_candidates_json(p_session_id IN VARCHAR2) RETURN CLOB;
  
    -- Utilities
    FUNCTION health_check RETURN VARCHAR2;

END p2p_api_pkg;
/

-- -----------------------------------------------------------------------------
-- 3. PACKAGE BODY
-- -----------------------------------------------------------------------------

CREATE OR REPLACE PACKAGE BODY p2p_api_pkg AS

    PROCEDURE register_user(p_user_id IN VARCHAR2, p_fcm_token IN VARCHAR2 DEFAULT NULL, p_name IN VARCHAR2 DEFAULT NULL, p_result OUT VARCHAR2) IS
    BEGIN
        MERGE INTO app_users u USING (SELECT p_user_id AS user_id FROM dual) q ON (u.user_id = q.user_id)
        WHEN MATCHED THEN UPDATE SET fcm_token = COALESCE(p_fcm_token, fcm_token), name = COALESCE(p_name, name), updated_at = CURRENT_TIMESTAMP
        WHEN NOT MATCHED THEN INSERT (user_id, fcm_token, name) VALUES (p_user_id, p_fcm_token, p_name);
        p_result := '{"success":true}';
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END register_user;
  
    FUNCTION get_user_fcm_token(p_user_id IN VARCHAR2) RETURN VARCHAR2 IS v_token VARCHAR2(1000);
    BEGIN SELECT fcm_token INTO v_token FROM app_users WHERE user_id = p_user_id; RETURN v_token; EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL; END get_user_fcm_token;
  
    FUNCTION get_user_name(p_user_id IN VARCHAR2) RETURN VARCHAR2 IS v_name VARCHAR2(100);
    BEGIN SELECT name INTO v_name FROM app_users WHERE user_id = p_user_id; RETURN v_name; EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL; END get_user_name;
  
    FUNCTION get_user_json(p_user_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT '{"user_id":"' || user_id || '","fcm_token":"' || SUBSTR(fcm_token, 1, 1000) || '","name":"' || SUBSTR(name, 1, 100) || '"}' INTO v_result FROM app_users WHERE user_id = p_user_id;
        RETURN v_result;
    EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_user_json;
  
    PROCEDURE initiate_call(p_caller_id IN VARCHAR2, p_callee_id IN VARCHAR2, p_session_id OUT VARCHAR2, p_result OUT VARCHAR2) IS
    BEGIN
        p_session_id := SYS_GUID();
        INSERT INTO call_sessions (session_id, caller_id, callee_id, status) VALUES (p_session_id, p_caller_id, p_callee_id, 'pending');
        p_result := '{"session_id":"' || p_session_id || '"}';
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END initiate_call;
  
    PROCEDURE update_session_status(p_session_id IN VARCHAR2, p_status IN VARCHAR2, p_result OUT VARCHAR2) IS
    BEGIN
        UPDATE call_sessions SET status = p_status, updated_at = CURRENT_TIMESTAMP WHERE session_id = p_session_id;
        p_result := CASE WHEN SQL%ROWCOUNT > 0 THEN '{"success":true}' ELSE '{"success":false,"error":"Session not found"}' END;
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END update_session_status;
  
    FUNCTION get_session_json(p_session_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT '{"session_id":"' || session_id || '","caller_id":"' || caller_id || '","callee_id":"' || callee_id || '","status":"' || status || '","created_at":"' || TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') || '"}' INTO v_result FROM call_sessions WHERE session_id = p_session_id;
        RETURN v_result;
    EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_session_json;
  
    PROCEDURE save_offer(p_session_id IN VARCHAR2, p_sdp IN CLOB, p_result OUT VARCHAR2) IS
    BEGIN
        MERGE INTO sdp_offers o USING (SELECT p_session_id AS session_id FROM dual) q ON (o.session_id = q.session_id) WHEN MATCHED THEN UPDATE SET offer_sdp = p_sdp WHEN NOT MATCHED THEN INSERT (session_id, offer_sdp) VALUES (p_session_id, p_sdp);
        p_result := '{"success":true}';
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END save_offer;
  
    FUNCTION get_offer(p_session_id IN VARCHAR2) RETURN CLOB IS v_sdp CLOB;
    BEGIN SELECT offer_sdp INTO v_sdp FROM sdp_offers WHERE session_id = p_session_id; RETURN v_sdp; EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_offer;
    FUNCTION get_offer_json(p_session_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT '{"session_id":"' || session_id || '","offer_sdp":' || DBMS_LOB.SUBSTR(offer_sdp, 4000, 1) || '}' INTO v_result FROM sdp_offers WHERE session_id = p_session_id;
        RETURN v_result;
    EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_offer_json;
  
    PROCEDURE save_answer(p_session_id IN VARCHAR2, p_sdp IN CLOB, p_result OUT VARCHAR2) IS
    BEGIN
        MERGE INTO sdp_answers a USING (SELECT p_session_id AS session_id FROM dual) q ON (a.session_id = q.session_id) WHEN MATCHED THEN UPDATE SET answer_sdp = p_sdp WHEN NOT MATCHED THEN INSERT (session_id, answer_sdp) VALUES (p_session_id, p_sdp);
        p_result := '{"success":true}';
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END save_answer;
  
    FUNCTION get_answer(p_session_id IN VARCHAR2) RETURN CLOB IS v_sdp CLOB;
    BEGIN SELECT answer_sdp INTO v_sdp FROM sdp_answers WHERE session_id = p_session_id; RETURN v_sdp; EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_answer;
    FUNCTION get_answer_json(p_session_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT '{"session_id":"' || session_id || '","answer_sdp":' || DBMS_LOB.SUBSTR(answer_sdp, 4000, 1) || '}' INTO v_result FROM sdp_answers WHERE session_id = p_session_id;
        RETURN v_result;
    EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
    END get_answer_json;
  
    PROCEDURE add_candidate(p_session_id IN VARCHAR2, p_candidate IN VARCHAR2, p_sdp_mid IN VARCHAR2, p_sdp_manced IN NUMBER, p_result OUT VARCHAR2) IS
    BEGIN
        INSERT INTO ice_candidates (session_id, candidate, sdp_mid, sdp_manced) VALUES (p_session_id, p_candidate, p_sdp_mid, p_sdp_manced);
        p_result := '{"success":true}';
    EXCEPTION WHEN OTHERS THEN p_result := '{"success":false,"error":"' || SUBSTR(SQLERRM, 1, 200) || '"}';
    END add_candidate;
  
    FUNCTION get_candidates(p_session_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT JSON_ARRAYAGG(JSON_OBJECT('candidate' VALUE candidate, 'sdp_mid' VALUE sdp_mid, 'sdp_manced' VALUE sdp_manced)) INTO v_result FROM ice_candidates WHERE session_id = p_session_id;
        RETURN v_result;
    EXCEPTION WHEN NO_DATA_FOUND THEN RETURN '[]';
    END get_candidates;
  
    FUNCTION get_candidates_json(p_session_id IN VARCHAR2) RETURN CLOB IS v_result CLOB;
    BEGIN
        SELECT '{"session_id":"' || p_session_id || '","candidates":' || get_candidates(p_session_id) || '}' INTO v_result FROM dual;
        RETURN v_result;
    EXCEPTION WHEN OTHERS THEN RETURN '[]';
    END get_candidates_json;
  
    FUNCTION health_check RETURN VARCHAR2 IS
    BEGIN
        RETURN '{"status":"ok","timestamp":"' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS') || '"}';
    END health_check;

END p2p_api_pkg;
/

-- ============================================================================
-- ORDS REST SERVICES
-- ============================================================================

BEGIN
    ORDS.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'P2P', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'p2p', p_validate_module => TRUE);
    COMMIT;
END;
/

-- Main module
BEGIN
    ORDS.DEFINE_MODULE(p_module_name => 'p2p_main', p_base_path => '/', p_status => 'PUBLISHED', p_comments => 'P2P Calling API');
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- HEALTH CHECK
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'health/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => 'health/', p_method => 'GET',
        p_source_type => 'plsql/block', p_source => ':status := p2p_api_pkg.health_check;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- USERS
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'users/{user_id}', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{user_id}', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    :fcm_token := p2p_api_pkg.get_user_fcm_token(:user_id);
    :name := p2p_api_pkg.get_user_name(:user_id);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- CALLS (Initiate call)
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'calls/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '', p_method => 'POST',
        p_source_type => 'plsql/block',
        p_mimes_allowed => 'application/json',
        p_source => '
DECLARE
    l_session_id VARCHAR2(50);
BEGIN
    p2p_api_pkg.initiate_call(:caller_id, :callee_id, l_session_id, :out);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- CALLS/{session_id} (Get session, Update status)
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'calls/{session_id}/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => ':out := p2p_api_pkg.get_session_json(:session_id);'
    );
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'PUT',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    p2p_api_pkg.update_session_status(:session_id, :status, :out);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- SDP OFFERS
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'sdp/offers/{session_id}/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => ':out := p2p_api_pkg.get_offer_json(:session_id);'
    );
    COMMIT;
END;
/

-- POST /ords/p2p/sdp/offers/
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'sdp/offers/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '', p_method => 'POST',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    p2p_api_pkg.save_offer(:session_id, :sdp, :out);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- SDP ANSWERS
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'sdp/answers/{session_id}/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => ':out := p2p_api_pkg.get_answer_json(:session_id);'
    );
    COMMIT;
END;
/

-- POST /ords/p2p/sdp/answers/
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'sdp/answers/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '', p_method => 'POST',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    p2p_api_pkg.save_answer(:session_id, :sdp, :out);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- ICE CANDIDATES
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'candidates/{session_id}/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => ':out := p2p_api_pkg.get_candidates_json(:session_id);'
    );
    COMMIT;
END;
/

-- POST /ords/p2p/candidates/
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'candidates/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '', p_method => 'POST',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    p2p_api_pkg.add_candidate(:session_id, :candidate, :sdp_mid, :sdp_manced, :out);
END;'
    );
    COMMIT;
END;
/

-- ==----==----==----==----==----==----==----==--
-- SESSION (Full JSON)
-- ==----==----==----==----==----==----==----==--
BEGIN
    ORDS.DEFINE_TEMPLATE(p_module_name => 'p2p_main', p_pattern => 'session/{session_id}/', p_priority => 1);
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => '{session_id}/', p_method => 'GET',
        p_source_type => 'plsql/block',
        p_source => ':out := p2p_api_pkg.get_session_json(:session_id);'
    );
    COMMIT;
END;
/

BEGIN
    ORDS.DEFINE_HANDLER(
        p_module_name => 'p2p_main', p_pattern => 'session/{session_id}/', p_method => 'PUT',
        p_source_type => 'plsql/block',
        p_source => '
BEGIN
    p2p_api_pkg.update_session_status(:session_id, :status, :out);
END;'
    );
    COMMIT;
END;
/

-- Enable schema
BEGIN
    ORDS.ENABLE_SCHEMA(p_enabled => TRUE, p_schema => 'P2P', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'p2p');
    COMMIT;
END;
/

-- Set default schema for ORDS
BEGIN
    ORDS.SET_DEFAULT_SCHEMA('P2P');
    COMMIT;
END;
/

-- ============================================================================
-- SUMMARY: ORDS ENDPOINTS
-- ============================================================================
-- GET    /ords/p2p/health/              - Health check
-- GET    /ords/p2p/users/{user_id}      - Get user info
-- POST   /ords/p2p/users/{user_id}      - Register/update user
-- POST   /ords/p2p/calls/               - Initiate call
-- GET    /ords/p2p/calls/{session_id}/  - Get session
-- PUT    /ords/p2p/calls/               - Update session status
-- POST   /ords/p2p/sdp/offers/          - Save offer
-- GET    /ords/p2p/sdp/offers/{id}/     - Get offer
-- POST   /ords/p2p/sdp/answers/         - Save answer
-- GET    /ords/p2p/sdp/answers/{id}/    - Get answer
-- POST   /ords/p2p/candidates/          - Add candidate
-- GET    /ords/p2p/candidates/{id}/     - Get candidates
-- GET    /ords/p2p/session/{id}/        - Get session JSON
-- PUT    /ords/p2p/session/{id}/        - Update session status
-- ============================================================================
