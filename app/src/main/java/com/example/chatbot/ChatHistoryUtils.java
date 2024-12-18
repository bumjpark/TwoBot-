package com.example.chatbot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryUtils {
    private static Gson gson = new Gson();

    public static String toJson(List<ChatMessage> chatHistory) {
        return gson.toJson(chatHistory);
    }

    public static List<ChatMessage> fromJson(String json) {
        Type listType = new TypeToken<ArrayList<ChatMessage>>(){}.getType();
        return gson.fromJson(json, listType);
    }
}
