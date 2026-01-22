package com.sprint.mission.discodeit.storage.s3;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.discodeit.config.AWSS3Properties;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@DisplayName("AWS S3 Test 단위 테스트")
public class AWSS3TestTest {

    private AWSS3Test createWithMocks(S3Client s3Client, S3Presigner presigner) {
        AWSS3Properties props = new AWSS3Properties();
        props.setAccessKey("test");
        props.setSecretKey("test");
        props.setRegion("us-east-1");
        props.setBucket("bucket");

        AWSS3Test target = new AWSS3Test(props);
        ReflectionTestUtils.setField(target, "s3Client", s3Client);
        ReflectionTestUtils.setField(target, "s3Presigner", presigner);
        return target;
    }

    @Test
    @DisplayName("testUpload는 S3 putObject를 호출한다")
    void testUpload_usesS3Client() {
        // given
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().eTag("etag").build());
        AWSS3Test target = createWithMocks(s3Client, mock(S3Presigner.class));

        // when
        assertThatNoException().isThrownBy(target::testUpload);

        // then
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    @DisplayName("testDownload는 S3 getObject를 호출한다")
    void testDownload_readsContent() throws Exception {
        // given
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(response.readAllBytes()).thenReturn(data);

        S3Client s3Client = mock(S3Client.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        AWSS3Test target = createWithMocks(s3Client, mock(S3Presigner.class));

        // when
        assertThatNoException().isThrownBy(target::testDownload);

        // then
        verify(s3Client).getObject(any(GetObjectRequest.class));
        verify(response).readAllBytes();
    }

    @Test
    @DisplayName("testPresignedUrl은 presigner를 호출한다")
    void testPresignedUrl_usesPresigner() throws Exception {
        // given
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("http://example.com"));

        S3Presigner presigner = mock(S3Presigner.class);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        AWSS3Test target = createWithMocks(mock(S3Client.class), presigner);

        // when
        assertThatNoException().isThrownBy(target::testPresignedUrl);

        // then
        verify(presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("testImageUpload는 리소스가 없어도 예외 없이 종료된다")
    void testImageUpload_handlesMissingResource() {
        // given
        AWSS3Test target = createWithMocks(mock(S3Client.class), mock(S3Presigner.class));

        // when
        assertThatNoException().isThrownBy(target::testImageUpload);

        // then
    }

    @Test
    @DisplayName("testImageDownload는 S3 getObject를 호출한다")
    void testImageDownload_readsImage() throws Exception {
        // given
        byte[] data = "img".getBytes(StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(response.readAllBytes()).thenReturn(data);
        when(response.response()).thenReturn(GetObjectResponse.builder().contentType("image/jpeg").build());

        S3Client s3Client = mock(S3Client.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        AWSS3Test target = createWithMocks(s3Client, mock(S3Presigner.class));

        // when
        assertThatNoException().isThrownBy(target::testImageDownload);

        // then
        verify(s3Client).getObject(any(GetObjectRequest.class));
        verify(response).readAllBytes();
        verify(response).response();
    }

    @Test
    @DisplayName("testImagePresignedUrl은 presigner를 호출한다")
    void testImagePresignedUrl_usesPresigner() throws Exception {
        // given
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("http://example.com/img"));

        S3Presigner presigner = mock(S3Presigner.class);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        AWSS3Test target = createWithMocks(mock(S3Client.class), presigner);

        // when
        assertThatNoException().isThrownBy(target::testImagePresignedUrl);

        // then
        verify(presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("runImageTests는 내부 메서드를 호출한다")
    void runImageTests_invokesImageFlows() {
        // given
        AWSS3Test target = spy(createWithMocks(mock(S3Client.class), mock(S3Presigner.class)));

        // when
        assertThatNoException().isThrownBy(target::runImageTests);

        // then
        verify(target).testImageUpload();
        verify(target).testImageDownload();
        verify(target).testImagePresignedUrl();
    }

    @Test
    @DisplayName("runAllTests는 전체 테스트 메서드를 호출한다")
    void runAllTests_invokesAllFlows() {
        // given
        AWSS3Test target = spy(createWithMocks(mock(S3Client.class), mock(S3Presigner.class)));

        // when
        assertThatNoException().isThrownBy(target::runAllTests);

        // then
        verify(target).testUpload();
        verify(target).testDownload();
        verify(target).testPresignedUrl();
        verify(target).testImageUpload();
        verify(target).testImageDownload();
        verify(target).testImagePresignedUrl();
    }
}

