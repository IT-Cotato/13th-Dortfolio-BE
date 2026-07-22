package com.itcotato.dortfolio.domain.mypage.service;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.repository.JobRepository;
import com.itcotato.dortfolio.domain.mypage.dto.*;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.aws.S3Provider;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final UserJobRepository userJobRepository;
    private final JobRepository jobRepository;
    private final S3Provider s3Provider;

    /* 마이페이지 기본 정보 조회 로직 */
    public MyPageResponse getMyPageInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String desiredJobName = userJobRepository.findByUserIdAndIsPrimaryTrue(userId)
                .map(userJob -> userJob.getJob().getName())
                .orElse(null);

        return MyPageResponse.of(user, desiredJobName);
    }

    /* 희망 직무 변경 로직 */
    @Transactional
    public void updateDesiredJob(UUID userId, UpdateDesiredJobRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new CustomException(JobErrorCode.JOB_NOT_FOUND));

        userJobRepository.findByUserIdAndIsPrimaryTrue(userId)
                .ifPresent(userJob -> userJob.changePrimary(false));

        userJobRepository.findByUserIdAndJobId(userId, request.jobId())
                .ifPresentOrElse(
                        existingUserJob -> existingUserJob.changePrimary(true),
                        () -> userJobRepository.save(UserJob.create(user, job, true))
                );
    }

    /* 프로필 이미지 업로드 Presigned URL 발급 로직 */
    public ProfileImagePresignedUrlResponse createProfileImagePresignedUrl(ProfileImagePresignedUrlRequest request) {
        S3Provider.PresignedUrlResponse result = s3Provider.generatePresignedUrl("profile", request.fileName());
        return new ProfileImagePresignedUrlResponse(result.presignedUrl(), result.s3Key());
    }

    /* 회원 프로필 정보(이름, 프로필 사진) 수정 로직 */
    @Transactional
    public void updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.name(), request.profileImageUrl());
    }
}