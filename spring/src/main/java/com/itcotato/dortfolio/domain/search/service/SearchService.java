package com.itcotato.dortfolio.domain.search.service;

import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.search.config.SearchProperties;
import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchPageResponse;
import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchResponse;
import com.itcotato.dortfolio.domain.search.repository.SearchQueryRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.SearchErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	// 기능명세서 5.3: 자음/모음만으로는 검색되지 않는다 (한글 호환 자모 영역)
	private static final String HANGUL_JAMO_ONLY = "^[ㄱ-ㅎㅏ-ㅣ\\s]+$";

	private final SearchQueryRepository searchQueryRepository;
	private final SearchProperties searchProperties;

	/* 기능명세서 5.3: 기록 제목과 기록 내용에서 키워드를 검색한다 (태그는 검색 대상 아님) */
	public RecordSearchPageResponse searchRecords(UUID userId, String keyword, int page, Integer size) {
		int pageSize = size == null ? searchProperties.defaultPageSize() : size;
		validatePageRequest(page, pageSize);

		if (isUnsearchable(keyword)) {
			return RecordSearchPageResponse.empty(page, pageSize);
		}

		String likeKeyword = toLikeKeyword(keyword);
		List<UUID> answerMatchedIds = searchQueryRepository.findRecordIdsWithMatchingAnswer(userId, likeKeyword);
		long totalElements = searchQueryRepository.countMatchingRecords(userId, likeKeyword, answerMatchedIds);

		if (totalElements == 0) {
			return RecordSearchPageResponse.empty(page, pageSize);
		}

		List<Record> records =
				searchQueryRepository.findMatchingRecords(userId, likeKeyword, answerMatchedIds, page, pageSize);
		Map<UUID, List<RecordAnswer>> answersByRecordId = findAnswersByRecordId(records);

		List<RecordSearchResponse> content = records.stream()
				.map(record -> RecordSearchResponse.of(
						record,
						pickContent(answersByRecordId.getOrDefault(record.getId(), List.of()), keyword)
				))
				.toList();

		return RecordSearchPageResponse.of(content, page, pageSize, totalElements);
	}

	private Map<UUID, List<RecordAnswer>> findAnswersByRecordId(List<Record> records) {
		List<UUID> recordIds = records.stream()
				.map(Record::getId)
				.toList();

		return searchQueryRepository.findAnswersByRecordIds(recordIds).stream()
				.collect(Collectors.groupingBy(answer -> answer.getRecord().getId()));
	}

	// 검색 결과에 보여줄 기록 내용은 키워드가 포함된 답변을 우선하고, 없으면 첫 번째 답변을 사용한다
	private String pickContent(List<RecordAnswer> answers, String keyword) {
		if (answers.isEmpty()) {
			return null;
		}

		String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);

		return answers.stream()
				.filter(answer -> answer.getAnswerText() != null
						&& answer.getAnswerText().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
				.findFirst()
				.orElse(answers.get(0))
				.getAnswerText();
	}

	// 빈 검색어이거나 자음/모음만 입력한 경우 검색 결과를 0건으로 처리한다
	private boolean isUnsearchable(String keyword) {
		return keyword == null || keyword.isBlank() || keyword.matches(HANGUL_JAMO_ONLY);
	}

	// LIKE 와일드카드를 사용자가 입력해도 문자 그대로 검색되도록 이스케이프한다
	private String toLikeKeyword(String keyword) {
		String escaped = keyword.trim()
				.toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");

		return "%" + escaped + "%";
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size <= 0 || size > searchProperties.maxPageSize()) {
			throw new CustomException(SearchErrorCode.INVALID_PAGE_REQUEST);
		}
	}
}
