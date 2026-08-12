package com.itcotato.dortfolio.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.dto.res.ActivityTypeResponse;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ActivityTypeServiceTest {

	@Autowired
	private ActivityTypeService activityTypeService;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("기본 활동 종류 네 개가 생성된다")
	void createsFourDefaultTypes() {
		User user = createUser();

		activityTypeService.createDefaultTypes(user);

		assertThat(activityTypeService.getActivityTypes(user.getId()))
				.extracting(ActivityTypeResponse::name)
				.containsExactlyInAnyOrder("동아리/학회", "프로젝트", "인턴", "공모전");
	}

	@Test
	@DisplayName("기본 활동 종류는 isDefault로 표시된다")
	void defaultTypesAreMarked() {
		User user = createUser();

		activityTypeService.createDefaultTypes(user);

		// allMatch는 빈 목록에서도 통과하므로 개수를 함께 확인한다
		assertThat(activityTypeService.getActivityTypes(user.getId()))
				.hasSize(4)
				.allMatch(ActivityTypeResponse::isDefault);
	}

	@Test
	@DisplayName("두 번 호출해도 중복 생성되지 않는다")
	void doesNotDuplicateOnSecondCall() {
		User user = createUser();

		activityTypeService.createDefaultTypes(user);
		activityTypeService.createDefaultTypes(user);

		assertThat(activityTypeService.getActivityTypes(user.getId())).hasSize(4);
	}

	@Test
	@DisplayName("이미 활동 종류가 있으면 기본값을 만들지 않는다")
	void doesNotCreateWhenUserAlreadyHasTypes() {
		User user = createUser();
		activityTypeRepository.save(ActivityType.create(user, "직접 만든 종류"));

		activityTypeService.createDefaultTypes(user);

		assertThat(activityTypeService.getActivityTypes(user.getId()))
				.extracting(ActivityTypeResponse::name)
				.containsExactly("직접 만든 종류");
	}

	@Test
	@DisplayName("기본 활동 종류는 사용자별로 따로 생성된다")
	void createsPerUser() {
		User first = createUser();
		User second = createUser();

		activityTypeService.createDefaultTypes(first);
		activityTypeService.createDefaultTypes(second);

		// 다른 테스트가 남긴 데이터와 섞이지 않도록 사용자별로 확인한다
		assertThat(activityTypeService.getActivityTypes(first.getId())).hasSize(4);
		assertThat(activityTypeService.getActivityTypes(second.getId())).hasSize(4);
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}
}
