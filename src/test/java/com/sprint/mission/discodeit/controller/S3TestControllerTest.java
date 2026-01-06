package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.sprint.mission.discodeit.storage.s3.AWSS3Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class S3TestControllerTest {

    @Mock private AWSS3Test awsS3Test;

    private S3TestController controller;

    private void prepareController() {
        controller = new S3TestController(awsS3Test);
    }

    @Test
    @DisplayName("전체 S3 점검이 성공하면 200 OK를 반환한다")
    void testAll_shouldReturnOkWhenChecksPass() {
        // given
        prepareController();
        doNothing().when(awsS3Test).runAllTests();

        // when
        ResponseEntity<String> response = controller.testAll();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotBlank();
        then(awsS3Test).should().runAllTests();
    }

    @Test
    @DisplayName("전체 S3 점검 중 오류가 있으면 500을 반환한다")
    void testAll_shouldPropagateFailure() {
        // given
        prepareController();
        doThrow(new IllegalStateException("boom")).when(awsS3Test).runAllTests();

        // when
        ResponseEntity<String> response = controller.testAll();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("boom");
    }

    @Test
    @DisplayName("업로드 테스트는 성공 시 200 OK를 내려준다")
    void testUpload_shouldReturnOk() {
        // given
        prepareController();
        doNothing().when(awsS3Test).testUpload();

        // when
        ResponseEntity<String> response = controller.testUpload();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotBlank();
        then(awsS3Test).should().testUpload();
    }

    @Test
    @DisplayName("업로드 테스트 중 예외는 500과 메시지를 반영한다")
    void testUpload_shouldReturnServerErrorOnFailure() {
        // given
        prepareController();
        doThrow(new RuntimeException("network")).when(awsS3Test).testUpload();

        // when
        ResponseEntity<String> response = controller.testUpload();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("network");
    }

    @Test
    @DisplayName("전체 S3 점검 성공 시 200, 실패 시 500을 반환한다")
    void testAll_shouldReturnExpectedStatuses() {
        // given
        prepareController();
        doNothing().when(awsS3Test).runAllTests();
        assertThat(controller.testAll().getStatusCode()).isEqualTo(HttpStatus.OK);
        then(awsS3Test).should(times(1)).runAllTests();

        doThrow(new IllegalStateException("boom")).when(awsS3Test).runAllTests();

        // when
        ResponseEntity<String> failure = controller.testAll();

        // then
        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(failure.getBody()).contains("boom");
    }

    @Test
    @DisplayName("텍스트 업/다운로드 및 Presigned 테스트의 성공/실패 흐름을 검증한다")
    void textEndpoints_shouldCoverBothBranches() {
        prepareController();

        // given – 성공 흐름
        doNothing().when(awsS3Test).testUpload();
        doNothing().when(awsS3Test).testDownload();
        doNothing().when(awsS3Test).testPresignedUrl();

        // when – 성공 흐름
        ResponseEntity<String> uploadSuccess   = controller.testUpload();
        ResponseEntity<String> downloadSuccess = controller.testDownload();
        ResponseEntity<String> presignedSuccess = controller.testPresigned();

        // then – 성공 흐름
        assertThat(uploadSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presignedSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);

        // given – 실패 흐름
        doThrow(new RuntimeException("upload-error")).when(awsS3Test).testUpload();
        doThrow(new RuntimeException("download-error")).when(awsS3Test).testDownload();
        doThrow(new RuntimeException("presigned-error")).when(awsS3Test).testPresignedUrl();

        // when – 실패 흐름
        ResponseEntity<String> uploadFailure   = controller.testUpload();
        ResponseEntity<String> downloadFailure = controller.testDownload();
        ResponseEntity<String> presignedFailure = controller.testPresigned();

        // then – 실패 흐름
        assertThat(uploadFailure.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(downloadFailure.getBody()).contains("download-error");
        assertThat(presignedFailure.getBody()).contains("presigned-error");
    }

    @Test
    @DisplayName("이미지 전용 엔드포인트도 성공/실패를 모두 처리한다")
    void imageEndpoints_shouldCoverSuccessAndFailure() {
        prepareController();

        // given – 성공 흐름
        doNothing().when(awsS3Test).runImageTests();
        doNothing().when(awsS3Test).testImageUpload();
        doNothing().when(awsS3Test).testImageDownload();
        doNothing().when(awsS3Test).testImagePresignedUrl();

        // when – 성공 흐름
        ResponseEntity<String> allSuccess        = controller.testImageAll();
        ResponseEntity<String> uploadSuccess     = controller.testImageUpload();
        ResponseEntity<String> downloadSuccess   = controller.testImageDownload();
        ResponseEntity<String> presignedSuccess  = controller.testImagePresigned();

        // then – 성공 흐름
        assertThat(allSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presignedSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);

        // given – 실패 흐름
        doThrow(new RuntimeException("image-all")).when(awsS3Test).runImageTests();
        doThrow(new RuntimeException("image-upload")).when(awsS3Test).testImageUpload();
        doThrow(new RuntimeException("image-download")).when(awsS3Test).testImageDownload();
        doThrow(new RuntimeException("image-presigned")).when(awsS3Test).testImagePresignedUrl();

        // when – 실패 흐름
        ResponseEntity<String> allFailure        = controller.testImageAll();
        ResponseEntity<String> uploadFailure     = controller.testImageUpload();
        ResponseEntity<String> downloadFailure   = controller.testImageDownload();
        ResponseEntity<String> presignedFailure  = controller.testImagePresigned();

        // then – 실패 흐름
        assertThat(allFailure.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(uploadFailure.getBody()).contains("image-upload");
        assertThat(downloadFailure.getBody()).contains("image-download");
        assertThat(presignedFailure.getBody()).contains("image-presigned");
    }
}
