package com.itcotato.dortfolio.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.repository.JobRepository;
import com.itcotato.dortfolio.domain.mypage.dto.ProfileImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.mypage.dto.UpdateDesiredJobRequest;
import com.itcotato.dortfolio.domain.mypage.dto.UpdateUserProfileRequest;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.aws.S3Provider;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private UserJobRepository userJobRepository;
    @Mock private JobRepository jobRepository;
    @Mock private S3Provider s3Provider;

    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        myPageService = new MyPageService(userRepository, userJobRepository, jobRepository, s3Provider);
    }

    @Test
    void returnsDownloadUrlImageKeyAndDesiredJobIdentity() {
        User user = mock(User.class);
        UserJob userJob = mock(UserJob.class);
        Job job = mock(Job.class);
        UUID jobId = UUID.randomUUID();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getNickname()).thenReturn("도토리");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getProfileImageUrl()).thenReturn("profile/old-image.png");
        when(s3Provider.generateDownloadUrl("profile/old-image.png")).thenReturn("https://download-url");
        when(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID)).thenReturn(Optional.of(userJob));
        when(userJob.getJob()).thenReturn(job);
        when(job.getId()).thenReturn(jobId);
        when(job.getName()).thenReturn("백엔드 개발자");

        var response = myPageService.getMyPageInfo(USER_ID);

        assertThat(response.profileImageUrl()).isEqualTo("https://download-url");
        assertThat(response.profileImageKey()).isEqualTo("profile/old-image.png");
        assertThat(response.desiredJobId()).isEqualTo(jobId);
        assertThat(response.desiredJob()).isEqualTo("백엔드 개발자");
    }

    @Test
    void createsPresignedUrlUnderAuthenticatedUserPrefix() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(s3Provider.generatePresignedUrl("profile/" + USER_ID, "avatar.png"))
                .thenReturn(new S3Provider.PresignedUrlResponse("https://upload-url", "profile/key.png"));

        var response = myPageService.createProfileImagePresignedUrl(
                USER_ID,
                new ProfileImagePresignedUrlRequest("avatar.png")
        );

        assertThat(response.presignedUrl()).isEqualTo("https://upload-url");
        assertThat(response.s3Key()).isEqualTo("profile/key.png");
    }

    @Test
    void updatesDesiredJobWhileHoldingUserLock() {
        User user = mock(User.class);
        Job job = mock(Job.class);
        UserJob existingUserJob = mock(UserJob.class);
        UUID jobId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userJobRepository.findByUserIdAndJobId(USER_ID, jobId))
                .thenReturn(Optional.of(existingUserJob));

        myPageService.updateDesiredJob(USER_ID, new UpdateDesiredJobRequest(jobId));

        verify(userJobRepository).resetPrimaryByUserId(USER_ID);
        verify(existingUserJob).changePrimary(true);
    }

    @Test
    void rejectsAnotherUsersProfileImageKey() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> myPageService.updateProfile(
                USER_ID,
                new UpdateUserProfileRequest(null, "profile/" + UUID.randomUUID() + "/image.png")
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(UserErrorCode.INVALID_PROFILE_IMAGE_KEY));

        verify(user, never()).updateProfile(any(), anyString());
    }

    @Test
    void replacesProfileImageAndDeletesPreviousObject() {
        User user = mock(User.class);
        String previousKey = "profile/legacy.png";
        String nextKey = "profile/" + USER_ID + "/next.png";
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getProfileImageUrl()).thenReturn(previousKey);

        myPageService.updateProfile(USER_ID, new UpdateUserProfileRequest(null, nextKey));

        verify(user).updateProfile(null, nextKey);
        verify(s3Provider).deleteObject(previousKey);
    }
}
