package com.example.chatting.chatRoom;

import com.example.chatting.chat.dto.ChatDto;
import com.example.chatting.common.websocket.WebSocketMessage;
import com.example.chatting.common.websocket.WebSocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Getter
@RequiredArgsConstructor
public class ChatRoom {
    private final Map<String, WebSocketSession> ActiveUserMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 입장
     * @param chatDto ChatDto
     * @param session 웹소켓 세션
     */
    public void enter(ChatDto chatDto, WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        ActiveUserMap.put(username, session);
        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if (!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.ENTER, chatDto));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 채팅방 퇴장
     * @param chatDto ChatDto
     */
    public void exit(String username, ChatDto chatDto) {
        ActiveUserMap.remove(chatDto.getUsername());
        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if (!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.EXIT, chatDto));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 메시지 전송
     * @param chatDto ChatDto
     */
    public void sendMessage(String username, ChatDto chatDto) {
        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if (!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.TALK, chatDto));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 메시지 전송
     * @param type 메시지 타입
     * @param chatDto ChatDto
     * @return TextMessage
     */
    private TextMessage getTextMessage(WebSocketMessageType type, ChatDto chatDto) {
        try {
            return new TextMessage(
                    objectMapper.writeValueAsString(
                            new WebSocketMessage(type, chatDto)
                    ));
        }catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
