ALTER TABLE templates
    ALTER COLUMN description TYPE varchar(255);

ALTER TABLE template_questions
    ALTER COLUMN description TYPE varchar(255);

INSERT INTO templates (
    id, created_at, updated_at, builtin_code, builtin_version, deleted_at,
    description, is_builtin, title, user_id
)
VALUES
    ('10000000-0000-0000-0000-000000000001', current_timestamp, current_timestamp,
     'IDEA_PLANNING', 1, NULL,
     '매일의 회의나 개인 작업 중 마주하는 고민과 선택의 순간을 세밀하게 기록하는 템플릿입니다.', TRUE, '아이디어·기획', NULL),
    ('10000000-0000-0000-0000-000000000002', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT', 1, NULL,
     '팀 프로젝트나 대외활동 중 매일 발생하는 의견 조율과 갈등 해결의 순간을 기록하는 일지입니다. 감정적인 다툼뿐만 아니라 ''일하는 방식의 차이'' 같은 세세한 마찰 속에서 나의 중재 역량을 증명해보세요.', TRUE, '협업·갈등', NULL),
    ('10000000-0000-0000-0000-000000000003', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT', 1, NULL,
     '외부 환경이나 예상치 못한 변수로 인해 발생한 문제를 어떻게 논리적으로 해결했는지 나의 위기 대처력을 기록하는 템플릿입니다.', TRUE, '문제해결·성과', NULL),
    ('10000000-0000-0000-0000-000000000004', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE', 1, NULL,
     '현실에 안주하지 않고 스스로 더 높은 목표를 설정하고, 무언가에 미치도록 몰입했던 순간을 기록하는 템플릿입니다.', TRUE, '몰입·도전', NULL)
ON CONFLICT (builtin_code) DO UPDATE
SET builtin_version = EXCLUDED.builtin_version,
    updated_at = current_timestamp,
    deleted_at = NULL,
    description = EXCLUDED.description,
    is_builtin = TRUE,
    title = EXCLUDED.title,
    user_id = NULL;

INSERT INTO template_questions (
    id, created_at, updated_at, builtin_code, description,
    question_text, required, sort_order, template_id
)
VALUES
    ('20000000-0000-0000-0000-000000000001', current_timestamp, current_timestamp,
     'IDEA_PLANNING_BACKGROUND', '오늘 회의나 일과 중에 해결해야 했던 과제나 새롭게 발견한 불편함은 무엇인가요?',
     '문제 정의', TRUE, 1, (SELECT id FROM templates WHERE builtin_code = 'IDEA_PLANNING')),
    ('20000000-0000-0000-0000-000000000002', current_timestamp, current_timestamp,
     'IDEA_PLANNING_IDEA', '이 문제를 해결하기 위해 회의나 머릿속에서 제안된 대안(아이디어)에는 어떤 것들이 있었나요?',
     '아이디어 나열', TRUE, 2, (SELECT id FROM templates WHERE builtin_code = 'IDEA_PLANNING')),
    ('20000000-0000-0000-0000-000000000003', current_timestamp, current_timestamp,
     'IDEA_PLANNING_PLAN', '수많은 대안 중 오늘 최종 방향을 결정짓게 만든 ''가장 중요한 판단 기준''은 무엇이었나요?',
     '나만의 선택 기준', TRUE, 3, (SELECT id FROM templates WHERE builtin_code = 'IDEA_PLANNING')),
    ('20000000-0000-0000-0000-000000000004', current_timestamp, current_timestamp,
     'IDEA_PLANNING_INSIGHT', '오늘 아이디어를 내고 판단하는 과정에서 새롭게 깨달은 ''나만의 기획/판단 기준''이나 ''효과적이었던 방식''은 무엇인가요?',
     '기획 인사이트', TRUE, 4, (SELECT id FROM templates WHERE builtin_code = 'IDEA_PLANNING')),

    ('20000000-0000-0000-0000-000000000005', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT_SITUATION', '협업 과정에서 팀원 간(혹은 나와 팀원 간)에 부딪힌 의견 차이나 협업의 걸림돌은 무엇이었나요?',
     '갈등/의견 대립 상황', TRUE, 1, (SELECT id FROM templates WHERE builtin_code = 'COLLABORATION_CONFLICT')),
    ('20000000-0000-0000-0000-000000000006', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT_ANALYSIS', '대립하는 각 주장의 핵심 논리는 무엇이었으며, 각각 어떤 장단점을 가지고 있었나요?',
     '입장 분석', TRUE, 2, (SELECT id FROM templates WHERE builtin_code = 'COLLABORATION_CONFLICT')),
    ('20000000-0000-0000-0000-000000000007', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT_CRITERIA', '이 갈등을 해결하거나 중재하기 위해 내가 가장 중요하게 생각한 ''판단 기준''은 무엇이었나요?',
     '나만의 조율 기준(의사결정)', TRUE, 3, (SELECT id FROM templates WHERE builtin_code = 'COLLABORATION_CONFLICT')),
    ('20000000-0000-0000-0000-000000000008', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT_ACTION', '내가 세운 기준을 바탕으로 팀원들과 어떻게 소통했으며, 최종적으로 도출한 합의점은 무엇인가요?',
     '행동과 최종 합의', TRUE, 4, (SELECT id FROM templates WHERE builtin_code = 'COLLABORATION_CONFLICT')),
    ('20000000-0000-0000-0000-000000000009', current_timestamp, current_timestamp,
     'COLLABORATION_CONFLICT_LESSON', '갈등을 조율하는 과정에서 새롭게 깨달은 ''나만의 협업 규칙''이나 ''효과적이었던 소통 방식''은 무엇인가요?',
     '협업 인사이트', TRUE, 5, (SELECT id FROM templates WHERE builtin_code = 'COLLABORATION_CONFLICT')),

    ('20000000-0000-0000-0000-000000000010', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT_PROBLEM', '계획과 달리 갑자기 터진 오류나 예상치 못한 난관은 무엇이었나요?',
     '문제 상황', TRUE, 1, (SELECT id FROM templates WHERE builtin_code = 'PROBLEM_SOLVING_RESULT')),
    ('20000000-0000-0000-0000-000000000011', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT_CAUSE', '이 문제가 발생한 ''가장 유력한 원인''은 무엇이라고 추정했나요? 현상만 보지 말고, 머릿속으로 짚어본 근본적인 원인을 기록하세요.',
     '원인 가설 세우기', TRUE, 2, (SELECT id FROM templates WHERE builtin_code = 'PROBLEM_SOLVING_RESULT')),
    ('20000000-0000-0000-0000-000000000012', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT_SOLUTION', '문제를 해결하기 위해 어떤 대안들을 고려했고, 왜 그 방향(순서)대로 실행했나요? 내가 가장 포기할 수 없었던 핵심 기준도 적어보세요.',
     '수습을 위한 의사결정', TRUE, 3, (SELECT id FROM templates WHERE builtin_code = 'PROBLEM_SOLVING_RESULT')),
    ('20000000-0000-0000-0000-000000000013', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT_OUTCOME', '내가 조치한 결과 상황이 어떻게 정상화되었으며, 어떤 정량적/정성적 성과로 이어졌나요?',
     '문제 해결 결과', TRUE, 4, (SELECT id FROM templates WHERE builtin_code = 'PROBLEM_SOLVING_RESULT')),
    ('20000000-0000-0000-0000-000000000014', current_timestamp, current_timestamp,
     'PROBLEM_SOLVING_RESULT_INSIGHT', '다음번에 이와 비슷한 문제가 또 터지지 않게 하려면 어떤 예방책이나 규칙이 필요할까요?',
     '협업 인사이트', TRUE, 5, (SELECT id FROM templates WHERE builtin_code = 'PROBLEM_SOLVING_RESULT')),

    ('20000000-0000-0000-0000-000000000015', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE_GOAL', '기존 방식에 안주하지 않고, 오늘 일부러 ''더 높은 기준''을 적용해 시도한 일은 무엇인가요?',
     '나의 목표', TRUE, 1, (SELECT id FROM templates WHERE builtin_code = 'IMMERSION_CHALLENGE')),
    ('20000000-0000-0000-0000-000000000016', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE_OBSTACLE', '목표에 도전하면서 오늘 나를 가장 지치게 하거나 유혹했던 ''방해 요소''는 무엇이었나요?',
     '방해 요소', TRUE, 2, (SELECT id FROM templates WHERE builtin_code = 'IMMERSION_CHALLENGE')),
    ('20000000-0000-0000-0000-000000000017', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE_EFFORT', '포기하거나 타협하지 않고 끝까지 몰입하기 위해 스스로 부여한 ''나만의 행동 원칙''은 무엇이었나요?',
     '페이스 조절과 원칙', TRUE, 3, (SELECT id FROM templates WHERE builtin_code = 'IMMERSION_CHALLENGE')),
    ('20000000-0000-0000-0000-000000000018', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE_OUTCOME', '집요하게 몰입한 결과, 어떤 결과물을 만들어냈거나 개인적인 성장을 이뤘나요? 내 한계가 넓어진 지점을 적으세요.',
     '몰입의 결과', TRUE, 4, (SELECT id FROM templates WHERE builtin_code = 'IMMERSION_CHALLENGE')),
    ('20000000-0000-0000-0000-000000000019', current_timestamp, current_timestamp,
     'IMMERSION_CHALLENGE_GROWTH', '나는 어떤 환경이나 마인드셋일 때 가장 폭발적으로 몰입하고 성장하나요? 깨달은 점은 무엇인가요? "처음 해보는 일은 가이드라인의 뼈대만 잡아두고 시작해야 두려움이 덜하다"처럼 나를 움직이는 치트키를 기록하세요.',
     '인사이트', TRUE, 5, (SELECT id FROM templates WHERE builtin_code = 'IMMERSION_CHALLENGE'))
ON CONFLICT (builtin_code) DO UPDATE
SET updated_at = current_timestamp,
    description = EXCLUDED.description,
    question_text = EXCLUDED.question_text,
    required = TRUE,
    sort_order = EXCLUDED.sort_order,
    template_id = EXCLUDED.template_id;
