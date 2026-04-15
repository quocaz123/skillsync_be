-- =============================================================================
-- AI MENTOR MATCHING — TEST DATA SEED SCRIPT
-- =============================================================================
-- Mục đích: Tạo dữ liệu đầy đủ để test hệ thống AI Chat tìm mentor.
--
-- Cách chạy:
--   Mở DBeaver / pgAdmin / psql, chọn đúng database SkillSync, chạy toàn bộ file.
--
-- Skills có sẵn trong DB (từ skills.json):
--   Java, Python, JavaScript, TypeScript, React, SQL, Git,
--   Machine Learning, Data Analysis, Power BI,
--   UI/UX Design, Figma, Quản Lý Dự Án, Business Analysis,
--   Digital Marketing, SEO, Tài Chính Cá Nhân, Tiếng Anh,
--   Giao Tiếp, Thuyết Trình
--
-- Script này sẽ:
--   1. Thêm các skills còn thiếu (Spring Boot, Node.js, Docker, ...)
--   2. Tạo 10 mentor users
--   3. Gán teaching skills với đầy đủ level/experience/style → APPROVED
-- =============================================================================

-- ─────────────────────────────────────────────────────────────
-- BƯỚC 1: Thêm Skills còn thiếu cho AI test cases
-- ─────────────────────────────────────────────────────────────

INSERT INTO skills (id, name, category, icon, created_at)
SELECT gen_random_uuid(), v.name, v.cat::text::"SkillCategory", v.icon, NOW()
FROM (VALUES
    ('Spring Boot',     'TECH',   'Lightning'),
    ('Node.js',         'TECH',   'TreeStructure'),
    ('Vue.js',          'TECH',   'FrameCorners'),
    ('Docker',          'TECH',   'Cube'),
    ('Kubernetes',      'TECH',   'Anchor'),
    ('AWS',             'TECH',   'Cloud'),
    ('Flutter',         'TECH',   'DeviceMobile'),
    ('Kotlin',          'TECH',   'Code'),
    ('Swift',           'TECH',   'Wind'),
    ('Figma',           'DESIGN', 'PencilSimple'),
    ('Cybersecurity',   'TECH',   'Shield'),
    ('Data Science',    'DATA',   'ChartPie'),
    ('Deep Learning',   'DATA',   'BrainCircuit'),
    ('Unity',           'TECH',   'GameController'),
    ('Solidity',        'TECH',   'CurrencyEth')
) AS v(name, cat, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM skills s WHERE LOWER(s.name) = LOWER(v.name)
);

-- ─────────────────────────────────────────────────────────────
-- BƯỚC 2: Tạo 10 Mentor Users
-- Password mặc định: Mentor@123
-- BCrypt hash của "Mentor@123" (cost=10):
-- $2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6
-- ─────────────────────────────────────────────────────────────

INSERT INTO users (
    id, email, password, full_name, status, role,
    credits_balance, trust_score, bio, has_password, created_at, updated_at
)
VALUES
-- Mentor 1: Java / Spring Boot
(
    gen_random_uuid(),
    'mentor.java@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Nguyễn Văn An',
    'ACTIVE', 'USER',
    200, 90,
    'Senior Java Developer với 7 năm kinh nghiệm tại các công ty fintech. Chuyên sâu Spring Boot, Microservices, JPA.',
    true, NOW(), NOW()
),
-- Mentor 2: Python Data Science
(
    gen_random_uuid(),
    'mentor.python@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Trần Thị Bảo',
    'ACTIVE', 'USER',
    150, 88,
    'Data Scientist tại Tiki, 5 năm làm việc với Python, Pandas, Scikit-learn, TensorFlow. Đam mê dạy học.',
    true, NOW(), NOW()
),
-- Mentor 3: React / Frontend
(
    gen_random_uuid(),
    'mentor.react@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Lê Minh Cường',
    'ACTIVE', 'USER',
    180, 92,
    'Frontend Lead tại một startup SaaS. 4 năm với React, TypeScript, Redux. Đã mentor 50+ học viên thành công xin việc.',
    true, NOW(), NOW()
),
-- Mentor 4: Machine Learning / AI
(
    gen_random_uuid(),
    'mentor.ml@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Phạm Thị Duyên',
    'ACTIVE', 'USER',
    300, 95,
    'AI Researcher, PhD tại ĐH Bách Khoa. Chuyên Machine Learning, Deep Learning, NLP. Công bố 10+ bài báo khoa học.',
    true, NOW(), NOW()
),
-- Mentor 5: UI/UX Design / Figma
(
    gen_random_uuid(),
    'mentor.uiux@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Hoàng Thị Em',
    'ACTIVE', 'USER',
    120, 85,
    'UX Designer tại Grab Vietnam. 3 năm thiết kế sản phẩm B2C/B2B. Portfolio với 20+ dự án thực tế.',
    true, NOW(), NOW()
),
-- Mentor 6: DevOps / Docker / AWS
(
    gen_random_uuid(),
    'mentor.devops@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Vũ Đức Phong',
    'ACTIVE', 'USER',
    250, 87,
    'DevOps Engineer tại VNG với 6 năm kinh nghiệm. AWS Certified Solutions Architect. Docker, K8s, CI/CD pipeline.',
    true, NOW(), NOW()
),
-- Mentor 7: Mobile Flutter
(
    gen_random_uuid(),
    'mentor.flutter@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Đinh Thị Giang',
    'ACTIVE', 'USER',
    130, 83,
    'Mobile Developer 4 năm, chuyên Flutter và Dart. Đã publish 5 app lên Google Play & App Store.',
    true, NOW(), NOW()
),
-- Mentor 8: Node.js / Backend JS
(
    gen_random_uuid(),
    'mentor.nodejs@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Ngô Thành Hưng',
    'ACTIVE', 'USER',
    160, 86,
    'Backend Developer tại MoMo. 5 năm với Node.js, Express, NestJS, MongoDB, PostgreSQL.',
    true, NOW(), NOW()
),
-- Mentor 9: Cybersecurity
(
    gen_random_uuid(),
    'mentor.security@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Bùi Quang Ích',
    'ACTIVE', 'USER',
    200, 91,
    'Penetration Tester & Security Consultant. CEH, OSCP certified. 8 năm kinh nghiệm tại các ngân hàng lớn.',
    true, NOW(), NOW()
),
-- Mentor 10: Data Analysis / SQL / Power BI
(
    gen_random_uuid(),
    'mentor.data@skillsync.com',
    '$2a$10$Ql.IFy/Pf9dGBm/IxYrLEeHSOIhR.FO.Z/YWEHbDYRuq5lNpjHJe6',
    'Cao Thị Kim',
    'ACTIVE', 'USER',
    140, 84,
    'Data Analyst tại Shopee Vietnam. 4 năm phân tích dữ liệu, SQL nâng cao, Power BI, Python pandas.',
    true, NOW(), NOW()
)
ON CONFLICT (email) DO NOTHING;


-- ─────────────────────────────────────────────────────────────
-- BƯỚC 3: Gán Teaching Skills cho từng Mentor
-- Tất cả đều APPROVED để AI tìm được
-- ─────────────────────────────────────────────────────────────

-- Helper: Lấy user_id và skill_id theo email/name
-- Sau đó insert user_teaching_skills

-- ── Mentor 1: Nguyễn Văn An — Java + Spring Boot ─────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '7 năm lập trình Java chuyên nghiệp, từng làm tại FPT Software và Techcombank. Giảng dạy từ zero đến hero.',
    'Học viên có thể viết ứng dụng Java cơ bản, hiểu OOP, Collections, Exception handling.',
    'Học qua project thực tế. Mỗi buổi build 1 tính năng nhỏ hoàn chỉnh.',
    15, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.java@skillsync.com' AND LOWER(s.name) = 'java'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'Chuyên sâu Spring Boot từ phiên bản 2.x đến 3.x. REST API, Security, JPA, Unit Testing.',
    'Xây dựng được REST API production-ready với Spring Boot, JWT auth, Swagger docs.',
    'Lý thuyết ngắn gọn → code along → review code → homework project.',
    20, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.java@skillsync.com' AND LOWER(s.name) = 'spring boot'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'ADVANCED',
    'Microservices với Spring Cloud, Kafka, Docker. Giải quyết bài toán scale hàng triệu user.',
    'Thiết kế và triển khai hệ thống Microservices hoàn chỉnh với CI/CD.',
    'Deep-dive architecture, code review intensive, production case studies.',
    30, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.java@skillsync.com' AND LOWER(s.name) = 'spring boot'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 2: Trần Thị Bảo — Python + Machine Learning ──────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '5 năm dùng Python trong data science. Dạy Python từ syntax cơ bản đến các thư viện phổ biến.',
    'Viết được scripts Python thực tế, sử dụng Pandas và Matplotlib để phân tích dữ liệu cơ bản.',
    'Học bằng notebook Jupyter, dataset thực tế từ Kaggle.',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.python@skillsync.com' AND LOWER(s.name) = 'python'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'Scikit-learn, feature engineering, model evaluation. Đã xây dựng 15+ ML models trong production tại Tiki.',
    'Xây dựng và deploy hoàn chỉnh một ML pipeline: từ EDA đến model serving.',
    'Project-based: mỗi học viên chọn 1 dataset riêng và build end-to-end project.',
    18, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.python@skillsync.com' AND LOWER(s.name) = 'machine learning'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    'Dạy Data Analysis từ Excel đến Python Pandas. Nhiều học viên chuyển ngành thành công.',
    'Phân tích được dataset thực tế, vẽ biểu đồ và trình bày insight rõ ràng.',
    'Case study từ ngành e-commerce và tài chính. Kết hợp Python và Google Sheets.',
    14, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.python@skillsync.com' AND LOWER(s.name) = 'data analysis'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 3: Lê Minh Cường — React + JavaScript ─────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '4 năm React chuyên nghiệp. Đã mentored 50+ học viên, nhiều người đã được nhận vào các công ty lớn.',
    'Xây dựng được ứng dụng React đầy đủ: components, hooks, state management, API call.',
    'Code live trong mỗi buổi học. Giao bài tập clone web thực tế (Trello, GitHub...).',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.react@skillsync.com' AND LOWER(s.name) = 'react'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'React + TypeScript + Redux Toolkit. Performance optimization, testing với Jest và React Testing Library.',
    'Refactor legacy code, tích hợp TypeScript vào project React hiện có, tối ưu bundle size.',
    'Review code thực tế của học viên, giải thích từng anti-pattern và cách fix.',
    20, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.react@skillsync.com' AND LOWER(s.name) = 'react'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    'Dạy JavaScript ES6+ từ cơ bản. Phủ đầy đủ closures, async/await, modules, DOM manipulation.',
    'Hiểu sâu JavaScript core concepts, viết code clean và không cần Google liên tục.',
    'Flashcard + coding challenge hàng ngày. Giải thích qua diagram và animation.',
    10, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.react@skillsync.com' AND LOWER(s.name) = 'javascript'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 4: Phạm Thị Duyên — Machine Learning + Deep Learning ──

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'Research PhD, 10+ publication về ML/NLP. Dạy từ linear regression đến transformer architecture.',
    'Triển khai được thuật toán ML từ scratch, hiểu sâu toán học đằng sau model.',
    'Math-first approach: hiểu formula trước khi code. Paper review hàng tuần.',
    25, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.ml@skillsync.com' AND LOWER(s.name) = 'machine learning'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'ADVANCED',
    'Deep Learning với PyTorch và TensorFlow. CNN, RNN, Transformer, BERT fine-tuning.',
    'Fine-tune pre-trained model cho bài toán domain-specific. Hiểu attention mechanism sâu.',
    'Reproduce paper + implement from scratch. Heavy on theory + experiments.',
    35, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.ml@skillsync.com' AND LOWER(s.name) = 'deep learning'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    '5 năm Python scientific stack: NumPy, SciPy, Matplotlib, Seaborn, Jupyter.',
    'Sử dụng Python thành thạo cho data science và automation scripts.',
    'Hands-on với real datasets. Từng học viên được assign 1 mini-project thực tế.',
    18, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.ml@skillsync.com' AND LOWER(s.name) = 'python'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 5: Hoàng Thị Em — UI/UX Design + Figma ────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '3 năm UX Design tại Grab. Dạy design thinking, wireframing, user research cho người mới.',
    'Tạo được wireframe và prototype cơ bản bằng Figma, hiểu UX process từ A-Z.',
    'Portfolio-driven: mỗi buổi học đều ra sản phẩm đưa vào portfolio.',
    10, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.uiux@skillsync.com' AND LOWER(s.name) = 'ui/ux design'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    'Figma poweruser: auto-layout, components, variants, prototyping, dev handoff.',
    'Thành thạo Figma từ frame đến interactive prototype. Biết handoff cho developer.',
    'Screen clone từ app thực tế (Shopee, Be, MoMo). Feedback trực tiếp trên Figma.',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.uiux@skillsync.com' AND LOWER(s.name) = 'figma'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'Advanced Figma: design system, component library, branching, plugins automation.',
    'Xây dựng design system hoàn chỉnh cho team. Giảm thời gian design 60%.',
    'Build design system from scratch cùng học viên. Sử dụng Figma + Storybook.',
    18, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.uiux@skillsync.com' AND LOWER(s.name) = 'figma'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 6: Vũ Đức Phong — Docker + AWS + Kubernetes ───────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '6 năm DevOps tại VNG. Dạy Docker từ căn bản: containerize app, docker-compose, basic networking.',
    'Containerize được bất kỳ app nào, dùng docker-compose cho local dev environment.',
    'Hands-on hoàn toàn. Mỗi học viên có VM riêng để practice.',
    15, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.devops@skillsync.com' AND LOWER(s.name) = 'docker'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'AWS Solutions Architect. EC2, RDS, S3, Lambda, ECS, CloudWatch, IAM, VPC.',
    'Triển khai được ứng dụng web lên AWS với auto-scaling và monitoring.',
    'Lab-based: mỗi bài học là 1 AWS lab thực hành. Chi phí AWS tối thiểu.',
    22, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.devops@skillsync.com' AND LOWER(s.name) = 'aws'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'ADVANCED',
    'Kubernetes production: cluster management, Helm charts, RBAC, HPA, service mesh (Istio).',
    'Quản lý K8s cluster production, deploy microservices, setup monitoring stack.',
    'CKA exam preparation path. Real cluster trên AWS EKS.',
    30, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.devops@skillsync.com' AND LOWER(s.name) = 'kubernetes'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 7: Đinh Thị Giang — Flutter Mobile ────────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '4 năm Flutter, 5 app trên store. Dạy từ Dart syntax đến mobile UI cơ bản.',
    'Tự viết được ứng dụng Flutter đơn giản và publish lên Google Play.',
    'Build clone app nổi tiếng theo từng bước (Instagram, Grab...). Code review nghiêm túc.',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.flutter@skillsync.com' AND LOWER(s.name) = 'flutter'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'State management (Bloc, Riverpod), REST API integration, Firebase, local storage.',
    'Xây dựng app Flutter full-stack với backend API, auth, push notification.',
    'Project mentor: học viên build app của riêng mình, có review hàng tuần.',
    18, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.flutter@skillsync.com' AND LOWER(s.name) = 'flutter'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 8: Ngô Thành Hưng — Node.js Backend ───────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '5 năm Node.js tại MoMo. Dạy từ EventLoop, async/await đến xây dựng REST API với Express.',
    'Xây dựng REST API hoàn chỉnh với Express, JWT auth, MongoDB/PostgreSQL.',
    'Mỗi buổi build 1 endpoint thực tế từ design → code → test → deploy.',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.nodejs@skillsync.com' AND LOWER(s.name) = 'node.js'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'NestJS architecture, microservices, Kafka integration, Redis caching, testing.',
    'Thiết kế và implement hệ thống backend scalable với NestJS và microservices.',
    'Architecture-first: vẽ diagram trước, code sau. Code review bắt buộc.',
    20, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.nodejs@skillsync.com' AND LOWER(s.name) = 'node.js'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'TypeScript chuyên sâu với Node.js: generics, decorators, type guards, utility types.',
    'Chuyển đổi project JavaScript sang TypeScript, kiểm soát type safety 100%.',
    'Refactor-based: lấy code JS của học viên và cùng migrate sang TypeScript.',
    16, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.nodejs@skillsync.com' AND LOWER(s.name) = 'typescript'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 9: Bùi Quang Ích — Cybersecurity ──────────────────

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '8 năm Penetration Testing, CEH & OSCP certified. Dạy security mindset và web security basics.',
    'Hiểu OWASP Top 10, nhận biết lỗ hổng cơ bản và cách phòng chống trong ứng dụng web.',
    'CTF-based learning. Thực hành trên lab riêng (Hack The Box, TryHackMe).',
    18, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.security@skillsync.com' AND LOWER(s.name) = 'cybersecurity'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'Penetration testing methodology: recon, scanning, exploitation, post-exploitation, reporting.',
    'Thực hiện được pentest hoàn chỉnh, viết báo cáo vulnerability chi tiết và chuyên nghiệp.',
    'Red team vs Blue team exercises. Real-world engagement simulation.',
    28, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.security@skillsync.com' AND LOWER(s.name) = 'cybersecurity'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ── Mentor 10: Cao Thị Kim — SQL + Data Analysis + Power BI ──

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    '4 năm SQL tại Shopee. Dạy SQL từ SELECT cơ bản đến JOIN phức tạp, subquery, window functions.',
    'Viết được SQL query phân tích nghiệp vụ thực tế, tối ưu query đơn giản.',
    'Dataset từ e-commerce thực tế. Bài tập theo kiểu interview SQL questions.',
    10, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.data@skillsync.com' AND LOWER(s.name) = 'sql'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'INTERMEDIATE',
    'SQL advanced: CTE, stored procedures, indexes, query plans, PostgreSQL/MySQL tuning.',
    'Tối ưu được slow query, hiểu execution plan, thiết kế schema hiệu quả.',
    'Analyze slow queries từ production Shopee (đã anonymized). Very practical.',
    16, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.data@skillsync.com' AND LOWER(s.name) = 'sql'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    'Power BI từ đầu: kết nối data source, DAX cơ bản, xây dựng dashboard tương tác.',
    'Tạo được dashboard Power BI đẹp và chuyên nghiệp từ dữ liệu thực tế.',
    'Template-driven: học viên download template, customize theo data của mình.',
    12, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.data@skillsync.com' AND LOWER(s.name) = 'power bi'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;

INSERT INTO user_teaching_skills (
    id, user_id, skill_id, level,
    experience_desc, outcome_desc, teaching_style,
    credits_per_hour, verification_status, verified_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    s.id,
    'BEGINNER',
    'Data Analysis workflow hoàn chỉnh: collect → clean → analyze → visualize → present.',
    'Phân tích được business problem từ đầu đến cuối, present kết quả cho stakeholder.',
    'Storytelling với data. Mỗi project là 1 real business case từ Shopee hoặc Lazada.',
    14, 'APPROVED', NOW(), NOW(), NOW()
FROM users u, skills s
WHERE u.email = 'mentor.data@skillsync.com' AND LOWER(s.name) = 'data analysis'
ON CONFLICT (user_id, skill_id, level) DO NOTHING;


-- ─────────────────────────────────────────────────────────────
-- BƯỚC 4: Verify data đã insert
-- ─────────────────────────────────────────────────────────────

-- Kiểm tra tất cả mentor và teaching skills đã được tạo
SELECT
    u.full_name           AS mentor,
    u.email,
    s.name                AS skill,
    uts.level,
    uts.verification_status,
    uts.credits_per_hour,
    LEFT(uts.experience_desc, 60) AS experience_preview
FROM user_teaching_skills uts
JOIN users u  ON uts.user_id  = u.id
JOIN skills s ON uts.skill_id = s.id
WHERE u.email LIKE 'mentor.%@skillsync.com'
ORDER BY u.full_name, s.name, uts.level;
