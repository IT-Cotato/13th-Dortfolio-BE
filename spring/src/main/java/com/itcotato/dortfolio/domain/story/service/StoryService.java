package com.itcotato.dortfolio.domain.story.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.story.dto.res.TimelineActivityResponse;
import com.itcotato.dortfolio.domain.story.dto.res.TimelineRecordResponse;
import com.itcotato.dortfolio.domain.story.repository.StoryQueryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

	private final StoryQueryRepository storyQueryRepository;

	/* 기능명세서 5.1: 보관된 활동을 시간순으로, 각 활동의 기록을 작성순으로 함께 반환 */
	public List<TimelineActivityResponse> getTimeline(UUID userId) {
		List<Activity> activities = storyQueryRepository.findArchivedActivities(userId);

		if (activities.isEmpty()) {
			return List.of();
		}

		List<UUID> activityIds = activities.stream()
				.map(Activity::getId)
				.toList();

		// 활동별로 기록을 각각 조회하면 N+1이라 한 번에 조회 후 그룹핑한다 (조회 순서가 곧 작성순)
		Map<UUID, List<TimelineRecordResponse>> recordsByActivityId =
				storyQueryRepository.findRecordsByActivityIds(activityIds).stream()
						.collect(Collectors.groupingBy(
								found -> found.getActivity().getId(),
								Collectors.mapping(TimelineRecordResponse::from, Collectors.toList())
						));

		return activities.stream()
				.map(activity -> TimelineActivityResponse.of(
						activity,
						recordsByActivityId.getOrDefault(activity.getId(), List.of())
				))
				.toList();
	}
}
