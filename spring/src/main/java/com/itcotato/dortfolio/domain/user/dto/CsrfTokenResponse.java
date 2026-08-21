package com.itcotato.dortfolio.domain.user.dto;

public record CsrfTokenResponse(String csrfToken) {

    public static CsrfTokenResponse of(String csrfToken) {
        return new CsrfTokenResponse(csrfToken);
    }
}
