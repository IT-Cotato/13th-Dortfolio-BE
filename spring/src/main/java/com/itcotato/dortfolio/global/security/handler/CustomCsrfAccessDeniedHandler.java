package com.itcotato.dortfolio.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomCsrfAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public CustomCsrfAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}
