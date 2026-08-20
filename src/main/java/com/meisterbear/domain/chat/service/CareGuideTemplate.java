package com.meisterbear.domain.chat.service;

import java.util.Map;

// 인스펙터(사진 분석) 실패 시 대신 내려주는 소재 기준 고정 케어 가이드
public class CareGuideTemplate {

    private CareGuideTemplate() {
    }

    private static final String DEFAULT_GUIDE =
            "표면의 먼지는 마른 부드러운 천으로 가볍게 닦아주시고, 직사광선과 습기가 많은 곳은 피해서 보관해주세요.";

    private static final Map<String, String> GUIDES_BY_MATERIAL = Map.of(
            "Leather", "가죽 소재예요. 직사광선과 습기를 피해 보관하고, 먼지는 마른 부드러운 천으로 가볍게 닦아주세요."
    );

    public static String guideFor(String material) {
        if (material == null) {
            return DEFAULT_GUIDE;
        }
        return GUIDES_BY_MATERIAL.getOrDefault(material, DEFAULT_GUIDE);
    }
}
