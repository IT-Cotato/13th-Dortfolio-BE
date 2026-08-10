package com.itcotato.dortfolio.domain.activity.service;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityTypeResponse;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityTypeService {

    // 활동 생성 화면에서 기본으로 보이는 활동 종류.
    // 목록을 활동 도메인 안에 두어야 나중에 추가·수정할 때 가입 로직을 건드리지 않는다
    private static final List<String> DEFAULT_TYPE_NAMES =
            List.of("동아리/학회", "프로젝트", "인턴", "공모전");

    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;

    /**
     * 가입한 사용자에게 기본 활동 종류를 만들어준다.
     *
     * 이미 활동 종류가 있으면 아무것도 하지 않는다. 사용자가 지운 기본 종류가
     * 되살아나면 안 되고, 가입 경로가 겹쳐 두 번 불려도 중복이 생기면 안 되기 때문이다.
     */
    @Transactional
    public void createDefaultTypes(User user) {
        if (activityTypeRepository.existsByUser_Id(user.getId())) {
            return;
        }

        activityTypeRepository.saveAll(
                DEFAULT_TYPE_NAMES.stream()
                        .map(name -> ActivityType.createDefault(user, name))
                        .toList()
        );
    }

    @Transactional
    public UUID createActivityType(UUID userId, ActivityTypeCreateRequest request) {
        User user = getUserOrThrow(userId);
        ActivityType activityType = ActivityType.create(user, request.name());
        return activityTypeRepository.save(activityType).getId();
    }

    public List<ActivityTypeResponse> getActivityTypes(UUID userId) {
        return activityTypeRepository.findAllByUser_Id(userId).stream()
                .map(ActivityTypeResponse::from)
                .toList();
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
