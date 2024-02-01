package com.example.chatting.chat.handler;

import com.example.chatting.chat.dto.ChatDto;
import com.example.chatting.chatRoom.ChatRoom;
import com.example.chatting.common.websocket.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final Map<Long, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        String username = (String) session.getAttributes().get("username");
        WebSocketMessage webSocketMessage = (WebSocketMessage) objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
        switch (webSocketMessage.getType().getValue()) {
            case "ENTER" -> enterChatRoom(webSocketMessage.getPayload(), session);
            case "TALK" -> sendMessage(username, webSocketMessage.getPayload());
            case "EXIT" -> exitChatRoom(username, webSocketMessage.getPayload());
        }
    }

    /**
     * 메시지 전송
     * @param chatDto ChatDto
     */
    private void sendMessage(String username, ChatDto chatDto) {
        log.info("send chatDto : " + chatDto.toString());
        ChatRoom chatRoom = chatRoomMap.get(chatDto.getChatRoomId());
        chatRoom.sendMessage(username, chatDto);
    }

    /**
     * 채팅방 입장
     * @param chatDto ChatDto
     * @param session 웹소켓 세션
     */
    private void enterChatRoom(ChatDto chatDto, WebSocketSession session) {
        log.info("enter chatDto : " + chatDto.toString());
        chatDto.setMessage(chatDto.getUsername() + "님이 입장하셨습니다.");
        ChatRoom chatRoom = chatRoomMap.getOrDefault(chatDto.getChatRoomId(), new ChatRoom(objectMapper));
        chatRoom.enter(chatDto, session);
        chatRoomMap.put(chatDto.getChatRoomId(), chatRoom);
    }

    /**
     * 채팅방 퇴장
     * @param chatDto ChatDto
     */
    private void exitChatRoom(String username, ChatDto chatDto) {
        log.info("exit chatDto : " + chatDto.toString());
        chatDto.setMessage(chatDto.getUsername() + "님이 퇴장하셨습니다.");
        ChatRoom chatRoom = chatRoomMap.get(chatDto.getChatRoomId());
        chatRoom.exit(username, chatDto);

        if(chatRoom.getActiveUserMap().isEmpty()){
            chatRoomMap.remove(chatDto.getChatRoomId());
        }
    }
}
