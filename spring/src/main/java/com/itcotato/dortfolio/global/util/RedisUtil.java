package com.itcotato.dortfolio.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    // Redis에 [Key: Value] 데이터 저장 (만료시간 없음)
    public void setData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // Redis에서 Key에 해당하는 Value 조회
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Redis에 [Key: Value] 데이터 저장 및 밀리초(ms) 단위 만료 시간 설정
    public void setDataExpire(String key, String value, long durationMillis) {
        redisTemplate.opsForValue().set(key, value, durationMillis, TimeUnit.MILLISECONDS);
    }

    // Redis에서 Key에 해당하는 데이터 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // Redis에 해당 Key가 존재하는지 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
