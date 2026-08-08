package com.itcotato.dortfolio.global.aws;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 접근을 한 곳에 모아둔다.
 *
 * 버킷은 비공개이므로 파일은 서버를 거치지 않고 presigned URL로 직접 주고받는다.
 * 업로드는 클라이언트가 발급받은 URL에 PUT하고, 조회는 응답을 만들 때마다 GET URL을 새로 발급한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3Provider {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    /**
     * 업로드용 presigned URL을 발급한다.
     *
     * 파일명은 UUID로 새로 만든다. 사용자가 올린 이름을 그대로 쓰면
     * 같은 이름끼리 덮어쓰고, 이름에 담긴 개인정보가 URL에 노출된다.
     */
    public PresignedUrlResponse generatePresignedUrl(String prefix, String fileName) {
        String s3Key = prefix + "/" + UUID.randomUUID() + "." + extractExtension(fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUrlResponse(presignedUrl, s3Key);
    }

    /**
     * 조회용 presigned URL을 발급한다.
     *
     * 버킷이 비공개라 저장된 URL을 그대로 주면 열리지 않는다.
     * 만료 시간이 있으므로 DB에 저장하지 않고 응답을 만들 때마다 새로 발급해야 한다.
     */
    public String generateDownloadUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.downloadUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * S3 객체를 삭제한다.
     *
     * 삭제 실패로 요청 전체를 되돌리지는 않는다.
     * DB에서 이미 지운 이미지의 S3 객체가 남는 건 낭비일 뿐이지만,
     * 여기서 예외를 던지면 사용자는 삭제에 실패했다고 보게 된다.
     */
    public void deleteObject(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(s3Key)
                    .build());
        } catch (Exception e) {
            log.warn("S3 객체 삭제 실패. 수동 정리가 필요할 수 있습니다. key={}", s3Key, e);
        }
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
