package com.sprint.mission.discodeit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.exception.auth.InvalidPasswordException;
import com.sprint.mission.discodeit.exception.auth.InvalidRefreshTokenException;
import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import java.nio.file.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

@DisplayName("GlobalExceptionHandler 단위 테스트")
public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("UserNotFoundException은 404를 반환한다")
    void handleUserNotFound() {
        // when
        ResponseEntity<ErrorResponse> response =
            handler.handleException(new UserNotFoundException());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("BinaryContentNotFoundException은 404를 반환한다")
    void handleBinaryContentNotFound() {
        // when
        ResponseEntity<ErrorResponse> response =
            handler.handleException(new BinaryContentNotFoundException());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("AuthException은 401을 반환한다")
    void handleAuthException() {
        // when
        ResponseEntity<ErrorResponse> response =
            handler.handleException(new InvalidPasswordException());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("RefreshToken 관련 예외는 401을 반환한다")
    void handleRefreshTokenException() {
        // when
        ResponseEntity<ErrorResponse> response =
            handler.handleRefreshTokenException(new InvalidRefreshTokenException());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AccessDeniedException은 403을 반환한다")
    void handleAccessDeniedException() throws Exception {
        // when
        ResponseEntity<ErrorResponse> response =
            handler.handleException(new AccessDeniedException("denied"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("AuthorizationDeniedException은 403을 반환한다")
    void handleAuthorizationDenied() {
        // given
        AuthorizationDeniedException ex =
            new AuthorizationDeniedException("denied", new AuthorizationDecision(false));

        // when
        ResponseEntity<?> response = handler.handleAuthorizationDenied(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("검증 실패 예외는 400을 반환한다")
    void handleValidationException() throws Exception {
        // given
        MethodParameter param = new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class), 0);

        class Dummy {

            String field;
        }

        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(new Dummy(), "request");
        bindingResult.addError(new FieldError("request", "field", "invalid"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param,
            bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void sample(String value) {}
}
