package com.meisterbear.domain.store.controller;

import com.meisterbear.domain.store.dto.response.StoreResponse;
import com.meisterbear.domain.store.service.StoreService;
import com.meisterbear.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Store", description = "매장 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    @Operation(
            summary = "구매 가능 매장 목록 조회",
            description = "제품 구매가 가능한 매장 전체 목록을 반환한다. "
                    + "매장 id는 매장 태그 이력/상세 API의 storeId와 동일한 값을 쓴다. "
                    + "hours는 요일별 운영시간 7개 항목이며, 미등록 매장은 빈 배열로 내려간다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": [
                                        {
                                          "id": 1,
                                          "name": "MCM 롯데백화점 본점",
                                          "address": "서울 중구 남대문로 81, 롯데백화점 본점 1F",
                                          "postalCode": "04533",
                                          "phone": "+82-2-772-3198",
                                          "hours": [
                                            { "day": "월요일", "time": "10:30 - 20:00" },
                                            { "day": "화요일", "time": "10:30 - 20:00" }
                                          ]
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping
    public BaseResponse<List<StoreResponse>> getStores() {
        return BaseResponse.success(storeService.getStores());
    }
}
