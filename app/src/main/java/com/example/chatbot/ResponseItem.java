package com.example.chatbot;


public class ResponseItem {
    private String botName;
    private String responseText;

    public ResponseItem(String botName, String responseText) {
        this.botName = botName;
        this.responseText = responseText;
    }

    public String getBotName() {
        return botName;
    }

    public String getResponseText() {
        return responseText;
    }
}
