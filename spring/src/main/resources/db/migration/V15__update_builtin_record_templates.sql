-- 기획에서 확정한 기본 제공 기록 템플릿 문구로 기존 데이터를 갱신한다.
SET LOCAL lock_timeout = '5s';

UPDATE templates AS template
SET builtin_version = 2,
    updated_at = current_timestamp,
    description = content.description
FROM (VALUES
    ('IDEA_PLANNING', '아이디어를 내고 더 나은 방향을 선택했던 경험을 기록해보세요.'),
    ('COLLABORATION_CONFLICT', '함께 일하며 의견을 맞춰갔던 경험을 기록해보세요.'),
    ('PROBLEM_SOLVING_RESULT', '예상치 못한 문제를 논리적으로 해결한 경험을 기록해보세요.'),
    ('IMMERSION_CHALLENGE', '스스로 더 높은 목표를 세우고 몰입했던 경험을 기록해보세요.')
) AS content(builtin_code, description)
WHERE template.builtin_code = content.builtin_code;

UPDATE template_questions AS question
SET updated_at = current_timestamp,
    question_text = content.question_text,
    description = content.description,
    required = TRUE,
    sort_order = content.sort_order
FROM (VALUES
    ('IDEA_PLANNING_BACKGROUND', '문제 정의', '오늘 회의나 일과 중에 해결해야 했던 과제나 새롭게 발견한 불편함은 무엇인가요?', 1),
    ('IDEA_PLANNING_IDEA', '아이디어 나열', '이 문제를 해결하기 위해 회의나 머릿속에서 제안된 아이디어에는 어떤 것들이 있었나요?', 2),
    ('IDEA_PLANNING_PLAN', '나만의 선택 기준', '수많은 대안 중 오늘 최종 방향을 결정짓게 만든 ''가장 중요한 판단 기준''은 무엇이었나요?', 3),
    ('IDEA_PLANNING_INSIGHT', '기획 인사이트', '오늘 아이디어를 내고 판단하는 과정에서 새롭게 깨달은 ''나만의 기준''이나 ''효과적이었던 방식''은 무엇인가요?', 4),

    ('COLLABORATION_CONFLICT_SITUATION', '갈등 상황', '협업 과정에서 팀원 간(혹은 나와 팀원 간)에 부딪힌 의견 차이나 협업의 걸림돌은 무엇이었나요?', 1),
    ('COLLABORATION_CONFLICT_ANALYSIS', '입장 분석', '대립하는 각 주장의 핵심 논리는 무엇이었으며, 각각 어떤 장단점을 가지고 있었나요?', 2),
    ('COLLABORATION_CONFLICT_CRITERIA', '나만의 조율 기준', '이 갈등을 해결하거나 중재하기 위해 내가 가장 중요하게 생각한 ''판단 기준''은 무엇이었나요?', 3),
    ('COLLABORATION_CONFLICT_ACTION', '행동과 최종 합의', '내가 세운 기준을 바탕으로 팀원들과 어떻게 소통했으며, 최종적으로 도출한 합의점은 무엇인가요?', 4),
    ('COLLABORATION_CONFLICT_LESSON', '협업 인사이트', '갈등을 조율하는 과정에서 새롭게 깨달은 ''나만의 협업 규칙''이나 ''효과적이었던 소통 방식''은 무엇인가요?', 5),

    ('PROBLEM_SOLVING_RESULT_PROBLEM', '문제 상황', '계획과 달리 갑자기 터진 오류나 예상치 못한 난관은 무엇이었나요?', 1),
    ('PROBLEM_SOLVING_RESULT_CAUSE', '원인 가설', '이 문제가 발생한 ''가장 유력한 원인''은 무엇이라고 추정했나요?', 2),
    ('PROBLEM_SOLVING_RESULT_SOLUTION', '의사결정', '문제를 해결하기 위해 어떤 대안들을 고려했고, 왜 그 방향(순서)대로 실행했나요?', 3),
    ('PROBLEM_SOLVING_RESULT_OUTCOME', '문제 해결 결과', '내가 조치한 결과 상황이 어떻게 정상화되었으며, 어떤 정량적/정성적 성과로 이어졌나요?', 4),
    ('PROBLEM_SOLVING_RESULT_INSIGHT', '문제 해결 인사이트', '다음번에 이와 비슷한 문제가 또 터지지 않게 하려면 어떤 예방책이나 규칙이 필요할까요?', 5),

    ('IMMERSION_CHALLENGE_GOAL', '나의 목표', '기존 방식에 안주하지 않고, 오늘 일부러 ''더 높은 기준''을 적용해 시도한 일은 무엇인가요?', 1),
    ('IMMERSION_CHALLENGE_OBSTACLE', '방해 요소', '목표에 도전하면서 오늘 나를 가장 지치게 하거나 유혹했던 ''방해 요소''는 무엇이었나요?', 2),
    ('IMMERSION_CHALLENGE_EFFORT', '나만의 행동 원칙', '포기하거나 타협하지 않고 끝까지 몰입하기 위해 스스로 부여한 ''나만의 행동 원칙''은 무엇이었나요?', 3),
    ('IMMERSION_CHALLENGE_OUTCOME', '몰입의 결과', '집요하게 몰입한 결과, 어떤 결과물을 만들어냈거나 개인적인 성장을 이뤘나요?', 4),
    ('IMMERSION_CHALLENGE_GROWTH', '인사이트', '나는 어떤 환경이나 마인드셋일 때 가장 폭발적으로 몰입하고 성장하나요?', 5)
) AS content(builtin_code, question_text, description, sort_order)
WHERE question.builtin_code = content.builtin_code;
