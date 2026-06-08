В примерах используются переменные среды окружения (замените http://localhost:8080 на ваш реальный хост).

0. Проверка доступности сервиса (Health Check)
   Перед началом тестирования бизнес-логики убедимся, что API работает.

Bash
curl -X GET https://oracleapex.com/ords/holayakay/p2p_main/health/ -H "Accept: application/json"
1. Получение данных пользователей
   Перед звонком проверим, существуют ли пользователи (вызывающий и вызываемый). Здесь {user_id} передается прямо в URL (например, 123).

Bash
curl -X GET "https://oracleapex.com/ords/holayakay/p2p_main/users/123" \
-H "Accept: application/json"
2. Инициация звонка (Создание сессии)
   Первый шаг бизнес-логики. Вызывающий пользователь (caller_id) инициирует звонок вызываемому (callee_id). Сервис должен сгенерировать и вернуть session_id.

Bash
curl -X POST "https://oracleapex.com/ords/holayakay/p2p_main/calls/" \
-H "Content-Type: application/json" \
-d '{
"caller_id": "user_abc",
"callee_id": "user_xyz"
}'
Предположим, в ответе нам вернулся session_id: "sess_999". Используем его в следующих запросах.

3. Передача и получение SDP-оффера (Offer)
   Вызывающая сторона отправляет свой SDP-оффер, а вызываемая сторона (или клиент-состояние) его забирает.

Отправка Offer (POST)
Bash
curl -X POST "https://oracleapex.com/ords/holayakay/p2p_main/sdp/offers/" \
-H "Content-Type: application/json" \
-d '{
"session_id": "sess_999",
"sdp": "v=0\no=- 453625164... (длинная строка SDP)"
}'
Получение Offer (GET)
Вызываемый абонент проверяет, пришел ли ему оффер для этой сессии:

Bash
curl -X GET "https://oracleapex.com/ords/holayakay/p2p_main/sdp/offers/sess_999/" \
-H "Accept: application/json"
4. Передача и получение SDP-ответа (Answer)
   Вызываемый абонент генерирует ответный SDP (Answer) и отправляет его на сервер. Вызывающий абонент его скачивает.

Отправка Answer (POST)
Bash
curl -X POST "https://oracleapex.com/ords/holayakay/p2p_main/sdp/answers/" \
-H "Content-Type: application/json" \
-d '{
"session_id": "sess_999",
"sdp": "v=0\no=- 987654321... (ответный SDP)"
}'
Получение Answer (GET)
Вызывающая сторона забирает ответ:

Bash
curl -X GET "https://oracleapex.com/ords/holayakay/p2p_main/sdp/answers/sess_999/" \
-H "Accept: application/json"
5. Обмен ICE-кандидатами (Candidates)
   В процессе установки соединения обе стороны могут слать сетевые кандидаты.

Добавление кандидата (POST)
Bash
curl -X POST "https://oracleapex.com/ords/holayakay/p2p_main/candidates/" \
-H "Content-Type: application/json" \
-d '{
"session_id": "sess_999",
"candidate": "candidate:842163049 1 udp 1677721503 192.168.1.50 56214 typ srflx raddr...",
"sdp_mid": "0",
"sdp_manced": 0
}'
Получение списка кандидатов (GET)
Bash
curl -X GET "https://oracleapex.com/ords/holayakay/p2p_main/candidates/sess_999/" \
-H "Accept: application/json"
6. Мониторинг и управление статусом сессии
   В процессе звонка или при его завершении мы проверяем состояние или обновляем его (например, на CONNECTED, DISCONNECTED, REJECTED). В вашем скрипте под это выделено два идентичных по логике эндпоинта (calls/ и session/).

Получение информации о сессии (GET)
Bash
curl -X GET "https://oracleapex.com/ords/holayakay/p2p_main/session/sess_999/" \
-H "Accept: application/json"
(Альтернативный URL из вашего скрипта: https://oracleapex.com/ords/holayakay/p2p_main/calls/sess_999/)

Обновление статуса сессии (PUT)
Изменение статуса звонка (например, завершение звонка):

Bash
curl -X PUT "https://oracleapex.com/ords/holayakay/p2p_main/session/sess_999/" \
-H "Content-Type: application/json" \
-d '{
"status": "TERMINATED"
}'
(Альтернативный URL: https://oracleapex.com/ords/holayakay/p2p_main/calls/sess_999/)