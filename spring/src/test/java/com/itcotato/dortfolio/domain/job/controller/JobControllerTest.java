package com.itcotato.dortfolio.domain.job.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itcotato.dortfolio.domain.job.dto.JobCategoryResponse;
import com.itcotato.dortfolio.domain.job.dto.JobListResponse;
import com.itcotato.dortfolio.domain.job.dto.JobSummaryResponse;
import com.itcotato.dortfolio.domain.job.service.JobQueryService;
import com.itcotato.dortfolio.global.config.SecurityConfig;
import com.itcotato.dortfolio.global.security.handler.CustomAuthenticationEntryPoint;
import com.itcotato.dortfolio.global.security.jwt.JwtTokenProvider;
import com.itcotato.dortfolio.global.security.oauth.CustomOAuth2UserService;
import com.itcotato.dortfolio.global.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.itcotato.dortfolio.global.security.oauth.OAuth2FailureHandler;
import com.itcotato.dortfolio.global.security.oauth.OAuth2SuccessHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = JobController.class,
        properties = "app.cors.allowed-origins=http://localhost"
)
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class
})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobQueryService jobQueryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotRejectUnsafeRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/jobs").with(user("user")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void returnsGroupedJobCatalogForAuthenticatedUser() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobListResponse expected = new JobListResponse(List.of(
                new JobCategoryResponse(
                        "IT_DEVELOPMENT",
                        "IT/개발",
                        List.of(new JobSummaryResponse(
                                jobId,
                                "JOB_013",
                                "백엔드/서버 개발"
                        ))
                )
        ));
        when(jobQueryService.getJobs()).thenReturn(expected);

        mockMvc.perform(get("/api/jobs").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("직무 목록 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.categories[0].code").value("IT_DEVELOPMENT"))
                .andExpect(jsonPath("$.data.categories[0].jobs[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$.data.categories[0].jobs[0].code").value("JOB_013"));

        verify(jobQueryService).getJobs();
    }
}
