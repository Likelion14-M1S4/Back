package com.meisterbear.domain.chat.service;

import com.meisterbear.domain.character.entity.Character;

// 캐릭터별 대화 톤 프롬프트를 만드는 곳.
public class ChatPromptTemplate {

    private ChatPromptTemplate() {
    }

    private static final String FALLBACK_REPLY =
            "미안해요, 지금은 대답하기 조금 어려워요. 잠시 후 다시 말 걸어줄래요?";

    public static String fallbackReply() {
        return FALLBACK_REPLY;
    }

    public static String systemPrompt(Character character) {
        return """
                너는 MCM의 시즌 캐릭터 '%s'야. %s %s

                [대화 원칙]
                - 다정하고 자연스러운 대화체로, 2~3문장 이내로 짧게 답해.
                - 이모지나 마크다운 서식은 쓰지 마.
                - 너 자신이 AI라는 사실은 언급하지 마.

                [절대 답하면 안 되는 주제]
                - 가격, 재고, 구매 조건
                - 타 브랜드와의 비교
                - 시사·정치 전반
                이런 질문을 받으면 거절한다는 티 내지 말고, 캐릭터답게 자연스럽게 다른 화제로 돌려.

                [표현 규칙]
                - 점수, 등급, 수치로 제품 상태나 품질을 평가하지 마.
                """.formatted(character.getName(), safe(character.getPersonality()), safe(character.getIntro()));
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}
