package com.fanci.Hyperion_be.controller;

import com.fanci.Hyperion_be.dto.request.ChatRequest;
import com.fanci.Hyperion_be.dto.response.ApiResponse;
import com.fanci.Hyperion_be.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ApiResponse<String> sendMessage(@RequestBody ChatRequest request){
        return ApiResponse.<String>builder()
                .result(chatService.sendMessage(request))
                .build();
    }

}
