package com.itcotato.dortfolio.domain.search.controller.docs;

import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchPageResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "검색 (Search)", description = "기록 검색 API입니다.")
public interface SearchControllerDocs {

    @Operation(summary = "기록 검색",
            description = "기록 제목과 기록 내용에 키워드가 포함된 기록을 최신순으로 검색합니다. "
                    + "활동 종류/템플릿 제목 등 태그는 검색 대상이 아닙니다. "
                    + "자음·모음만 입력하거나 일치하는 기록이 없으면 결과가 0건으로 반환됩니다. "
                    + "검색 결과 개수는 totalElements이며, content는 키워드가 포함된 답변을 우선해 보여줍니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공 (결과 없음 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "S001: 올바르지 않은 페이지 요청")
    })
    ResponseEntity<ApiResponse<RecordSearchPageResponse>> searchRecords(
            UUID userId, String keyword, int page, Integer size);
}
