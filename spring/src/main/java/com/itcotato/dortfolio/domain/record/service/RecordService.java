package com.itcotato.dortfolio.domain.record.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.record.config.RecordProperties;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordSearchCondition;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordAnswerResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordMemoResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordSummaryResponse;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.GlobalErrorCode;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final RecordAnswerService recordAnswerService;
    private final RecordMemoService recordMemoService;
    private final RecordValidator recordValidator;
    private final RecordProperties recordProperties;

    @Transactional
    public RecordResponse createRecord(UUID userId, RecordCreateRequest request) {
        User user = getUserOrThrow(userId);
        Activity activity = recordValidator.getActiveActivityOrThrow(userId, request.activityId());
        Template template = recordValidator.getReadableActiveTemplateOrThrow(userId, request.templateId());

        recordValidator.validateActivityTemplate(activity.getId(), template.getId());
        recordAnswerService.validateCreateAnswers(template, request.answersOrEmpty());

        Record record = recordRepository.save(Record.builder()
                .user(user)
                .activity(activity)
                .template(template)
                .title(request.title())
                .build());

        recordAnswerService.createAnswers(record, template, request.answersOrEmpty());
        recordMemoService.createRecordMemos(userId, record, request.memosOrEmpty());
        applyStatus(record, request.status());

        return toRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public RecordResponse getRecord(UUID userId, UUID recordId) {
        Record record = getActiveRecordOrThrow(userId, recordId);
        return toRecordResponse(record);
    }

    @Transactional
    public RecordResponse updateRecord(UUID userId, UUID recordId, RecordUpdateRequest request) {
        Record record = getActiveRecordOrThrow(userId, recordId);

        recordAnswerService.validateUpdateAnswers(record, request.answersOrEmpty());

        record.updateTitle(request.title());
        recordAnswerService.replaceAnswers(record, request.answersOrEmpty());
        recordMemoService.replaceMemos(userId, record, request.memosOrEmpty());
        applyStatus(record, request.status());

        return toRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public List<RecordSummaryResponse> getActivityRecords(UUID userId, UUID activityId) {
        recordValidator.getActiveActivityOrThrow(userId, activityId);

        return recordRepository.searchRecords(userId, RecordSearchCondition.of(activityId, null, null)).stream()
                .map(RecordSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecordSummaryResponse> getRecords(UUID userId, UUID activityId, UUID templateId, RecordStatus status) {
        validateSearchFilters(userId, activityId, templateId);

        return recordRepository.searchRecords(userId, RecordSearchCondition.of(activityId, templateId, status)).stream()
                .map(RecordSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecordSummaryResponse> getRecentRecords(UUID userId) {
        return recordRepository.findRecentRecords(userId, recordProperties.recentRecordLimit()).stream()
                .map(RecordSummaryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteRecord(UUID userId, UUID recordId) {
        Record record = getActiveRecordOrThrow(userId, recordId);

        recordMemoService.decreaseUseCounts(record);
        record.markDeleted(recordProperties.deleteGracePeriodDays());
    }

    @Transactional
    public void restoreRecord(UUID userId, UUID recordId) {
        Record record = recordRepository.findByIdAndUser_Id(recordId, userId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));

        if (!record.isDeleted()) {
            return;
        }

        validateRestorable(record);
        recordMemoService.increaseUseCounts(record);
        record.restore();
    }

    private RecordResponse toRecordResponse(Record record) {
        List<RecordAnswerResponse> answers = recordAnswerService.getAnswerResponses(record);
        List<RecordMemoResponse> memos = recordMemoService.getMemoResponses(record);

        return RecordResponse.of(record, answers, memos);
    }

    private void applyStatus(Record record, RecordStatus requestedStatus) {
        if (record.getStatus() == RecordStatus.COMPLETED && requestedStatus == RecordStatus.DRAFT) {
            throw new CustomException(RecordErrorCode.RECORD_STATUS_TRANSITION_NOT_ALLOWED);
        }

        if (requestedStatus == RecordStatus.DRAFT) {
            record.saveDraft();
            return;
        }

        recordAnswerService.validateRequiredAnswers(record.getId());

        record.complete();
    }

    private void validateSearchFilters(UUID userId, UUID activityId, UUID templateId) {
        if (activityId != null) {
            recordValidator.getActiveActivityOrThrow(userId, activityId);
        }

        if (templateId != null) {
            recordValidator.getReadableActiveTemplateOrThrow(userId, templateId);
        }
    }

    private void validateRestorable(Record record) {
        if (record.getDeletePendingUntil() != null && LocalDateTime.now().isAfter(record.getDeletePendingUntil())) {
            throw new CustomException(RecordErrorCode.RECORD_RESTORE_NOT_ALLOWED);
        }
        if (record.getActivity().isDeleted() || record.getTemplate().isDeleted()) {
            throw new CustomException(RecordErrorCode.RECORD_RESTORE_NOT_ALLOWED);
        }
        if (recordMemoService.hasDeletedMemo(record)) {
            throw new CustomException(RecordErrorCode.RECORD_RESTORE_NOT_ALLOWED);
        }
    }

    private Record getActiveRecordOrThrow(UUID userId, UUID recordId) {
        Record record = recordRepository.findByIdAndUser_IdAndDeletedAtIsNull(recordId, userId)
                .orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));
        if (record.getActivity().isDeleted() || record.getTemplate().isDeleted()) {
            throw new CustomException(RecordErrorCode.RECORD_NOT_FOUND);
        }
        return record;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.INVALID_INPUT_VALUE));
    }
}
