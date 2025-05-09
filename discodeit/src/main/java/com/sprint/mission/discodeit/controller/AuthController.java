package com.sprint.mission.discodeit.controller;


import com.sprint.mission.discodeit.dto.user.LoginRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
        private final AuthService authService;

        @RequestMapping(value = "/login", method = RequestMethod.POST)
        public ResponseEntity<String> login(@RequestBody LoginRequest request) {
            User login = authService.login(request);
            return ResponseEntity.ok("사용자 ID : " + login.getId() + " 인 유저가 로그인하였습니다." );

        }

}
