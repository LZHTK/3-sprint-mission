package com.sprint.mission.discodeit.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Testcontainers
@MockitoSettings(strictness = Strictness.LENIENT)
class S3BinaryContentStorageTest {

    @Container
    static LocalStackContainer localstack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.S3);

    private TestS3BinaryContentStorage storage;
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);

    private static final String BUCKET_NAME = "test-bucket";
    private static final String ACCESS_KEY = "test";
    private static final String SECRET_KEY = "test";
    private static final String REGION = "us-east-1";

    @BeforeEach
    void setUp() {
        S3Client client = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
            .region(Region.of(REGION))
            .forcePathStyle(true)
            .build();
        client.createBucket(
            CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build()
        );

        storage = new TestS3BinaryContentStorage(
            ACCESS_KEY, SECRET_KEY, REGION, BUCKET_NAME, 600,
            localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            notificationRepository);
    }

    private static class TestS3BinaryContentStorage extends S3BinaryContentStorage {
        private final String endpointOverride;

        TestS3BinaryContentStorage(
            String accessKey, String secretKey, String region, String bucket,
            int presignedUrlExpiration, String endpointOverride,
            NotificationRepository notificationRepository) {
            super(accessKey, secretKey, region, bucket, presignedUrlExpiration, notificationRepository);
            this.endpointOverride = endpointOverride;
        }

        protected S3Client buildLocalStackClient() {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(S3BinaryContentStorageTest.ACCESS_KEY,
                S3BinaryContentStorageTest.SECRET_KEY);
            return S3Client.builder()
                .region(Region.of(S3BinaryContentStorageTest.REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpointOverride))
                .forcePathStyle(true)
                .build();
        }

        protected String buildLocalStackPresignedUrl(String key, String contentType) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(S3BinaryContentStorageTest.ACCESS_KEY,
                S3BinaryContentStorageTest.SECRET_KEY);

            try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(S3BinaryContentStorageTest.REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpointOverride))
                .build()) {

                GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(S3BinaryContentStorageTest.BUCKET_NAME)
                    .key(key)
                    .responseContentType(contentType)
                    .responseContentDisposition(
                        "attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"")
                    .build();

                GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(600))
                    .getObjectRequest(request)
                    .build();

                PresignedGetObjectRequest presigned = presigner.presignGetObject(presign);
                return presigned.url().toString();
            }
        }

        @Override
        public UUID put(UUID binaryContentId, byte[] bytes) {
            try {
                S3Client s3Client = buildLocalStackClient();
                String key = generateS3Key(binaryContentId);

                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentLength((long) bytes.length)
                    .build();

                s3Client.putObject(request, RequestBody.fromBytes(bytes));
                return binaryContentId;
            } catch (Exception e) {
                throw new RuntimeException("S3 업로드 실패", e);
            }
        }

        @Override
        public InputStream get(UUID binaryContentId) {
            try {
                S3Client s3Client = buildLocalStackClient();
                String key = generateS3Key(binaryContentId);

                GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();

                return s3Client.getObject(request);
            } catch (Exception e) {
                return new ByteArrayInputStream(new byte[0]);
            }
        }

        @Override
        public ResponseEntity<?> download(BinaryContentDto metaData) {
            try {
                String key = generateS3Key(metaData.id());
                String url = buildLocalStackPresignedUrl(key, metaData.contentType());

                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", url)
                    .build();
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }

        private String generateS3Key(UUID id) {
            String uuidStr = id.toString();
            return "binary-content/" + uuidStr.substring(0, 2)
                + "/" + uuidStr.substring(2, 4)
                + "/" + uuidStr;
        }
    }

    @Test
    void put_uploadsFileSuccessfully() {
        UUID id = UUID.randomUUID();
        byte[] data = "test content".getBytes();
        UUID result = storage.put(id, data);
        assertThat(result).isEqualTo(id);
    }

    @Test
    void get_returnsStoredFile() throws IOException {
        UUID id = UUID.randomUUID();
        byte[] data = "test content".getBytes();
        storage.put(id, data);

        InputStream result = storage.get(id);

        assertThat(result.readAllBytes()).isEqualTo(data);
    }

    @Test
    void download_returnsPresignedUrl() {
        UUID id = UUID.randomUUID();
        BinaryContentDto dto = new BinaryContentDto(id, "test.txt", 10L, "text/plain", BinaryContentStatus.SUCCESS);

        ResponseEntity<?> response = storage.download(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst("Location")).contains("binary-content");
    }

    @Test
    @DisplayName("put 호출 시 LocalStack 클라이언트 생성 실패 → RuntimeException")
    void put_whenClientFails_throws() {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        doThrow(SdkClientException.create("down")).when(spy).buildLocalStackClient();

        ThrowingCallable when = () -> spy.put(UUID.randomUUID(), "bytes".getBytes());

        assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("download 호출 시 Presigned URL helper 스텁")
    void download_returnsMetadata() {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        doReturn("http://example.com/test.txt").when(spy).buildLocalStackPresignedUrl(any(), any());

        BinaryContentDto dto = new BinaryContentDto(
            UUID.randomUUID(), "test.txt", 10L, "text/plain", BinaryContentStatus.SUCCESS);

        ResponseEntity<?> response = spy.download(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst("Location")).contains("test.txt");
    }

    @Test
    @DisplayName("get 호출 시 LocalStack 예외 → 빈 InputStream")
    void get_whenClientThrows_returnsEmptyStream() throws IOException {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        S3Client failingClient = mock(S3Client.class);
        doReturn(failingClient).when(spy).buildLocalStackClient();
        doThrow(SdkClientException.create("fail")).when(failingClient).getObject(any(GetObjectRequest.class));

        InputStream result = spy.get(UUID.randomUUID());

        assertThat(result.readAllBytes()).isEmpty();
    }

    @Test
    @DisplayName("download 호출 시 Presigned URL 생성 실패 → 404")
    void download_whenPresignedUrlFails_returns404() {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        doThrow(RuntimeException.class).when(spy).buildLocalStackPresignedUrl(any(), any());

        BinaryContentDto meta = new BinaryContentDto(
            UUID.randomUUID(), "file.txt", 10L, "text/plain", BinaryContentStatus.SUCCESS);

        ResponseEntity<?> response = spy.download(meta);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("S3Client 생성이 실패하면 put은 RuntimeException을 던진다")
    void put_s3ClientCreationFails() {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        doReturn(null).when(spy).buildLocalStackClient();

        BinaryContentDto dto = new BinaryContentDto(
            UUID.randomUUID(), "file.png", 128L, "image/png", BinaryContentStatus.SUCCESS);

        ThrowingCallable when = () -> spy.put(dto.id(), new byte[]{1, 2});

        assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("S3 다운로드 중 예외가 발생하면 빈 InputStream을 반환한다")
    void get_returnsEmptyStreamOnS3Exception() throws Exception {
        TestS3BinaryContentStorage spy = Mockito.spy(storage);
        S3Client failingClient = mock(S3Client.class);
        doReturn(failingClient).when(spy).buildLocalStackClient();
        doThrow(SdkClientException.create("boom")).when(failingClient).getObject(any(GetObjectRequest.class));

        InputStream result = spy.get(UUID.randomUUID());

        assertThat(result.readAllBytes()).isEmpty();
    }

    @Test
    @DisplayName("recoverPut은 관리자 알림을 저장하고 RuntimeException을 던진다")
    void recoverPut_sendsAdminNotification() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        S3BinaryContentStorage storage = new S3BinaryContentStorage(
            "access", "secret", "us-east-1", "bucket", 600, notificationRepository);
        ReflectionTestUtils.setField(storage, "adminUserId", UUID.randomUUID().toString());

        ThrowingCallable when = () -> storage.recoverPut(
            new RuntimeException("fail"), UUID.randomUUID(), "bytes".getBytes());

        assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
        then(notificationRepository).should().save(any(Notification.class));
    }

    @Test
    @DisplayName("S3Client가 putObject에서 예외를 던지면 RuntimeException을 전파한다")
    void put_originalClass_clientThrows() {
        // given: 실제 구현을 사용하지만 S3Client 빌더만 static mock
        S3BinaryContentStorage storage = new S3BinaryContentStorage(
            "access", "secret", "us-east-1", "bucket", 600, notificationRepository);

        S3Client mockClient = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> mocked = Mockito.mockStatic(S3Client.class)) {
            mocked.when(S3Client::builder).thenReturn(builder);
            given(builder.region(any(Region.class))).willReturn(builder);
            given(builder.credentialsProvider(any())).willReturn(builder);
            given(builder.build()).willReturn(mockClient);

            doThrow(SdkClientException.create("boom"))
                .when(mockClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));

            // when
            ThrowingCallable when = () -> storage.put(UUID.randomUUID(), "data".getBytes(
                StandardCharsets.UTF_8));

            // then
            assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("원본 S3BinaryContentStorage에서 put 중 S3Client가 예외를 던지면 RuntimeException이 발생한다")
    void put_originalClass_clientFails() {
        // given
        S3BinaryContentStorage storage = new S3BinaryContentStorage(
            "access", "secret", "us-east-1", "bucket", 600, notificationRepository);
        S3Client mockClient = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> mocked = Mockito.mockStatic(S3Client.class)) {
            mocked.when(S3Client::builder).thenReturn(builder);
            given(builder.region(any(Region.class))).willReturn(builder);
            given(builder.credentialsProvider(any())).willReturn(builder);
            given(builder.build()).willReturn(mockClient);
            doThrow(SdkClientException.create("boom"))
                .when(mockClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));

            // when
            ThrowingCallable when = () -> storage.put(UUID.randomUUID(), "data".getBytes());

            // then
            assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("원본 S3BinaryContentStorage에서 Presigned URL 생성 실패 시 download는 404를 반환한다")
    void download_originalClass_presignFails() {
        // given
        S3BinaryContentStorage storage = new S3BinaryContentStorage(
            "access", "secret", "us-east-1", "bucket", 600, notificationRepository);
        S3Presigner mockPresigner = mock(S3Presigner.class);
        S3Presigner.Builder builder = mock(S3Presigner.Builder.class);

        try (MockedStatic<S3Presigner> mocked = Mockito.mockStatic(S3Presigner.class)) {
            mocked.when(S3Presigner::builder).thenReturn(builder);
            given(builder.region(any(Region.class))).willReturn(builder);
            given(builder.credentialsProvider(any())).willReturn(builder);
            given(builder.build()).willReturn(mockPresigner);
            doThrow(RuntimeException.class)
                .when(mockPresigner).presignGetObject(any(GetObjectPresignRequest.class));

            BinaryContentDto dto = new BinaryContentDto(
                UUID.randomUUID(), "file.txt", 10L, "text/plain", BinaryContentStatus.SUCCESS);

            // when
            ResponseEntity<?> response = storage.download(dto);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
