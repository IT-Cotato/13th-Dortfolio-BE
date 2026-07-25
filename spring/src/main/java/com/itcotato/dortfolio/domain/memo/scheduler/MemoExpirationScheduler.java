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
}
