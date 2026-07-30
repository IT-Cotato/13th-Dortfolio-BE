package com.itcotato.dortfolio.domain.record.analysis.dto;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import java.util.List;
import java.util.UUID;

public record RecordAnalysisRequest(
	UUID recordId,
	String title,
	ActivityPayload activity,
	TemplatePayload template,
	List<AnswerPayload> answers,
	List<MemoPayload> memos,
	List<CompetencyTagCandidatePayload> competencyTagCandidates
) {
	public static RecordAnalysisRequest of(
		Record record,
		List<RecordAnswer> answers,
		List<RecordMemo> memos,
		List<CompetencyTag> competencyTagCandidates
	) {
		return new RecordAnalysisRequest(
			record.getId(),
			record.getTitle(),
			ActivityPayload.from(record),
			TemplatePayload.from(record),
			answers.stream()
				.map(AnswerPayload::from)
				.toList(),
			memos.stream()
				.map(MemoPayload::from)
				.toList(),
			competencyTagCandidates.stream()
				.map(CompetencyTagCandidatePayload::from)
				.toList()
		);
	}

	public record ActivityPayload(
		String title,
		String description
	) {
		public static ActivityPayload from(Record record) {
			return new ActivityPayload(
				record.getActivity().getTitle(),
				record.getActivity().getDescription()
			);
		}
	}

	public record TemplatePayload(
		String title
	) {
		public static TemplatePayload from(Record record) {
			return new TemplatePayload(record.getTemplate().getTitle());
		}
	}

	public record AnswerPayload(
		String questionText,
		String answerText
	) {
		public static AnswerPayload from(RecordAnswer answer) {
			return new AnswerPayload(answer.getQuestionText(), answer.getAnswerText());
		}
	}

	public record MemoPayload(
		String title,
		String content
	) {
		public static MemoPayload from(RecordMemo recordMemo) {
			Memo memo = recordMemo.getMemo();
			return new MemoPayload(memo.getTitle(), memo.getContent());
		}
	}

	public record CompetencyTagCandidatePayload(
		UUID id,
		String name,
		String description
	) {
		public static CompetencyTagCandidatePayload from(CompetencyTag competencyTag) {
			return new CompetencyTagCandidatePayload(
				competencyTag.getId(),
				competencyTag.getName(),
				competencyTag.getDescription()
			);
		}
	}
}
