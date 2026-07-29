package com.itcotato.dortfolio.domain.record.service;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.record.dto.req.RecordMemoRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordMemoResponse;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordMemoService {

	private final RecordMemoRepository recordMemoRepository;
	private final MemoRepository memoRepository;

	public void createRecordMemos(UUID userId, Record record, List<RecordMemoRequest> memoRequests) {
		List<Memo> memos = getValidMemos(userId, record.getActivity().getId(), memoRequests);
		saveRecordMemos(record, memoRequests, memos, memo -> true);
	}

	public void replaceMemos(UUID userId, Record record, List<RecordMemoRequest> memoRequests) {
		List<Memo> memos = getValidMemos(userId, record.getActivity().getId(), memoRequests);
		List<RecordMemo> existingRecordMemos =
			recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId());

		Set<UUID> nextMemoIds = memoRequests.stream()
			.map(RecordMemoRequest::memoId)
			.collect(Collectors.toSet());

		existingRecordMemos.stream()
			.filter(recordMemo -> !nextMemoIds.contains(recordMemo.getMemo().getId()))
			.map(RecordMemo::getMemo)
			.forEach(Memo::decreaseUseCount);

		Set<UUID> existingMemoIds = existingRecordMemos.stream()
			.map(recordMemo -> recordMemo.getMemo().getId())
			.collect(Collectors.toSet());

		recordMemoRepository.deleteAll(existingRecordMemos);
		recordMemoRepository.flush();

		saveRecordMemos(record, memoRequests, memos, memo -> !existingMemoIds.contains(memo.getId()));
	}

	public void decreaseUseCounts(Record record) {
		recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId()).stream()
			.map(RecordMemo::getMemo)
			.forEach(Memo::decreaseUseCount);
	}

	public void increaseUseCounts(Record record) {
		recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId()).stream()
			.map(RecordMemo::getMemo)
			.forEach(Memo::increaseUseCount);
	}

	public List<RecordMemoResponse> getMemoResponses(Record record) {
		return recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(record.getId())
			.stream()
			.map(RecordMemoResponse::from)
			.toList();
	}

	public void deleteRecordMemos(UUID recordId) {
		recordMemoRepository.deleteAllByRecord_Id(recordId);
	}

	private void saveRecordMemos(
		Record record,
		List<RecordMemoRequest> memoRequests,
		List<Memo> memos,
		Predicate<Memo> useCountIncreaseCondition
	) {
		Map<UUID, Memo> memosById = memos.stream()
			.collect(Collectors.toMap(Memo::getId, Function.identity()));

		List<RecordMemo> recordMemos = new ArrayList<>();
		for (int index = 0; index < memoRequests.size(); index++) {
			RecordMemoRequest request = memoRequests.get(index);
			Memo memo = memosById.get(request.memoId());

			if (useCountIncreaseCondition.test(memo)) {
				memo.increaseUseCount();
			}

			recordMemos.add(RecordMemo.builder()
				.record(record)
				.memo(memo)
				.sortOrder(index + 1)
				.isCollapsed(request.collapsed())
				.build());
		}

		recordMemoRepository.saveAll(recordMemos);
	}

	private List<Memo> getValidMemos(UUID userId, UUID activityId, List<RecordMemoRequest> memoRequests) {
		validateMemoSelection(memoRequests);

		List<UUID> memoIds = memoRequests.stream()
			.map(RecordMemoRequest::memoId)
			.toList();

		Map<UUID, Memo> memosById = memoRepository.findAllByIdInAndUser_IdAndDeletedAtIsNull(memoIds, userId)
			.stream()
			.collect(Collectors.toMap(Memo::getId, Function.identity()));

		return memoIds.stream()
			.map(memoId -> {
				Memo memo = memosById.get(memoId);

				if (memo == null) {
					throw new CustomException(RecordErrorCode.RECORD_MEMO_NOT_FOUND);
				}

				if (!memo.getActivity().getId().equals(activityId)) {
					throw new CustomException(RecordErrorCode.RECORD_MEMO_ACTIVITY_MISMATCH);
				}

				return memo;
			})
			.toList();
	}

	private void validateMemoSelection(List<RecordMemoRequest> memoRequests) {
		List<UUID> requestedMemoIds = memoRequests.stream()
			.map(RecordMemoRequest::memoId)
			.toList();

		if (new HashSet<>(requestedMemoIds).size() != memoRequests.size()) {
			throw new CustomException(RecordErrorCode.DUPLICATE_RECORD_MEMO_SELECTION);
		}
	}
}
