package com.itcotato.dortfolio.domain.insight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class InsightRepositoryTest {

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        insightRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findLatestCompletedByUserIdReturnsLatestCompletedInsight() {
        User user = createUser();

        Insight older = Insight.pending(user);
        older.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight latest = Insight.pending(user);
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

        Insight completed = Insight.pending(user);
        completed.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight pending = Insight.pending(user);

        Insight failed = Insight.pending(user);
        failed.fail(LocalDateTime.of(2026, 8, 1, 10, 0));

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

        Insight firstUserInsight = Insight.pending(firstUser);
        firstUserInsight.complete(LocalDateTime.of(2026, 7, 30, 10, 0));

        Insight secondUserInsight = Insight.pending(secondUser);
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

        insightRepository.save(Insight.pending(user));

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
}