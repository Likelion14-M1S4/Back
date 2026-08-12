package com.meisterbear.domain.story.controller;

import com.meisterbear.domain.story.dto.response.StoryListResponse;
import com.meisterbear.domain.story.service.StoryService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story", description = "시즌 스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
public class StoryController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 permitAll("/api/stories/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final StoryService storyService;

    @Operation(
            summary = "스토리 탭 조회",
            description = "챕터 목록과 유저의 진행 상태를 unlock_order 순으로 반환한다. "
                    + "currentSeason은 유저가 가장 최근 등록한 제품의 캐릭터를 기준으로 판단하며, "
                    + "등록된 제품이 없으면 currentSeason은 null, pastSeasons는 빈 배열로 응답한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스토리 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "스토리 목록 조회 성공",
                                      "data": {
                                        "currentSeason": {
                                          "season": "AW2026",
                                          "stories": [
                                            {
                                              "id": 1,
                                              "title": "챕터 1 - 루나의 시작",
                                              "unlockOrder": 1,
                                              "isLocked": false,
                                              "isDone": true,
                                              "readAt": "2026-08-10T11:00:00"
                                            }
                                          ]
                                        },
                                        "pastSeasons": [
                                          {
                                            "season": "SS2026",
                                            "thumbnailUrl": "https://cdn.meisterbear.com/story/ss2026-thumb.png"
                                          }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "인증 실패", value = """
                                    {
                                      "success": false,
                                      "code": "G006",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping
    public BaseResponse<StoryListResponse> findStories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        StoryListResponse response = storyService.findStories(userId);
        return BaseResponse.success("스토리 목록 조회 성공", response);
    }
}
