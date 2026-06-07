-- SQL скрипты для Oracle APEX/ORDS
-- Создание таблиц для P2P звонков

-- 1. Таблица пользователей
CREATE TABLE app_users (
    user_id VARCHAR2(50) PRIMARY KEY,
    fcm_token VARCHAR2(1000),
    name VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы
CREATE INDEX idx_app_users_fcm ON app_users(fcm_token);

-- 2. Таблица сессий звонков
CREATE TABLE call_sessions (
    session_id VARCHAR2(50) PRIMARY KEY,
    caller_id VARCHAR2(50) NOT NULL,
    callee_id VARCHAR2(50) NOT NULL,
    status VARCHAR2(20) DEFAULT 'pending' CHECK (status IN ('pending', 'ringing', 'active', 'ended', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Внешние ключи
ALTER TABLE call_sessions 
ADD CONSTRAINT fk_caller FOREIGN KEY (caller_id) REFERENCES app_users(user_id);

ALTER TABLE call_sessions 
ADD CONSTRAINT fk_callee FOREIGN KEY (callee_id) REFERENCES app_users(user_id);

-- Индексы
CREATE INDEX idx_call_sessions_status ON call_sessions(status);
CREATE INDEX idx_call_sessions_caller ON call_sessions(caller_id);
CREATE INDEX idx_call_sessions_callee ON call_sessions(callee_id);

-- 3. Таблица ICE кандидатов
CREATE TABLE ice_candidates (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id VARCHAR2(50) NOT NULL,
    candidate CLOB,
    sdp_mid VARCHAR2(50),
    sdp_manced NUMBER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Внешний ключ
ALTER TABLE ice_candidates
ADD CONSTRAINT fk_ice_session FOREIGN KEY (session_id) REFERENCES call_sessions(session_id) ON DELETE CASCADE;

-- Индекс
CREATE INDEX idx_ice_session ON ice_candidates(session_id);

-- 4. Процедура регистрации токена
CREATE OR REPLACE PROCEDURE register_token(
    p_user_id IN VARCHAR2,
    p_fcm_token IN VARCHAR2,
    p_name IN VARCHAR2
) AS
BEGIN
    MERGE INTO app_users u
    USING (SELECT p_user_id as user_id FROM dual) v
    ON (u.user_id = v.user_id)
    WHEN MATCHED THEN
        UPDATE SET 
            fcm_token = p_fcm_token,
            name = p_name,
            updated_at = CURRENT_TIMESTAMP
    WHEN NOT MATCHED THEN
        INSERT (user_id, fcm_token, name)
        VALUES (p_user_id, p_fcm_token, p_name);
    COMMIT;
END;
/

-- 5. Процедура инициации звонка
CREATE OR REPLACE PROCEDURE initiate_call(
    p_caller_id IN VARCHAR2,
    p_callee_id IN VARCHAR2,
    p_session_id OUT VARCHAR2
) AS
    v_session_id VARCHAR2(50);
BEGIN
    v_session_id := SYS_GUID();
    
    INSERT INTO call_sessions (session_id, caller_id, callee_id, status)
    VALUES (v_session_id, p_caller_id, p_callee_id, 'pending');
    
    p_session_id := v_session_id;
    COMMIT;
END;
/

-- 6. Процедура обновления статуса
CREATE OR REPLACE PROCEDURE update_call_status(
    p_session_id IN VARCHAR2,
    p_status IN VARCHAR2
) AS
BEGIN
    UPDATE call_sessions 
    SET status = p_status,
        updated_at = CURRENT_TIMESTAMP
    WHERE session_id = p_session_id;
    
    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Session not found');
    END IF;
    
    COMMIT;
END;
/

-- 7. Процедура добавления SDP
CREATE OR REPLACE PROCEDURE save_sdp(
    p_session_id IN VARCHAR2,
    p_sdp_type IN VARCHAR2,
    p_sdp_content IN CLOB
) AS
BEGIN
    -- Обновляем существующую сессию
    UPDATE call_sessions 
    SET updated_at = CURRENT_TIMESTAMP
    WHERE session_id = p_session_id;
    
    -- Здесь нужно добавить логику сохранения SDP
    -- В будущем: в отдельную таблицу или как BLOB
    COMMIT;
END;
/
