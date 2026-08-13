package com.meisterbear.domain.story.controller;

import com.meisterbear.domain.story.dto.request.SelectStoryChoiceRequest;
import com.meisterbear.domain.story.dto.response.StoryChoiceResultResponse;
import com.meisterbear.domain.story.dto.response.StoryCompleteResultResponse;
import com.meisterbear.domain.story.dto.response.StoryDetailResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story", description = "시즌 스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
public class StoryController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 SecurityConfig의 permitAll("/api/stories/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final StoryService storyService;

    @Operation(
            summary = "스토리 챕터 조회",
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
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "currentSeason": {
                                          "season": "AW2026",
                                          "characterName": "비세토스 라이언",
                                          "characterImgUrl": "https://cdn.meisterbear.com/character/1.png",
                                          "stories": [
                                            {
                                              "id": 1,
                                              "title": "Introduction",
                                              "unlockOrder": 1,
                                              "isLocked": false,
                                              "isDone": true,
                                              "readAt": "2026-08-10T11:00:00",
                                              "teaser": null,
                                              "thumbnailUrl": "https://cdn.meisterbear.com/story/1-thumb.png"
                                            },
                                            {
                                              "id": 2,
                                              "title": "Collection History",
                                              "unlockOrder": 2,
                                              "isLocked": false,
                                              "isDone": true,
                                              "readAt": "2026-08-11T09:30:00",
                                              "teaser": null,
                                              "thumbnailUrl": "https://cdn.meisterbear.com/story/2-thumb.png"
                                            },
                                            {
                                              "id": 3,
                                              "title": "Craftmanship",
                                              "unlockOrder": 3,
                                              "isLocked": false,
                                              "isDone": false,
                                              "readAt": null,
                                              "teaser": "제품을 만든 장인과 공방 속의 이야기를 들여다봅니다.",
                                              "thumbnailUrl": "https://cdn.meisterbear.com/story/3-thumb.png"
                                            }
                                          ],
                                          "isAllCompleted": false
                                        },
                                        "pastSeasons": [
                                          { "season": "SS2026", "thumbnailUrl": "https://cdn.meisterbear.com/story/ss2026-thumb.png" }
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
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "특정 챕터의 전체 내용 조회",
            description = "특정 챕터의 장면 목록과 마지막 질문을 반환한다. 잠긴 챕터를 조회하면 403을 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "챕터 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "id": 3,
                                        "title": "Craftmanship",
                                        "unlockOrder": 3,
                                        "isDone": false,
                                        "readAt": null,
                                        "scenes": [
                                          { "order": 1, "imgUrl": "https://cdn.meisterbear.com/story/3-1.png", "content": "스터드 디테일의 실루엣과 혁신적인 가죽 제품은 예술과 기술, 여행이 교차하는 하우스의 정체성을 드러내요." }
                                        ],
                                        "question": {
                                          "question": "무엇이 궁금하신가요?",
                                          "choices": [
                                            { "id": 1, "label": "이번 시즌의 제품에는 어떤 것이 있어?" },
                                            { "id": 2, "label": "이번 시즌 컬렉션의 주목해야할 부분은?" },
                                            { "id": 3, "label": "50주년 프로젝트에 대해 알고 싶어." }
                                          ]
                                        },
                                        "isSeasonCompleted": false,
                                        "characterName": "비세토스 라이언",
                                        "characterImgUrl": "https://cdn.meisterbear.com/character/1.png"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "잠긴 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "잠긴 챕터", value = """
                                    {
                                      "success": false,
                                      "code": "STORY403",
                                      "message": "잠긴 챕터입니다.",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "챕터 없음", value = """
                                    {
                                      "success": false,
                                      "code": "STORY404",
                                      "message": "해당 스토리를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/{storyId}")
    public BaseResponse<StoryDetailResponse> findStoryDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storyId) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        StoryDetailResponse response = storyService.findStoryDetail(userId, storyId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "스토리 선택지 선택",
            description = "질문의 선택지 중 하나를 선택한다. 선택에 따라 스토리 내용이 갈라지지는 않고, "
                    + "선택한 choice의 tagName만 응답으로 돌려준다(선택 자체는 DB에 저장되지 않음). "
                    + "챕터 완료 처리는 하지 않으며, 완료는 POST /{storyId}/complete에서 별도로 처리한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "선택 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "storyId": 3,
                                        "tagName": "classic"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 선택지",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "잘못된 선택지", value = """
                                    {
                                      "success": false,
                                      "code": "STORY400",
                                      "message": "존재하지 않는 선택지입니다.",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "잠긴 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "잠긴 챕터", value = """
                                    {
                                      "success": false,
                                      "code": "STORY403",
                                      "message": "잠긴 챕터입니다.",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "챕터 없음", value = """
                                    {
                                      "success": false,
                                      "code": "STORY404",
                                      "message": "해당 스토리를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/{storyId}/choice")
    public BaseResponse<StoryChoiceResultResponse> selectChoice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storyId,
            @RequestBody SelectStoryChoiceRequest request) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        StoryChoiceResultResponse response = storyService.selectChoice(userId, storyId, request.getChoiceId());
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "스토리 챕터 완료",
            description = "장면과 질문을 끝까지 다 본 뒤 완료 버튼을 눌렀을 때 호출한다. "
                    + "이 챕터가 속한 시즌의 마지막 챕터였다면 isSeasonCompleted=true와 캐릭터 정보를 함께 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "완료 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "storyId": 3,
                                        "isDone": true,
                                        "readAt": "2026-08-13T13:20:00",
                                        "isSeasonCompleted": true,
                                        "characterName": "비세토스 라이언",
                                        "characterImgUrl": "https://cdn.meisterbear.com/character/1.png"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "잠긴 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "잠긴 챕터", value = """
                                    {
                                      "success": false,
                                      "code": "STORY403",
                                      "message": "잠긴 챕터입니다.",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 챕터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "챕터 없음", value = """
                                    {
                                      "success": false,
                                      "code": "STORY404",
                                      "message": "해당 스토리를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/{storyId}/complete")
    public BaseResponse<StoryCompleteResultResponse> completeStory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storyId) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        StoryCompleteResultResponse response = storyService.completeStory(userId, storyId);
        return BaseResponse.success(response);
    }
}
