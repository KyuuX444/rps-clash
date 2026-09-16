package com.kyuu.rpsclash.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

interface OnlineGameListener {
    fun onConnected(sessionId: String) {}
    fun onRoomCreated(roomCode: String) {}
    fun onRoomJoined(roomCode: String, opponentName: String) {}
    fun onWaitingOpponent() {}
    fun onMatchStart(opponentName: String, bestOf: Int) {}
    fun onRoundStart(roundNumber: Int, timeoutSeconds: Int) {}
    fun onOpponentChoosing() {}
    fun onOpponentChose() {}
    fun onRoundResult(round: Int, playerMove: Int, opponentMove: Int, result: Int, myScore: Int, oppScore: Int) {}
    fun onMatchResult(won: Boolean, finalMyScore: Int, finalOppScore: Int) {}
    fun onRematchRequested() {}
    fun onRematchAccepted() {}
    fun onOpponentDisconnected(gracePeriodSeconds: Int) {}
    fun onOpponentReconnected() {}
    fun onOpponentLeft() {}
    fun onError(errorMessage: String) {}
    fun onConnectionStateChanged(state: ConnectionState) {}
}

class WebSocketManager {

    private var client: WebSocketClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var heartbeatTask: ScheduledFuture<*>? = null

    private var currentServerUrl: String = ""
    private var isManualDisconnect = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    var sessionId: String? = null
        private set
    var currentRoomCode: String? = null
        private set
    var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    var listener: OnlineGameListener? = null

    fun connect(serverUrl: String) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) return

        currentServerUrl = serverUrl
        isManualDisconnect = false
        setState(if (reconnectAttempts > 0) ConnectionState.RECONNECTING else ConnectionState.CONNECTING)

        try {
            val uri = URI(serverUrl)
            client = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    reconnectAttempts = 0
                    setState(ConnectionState.CONNECTED)
                    startHeartbeat()

                    // If we have an existing session, attempt to reconnect/resume
                    sessionId?.let { sess ->
                        currentRoomCode?.let { room ->
                            sendReconnect(sess, room)
                        }
                    }
                }

                override fun onMessage(message: String?) {
                    message?.let { handleMessage(it) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    stopHeartbeat()
                    if (!isManualDisconnect) {
                        scheduleReconnect()
                    } else {
                        setState(ConnectionState.DISCONNECTED)
                    }
                }

                override fun onError(ex: Exception?) {
                    Log.e("RPSClashWS", "WebSocket error: ${ex?.message}")
                    notifyListener { it.onError(ex?.message ?: "WebSocket error") }
                }
            }
            client?.connect()
        } catch (e: Exception) {
            Log.e("RPSClashWS", "Connect failed: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            setState(ConnectionState.RECONNECTING)
            val delaySec = (1L shl (reconnectAttempts - 1)).coerceAtMost(16L)
            scheduler.schedule({
                connect(currentServerUrl)
            }, delaySec, TimeUnit.SECONDS)
        } else {
            setState(ConnectionState.DISCONNECTED)
            notifyListener { it.onError("Gagal menghubungkan ke server setelah beberapa percobaan.") }
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        stopHeartbeat()
        reconnectAttempts = 0
        try {
            client?.close()
        } catch (_: Exception) {}
        client = null
        setState(ConnectionState.DISCONNECTED)
    }

    private fun setState(newState: ConnectionState) {
        state = newState
        notifyListener { it.onConnectionStateChanged(newState) }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatTask = scheduler.scheduleAtFixedRate({
            sendPing()
        }, 15, 15, TimeUnit.SECONDS)
    }

    private fun stopHeartbeat() {
        heartbeatTask?.cancel(true)
        heartbeatTask = null
    }

    // --- Outgoing Messages ---

    fun sendQuickMatch() {
        val payload = JsonObject().apply {
            addProperty("type", "quick_match")
            addProperty("sessionId", sessionId)
        }
        sendJson(payload)
    }

    fun sendCreateRoom(bestOf: Int = 3) {
        val payload = JsonObject().apply {
            addProperty("type", "create_room")
            addProperty("bestOf", bestOf)
            addProperty("sessionId", sessionId)
        }
        sendJson(payload)
    }

    fun sendJoinRoom(roomCode: String) {
        val payload = JsonObject().apply {
            addProperty("type", "join_room")
            addProperty("roomCode", roomCode.trim().uppercase())
            addProperty("sessionId", sessionId)
        }
        sendJson(payload)
    }

    fun sendSubmitMove(move: Int) {
        val payload = JsonObject().apply {
            addProperty("type", "submit_move")
            addProperty("roomCode", currentRoomCode)
            addProperty("sessionId", sessionId)
            addProperty("move", move)
        }
        sendJson(payload)
    }

    fun sendRematchVote() {
        val payload = JsonObject().apply {
            addProperty("type", "rematch_vote")
            addProperty("roomCode", currentRoomCode)
            addProperty("sessionId", sessionId)
        }
        sendJson(payload)
    }

    fun sendLeaveMatch() {
        val payload = JsonObject().apply {
            addProperty("type", "leave_match")
            addProperty("roomCode", currentRoomCode)
            addProperty("sessionId", sessionId)
        }
        sendJson(payload)
        currentRoomCode = null
    }

    private fun sendReconnect(sessId: String, roomCode: String) {
        val payload = JsonObject().apply {
            addProperty("type", "reconnect_session")
            addProperty("sessionId", sessId)
            addProperty("roomCode", roomCode)
        }
        sendJson(payload)
    }

    private fun sendPing() {
        val payload = JsonObject().apply {
            addProperty("type", "ping")
        }
        sendJson(payload)
    }

    private fun sendJson(json: JsonObject) {
        try {
            if (client?.isOpen == true) {
                client?.send(json.toString())
            }
        } catch (e: Exception) {
            Log.e("RPSClashWS", "Failed to send message: ${e.message}")
        }
    }

    // --- Message Dispatcher ---

    private fun handleMessage(jsonString: String) {
        try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val type = json.get("type")?.asString ?: return

            when (type) {
                "welcome" -> {
                    sessionId = json.get("sessionId")?.asString
                    notifyListener { it.onConnected(sessionId ?: "") }
                }
                "room_created" -> {
                    currentRoomCode = json.get("roomCode")?.asString
                    notifyListener {
                        it.onRoomCreated(currentRoomCode ?: "")
                        it.onWaitingOpponent()
                    }
                }
                "room_joined" -> {
                    currentRoomCode = json.get("roomCode")?.asString
                    val oppName = json.get("opponentName")?.asString ?: "Lawan"
                    notifyListener { it.onRoomJoined(currentRoomCode ?: "", oppName) }
                }
                "waiting_opponent" -> {
                    notifyListener { it.onWaitingOpponent() }
                }
                "match_start" -> {
                    val oppName = json.get("opponentName")?.asString ?: "Lawan"
                    val bestOf = json.get("bestOf")?.asInt ?: 3
                    notifyListener { it.onMatchStart(oppName, bestOf) }
                }
                "round_start" -> {
                    val round = json.get("round")?.asInt ?: 1
                    val timeout = json.get("timeout")?.asInt ?: 5
                    notifyListener { it.onRoundStart(round, timeout) }
                }
                "opponent_choosing" -> {
                    notifyListener { it.onOpponentChoosing() }
                }
                "opponent_chose" -> {
                    notifyListener { it.onOpponentChose() }
                }
                "round_result" -> {
                    val round = json.get("round")?.asInt ?: 1
                    val pMove = json.get("yourMove")?.asInt ?: -1
                    val oMove = json.get("oppMove")?.asInt ?: -1
                    val res = json.get("result")?.asInt ?: 2
                    val myScore = json.get("yourScore")?.asInt ?: 0
                    val oppScore = json.get("oppScore")?.asInt ?: 0
                    notifyListener { it.onRoundResult(round, pMove, oMove, res, myScore, oppScore) }
                }
                "match_result" -> {
                    val won = json.get("won")?.asBoolean ?: false
                    val myScore = json.get("yourScore")?.asInt ?: 0
                    val oppScore = json.get("oppScore")?.asInt ?: 0
                    notifyListener { it.onMatchResult(won, myScore, oppScore) }
                }
                "rematch_request" -> {
                    notifyListener { it.onRematchRequested() }
                }
                "rematch_accepted" -> {
                    notifyListener { it.onRematchAccepted() }
                }
                "opponent_disconnected" -> {
                    val gracePeriod = json.get("gracePeriod")?.asInt ?: 30
                    notifyListener { it.onOpponentDisconnected(gracePeriod) }
                }
                "opponent_reconnected" -> {
                    notifyListener { it.onOpponentReconnected() }
                }
                "opponent_left" -> {
                    notifyListener { it.onOpponentLeft() }
                }
                "error_msg" -> {
                    val msg = json.get("message")?.asString ?: "Error tidak diketahui"
                    notifyListener { it.onError(msg) }
                }
                "pong" -> {
                    // Heartbeat acknowledged
                }
            }
        } catch (e: Exception) {
            Log.e("RPSClashWS", "Error parsing message: ${e.message}")
        }
    }

    private fun notifyListener(block: (OnlineGameListener) -> Unit) {
        mainHandler.post {
            listener?.let { block(it) }
        }
    }
}
