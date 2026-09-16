#pragma once

#include <string>

namespace rps::network {

namespace MsgType {
    inline const std::string WELCOME = "welcome";
    inline const std::string QUICK_MATCH = "quick_match";
    inline const std::string CREATE_ROOM = "create_room";
    inline const std::string JOIN_ROOM = "join_room";
    inline const std::string ROOM_CREATED = "room_created";
    inline const std::string ROOM_JOINED = "room_joined";
    inline const std::string WAITING_OPPONENT = "waiting_opponent";
    inline const std::string MATCH_START = "match_start";
    inline const std::string ROUND_START = "round_start";
    inline const std::string SUBMIT_MOVE = "submit_move";
    inline const std::string OPPONENT_CHOOSING = "opponent_choosing";
    inline const std::string OPPONENT_CHOSE = "opponent_chose";
    inline const std::string ROUND_RESULT = "round_result";
    inline const std::string MATCH_RESULT = "match_result";
    inline const std::string REMATCH_REQUEST = "rematch_request";
    inline const std::string REMATCH_VOTE = "rematch_vote";
    inline const std::string REMATCH_ACCEPTED = "rematch_accepted";
    inline const std::string LEAVE_MATCH = "leave_match";
    inline const std::string OPPONENT_LEFT = "opponent_left";
    inline const std::string OPPONENT_DISCONNECTED = "opponent_disconnected";
    inline const std::string OPPONENT_RECONNECTED = "opponent_reconnected";
    inline const std::string RECONNECT_SESSION = "reconnect_session";
    inline const std::string RECONNECT_SUCCESS = "reconnect_success";
    inline const std::string ERROR_MSG = "error_msg";
    inline const std::string PING = "ping";
    inline const std::string PONG = "pong";
}

} // namespace rps::network
