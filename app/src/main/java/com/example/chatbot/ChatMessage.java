package com.example.chatbot;

public class ChatMessage {
    private String sender; // "User", "Gemini", "ChatGPT"
    private String message;

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }
}
