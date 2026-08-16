package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.global.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;

class NonWebSecurityConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            SecuritySupportConfig.class,
                            SecurityConfig.class,
                            CustomAuthenticationEntryPoint.class
                    );

    @Test
    void loadsSharedSecurityBeansWithoutServletSecurityBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PasswordEncoder.class);
            assertThat(context).hasSingleBean(CsrfTokenRepository.class);
            assertThat(context).doesNotHaveBean(SecurityConfig.class);
            assertThat(context)
                    .doesNotHaveBean(CustomAuthenticationEntryPoint.class);
        });
    }
}
