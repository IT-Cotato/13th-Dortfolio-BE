package com.itcotato.dortfolio.domain.memo.scheduler;

import com.itcotato.dortfolio.domain.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 기능명세서 3. 메모하기: "자동 삭제까지 남은 일수 D-XX (30일 이후 자동 삭제)"
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoExpirationScheduler {

    private final MemoService memoService;

    // 매일 새벽 4시, 생성일로부터 30일(expiresAt) 지난 메모 하드 삭제
    @Scheduled(cron = "0 0 4 * * *")
    public void deleteExpiredMemos() {
        int deletedCount = memoService.deleteExpiredMemos();

        if (deletedCount > 0) {
            log.info("만료된 메모 {}건 자동 삭제 완료", deletedCount);
        }
    }

    /**
     * 사용자가 삭제한 메모를 실제로 지운다 (기능명세서 3.2.3.1.1).
     *
     * 삭제 직후에는 실행취소할 수 있어야 해서 감추기만 하고, 유예가 지난 뒤 여기서 정리한다.
     * 삭제한 데이터가 오래 남지 않도록 만료 정리보다 자주 돌린다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void deleteMemosPastGracePeriod() {
        int deletedCount = memoService.deleteMemosPastGracePeriod();

        if (deletedCount > 0) {
            log.info("유예 기간이 지난 메모 {}건 삭제 완료", deletedCount);
        }
    }
}
