package com.fanci.Hyperion_be.service.serviceImpl;

import com.fanci.Hyperion_be.dto.request.ChatRequest;
import com.fanci.Hyperion_be.exception.AppException;
import com.fanci.Hyperion_be.exception.ErrorCode;
import com.fanci.Hyperion_be.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {
    @Value("${gpt.apiKey}")
    private String apiKey;

    @Override
    public String sendMessage(ChatRequest chatRequest){
        String userMessage = chatRequest.getMessage();
        String botResponse = getChatbotResponse(userMessage);
       try {
           ObjectMapper objectMapper = new ObjectMapper();
           JsonNode root = objectMapper.readTree(botResponse);
           return  root.path("choices").get(0).path("message").path("content").asText();

       } catch (JsonProcessingException e) {
           throw new AppException(ErrorCode.JSON_PARSE_ERROR);
       }
    }


    private String getChatbotResponse(String message) {
        String endpoint = "https://api.openai.com/v1/chat/completions";
        WebClient webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        try {
            // Gửi yêu cầu POST và xử lý kết quả
            return webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Chuyển từ bất đồng bộ sang đồng bộ
        } catch (WebClientResponseException ex) {
            // Xử lý các lỗi HTTP
            System.err.println("Error response from server: " + ex.getResponseBodyAsString());
            throw new RuntimeException("Failed to get response from OpenAI: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Xử lý các lỗi khác
            throw new RuntimeException("An unexpected error occurred: " + ex.getMessage(), ex);
        }
    }
}
