package com.itcotato.dortfolio.domain.record.dto.req;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordMemoRequest(
	@NotNull(message = "메모 ID는 필수입니다.")
	UUID memoId,

	boolean collapsed
) {
}
