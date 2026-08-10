-- 기본 활동 종류(동아리/학회, 프로젝트, 인턴, 공모전)는 회원가입 시점에 생성되지만,
-- 그 이전에 가입한 사용자에게는 없다. 활동 종류가 하나도 없는 사용자에게만 채워 넣는다.
--
-- 이미 종류를 가진 사용자는 건드리지 않는다. 사용자가 지운 기본 종류가 되살아나면 안 되기 때문이다.
INSERT INTO activity_type (id, created_at, updated_at, user_id, name, is_default)
SELECT gen_random_uuid(),
       now(),
       now(),
       u.id,
       default_name,
       true
FROM users u
         CROSS JOIN (VALUES ('동아리/학회'), ('프로젝트'), ('인턴'), ('공모전')) AS defaults(default_name)
WHERE NOT EXISTS (SELECT 1
                  FROM activity_type t
                  WHERE t.user_id = u.id);
