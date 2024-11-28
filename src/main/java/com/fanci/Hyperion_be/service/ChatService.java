package com.fanci.Hyperion_be.service;

import com.fanci.Hyperion_be.dto.request.ChatRequest;

public interface ChatService {
    String sendMessage(ChatRequest chatRequest);
}
