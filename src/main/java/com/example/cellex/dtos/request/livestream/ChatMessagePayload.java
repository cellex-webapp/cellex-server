package com.example.cellex.dtos.request.livestream;

import lombok.Data;

@Data
public class ChatMessagePayload {
    private String userName;
    private String content;
}