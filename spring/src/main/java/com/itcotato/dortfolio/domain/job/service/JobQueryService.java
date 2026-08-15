package com.itcotato.dortfolio.domain.job.service;

import com.itcotato.dortfolio.domain.job.dto.JobCategoryResponse;
import com.itcotato.dortfolio.domain.job.dto.JobListResponse;
import com.itcotato.dortfolio.domain.job.dto.JobSummaryResponse;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCategory;
import com.itcotato.dortfolio.domain.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueryService {

    private final JobRepository jobRepository;

    public JobListResponse getJobs() {
        List<Job> jobs = jobRepository.findAllByOrderByCodeAsc();

        Map<String, JobCategory> categoriesByCode = Arrays.stream(JobCategory.values())
                .collect(Collectors.toMap(Enum::name, category -> category));

        Map<JobCategory, List<Job>> groupedJobs = jobs.stream()
                .filter(job -> {
                    boolean supported = categoriesByCode.containsKey(job.getCategoryCode());
                    if (!supported) {
                        log.warn(
                                "지원하지 않는 직무 대분류 코드입니다. jobCode={}, categoryCode={}",
                                job.getCode(),
                                job.getCategoryCode()
                        );
                    }
                    return supported;
                })
                .collect(Collectors.groupingBy(
                        job -> categoriesByCode.get(job.getCategoryCode())
                ));

        List<JobCategoryResponse> categories =
                Arrays.stream(JobCategory.values())
                        .map(category -> new JobCategoryResponse(
                                category.name(),
                                category.getDisplayName(),
                                groupedJobs.getOrDefault(
                                                category,
                                                List.of()
                                        ).stream()
                                        .map(JobSummaryResponse::from)
                                        .toList()
                        ))
                        .toList();

        return new JobListResponse(categories);
    }
}
