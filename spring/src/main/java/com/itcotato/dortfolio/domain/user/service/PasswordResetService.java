package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.domain.user.dto.PasswordResetRequest;
import com.itcotato.dortfolio.domain.user.dto.PasswordResetConfirmRequest;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.GlobalErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import com.itcotato.dortfolio.global.util.RedisUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private final UserRepository userRepository;
    private final RedisUtil redisUtil;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final long TOKEN_EXPIRATION_MINUTES = 3L;

    /* 비밀번호 재설정 링크 이메일 발송 요청 로직 */
    @Transactional
    public void sendResetLink(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != null && !user.getProvider().equals("LOCAL")) {
            throw new CustomException(UserErrorCode.SOCIAL_USER_PASSWORD_RESET_NOT_ALLOWED);
        }

        String token = UUID.randomUUID().toString();
        redisUtil.setDataExpire(token, user.getEmail(), TOKEN_EXPIRATION_MINUTES * 60 * 1000);
        sendEmail(user.getEmail(), token);
    }

    /* 토큰 및 이메일 교차 검증 후 비밀번호 최종 변경 로직 */
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        String redisEmail = redisUtil.getData(request.token());

        if (redisEmail == null) {
            throw new CustomException(UserErrorCode.INVALID_RESET_TOKEN);
        }

        if (!redisEmail.equalsIgnoreCase(request.email())) {
            throw new CustomException(UserErrorCode.RESET_EMAIL_MISMATCH);
        }

        User user = userRepository.findByEmail(redisEmail)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedPassword);

        redisUtil.deleteData(request.token());
    }

    /* Thymeleaf 템플릿 파일 기반 메일 발송 이너 메서드 */
    private void sendEmail(String toEmail, String token) {

        String encodedEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String resetLink = frontendUrl + "/reset-password?token=" + token + "&email=" + encodedEmail;

        Context context = new Context();
        context.setVariable("resetLink", resetLink);

        String htmlContent = templateEngine.process("mail/password-reset", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[도트폴리오] 비밀번호 재설정 요청 링크입니다.");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
