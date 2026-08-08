package com.itcotato.dortfolio.domain.record.service;

import com.itcotato.dortfolio.domain.record.dto.req.RecordAnswerRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordAnswerResponse;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordAnswerService {

	private final RecordAnswerRepository recordAnswerRepository;

	public void createAnswers(Record record, Template template, List<RecordAnswerRequest> answerRequests) {
		Map<UUID, String> answerTextByQuestionId = toAnswerTextMap(answerRequests);
		List<RecordAnswer> answers = questions(template).stream()
			.map(question -> toRecordAnswer(record, question, answerTextByQuestionId.get(question.getId())))
			.toList();

		recordAnswerRepository.saveAll(answers);
	}

	public void replaceAnswers(Record record, List<RecordAnswerRequest> answerRequests) {
		Map<UUID, String> answerTextByQuestionId = toAnswerTextMap(answerRequests);
		recordAnswerRepository.findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(record.getId())
			.forEach(answer -> answer.updateAnswer(answerTextByQuestionId.getOrDefault(answer.getTemplateQuestionId(), "")));
	}

	public List<RecordAnswerResponse> getAnswerResponses(Record record) {
		List<RecordAnswer> recordAnswers =
			recordAnswerRepository.findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(record.getId());

		return recordAnswers.stream()
			.map(RecordAnswerResponse::from)
			.sorted(Comparator.comparingInt(RecordAnswerResponse::sortOrder))
			.toList();
	}

	public void validateCreateAnswers(Template template, List<RecordAnswerRequest> answerRequests) {
		List<UUID> requestedQuestionIds = answerRequests.stream()
			.map(RecordAnswerRequest::templateQuestionId)
			.toList();

		if (new HashSet<>(requestedQuestionIds).size() != answerRequests.size()) {
			throw new CustomException(RecordErrorCode.DUPLICATE_RECORD_ANSWER_SELECTION);
		}

		Set<UUID> questionIds = questions(template).stream()
			.map(TemplateQuestion::getId)
			.collect(Collectors.toSet());

		answerRequests.stream()
			.map(RecordAnswerRequest::templateQuestionId)
			.filter(questionId -> !questionIds.contains(questionId))
			.findAny()
			.ifPresent(questionId -> {
				throw new CustomException(RecordErrorCode.RECORD_QUESTION_NOT_FOUND);
			});
	}

	public void validateUpdateAnswers(Record record, List<RecordAnswerRequest> answerRequests) {
		List<UUID> requestedQuestionIds = answerRequests.stream()
			.map(RecordAnswerRequest::templateQuestionId)
			.toList();

		if (new HashSet<>(requestedQuestionIds).size() != answerRequests.size()) {
			throw new CustomException(RecordErrorCode.DUPLICATE_RECORD_ANSWER_SELECTION);
		}

		Set<UUID> recordQuestionIds = recordAnswerRepository.findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(record.getId()).stream()
			.map(RecordAnswer::getTemplateQuestionId)
			.collect(Collectors.toSet());

		requestedQuestionIds.stream()
			.filter(questionId -> !recordQuestionIds.contains(questionId))
			.findAny()
			.ifPresent(questionId -> {
				throw new CustomException(RecordErrorCode.RECORD_QUESTION_NOT_FOUND);
			});
	}

	public void validateRequiredAnswers(UUID recordId) {
		List<RecordAnswer> answers =
			recordAnswerRepository.findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(recordId);

		answers.stream()
			.filter(answer -> answer.getTemplateQuestion().isRequired())
			.filter(answer -> !hasText(answer.getAnswerText()))
			.findAny()
			.ifPresent(answer -> {
				throw new CustomException(RecordErrorCode.RECORD_REQUIRED_ANSWER_MISSING);
			});
	}

	public void deleteAnswers(UUID recordId) {
		recordAnswerRepository.deleteAllByRecord_Id(recordId);
	}

	private RecordAnswer toRecordAnswer(Record record, TemplateQuestion question, String answerText) {
		return RecordAnswer.builder()
			.record(record)
			.templateQuestion(question)
			.answerText(normalizeAnswerText(answerText))
			.build();
	}

	private Map<UUID, String> toAnswerTextMap(List<RecordAnswerRequest> answerRequests) {
		return answerRequests.stream()
			.collect(Collectors.toMap(
				RecordAnswerRequest::templateQuestionId,
				request -> normalizeAnswerText(request.answerText())
			));
	}

	private List<TemplateQuestion> questions(Template template) {
		return template.getQuestions().stream()
			.sorted(Comparator.comparingInt(TemplateQuestion::getSortOrder))
			.toList();
	}

	private boolean hasText(String text) {
		return text != null && !text.isBlank();
	}

	private String normalizeAnswerText(String text) {
		return hasText(text) ? text.trim() : "";
	}
}
