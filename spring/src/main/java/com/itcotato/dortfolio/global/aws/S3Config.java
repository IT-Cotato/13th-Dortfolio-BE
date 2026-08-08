package com.itcotato.dortfolio.global.aws;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    // 키가 없어도(로컬 개발, 테스트) 앱은 떠야 하므로 자리표시자를 넣어 빈 생성만 통과시킨다.
    // SDK가 빈 문자열을 거부할 수 있어 공백이 아닌 값을 쓴다.
    // 실제 S3 호출 시점에 AWS가 거부하므로, S3를 쓰지 않는 기능은 그대로 동작한다
    private static final String NOT_CONFIGURED = "not-configured";

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        orPlaceholder(properties.accessKey()),
                        orPlaceholder(properties.secretKey())
                )
        );
    }

    private String orPlaceholder(String value) {
        return (value == null || value.isBlank()) ? NOT_CONFIGURED : value;
    }

    private Region region() {
        return Region.of(properties.region() == null ? "ap-northeast-2" : properties.region());
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
