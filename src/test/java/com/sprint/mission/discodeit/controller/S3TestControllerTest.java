package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.then;

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
}
