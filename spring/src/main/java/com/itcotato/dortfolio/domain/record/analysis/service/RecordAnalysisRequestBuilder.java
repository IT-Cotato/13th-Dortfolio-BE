package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordAnalysisRequestBuilder {

	private final RecordAnswerRepository recordAnswerRepository;
	private final RecordMemoRepository recordMemoRepository;
	private final CompetencyTagRepository competencyTagRepository;

	public RecordAnalysisRequest build(Record record) {
		List<RecordAnswer> answers = recordAnswerRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId());
		List<RecordMemo> memos = recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId());
		List<CompetencyTag> competencyTagCandidates = competencyTagRepository.findAll();

		return RecordAnalysisRequest.of(record, answers, memos, competencyTagCandidates);
	}
}
