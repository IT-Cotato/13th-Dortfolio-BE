package com.itcotato.dortfolio.global.config;

import com.itcotato.dortfolio.global.security.handler.CustomAuthenticationEntryPoint;
import com.itcotato.dortfolio.global.security.jwt.JwtAuthenticationFilter;
import com.itcotato.dortfolio.global.security.jwt.JwtTokenProvider;
import com.itcotato.dortfolio.global.security.oauth.CustomOAuth2UserService;
import com.itcotato.dortfolio.global.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.itcotato.dortfolio.global.security.oauth.OAuth2FailureHandler;
import com.itcotato.dortfolio.global.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(SecuritySupportConfig.class)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // 허용할 프론트엔드 출처. 배포 환경마다 달라서 설정으로 뺀다
    // (기본값은 기존과 동일한 로컬 개발 주소라, 설정을 안 해도 동작이 바뀌지 않는다)
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    // 비밀번호 재설정은 로그인하지 못하는 사용자가 쓰는 기능이라 인증을 요구할 수 없다.
    // 경로는 AuthController의 매핑과 정확히 일치해야 한다
    private static final String[] PUBLIC_AUTH_PATHS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/reset-request",
            "/api/auth/reset"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(new CookieAuthenticatedCsrfProtectionMatcher())
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        // 컨테이너/로드밸런서 헬스체크용. 상세 정보는 노출하지 않는다(management.endpoint.health.show-details=never)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
