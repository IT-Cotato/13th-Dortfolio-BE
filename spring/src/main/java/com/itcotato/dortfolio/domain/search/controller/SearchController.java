package com.itcotato.dortfolio.domain.search.controller;

import com.itcotato.dortfolio.domain.search.controller.docs.SearchControllerDocs;
import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchPageResponse;
import com.itcotato.dortfolio.domain.search.service.SearchService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController implements SearchControllerDocs {

    private final SearchService searchService;

    /* 기록 검색 API (기능명세서 5.3) */
    @Override
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<RecordSearchPageResponse>> searchRecords(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "기록을 검색했습니다.",
                searchService.searchRecords(userId, keyword, page, size)
        ));
    }
}
