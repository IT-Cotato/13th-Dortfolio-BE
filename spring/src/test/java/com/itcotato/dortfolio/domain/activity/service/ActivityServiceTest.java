package com.itcotato.dortfolio.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ActivityServiceTest {

	@Autowired
	private ActivityService activityService;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createActivity() {
		User user = createUser();
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "프로젝트"));

		UUID activityId = activityService.createActivity(user.getId(), new ActivityCreateRequest(
				activityType.getId(),
				"도트폴리오",
				"설명",
				LocalDate.now(),
				null,
				true
		));
		assertThat(activityRepository.findById(activityId)).isPresent();
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}
}
