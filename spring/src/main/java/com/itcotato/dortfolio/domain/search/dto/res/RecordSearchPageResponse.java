package com.itcotato.dortfolio.domain.search.dto.res;

import java.util.List;

// record 도메인의 RecordPageResponse와 동일한 페이징 형식을 따른다.
// totalElements가 기능명세서 5.3.1.1의 "검색 결과 개수"에 해당한다.
public record RecordSearchPageResponse(
		List<RecordSearchResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {

	public static RecordSearchPageResponse of(
			List<RecordSearchResponse> content,
			int page,
			int size,
			long totalElements
	) {
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		return new RecordSearchPageResponse(
				content,
				page,
				size,
				totalElements,
				totalPages,
				page == 0,
				totalPages == 0 || page >= totalPages - 1
		);
	}

	public static RecordSearchPageResponse empty(int page, int size) {
		return of(List.of(), page, size, 0);
	}
}
