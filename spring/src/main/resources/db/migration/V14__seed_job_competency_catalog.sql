-- 직무·핵심 역량 기준 데이터 스키마 및 seed
-- 기존 운영 데이터의 UUID 참조를 보존하기 위해 이름으로 backfill한 뒤 누락 데이터만 삽입한다.

ALTER TABLE jobs ADD COLUMN code varchar(20);
ALTER TABLE jobs ADD COLUMN category_code varchar(50);
ALTER TABLE competency_tags ADD COLUMN code varchar(20);

CREATE TEMPORARY TABLE job_competency_seed (
    category_code varchar(50) NOT NULL,
    job_code varchar(20) NOT NULL,
    job_name varchar(255) NOT NULL,
    competency_code varchar(20) NOT NULL,
    competency_name varchar(255) NOT NULL,
    competency_description varchar(255) NOT NULL,
    sort_order integer NOT NULL
) ON COMMIT DROP;

INSERT INTO job_competency_seed (
    category_code, job_code, job_name,
    competency_code, competency_name, competency_description, sort_order
) VALUES
    ('PLANNING_MANAGEMENT', 'JOB_001', '경영·사업기획', 'COMP_001', '사업 전략 수립', '사업 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('PLANNING_MANAGEMENT', 'JOB_001', '경영·사업기획', 'COMP_002', '시장·경쟁사 분석', '시장·경쟁사 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('PLANNING_MANAGEMENT', 'JOB_001', '경영·사업기획', 'COMP_003', '사업성 검증', '사업성 검증의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 3),
    ('PLANNING_MANAGEMENT', 'JOB_001', '경영·사업기획', 'COMP_004', 'KPI·성과관리', 'KPI·성과관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('PLANNING_MANAGEMENT', 'JOB_001', '경영·사업기획', 'COMP_005', '손익(P&L) 관리', '손익(P&L) 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('PLANNING_MANAGEMENT', 'JOB_002', '인사/채용/HRD', 'COMP_006', '채용 전략 수립', '채용 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('PLANNING_MANAGEMENT', 'JOB_002', '인사/채용/HRD', 'COMP_007', 'HR 데이터 분석', 'HR 데이터 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('PLANNING_MANAGEMENT', 'JOB_002', '인사/채용/HRD', 'COMP_008', '조직문화 기획', '조직문화 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 3),
    ('PLANNING_MANAGEMENT', 'JOB_002', '인사/채용/HRD', 'COMP_009', '교육 프로그램 설계', '교육 프로그램 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 4),
    ('PLANNING_MANAGEMENT', 'JOB_002', '인사/채용/HRD', 'COMP_010', '이해관계자 커뮤니케이션', '이해관계자 커뮤니케이션을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 5),
    ('PLANNING_MANAGEMENT', 'JOB_003', '총무/법무', 'COMP_011', '리스크 관리', '리스크 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 1),
    ('PLANNING_MANAGEMENT', 'JOB_003', '총무/법무', 'COMP_012', '계약·법률 문서 이해', '계약·법률 문서 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 2),
    ('PLANNING_MANAGEMENT', 'JOB_003', '총무/법무', 'COMP_013', '컴플라이언스 관리', '컴플라이언스 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 3),
    ('PLANNING_MANAGEMENT', 'JOB_003', '총무/법무', 'COMP_014', '협상 및 조율', '협상 및 조율을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 4),
    ('PLANNING_MANAGEMENT', 'JOB_003', '총무/법무', 'COMP_015', '운영 프로세스 관리', '운영 프로세스 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('PLANNING_MANAGEMENT', 'JOB_004', '재무/회계/IR', 'COMP_016', '재무 분석', '재무 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('PLANNING_MANAGEMENT', 'JOB_004', '재무/회계/IR', 'COMP_017', '예산 관리', '예산 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('PLANNING_MANAGEMENT', 'JOB_004', '재무/회계/IR', 'COMP_018', '자금 운용', '자금 운용 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('PLANNING_MANAGEMENT', 'JOB_004', '재무/회계/IR', 'COMP_019', '회계·세무 이해', '회계·세무 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 4),
    ('PLANNING_MANAGEMENT', 'JOB_004', '재무/회계/IR', 'COMP_020', '투자자 커뮤니케이션(IR)', '투자자 커뮤니케이션(IR)를 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 5),
    ('PLANNING_MANAGEMENT', 'JOB_005', '경영컨설팅·분석', 'COMP_021', '문제 정의', '문제 정의 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('PLANNING_MANAGEMENT', 'JOB_005', '경영컨설팅·분석', 'COMP_022', '가설 수립 및 검증', '가설 수립 및 검증의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 2),
    ('PLANNING_MANAGEMENT', 'JOB_005', '경영컨설팅·분석', 'COMP_023', '데이터 기반 분석', '데이터 기반 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('PLANNING_MANAGEMENT', 'JOB_005', '경영컨설팅·분석', 'COMP_024', '전략 수립', '전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 4),
    ('PLANNING_MANAGEMENT', 'JOB_005', '경영컨설팅·분석', 'COMP_025', '논리적 사고', '논리적 사고 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_006', '마케팅 기획/전략', 'COMP_026', '고객 인사이트 도출', '고객 인사이트 도출을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_006', '마케팅 기획/전략', 'COMP_027', '시장 분석', '시장 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_006', '마케팅 기획/전략', 'COMP_028', '캠페인 기획', '캠페인 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_006', '마케팅 기획/전략', 'COMP_029', '브랜드 전략 수립', '브랜드 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_006', '마케팅 기획/전략', 'COMP_030', '성과 분석(KPI)', '성과 분석(KPI)를 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_007', '브랜드 마케팅', 'COMP_031', '브랜드 포지셔닝', '브랜드 포지셔닝 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_007', '브랜드 마케팅', 'COMP_032', '소비자 인사이트 분석', '소비자 인사이트 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_007', '브랜드 마케팅', 'COMP_033', '브랜드 스토리텔링', '브랜드 스토리텔링을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_007', '브랜드 마케팅', 'COMP_034', '브랜드 경험 설계', '브랜드 경험 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_007', '브랜드 마케팅', 'COMP_035', 'IMC 기획', 'IMC 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_008', '퍼포먼스/디지털 마케팅', 'COMP_036', '데이터 분석(GA4 등)', '데이터 분석(GA4 등)를 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_008', '퍼포먼스/디지털 마케팅', 'COMP_037', '광고 성과 최적화', '광고 성과 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_008', '퍼포먼스/디지털 마케팅', 'COMP_038', 'A/B 테스트 설계', 'A/B 테스트 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_008', '퍼포먼스/디지털 마케팅', 'COMP_039', '고객 퍼널 분석', '고객 퍼널 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_008', '퍼포먼스/디지털 마케팅', 'COMP_040', '예산 및 ROAS 관리', '예산 및 ROAS 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_009', 'SNS/콘텐츠 마케팅', 'COMP_041', '콘텐츠 기획', '콘텐츠 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_009', 'SNS/콘텐츠 마케팅', 'COMP_042', '카피라이팅', '카피라이팅 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_009', 'SNS/콘텐츠 마케팅', 'COMP_043', '채널 운영 전략', '채널 운영 전략의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_009', 'SNS/콘텐츠 마케팅', 'COMP_044', '콘텐츠 성과 분석', '콘텐츠 성과 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_009', 'SNS/콘텐츠 마케팅', 'COMP_045', '커뮤니티 운영', '커뮤니티 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_010', '광고기획/AE', 'COMP_046', '광고 전략 기획', '광고 전략 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_010', '광고기획/AE', 'COMP_047', '프로젝트 관리', '프로젝트 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_010', '광고기획/AE', 'COMP_048', '클라이언트 커뮤니케이션', '클라이언트 커뮤니케이션을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_010', '광고기획/AE', 'COMP_049', '제안서 작성', '제안서 작성을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_010', '광고기획/AE', 'COMP_050', '매체 전략 수립', '매체 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 5),
    ('MARKETING_ADVERTISING', 'JOB_011', 'PR/홍보', 'COMP_051', '미디어 커뮤니케이션', '미디어 커뮤니케이션을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 1),
    ('MARKETING_ADVERTISING', 'JOB_011', 'PR/홍보', 'COMP_052', '보도자료 작성', '보도자료 작성을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 2),
    ('MARKETING_ADVERTISING', 'JOB_011', 'PR/홍보', 'COMP_053', '이슈 및 위기 대응', '이슈 및 위기 대응을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 3),
    ('MARKETING_ADVERTISING', 'JOB_011', 'PR/홍보', 'COMP_054', '대외 협력', '대외 협력 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('MARKETING_ADVERTISING', 'JOB_011', 'PR/홍보', 'COMP_055', '기업 이미지 관리', '기업 이미지 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_012', '프론트엔드 개발', 'COMP_056', 'UI 구현', 'UI 구현에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_012', '프론트엔드 개발', 'COMP_057', '상태 관리', '상태 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_012', '프론트엔드 개발', 'COMP_058', '웹 성능 최적화', '웹 성능 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_012', '프론트엔드 개발', 'COMP_059', '반응형 웹 개발', '반응형 웹 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_012', '프론트엔드 개발', 'COMP_060', '협업 및 코드 관리', '협업 및 코드 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_013', '백엔드/서버 개발', 'COMP_061', 'API 설계', 'API 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_013', '백엔드/서버 개발', 'COMP_062', '데이터베이스 설계', '데이터베이스 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_013', '백엔드/서버 개발', 'COMP_063', '서버 아키텍처 설계', '서버 아키텍처 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_013', '백엔드/서버 개발', 'COMP_064', '성능 최적화', '성능 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_013', '백엔드/서버 개발', 'COMP_065', '안정성 및 유지보수', '안정성 및 유지보수 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_014', '앱 개발', 'COMP_066', '모바일 앱 개발', '모바일 앱 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_014', '앱 개발', 'COMP_067', '앱 성능 최적화', '앱 성능 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_014', '앱 개발', 'COMP_068', '플랫폼 이해', '플랫폼 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_014', '앱 개발', 'COMP_069', '사용자 경험 구현', '사용자 경험 구현에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_014', '앱 개발', 'COMP_070', '배포 및 유지보수', '배포 및 유지보수 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_015', 'IT기획/PM/PO', 'COMP_071', '사용자 문제 정의', '사용자 문제 정의 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_015', 'IT기획/PM/PO', 'COMP_072', '요구사항 분석', '요구사항 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_015', 'IT기획/PM/PO', 'COMP_073', '서비스 기획', '서비스 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_015', 'IT기획/PM/PO', 'COMP_047', '프로젝트 관리', '프로젝트 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_015', 'IT기획/PM/PO', 'COMP_074', '데이터 기반 의사결정', '데이터 기반 의사결정 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_016', '데이터 분석/엔지니어링', 'COMP_075', '데이터 분석', '데이터 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_016', '데이터 분석/엔지니어링', 'COMP_076', '데이터 모델링', '데이터 모델링의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_016', '데이터 분석/엔지니어링', 'COMP_077', '데이터 시각화', '데이터 시각화 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_016', '데이터 분석/엔지니어링', 'COMP_078', '실험 설계', '실험 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_016', '데이터 분석/엔지니어링', 'COMP_079', '지표 설계 및 관리', '지표 설계 및 관리의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_017', 'AI/ML 엔지니어', 'COMP_080', '데이터 전처리', '데이터 전처리 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_017', 'AI/ML 엔지니어', 'COMP_081', '모델 설계', '모델 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_017', 'AI/ML 엔지니어', 'COMP_082', '모델 학습 및 평가', '모델 학습 및 평가의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_017', 'AI/ML 엔지니어', 'COMP_083', '모델 배포', '모델 배포 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_017', 'AI/ML 엔지니어', 'COMP_084', 'LLM 활용 및 최적화', 'LLM 활용 및 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_018', 'DevOps/클라우드/보안', 'COMP_085', '클라우드 인프라 운영', '클라우드 인프라 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_018', 'DevOps/클라우드/보안', 'COMP_086', 'CI/CD 구축', 'CI/CD 구축에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_018', 'DevOps/클라우드/보안', 'COMP_087', '인프라 자동화', '인프라 자동화 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_018', 'DevOps/클라우드/보안', 'COMP_088', '시스템 모니터링', '시스템 모니터링 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_018', 'DevOps/클라우드/보안', 'COMP_089', '보안 및 취약점 관리', '보안 및 취약점 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_019', 'QA/SW 테스트', 'COMP_090', '테스트 설계', '테스트 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_019', 'QA/SW 테스트', 'COMP_091', '품질 관리', '품질 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_019', 'QA/SW 테스트', 'COMP_092', '요구사항 검증', '요구사항 검증의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_019', 'QA/SW 테스트', 'COMP_093', '버그 분석', '버그 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_019', 'QA/SW 테스트', 'COMP_094', '자동화 테스트', '자동화 테스트의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 5),
    ('IT_DEVELOPMENT', 'JOB_020', '게임 개발/기획', 'COMP_095', '게임 시스템 설계', '게임 시스템 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('IT_DEVELOPMENT', 'JOB_020', '게임 개발/기획', 'COMP_096', '밸런스 기획', '밸런스 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 2),
    ('IT_DEVELOPMENT', 'JOB_020', '게임 개발/기획', 'COMP_097', '사용자 경험 분석', '사용자 경험 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('IT_DEVELOPMENT', 'JOB_020', '게임 개발/기획', 'COMP_041', '콘텐츠 기획', '콘텐츠 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 4),
    ('IT_DEVELOPMENT', 'JOB_020', '게임 개발/기획', 'COMP_064', '성능 최적화', '성능 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 5),
    ('DESIGN', 'JOB_021', 'UI/UX 디자인', 'COMP_098', '사용자 리서치', '사용자 리서치 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('DESIGN', 'JOB_021', 'UI/UX 디자인', 'COMP_099', '사용자 경험 설계', '사용자 경험 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('DESIGN', 'JOB_021', 'UI/UX 디자인', 'COMP_100', '와이어프레임·프로토타이핑', '와이어프레임·프로토타이핑 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('DESIGN', 'JOB_021', 'UI/UX 디자인', 'COMP_101', '사용성 테스트', '사용성 테스트의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 4),
    ('DESIGN', 'JOB_021', 'UI/UX 디자인', 'COMP_102', '디자인 시스템 구축', '디자인 시스템 구축에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 5),
    ('DESIGN', 'JOB_022', '그래픽/시각 디자인', 'COMP_103', '비주얼 디자인', '비주얼 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('DESIGN', 'JOB_022', '그래픽/시각 디자인', 'COMP_104', '레이아웃 구성', '레이아웃 구성 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('DESIGN', 'JOB_022', '그래픽/시각 디자인', 'COMP_105', '타이포그래피', '타이포그래피 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('DESIGN', 'JOB_022', '그래픽/시각 디자인', 'COMP_106', '브랜딩 디자인', '브랜딩 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('DESIGN', 'JOB_022', '그래픽/시각 디자인', 'COMP_107', '디자인 툴 활용', '디자인 툴 활용 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('DESIGN', 'JOB_023', '영상/모션 그래픽', 'COMP_108', '영상 기획', '영상 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('DESIGN', 'JOB_023', '영상/모션 그래픽', 'COMP_109', '모션 디자인', '모션 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('DESIGN', 'JOB_023', '영상/모션 그래픽', 'COMP_110', '영상 편집', '영상 편집 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('DESIGN', 'JOB_023', '영상/모션 그래픽', 'COMP_111', '스토리보드 구성', '스토리보드 구성 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('DESIGN', 'JOB_023', '영상/모션 그래픽', 'COMP_112', '시각적 연출', '시각적 연출 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('DESIGN', 'JOB_024', '제품/산업 디자인', 'COMP_113', '제품 설계', '제품 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('DESIGN', 'JOB_024', '제품/산업 디자인', 'COMP_114', '사용자 중심 설계', '사용자 중심 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('DESIGN', 'JOB_024', '제품/산업 디자인', 'COMP_115', '시제품 제작', '시제품 제작에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 3),
    ('DESIGN', 'JOB_024', '제품/산업 디자인', 'COMP_116', '양산 프로세스 이해', '양산 프로세스 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 4),
    ('DESIGN', 'JOB_024', '제품/산업 디자인', 'COMP_117', '문제 해결', '문제 해결을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 5),
    ('DESIGN', 'JOB_025', 'BX/브랜드 디자인', 'COMP_118', '브랜드 아이덴티티 구축', '브랜드 아이덴티티 구축에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 1),
    ('DESIGN', 'JOB_025', 'BX/브랜드 디자인', 'COMP_119', 'BI·CI 디자인', 'BI·CI 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('DESIGN', 'JOB_025', 'BX/브랜드 디자인', 'COMP_034', '브랜드 경험 설계', '브랜드 경험 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 3),
    ('DESIGN', 'JOB_025', 'BX/브랜드 디자인', 'COMP_120', '패키지 디자인', '패키지 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('DESIGN', 'JOB_025', 'BX/브랜드 디자인', 'COMP_121', '브랜드 가이드 제작', '브랜드 가이드 제작에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 5),
    ('DESIGN', 'JOB_026', '캐릭터/일러스트/3D', 'COMP_122', '캐릭터 기획', '캐릭터 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('DESIGN', 'JOB_026', '캐릭터/일러스트/3D', 'COMP_123', '3D 모델링', '3D 모델링의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 2),
    ('DESIGN', 'JOB_026', '캐릭터/일러스트/3D', 'COMP_124', '텍스처링', '텍스처링 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('DESIGN', 'JOB_026', '캐릭터/일러스트/3D', 'COMP_125', '스타일 가이드 제작', '스타일 가이드 제작에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 4),
    ('DESIGN', 'JOB_026', '캐릭터/일러스트/3D', 'COMP_126', '비주얼 스토리텔링', '비주얼 스토리텔링을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 5),
    ('SALES_CS', 'JOB_027', 'B2B 영업', 'COMP_127', '고객 발굴', '고객 발굴 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('SALES_CS', 'JOB_027', 'B2B 영업', 'COMP_128', '제안 및 협상', '제안 및 협상을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 2),
    ('SALES_CS', 'JOB_027', 'B2B 영업', 'COMP_129', '관계 구축', '관계 구축에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 3),
    ('SALES_CS', 'JOB_027', 'B2B 영업', 'COMP_130', '계약 관리', '계약 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('SALES_CS', 'JOB_027', 'B2B 영업', 'COMP_131', '영업 전략 수립', '영업 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 5),
    ('SALES_CS', 'JOB_028', '해외 영업', 'COMP_132', '글로벌 시장 분석', '글로벌 시장 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('SALES_CS', 'JOB_028', '해외 영업', 'COMP_133', '바이어 발굴', '바이어 발굴 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('SALES_CS', 'JOB_028', '해외 영업', 'COMP_134', '협상 및 커뮤니케이션', '협상 및 커뮤니케이션을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 3),
    ('SALES_CS', 'JOB_028', '해외 영업', 'COMP_135', '무역 실무 이해', '무역 실무 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 4),
    ('SALES_CS', 'JOB_028', '해외 영업', 'COMP_136', '고객 관계 관리', '고객 관계 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('SALES_CS', 'JOB_029', '영업관리/기획', 'COMP_137', '매출 분석', '매출 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('SALES_CS', 'JOB_029', '영업관리/기획', 'COMP_138', 'CRM 활용', 'CRM 활용 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('SALES_CS', 'JOB_029', '영업관리/기획', 'COMP_139', '영업 프로세스 개선', '영업 프로세스 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 3),
    ('SALES_CS', 'JOB_029', '영업관리/기획', 'COMP_140', '채널 관리', '채널 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('SALES_CS', 'JOB_029', '영업관리/기획', 'COMP_141', '프로모션 기획', '프로모션 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 5),
    ('SALES_CS', 'JOB_030', '고객경험(CX)/CS 기획', 'COMP_142', 'VOC 분석', 'VOC 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('SALES_CS', 'JOB_030', '고객경험(CX)/CS 기획', 'COMP_143', '고객 여정 분석', '고객 여정 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('SALES_CS', 'JOB_030', '고객경험(CX)/CS 기획', 'COMP_144', '고객 만족도 개선', '고객 만족도 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 3),
    ('SALES_CS', 'JOB_030', '고객경험(CX)/CS 기획', 'COMP_145', 'CS 프로세스 개선', 'CS 프로세스 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 4),
    ('SALES_CS', 'JOB_030', '고객경험(CX)/CS 기획', 'COMP_146', '데이터 기반 서비스 개선', '데이터 기반 서비스 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 5),
    ('PRODUCTION_MANUFACTURING', 'JOB_031', '생산/공정관리', 'COMP_147', '생산 계획 수립', '생산 계획 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('PRODUCTION_MANUFACTURING', 'JOB_031', '생산/공정관리', 'COMP_148', '공정 최적화', '공정 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 2),
    ('PRODUCTION_MANUFACTURING', 'JOB_031', '생산/공정관리', 'COMP_149', '생산성 개선', '생산성 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 3),
    ('PRODUCTION_MANUFACTURING', 'JOB_031', '생산/공정관리', 'COMP_150', '설비 운영 관리', '설비 운영 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('PRODUCTION_MANUFACTURING', 'JOB_031', '생산/공정관리', 'COMP_117', '문제 해결', '문제 해결을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 5),
    ('PRODUCTION_MANUFACTURING', 'JOB_032', '품질관리', 'COMP_091', '품질 관리', '품질 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 1),
    ('PRODUCTION_MANUFACTURING', 'JOB_032', '품질관리', 'COMP_151', '품질 개선', '품질 개선을 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 2),
    ('PRODUCTION_MANUFACTURING', 'JOB_032', '품질관리', 'COMP_152', '원인 분석', '원인 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('PRODUCTION_MANUFACTURING', 'JOB_032', '품질관리', 'COMP_153', '품질 표준 관리', '품질 표준 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('PRODUCTION_MANUFACTURING', 'JOB_032', '품질관리', 'COMP_154', '통계적 품질 분석', '통계적 품질 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 5),
    ('PRODUCTION_MANUFACTURING', 'JOB_033', '자재/재고관리', 'COMP_155', '재고 최적화', '재고 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 1),
    ('PRODUCTION_MANUFACTURING', 'JOB_033', '자재/재고관리', 'COMP_156', '자재 수급 관리', '자재 수급 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('PRODUCTION_MANUFACTURING', 'JOB_033', '자재/재고관리', 'COMP_157', '납기 관리', '납기 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 3),
    ('PRODUCTION_MANUFACTURING', 'JOB_033', '자재/재고관리', 'COMP_158', '공급망 협업', '공급망 협업을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 4),
    ('PRODUCTION_MANUFACTURING', 'JOB_033', '자재/재고관리', 'COMP_159', '비용 관리', '비용 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('PRODUCTION_MANUFACTURING', 'JOB_034', '설비/기계 유지보수', 'COMP_160', '예방 보전', '예방 보전 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('PRODUCTION_MANUFACTURING', 'JOB_034', '설비/기계 유지보수', 'COMP_161', '설비 진단', '설비 진단의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 2),
    ('PRODUCTION_MANUFACTURING', 'JOB_034', '설비/기계 유지보수', 'COMP_162', '트러블슈팅', '트러블슈팅을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 3),
    ('PRODUCTION_MANUFACTURING', 'JOB_034', '설비/기계 유지보수', 'COMP_163', '설비 운영', '설비 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('PRODUCTION_MANUFACTURING', 'JOB_034', '설비/기계 유지보수', 'COMP_164', '안전 관리', '안전 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('PRODUCTION_MANUFACTURING', 'JOB_035', '환경/안전관리', 'COMP_165', '산업안전 관리', '산업안전 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 1),
    ('PRODUCTION_MANUFACTURING', 'JOB_035', '환경/안전관리', 'COMP_166', '위험성 평가', '위험성 평가의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 2),
    ('PRODUCTION_MANUFACTURING', 'JOB_035', '환경/안전관리', 'COMP_167', '법규 준수', '법규 준수 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('PRODUCTION_MANUFACTURING', 'JOB_035', '환경/안전관리', 'COMP_168', '사고 예방', '사고 예방 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('PRODUCTION_MANUFACTURING', 'JOB_035', '환경/안전관리', 'COMP_169', '안전 교육', '안전 교육 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 5),
    ('RESEARCH_RND', 'JOB_036', '기계/자동차/조선', 'COMP_170', '기계 설계', '기계 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('RESEARCH_RND', 'JOB_036', '기계/자동차/조선', 'COMP_171', '시뮬레이션 분석', '시뮬레이션 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('RESEARCH_RND', 'JOB_036', '기계/자동차/조선', 'COMP_172', '제품 개발', '제품 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 3),
    ('RESEARCH_RND', 'JOB_036', '기계/자동차/조선', 'COMP_173', '성능 평가', '성능 평가의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 4),
    ('RESEARCH_RND', 'JOB_036', '기계/자동차/조선', 'COMP_117', '문제 해결', '문제 해결을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 5),
    ('RESEARCH_RND', 'JOB_037', '전기/전자/반도체', 'COMP_174', '회로 설계', '회로 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('RESEARCH_RND', 'JOB_037', '전기/전자/반도체', 'COMP_175', '시스템 분석', '시스템 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('RESEARCH_RND', 'JOB_037', '전기/전자/반도체', 'COMP_176', '임베디드 개발', '임베디드 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 3),
    ('RESEARCH_RND', 'JOB_037', '전기/전자/반도체', 'COMP_064', '성능 최적화', '성능 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 4),
    ('RESEARCH_RND', 'JOB_037', '전기/전자/반도체', 'COMP_177', '신기술 개발', '신기술 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 5),
    ('RESEARCH_RND', 'JOB_038', '화학/에너지/신소재', 'COMP_078', '실험 설계', '실험 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('RESEARCH_RND', 'JOB_038', '화학/에너지/신소재', 'COMP_178', '소재 분석', '소재 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('RESEARCH_RND', 'JOB_038', '화학/에너지/신소재', 'COMP_179', '신소재 개발', '신소재 개발에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 3),
    ('RESEARCH_RND', 'JOB_038', '화학/에너지/신소재', 'COMP_075', '데이터 분석', '데이터 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 4),
    ('RESEARCH_RND', 'JOB_038', '화학/에너지/신소재', 'COMP_180', '연구 문제 해결', '연구 문제 해결을 위해 원인을 파악하고 적절한 대안을 실행하여 결과를 확인하는 역량', 5),
    ('RESEARCH_RND', 'JOB_039', '바이오/제약/식품', 'COMP_181', '연구 설계', '연구 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 1),
    ('RESEARCH_RND', 'JOB_039', '바이오/제약/식품', 'COMP_182', '실험 수행', '실험 수행 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('RESEARCH_RND', 'JOB_039', '바이오/제약/식품', 'COMP_075', '데이터 분석', '데이터 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('RESEARCH_RND', 'JOB_039', '바이오/제약/식품', 'COMP_091', '품질 관리', '품질 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('RESEARCH_RND', 'JOB_039', '바이오/제약/식품', 'COMP_183', '규제 이해', '규제 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 5),
    ('FINANCE', 'JOB_040', '재무/회계/세무', 'COMP_016', '재무 분석', '재무 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('FINANCE', 'JOB_040', '재무/회계/세무', 'COMP_184', '관리회계', '관리회계를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('FINANCE', 'JOB_040', '재무/회계/세무', 'COMP_185', '세무 관리', '세무 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 3),
    ('FINANCE', 'JOB_040', '재무/회계/세무', 'COMP_186', '예산 수립', '예산 수립 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('FINANCE', 'JOB_040', '재무/회계/세무', 'COMP_011', '리스크 관리', '리스크 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('FINANCE', 'JOB_041', '자산운용', 'COMP_187', '투자 분석', '투자 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('FINANCE', 'JOB_041', '자산운용', 'COMP_188', '포트폴리오 관리', '포트폴리오 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('FINANCE', 'JOB_041', '자산운용', 'COMP_027', '시장 분석', '시장 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('FINANCE', 'JOB_041', '자산운용', 'COMP_011', '리스크 관리', '리스크 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('FINANCE', 'JOB_041', '자산운용', 'COMP_189', '성과 분석', '성과 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 5),
    ('FINANCE', 'JOB_042', '증권/애널리스트', 'COMP_190', '기업 분석', '기업 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 1),
    ('FINANCE', 'JOB_042', '증권/애널리스트', 'COMP_191', '산업 분석', '산업 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('FINANCE', 'JOB_042', '증권/애널리스트', 'COMP_192', '재무 모델링', '재무 모델링의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 3),
    ('FINANCE', 'JOB_042', '증권/애널리스트', 'COMP_193', '가치평가', '가치평가의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 4),
    ('FINANCE', 'JOB_042', '증권/애널리스트', 'COMP_194', '투자 인사이트 도출', '투자 인사이트 도출을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 5),
    ('FINANCE', 'JOB_043', 'IB/M&A', 'COMP_195', '기업 가치평가', '기업 가치평가의 기준과 절차를 수립하고 결과를 객관적으로 확인하여 품질을 높이는 역량', 1),
    ('FINANCE', 'JOB_043', 'IB/M&A', 'COMP_196', '실사', '실사 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('FINANCE', 'JOB_043', 'IB/M&A', 'COMP_197', '투자 제안서 작성', '투자 제안서 작성을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 3),
    ('FINANCE', 'JOB_043', 'IB/M&A', 'COMP_198', '거래 구조 설계', '거래 구조 설계의 요구사항을 구조화하고 목적과 제약에 맞는 해결 방안을 구체화하는 역량', 4),
    ('FINANCE', 'JOB_043', 'IB/M&A', 'COMP_134', '협상 및 커뮤니케이션', '협상 및 커뮤니케이션을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 5),
    ('FINANCE', 'JOB_044', '리스크/컴플라이언스', 'COMP_199', '내부통제', '내부통제 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('FINANCE', 'JOB_044', '리스크/컴플라이언스', 'COMP_200', '리스크 분석', '리스크 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('FINANCE', 'JOB_044', '리스크/컴플라이언스', 'COMP_201', '금융 규제 이해', '금융 규제 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 3),
    ('FINANCE', 'JOB_044', '리스크/컴플라이언스', 'COMP_202', '모니터링 체계 구축', '모니터링 체계 구축에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 4),
    ('FINANCE', 'JOB_044', '리스크/컴플라이언스', 'COMP_013', '컴플라이언스 관리', '컴플라이언스 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('MEDIA_CONTENT', 'JOB_045', 'PD/방송제작', 'COMP_041', '콘텐츠 기획', '콘텐츠 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MEDIA_CONTENT', 'JOB_045', 'PD/방송제작', 'COMP_203', '제작 관리', '제작 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('MEDIA_CONTENT', 'JOB_045', 'PD/방송제작', 'COMP_204', '현장 디렉팅', '현장 디렉팅 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('MEDIA_CONTENT', 'JOB_045', 'PD/방송제작', 'COMP_205', '일정 관리', '일정 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('MEDIA_CONTENT', 'JOB_045', 'PD/방송제작', 'COMP_206', '프로젝트 운영', '프로젝트 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('MEDIA_CONTENT', 'JOB_046', '영상 촬영/편집', 'COMP_108', '영상 기획', '영상 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MEDIA_CONTENT', 'JOB_046', '영상 촬영/편집', 'COMP_207', '촬영', '촬영 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('MEDIA_CONTENT', 'JOB_046', '영상 촬영/편집', 'COMP_208', '편집', '편집 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('MEDIA_CONTENT', 'JOB_046', '영상 촬영/편집', 'COMP_209', '색보정', '색보정 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('MEDIA_CONTENT', 'JOB_046', '영상 촬영/편집', 'COMP_210', '시각적 스토리텔링', '시각적 스토리텔링을 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 5),
    ('MEDIA_CONTENT', 'JOB_047', '콘텐츠 에디터/작가', 'COMP_041', '콘텐츠 기획', '콘텐츠 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MEDIA_CONTENT', 'JOB_047', '콘텐츠 에디터/작가', 'COMP_211', '글쓰기', '글쓰기를 통해 핵심 메시지를 대상과 목적에 맞게 명확히 전달하는 역량', 2),
    ('MEDIA_CONTENT', 'JOB_047', '콘텐츠 에디터/작가', 'COMP_212', '인터뷰 및 취재', '인터뷰 및 취재 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('MEDIA_CONTENT', 'JOB_047', '콘텐츠 에디터/작가', 'COMP_213', 'SEO 기반 콘텐츠 제작', 'SEO 기반 콘텐츠 제작에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 4),
    ('MEDIA_CONTENT', 'JOB_047', '콘텐츠 에디터/작가', 'COMP_044', '콘텐츠 성과 분석', '콘텐츠 성과 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 5),
    ('MEDIA_CONTENT', 'JOB_048', '엔터테인먼트/공연기획', 'COMP_214', '공연 기획', '공연 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('MEDIA_CONTENT', 'JOB_048', '엔터테인먼트/공연기획', 'COMP_206', '프로젝트 운영', '프로젝트 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('MEDIA_CONTENT', 'JOB_048', '엔터테인먼트/공연기획', 'COMP_215', '아티스트 협업', '아티스트 협업을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 3),
    ('MEDIA_CONTENT', 'JOB_048', '엔터테인먼트/공연기획', 'COMP_216', '팬덤 마케팅', '팬덤 마케팅 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('MEDIA_CONTENT', 'JOB_048', '엔터테인먼트/공연기획', 'COMP_217', '행사 운영', '행사 운영을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('MEDIA_CONTENT', 'JOB_049', '음악/음향', 'COMP_218', '음악 제작', '음악 제작에 필요한 요구사항과 기술을 이해하고 안정적인 결과물을 완성하는 역량', 1),
    ('MEDIA_CONTENT', 'JOB_049', '음악/음향', 'COMP_219', '음향 편집', '음향 편집 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('MEDIA_CONTENT', 'JOB_049', '음악/음향', 'COMP_220', '사운드 디자인', '사운드 디자인 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 3),
    ('MEDIA_CONTENT', 'JOB_049', '음악/음향', 'COMP_221', '믹싱·마스터링', '믹싱·마스터링 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 4),
    ('MEDIA_CONTENT', 'JOB_049', '음악/음향', 'COMP_222', '음원 콘텐츠 기획', '음원 콘텐츠 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 5),
    ('LOGISTICS_DISTRIBUTION', 'JOB_050', '무역영업/수출입 관리', 'COMP_135', '무역 실무 이해', '무역 실무 이해는 바탕으로 관련 원리와 맥락을 업무 의사결정과 실행에 적용하는 역량', 1),
    ('LOGISTICS_DISTRIBUTION', 'JOB_050', '무역영업/수출입 관리', 'COMP_223', '통관 관리', '통관 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 2),
    ('LOGISTICS_DISTRIBUTION', 'JOB_050', '무역영업/수출입 관리', 'COMP_224', '운송 관리', '운송 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 3),
    ('LOGISTICS_DISTRIBUTION', 'JOB_050', '무역영업/수출입 관리', 'COMP_225', '해외 거래처 협업', '해외 거래처 협업을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 4),
    ('LOGISTICS_DISTRIBUTION', 'JOB_050', '무역영업/수출입 관리', 'COMP_011', '리스크 관리', '리스크 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('LOGISTICS_DISTRIBUTION', 'JOB_051', '구매/자재관리', 'COMP_226', '공급사 발굴', '공급사 발굴 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 1),
    ('LOGISTICS_DISTRIBUTION', 'JOB_051', '구매/자재관리', 'COMP_227', '구매 협상', '구매 협상을 바탕으로 이해관계자의 목표를 조율하고 공동의 결과를 만드는 역량', 2),
    ('LOGISTICS_DISTRIBUTION', 'JOB_051', '구매/자재관리', 'COMP_228', '원가 관리', '원가 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 3),
    ('LOGISTICS_DISTRIBUTION', 'JOB_051', '구매/자재관리', 'COMP_130', '계약 관리', '계약 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('LOGISTICS_DISTRIBUTION', 'JOB_051', '구매/자재관리', 'COMP_229', '공급망 관리', '공급망 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5),
    ('LOGISTICS_DISTRIBUTION', 'JOB_052', '물류/SCM 관리', 'COMP_229', '공급망 관리', '공급망 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 1),
    ('LOGISTICS_DISTRIBUTION', 'JOB_052', '물류/SCM 관리', 'COMP_230', '수요 예측', '수요 예측 관련 지식과 방법을 활용하여 과업을 정확하고 효과적으로 수행하는 역량', 2),
    ('LOGISTICS_DISTRIBUTION', 'JOB_052', '물류/SCM 관리', 'COMP_155', '재고 최적화', '재고 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 3),
    ('LOGISTICS_DISTRIBUTION', 'JOB_052', '물류/SCM 관리', 'COMP_231', '물류 운영 개선', '물류 운영 개선을 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 4),
    ('LOGISTICS_DISTRIBUTION', 'JOB_052', '물류/SCM 관리', 'COMP_232', '비용 최적화', '비용 최적화를 위해 문제와 병목을 진단하고 효과적인 개선안을 실행하는 역량', 5),
    ('LOGISTICS_DISTRIBUTION', 'JOB_053', 'MD/상품기획', 'COMP_233', '상품 기획', '상품 기획의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 1),
    ('LOGISTICS_DISTRIBUTION', 'JOB_053', 'MD/상품기획', 'COMP_234', '상품 분석', '상품 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 2),
    ('LOGISTICS_DISTRIBUTION', 'JOB_053', 'MD/상품기획', 'COMP_235', '트렌드 분석', '트렌드 분석을 통해 데이터와 상황의 의미를 파악하고 실행 가능한 판단을 도출하는 역량', 3),
    ('LOGISTICS_DISTRIBUTION', 'JOB_053', 'MD/상품기획', 'COMP_236', '판매 전략 수립', '판매 전략 수립의 목표와 실행 방안을 정의하고 필요한 자원과 일정을 체계화하는 역량', 4),
    ('LOGISTICS_DISTRIBUTION', 'JOB_053', 'MD/상품기획', 'COMP_237', '수익성 관리', '수익성 관리를 체계적으로 수행하고 성과와 위험을 지속적으로 점검·개선하는 역량', 5);

DO $$
BEGIN
    IF EXISTS (
        SELECT job_code
        FROM job_competency_seed
        GROUP BY job_code
        HAVING COUNT(DISTINCT job_name) <> 1
            OR COUNT(DISTINCT category_code) <> 1
    ) THEN
        RAISE EXCEPTION 'Seed에 동일 직무 코드의 이름 또는 대분류가 일치하지 않습니다.';
    END IF;

    IF EXISTS (
        SELECT competency_code
        FROM job_competency_seed
        GROUP BY competency_code
        HAVING COUNT(DISTINCT competency_name) <> 1
            OR COUNT(DISTINCT competency_description) <> 1
    ) THEN
        RAISE EXCEPTION 'Seed에 동일 역량 코드의 이름 또는 설명이 일치하지 않습니다.';
    END IF;

    IF EXISTS (
        SELECT job_code
        FROM job_competency_seed
        GROUP BY job_code
        HAVING COUNT(*) <> 5
            OR MIN(sort_order) <> 1
            OR MAX(sort_order) <> 5
            OR COUNT(DISTINCT sort_order) <> 5
    ) THEN
        RAISE EXCEPTION '모든 직무는 정렬 순서 1~5의 역량을 정확히 5개 가져야 합니다.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM jobs
        GROUP BY name
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '기존 jobs에 중복 이름이 있어 안전하게 코드를 backfill할 수 없습니다.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM competency_tags
        GROUP BY name
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '기존 competency_tags에 중복 이름이 있어 안전하게 코드를 backfill할 수 없습니다.';
    END IF;
END
$$;

UPDATE jobs job
SET code = seed.job_code,
    category_code = seed.category_code,
    updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT job_code, category_code, job_name
    FROM job_competency_seed
) seed
WHERE job.name = seed.job_name
  AND job.code IS NULL;

INSERT INTO jobs (
    id, created_at, updated_at,
    code, category_code, name, description
)
SELECT
    md5('dortfolio:job:' || seed.job_code)::uuid,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    seed.job_code,
    seed.category_code,
    seed.job_name,
    NULL
FROM (
    SELECT DISTINCT job_code, category_code, job_name
    FROM job_competency_seed
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM jobs job
    WHERE job.code = seed.job_code
       OR job.name = seed.job_name
);

UPDATE competency_tags tag
SET code = seed.competency_code,
    description = seed.competency_description,
    updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT
        competency_code,
        competency_name,
        competency_description
    FROM job_competency_seed
) seed
WHERE tag.name = seed.competency_name
  AND tag.code IS NULL;

INSERT INTO competency_tags (
    id, created_at, updated_at,
    code, name, description
)
SELECT
    md5('dortfolio:competency:' || seed.competency_code)::uuid,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    seed.competency_code,
    seed.competency_name,
    seed.competency_description
FROM (
    SELECT DISTINCT
        competency_code,
        competency_name,
        competency_description
    FROM job_competency_seed
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM competency_tags tag
    WHERE tag.code = seed.competency_code
       OR tag.name = seed.competency_name
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM jobs WHERE code IS NULL OR category_code IS NULL) THEN
        RAISE EXCEPTION 'Seed에 매핑되지 않은 기존 직무가 있습니다.';
    END IF;

    IF EXISTS (SELECT 1 FROM competency_tags WHERE code IS NULL) THEN
        RAISE EXCEPTION 'Seed에 매핑되지 않은 기존 역량이 있습니다.';
    END IF;

    IF EXISTS (
        SELECT code FROM jobs GROUP BY code HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '중복 직무 코드가 있습니다.';
    END IF;

    IF EXISTS (
        SELECT code FROM competency_tags GROUP BY code HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '중복 역량 코드가 있습니다.';
    END IF;
END
$$;

INSERT INTO job_competencies (
    id, created_at, updated_at,
    sort_order, competency_tag_id, job_id
)
SELECT
    md5(
        'dortfolio:job-competency:'
        || seed.job_code || ':' || seed.competency_code
    )::uuid,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    seed.sort_order,
    tag.id,
    job.id
FROM job_competency_seed seed
JOIN jobs job ON job.code = seed.job_code
JOIN competency_tags tag ON tag.code = seed.competency_code
ON CONFLICT (job_id, competency_tag_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    updated_at = CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT job_id
        FROM job_competencies
        GROUP BY job_id
        HAVING COUNT(*) <> 5
            OR MIN(sort_order) <> 1
            OR MAX(sort_order) <> 5
            OR COUNT(DISTINCT sort_order) <> 5
    ) THEN
        RAISE EXCEPTION 'DB의 모든 직무는 정렬 순서 1~5의 역량을 정확히 5개 가져야 합니다.';
    END IF;
END
$$;

ALTER TABLE jobs ALTER COLUMN code SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN category_code SET NOT NULL;
ALTER TABLE competency_tags ALTER COLUMN code SET NOT NULL;

ALTER TABLE jobs ADD CONSTRAINT uk_jobs_code UNIQUE (code);
ALTER TABLE competency_tags ADD CONSTRAINT uk_competency_tags_code UNIQUE (code);
ALTER TABLE job_competencies
    ADD CONSTRAINT uk_job_competency_sort_order
    UNIQUE (job_id, sort_order);
