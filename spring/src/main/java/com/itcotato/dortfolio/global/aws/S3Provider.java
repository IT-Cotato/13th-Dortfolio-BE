package com.itcotato.dortfolio.global.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class S3Provider {

     /* Presigned URL 발급 메서드 (Mock) */
    public PresignedUrlResponse generatePresignedUrl(String prefix, String fileName) {
        String extension = extractExtension(fileName);

        String s3Key = prefix + "/" + UUID.randomUUID() + "." + extension;

        // TODO: AWS 계정 생성 및 S3 버킷 구축 후, 실제 AWS S3 SDK(S3Presigner) 코드로 교체 예정
        String mockPresignedUrl = "https://mock-dortfolio-bucket.s3.ap-northeast-2.amazonaws.com/"
                + s3Key
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=mock";

        return new PresignedUrlResponse(mockPresignedUrl, s3Key);
    }

    /* S3 객체 삭제 메서드 (Mock) */
    public void deleteObject(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        // TODO: AWS 계정 생성 및 S3 버킷 구축 후, 실제 AWS S3 SDK(DeleteObjectRequest) 코드로 교체 예정
        log.info("[Mock] S3 객체 삭제 요청: {}", s3Key);
    }

    private String extractExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return "png";
        }
        return fileName.substring(lastIndexOfDot + 1);
    }

    public record PresignedUrlResponse(String presignedUrl, String s3Key) {}
}