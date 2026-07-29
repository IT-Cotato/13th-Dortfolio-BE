package com.itcotato.dortfolio.domain.record.dto.res;

import java.util.List;

public record RecordPageResponse(
	List<RecordSummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {

	public static RecordPageResponse of(
		List<RecordSummaryResponse> content,
		int page,
		int size,
		long totalElements
	) {
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		return new RecordPageResponse(
			content,
			page,
			size,
			totalElements,
			totalPages,
			page == 0,
			totalPages == 0 || page >= totalPages - 1
		);
	}
}
