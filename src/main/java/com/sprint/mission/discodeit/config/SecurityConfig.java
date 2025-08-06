package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.security.DiscodeitUserDetailsService;
import com.sprint.mission.discodeit.security.LoginFailureHandler;
import com.sprint.mission.discodeit.security.LoginSuccessHandler;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final DiscodeitUserDetailsService discodeitUserDetailsService;
    private final DataSource dataSource;


    /**
     * 정적 리소스에 대한 Security 필터 체인을 완전히 우회
     * 성능 최적화를 위해 Security 처리 없이 바로 서빙
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
            // 정적 리소스 완전 무시 (Security 필터 체인 우회)
            .requestMatchers(
                "/favicon.ico",
                "/error",
                "/assets/**",
                "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.ico", "/*.svg",
                "/webjars/**"
            );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry,
        UserDetailsService userDetailsService) throws Exception{
        return http
            // CSRF 설정 - 쿠키 기반 CSRF 토큰 사용
            .csrf(csrf -> csrf
                // 쿠키 기반 CSRF 토큰 사용
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // CSRF 토큰 요청 처리 핸들러 설정
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/auth/login")
            )
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
            )
            .rememberMe(rememberMe -> rememberMe
                .key("discodeit-remember-me-key") // RememberMe 토큰 생성에 사용할 고유 키
                .tokenRepository(persistentTokenRepository()) // 토큰 저장소 설정
                .userDetailsService(userDetailsService) // 사용자 정보를 조회할 서비스
                .tokenValiditySeconds(60 * 60 * 24) // 토큰 유효시간
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("REMEMBER_ME")
                .alwaysRemember(false)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(management -> management
                .sessionConcurrency(concurrency -> concurrency
                    .maximumSessions(1) // 동일 계정으로 최대 1개의 세션만 허용
                    .maxSessionsPreventsLogin(true) // 새로운 로그인이 기존 세션을 만료시킨다.
                    .sessionRegistry(sessionRegistry)
                    .expiredUrl("/login?expired=true") // 세션 만료 시 리다이렉트할 URL
                )
            )
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 허용
                .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**").permitAll()
                // 인증하지 않을 요청들
                .requestMatchers("/api/auth/csrf-token").permitAll()  // CSRF 토큰 발급
                .requestMatchers("/api/users").permitAll()          // 회원가입
                .requestMatchers("/api/auth/login").permitAll()     // 로그인
                .requestMatchers("/api/auth/logout").permitAll()    // 로그아웃
                // API가 아닌 요청 (Swagger, Actuator 등)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // 모든 요청을 인증하도록 설정
                .anyRequest().authenticated()
            )
            // 예외 처리 설정
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"인증이 필요합니다.\",\"status\":401}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"접근 권한이 없습니다.\",\"status\":403}");
                })
            )
            .build();
    }

    /**
     * RememberMe 토큰을 데이터베이스에 저장하기 위한 Repository 설정
     * */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    /**
     * SessionRegistry Bean - 세션 관리를 위해 추가
     * */
    @Bean
    public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    /**
     * HttpSessionEvenPublisher Bean - 세션 이벤트 처리를 위해 추가
     * */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * 권한 계층 구조 정의
     * ADMIN > CHANNEL_MANAGER > USER
     * */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(
            "ROLE_ADMIN > ROLE_CHANNEL_MANAGER\n" +
                "ROLE_CHANNEL_MANAGER > ROLE_USER"
        );
        return roleHierarchy;
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     * BCrypt 알고리즘을 사용하여 비밀번호를 해시화
     * */
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /**
     * 메서드 보안에서 권한 계층 구조를 사용하도록 설정
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
        RoleHierarchy roleHierarchy
    ) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}