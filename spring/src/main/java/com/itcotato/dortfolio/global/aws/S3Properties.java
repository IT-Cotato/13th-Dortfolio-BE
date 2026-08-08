package com.itcotato.dortfolio.global.aws;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 접근에 필요한 설정.
 *
 * 버킷은 외부에 공개하지 않고, 업로드와 조회 모두 만료 시간이 있는 presigned URL로만 접근한다.
 * 업로드 URL은 발급 직후 한 번 쓰이므로 짧게, 조회 URL은 화면을 보는 동안 유효해야 하므로 조금 길게 잡는다.
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        Duration uploadUrlExpiration,
        Duration downloadUrlExpiration
) {
    public S3Properties {
        if (uploadUrlExpiration == null) {
            uploadUrlExpiration = Duration.ofMinutes(5);
        }
        if (downloadUrlExpiration == null) {
            downloadUrlExpiration = Duration.ofMinutes(30);
        }
    }
}
