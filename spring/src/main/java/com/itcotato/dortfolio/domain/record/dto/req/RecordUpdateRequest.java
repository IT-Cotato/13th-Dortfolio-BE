package com.itcotato.dortfolio.domain.record.dto.req;

import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecordUpdateRequest(
	@NotBlank(message = "기록 제목은 필수입니다.")
	@Size(max = 100, message = "기록 제목은 100자 이하로 입력해주세요.")
	String title,

	@Valid
	List<RecordAnswerRequest> answers,

	@Valid
	List<RecordMemoRequest> memos,

	RecordStatus status
) {
	public List<RecordAnswerRequest> answersOrEmpty() {
		return answers == null ? List.of() : answers;
	}

	public List<RecordMemoRequest> memosOrEmpty() {
		return memos == null ? List.of() : memos;
	}
}
