package com.itcotato.dortfolio.domain.job.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobCategory {

    PLANNING_MANAGEMENT("기획/경영"),
    MARKETING_ADVERTISING("마케팅/광고"),
    IT_DEVELOPMENT("IT/개발"),
    DESIGN("디자인"),
    SALES_CS("영업/CS"),
    PRODUCTION_MANUFACTURING("생산/제조"),
    RESEARCH_RND("연구/R&D"),
    FINANCE("금융"),
    MEDIA_CONTENT("미디어/콘텐츠"),
    LOGISTICS_DISTRIBUTION("물류/유통");

    private final String displayName;
}
