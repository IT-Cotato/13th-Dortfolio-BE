package com.itcotato.dortfolio.domain.insight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InsightRepositoryTest {

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findLatestCompletedByUserIdReturnsLatestCompletedInsight() {
        User user = createUser();

        Insight older = createPendingInsight(user, LocalDateTime.of(2026, 7, 30, 9, 0));
        older.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight latest = createPendingInsight(user, LocalDateTime.of(2026, 7, 31, 9, 0));
        latest.complete(LocalDateTime.of(2026, 7, 31, 10, 0));

        insightRepository.save(older);
        insightRepository.save(latest);

        Insight result = insightRepository
                .findLatestCompletedByUserId(user.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(latest.getId());
    }

    @Test
    void findLatestCompletedByUserIdExcludesPendingAndFailedInsights() {
        User user = createUser();

        Insight completed = createPendingInsight(user, LocalDateTime.of(2026, 7, 30, 9, 0));
        completed.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight pending = createPendingInsight(user, LocalDateTime.of(2026, 7, 31, 9, 0));

        Insight failed = createPendingInsight(user, LocalDateTime.of(2026, 8, 1, 9, 0));
        failed.fail(LocalDateTime.of(2026, 8, 1, 10, 0), "AI_ERROR", "AI response failed");

        insightRepository.save(completed);
        insightRepository.save(pending);
        insightRepository.save(failed);

        Insight result = insightRepository
                .findLatestCompletedByUserId(user.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(completed.getId());
    }

    @Test
    void findLatestCompletedByUserIdDoesNotReturnAnotherUsersInsight() {
        User firstUser = createUser();
        User secondUser = createUser();

        Insight firstUserInsight = createPendingInsight(firstUser, LocalDateTime.of(2026, 7, 30, 9, 0));
        firstUserInsight.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight secondUserInsight = createPendingInsight(secondUser, LocalDateTime.of(2026, 8, 1, 9, 0));
        secondUserInsight.complete(LocalDateTime.of(2026, 8, 1, 10, 0));

        insightRepository.save(firstUserInsight);
        insightRepository.save(secondUserInsight);

        Insight result = insightRepository
                .findLatestCompletedByUserId(firstUser.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(firstUserInsight.getId());
    }

    @Test
    void findLatestCompletedByUserIdReturnsEmptyWhenCompletedInsightDoesNotExist() {
        User user = createUser();

        insightRepository.save(createPendingInsight(user, LocalDateTime.of(2026, 8, 1, 9, 0)));

        assertThat(
                insightRepository.findLatestCompletedByUserId(user.getId())
        ).isEmpty();
    }

    private User createUser() {
        String uniqueEmail = UUID.randomUUID() + "@test.com";

        return userRepository.save(User.of(
                uniqueEmail,
                "encoded-password",
                "인사이트 테스트"
        ));
    }

    private Insight createPendingInsight(User user, LocalDateTime requestedAt) {
        return Insight.pending(
                user,
                UUID.randomUUID(),
                "백엔드 개발자",
                requestedAt,
                10,
                requestedAt
        );
    }
}
