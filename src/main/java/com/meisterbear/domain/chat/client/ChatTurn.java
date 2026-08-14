package com.meisterbear.domain.chat.client;

// OpenAI Chat Completions role 표기("user"/"assistant") 그대로 사용
public record ChatTurn(String role, String content) {

    public static ChatTurn user(String content) {
        return new ChatTurn("user", content);
    }

    public static ChatTurn assistant(String content) {
        return new ChatTurn("assistant", content);
    }
}
