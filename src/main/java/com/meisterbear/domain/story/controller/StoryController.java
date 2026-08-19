package com.meisterbear.domain.story.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story", description = "시즌 스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;

    @Operation(
            summary = "스토리 챕터 목록 조회",
            description = "유저가 가장 최근 등록한 제품의 캐릭터에 연결된 챕터 목록을 unlock_order 순으로 반환한다 "
                    + "(시즌은 하나만 운영되므로 별도 시즌 판별 없이 그 캐릭터의 스토리를 그대로 반환). "
                    + "챕터별 해금 여부(isLocked)는 직전 챕터를 이 유저가 완주했는지로 판단한다(1번 챕터는 항상 해금). "
                    + "등록된 제품이 없거나 그 캐릭터에 스토리가 없으면 season은 null, stories는 빈 배열로 응답한다.")
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
                                        "season": "AW2026",
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
        Long userId = userDetails.getUser().getId();
        StoryListResponse response = storyService.findStories(userId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "특정 챕터의 전체 내용 조회",
            description = "특정 챕터의 장면 목록을 순서대로 반환한다. 잠긴 챕터를 조회하면 403을 반환한다.")
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
                                        "isSeasonCompleted": false
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
        Long userId = userDetails.getUser().getId();
        StoryDetailResponse response = storyService.findStoryDetail(userId, storyId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "스토리 챕터 완료",
            description = "그 챕터의 마지막 장면(scene)까지 다 본 뒤 완료 버튼을 눌렀을 때 호출한다. "
                    + "이 챕터가 속한 시즌의 마지막 챕터였다면 isSeasonCompleted=true와 시즌 보상 참 목록(charms)을 함께 반환한다. "
                    + "charms는 화면 노출용 정보이며 실제 참 수령(보유) 처리는 하지 않는다.")
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
                                        "charms": [
                                          { "id": 1, "name": "라이언 참", "imgUrl": "https://cdn.meisterbear.com/charm/1.png", "collectionName": "라이언 컬렉션" },
                                          { "id": 2, "name": "라이언 참2", "imgUrl": "https://cdn.meisterbear.com/charm/2.png", "collectionName": "라이언 컬렉션" },
                                          { "id": 3, "name": "라이언 참3", "imgUrl": "https://cdn.meisterbear.com/charm/3.png", "collectionName": "라이언 컬렉션" },
                                          { "id": 4, "name": "라이언 참4", "imgUrl": "https://cdn.meisterbear.com/charm/4.png", "collectionName": "라이언 컬렉션" },
                                          { "id": 5, "name": "라이언 참5", "imgUrl": "https://cdn.meisterbear.com/charm/5.png", "collectionName": "라이언 컬렉션" }
                                        ]
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
        Long userId = userDetails.getUser().getId();
        StoryCompleteResultResponse response = storyService.completeStory(userId, storyId);
        return BaseResponse.success(response);
    }
}
