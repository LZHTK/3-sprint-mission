package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.AuthApi;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;

  /**
   * CSRF 토큰 발급 API
   * <p>
   * 프론트엔드에서 CSRF 토큰을 받아올 수 있도록 제공하는 엔드포인트.
   * Spring Security가 자동으로 CsrfToken 객체를 주입해준다.
   *
   * @param csrfToken Spring Security가 자동 주입하는 CSRF 토큰
   * @return CSRF 토큰 정보
   * </p>*/
  @GetMapping("csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    String tokenValue = csrfToken.getToken();
    log.debug("CSRF 토큰 요청 : {}", tokenValue);

    // GET 요청에는 CSRF 인증이 이루어지지 않기 때문에 토큰이 초기화 되지 않는다.
    // 따라서 명시적으로 메소드에서 토큰을 호출한다.
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT) // 상태 코드 203
        .build();
  }

  /**
   * 현재 사용자 정보 조회 API
   *
   * @param userDetails 인증된 사용자 정보
   * @return 사용자 정보 DTO
   */
  @Override
  @GetMapping("/me")
  public ResponseEntity<UserDto> getCurrentUser(
      @AuthenticationPrincipal DiscodeitUserDetails userDetails) {

    log.debug("현재 사용자 정보 조회 요청 : {} ", userDetails.getUsername());

    // DiscodeitUserDetials에서 UserDto 추출
    UserDto currentUser = userDetails.getUserDto();

    log.debug("현재 사용자 정보 조회 완료 : {} ", currentUser.username());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(currentUser);
  }
}