package com.sprint.mission.discodeit.storage.s3;

import com.sprint.mission.discodeit.config.AWSS3Properties;
import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class AWSS3Test {

    private final AWSS3Properties awsS3Properties;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        /* AWS 자격 증명 설정 */
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            awsS3Properties.getAccessKey(),
            awsS3Properties.getSecretKey()
        );

        /* S3 클라이언트 초기화 */
        s3Client = S3Client.builder()
            .region(Region.of(awsS3Properties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();

        /* S3 Presigner 초기화 */
        s3Presigner = S3Presigner.builder()
            .region(Region.of(awsS3Properties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();

        log.info("AWS S3 Client 초기화 완료 - bucket: {}, region: {}", awsS3Properties.getBucket(), awsS3Properties.getRegion());
    }

    /* S3 업로드 테스트 */
    public void testUpload() {
        try {
            String key = "test/upload-test.txt";
            String content = "AWS S3 업로드 테스트 - " + System.currentTimeMillis();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(key)
                .contentType("text/plain; charset=UTF-8")
                .contentEncoding("UTF-8")
                .build();

            PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromString(content, StandardCharsets.UTF_8)
            );

            log.info("업로드 성공 - Key : {}, ETag : {}", key, response.eTag());
        } catch (Exception e) {
            log.error("업로드 실패 : {}", e.getMessage(), e);
        }
    }

    /* S3 다운로드 테스트 */
    public void testDownload() {
        try {
            String key = "test/upload-test.txt";

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(key)
                .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);

            String content = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            response.close();

            log.info("다운로드 성공 - Key: {}, Content: {}", key, content);

        } catch (Exception e) {
            log.error("다운로드 실패: {}", e.getMessage(), e);
        }
    }

    /* S3 Presigned URL 생성 테스트 */
    public void testPresignedUrl() {
        try {
            String key = "test/upload-test.txt";

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(key)
                .responseContentType("text/plain; charset=UTF-8")
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            URL presignedUrl = presignedRequest.url();

            log.info("Presinged URL 생성 성공 - Key: {}, URL: {}", key, presignedUrl);
            log.info("만료시간 10분");
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패 : {}", e.getMessage(), e);
        }
    }
    /* 모든 테스트 실행 */
    public void runAllTests() {
        log.info("AWS S3 테스트 시작!");

        testUpload();
        testDownload();
        testPresignedUrl();

        log.info("AWS S3 테스트 종료...");
    }
}
