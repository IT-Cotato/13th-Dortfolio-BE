package com.itcotato.dortfolio.domain.job.embedding.batch;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingBatchResult;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingCoverage;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingFailure;
import com.itcotato.dortfolio.domain.job.embedding.service.JobCompetencyEmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class JobCompetencyEmbeddingBatchRunnerTest {

    @Mock
    private JobCompetencyEmbeddingService embeddingService;

    @Test
    void generateCommandCompletesWhenBatchAndCoverageSucceed() {
        JobCompetencyEmbeddingBatchRunner runner = runner(
                JobCompetencyEmbeddingBatchCommand.GENERATE_MISSING
        );
        when(embeddingService.generateAllMissing()).thenReturn(
                new JobCompetencyEmbeddingBatchResult(5, 2, 3, 0, List.of())
        );
        when(embeddingService.inspectCoverage()).thenReturn(
                new JobCompetencyEmbeddingCoverage(
                        "gemini-embedding-2", 5, 5, List.of()
                )
        );

        assertThatNoException().isThrownBy(
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        );
    }

    @Test
    void generateCommandFailsWhenAnyItemFails() {
        UUID failedId = UUID.randomUUID();
        JobCompetencyEmbeddingBatchRunner runner = runner(
                JobCompetencyEmbeddingBatchCommand.GENERATE_MISSING
        );
        when(embeddingService.generateAllMissing()).thenReturn(
                new JobCompetencyEmbeddingBatchResult(
                        5,
                        4,
                        0,
                        1,
                        List.of(new JobCompetencyEmbeddingFailure(failedId, "timeout"))
                )
        );
        when(embeddingService.inspectCoverage()).thenReturn(
                new JobCompetencyEmbeddingCoverage(
                        "gemini-embedding-2", 5, 4, List.of(failedId)
                )
        );

        assertThatThrownBy(
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed=1")
                .hasMessageContaining("missing=1");
    }

    @Test
    void verifyCommandDoesNotGenerateEmbeddings() {
        JobCompetencyEmbeddingBatchRunner runner = runner(
                JobCompetencyEmbeddingBatchCommand.VERIFY
        );
        when(embeddingService.inspectCoverage()).thenReturn(
                new JobCompetencyEmbeddingCoverage(
                        "gemini-embedding-2", 5, 5, List.of()
                )
        );

        assertThatNoException().isThrownBy(
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        );

        verify(embeddingService, never()).generateAllMissing();
        verify(embeddingService).inspectCoverage();
    }

    @Test
    void verifyCommandFailsWhenEmbeddingIsMissing() {
        UUID missingId = UUID.randomUUID();
        JobCompetencyEmbeddingBatchRunner runner = runner(
                JobCompetencyEmbeddingBatchCommand.VERIFY
        );
        when(embeddingService.inspectCoverage()).thenReturn(
                new JobCompetencyEmbeddingCoverage(
                        "gemini-embedding-2", 5, 4, List.of(missingId)
                )
        );

        assertThatThrownBy(
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing=1");
        verify(embeddingService, never()).generateAllMissing();
    }

    private JobCompetencyEmbeddingBatchRunner runner(
            JobCompetencyEmbeddingBatchCommand command
    ) {
        return new JobCompetencyEmbeddingBatchRunner(
                embeddingService,
                new JobCompetencyEmbeddingBatchProperties(true, command)
        );
    }
}
