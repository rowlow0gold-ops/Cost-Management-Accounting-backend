-- ===== Departments =====
INSERT INTO department (code, name) VALUES ('HQ1','경영기획본부');
INSERT INTO department (code, name) VALUES ('HQ2','자산운용본부');
INSERT INTO department (code, name) VALUES ('HQ3','IT본부');
INSERT INTO department (code, name) VALUES ('HQ4','리스크관리본부');
INSERT INTO department (code, name) VALUES ('HQ5','컴플라이언스본부');

-- (Demo users are created programmatically in DataBootstrap with real BCrypt hashes)

-- ===== Standard rates =====
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-04','사원',25000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-04','대리',35000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-04','과장',50000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-04','차장',65000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-04','부장',85000);

-- ===== Employees (10 per department, 50 total) =====
-- Dept 1
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0001','김민수','사원',1,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0002','이영희','사원',1,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0003','박지훈','대리',1,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0004','최서연','대리',1,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0005','정우진','대리',1,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0006','강하늘','과장',1,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0007','조민재','과장',1,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0008','윤서아','차장',1,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0009','임도현','차장',1,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0010','한지민','부장',1,85000);
-- Dept 2
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0011','오은서','사원',2,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0012','신유진','사원',2,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0013','홍기범','대리',2,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0014','송채원','대리',2,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0015','문수아','대리',2,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0016','노태현','과장',2,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0017','구민호','과장',2,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0018','유나래','차장',2,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0019','곽재훈','차장',2,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0020','백승호','부장',2,85000);
-- Dept 3
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0021','권다은','사원',3,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0022','전소희','사원',3,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0023','류준영','대리',3,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0024','서가영','대리',3,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0025','남궁현','대리',3,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0026','채윤석','과장',3,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0027','지은별','과장',3,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0028','우상현','차장',3,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0029','안세빈','차장',3,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0030','배준영','부장',3,85000);
-- Dept 4
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0031','심예린','사원',4,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0032','전미라','사원',4,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0033','석호준','대리',4,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0034','민가현','대리',4,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0035','도재민','대리',4,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0036','함은수','과장',4,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0037','진세호','과장',4,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0038','오리아','차장',4,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0039','감지효','차장',4,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0040','피영준','부장',4,85000);
-- Dept 5
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0041','진수아','사원',5,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0042','임혁','사원',5,25000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0043','반지현','대리',5,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0044','노정훈','대리',5,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0045','유선아','대리',5,35000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0046','금하늘','과장',5,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0047','단우현','과장',5,50000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0048','갈예진','차장',5,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0049','맹승현','차장',5,65000);
INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES ('E0050','우경한','부장',5,85000);

-- ===== Projects (20 projects, 4 per department) =====
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-001','경영전략 수립',          1, 1600, 100000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-002','중장기 사업계획',        1, 1400,  90000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-003','예산편성 시스템',        1, 1200,  80000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-004','조직혁신 프로젝트',      1, 1000,  70000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-005','자산배분 모델링',        2, 1800, 120000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-006','대체투자 운용',          2, 1600, 110000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-007','글로벌 펀드 운용',       2, 1500, 105000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-008','채권 포트폴리오 최적화', 2, 1400,  95000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-009','코어뱅킹 차세대',        3, 2400, 180000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-010','클라우드 마이그레이션',  3, 2000, 150000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-011','데이터 플랫폼 구축',     3, 1800, 140000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-012','보안관제 고도화',        3, 1500, 110000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-013','VaR 모델 개선',          4, 1400,  95000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-014','신용리스크 관리',        4, 1300,  90000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-015','시장리스크 한도관리',    4, 1200,  85000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-016','스트레스 테스트',        4, 1100,  80000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-017','내부통제 점검',          5, 1200,  85000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-018','자금세탁방지 (AML)',     5, 1400, 100000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-019','법규 변경 대응',         5, 1100,  80000000, DATE '2025-01-01', DATE '2026-12-31');
INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES
 ('PRJ-020','컴플라이언스 교육',      5,  900,  60000000, DATE '2025-01-01', DATE '2026-12-31');

-- ===== Standard rates for additional months =====
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-01','사원',24500);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-01','대리',34000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-01','과장',49000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-01','차장',64000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2026-01','부장',83000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2025-12','사원',24000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2025-12','대리',33000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2025-12','과장',48000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2025-12','차장',63000);
INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES ('2025-12','부장',82000);

-- ===== Indirect cost items =====
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',1,'임차료',12000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',1,'전산비',8000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',2,'전산비',15000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',3,'전산비',25000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',3,'외주용역비',18000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',4,'리서치비',6000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-04','INDIRECT',5,'법무자문비',4000000);

-- Indirect cost items for earlier months too
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-03','INDIRECT',1,'임차료',12000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-03','INDIRECT',3,'전산비',24000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-03','INDIRECT',4,'리서치비',5500000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-02','INDIRECT',1,'임차료',12000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-02','INDIRECT',3,'전산비',22000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-01','INDIRECT',1,'임차료',12000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2026-01','INDIRECT',3,'전산비',20000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2025-12','INDIRECT',1,'임차료',11500000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2025-12','INDIRECT',3,'전산비',18000000);
INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES ('2025-12','INDIRECT',4,'리서치비',5000000);

-- ===== Sample timesheets (a few APPROVED entries so dashboard isn't empty) =====
-- Each project gets one approved entry from one of its dept's employees
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (1, 1, DATE '2026-04-02', 80, '경영전략 분석', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (3, 2, DATE '2026-04-02', 75, '사업계획 작성', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (6, 3, DATE '2026-04-02', 60, '예산 시스템 개발', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (8, 4, DATE '2026-04-02', 50, '조직혁신 분석', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (11, 5, DATE '2026-04-02', 90, '자산배분 모델', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (13, 6, DATE '2026-04-02', 80, '대체투자 평가', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (16, 7, DATE '2026-04-02', 70, '글로벌 펀드 분석', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (18, 8, DATE '2026-04-02', 65, '채권 최적화 모델', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (21, 9, DATE '2026-04-02', 120, '코어뱅킹 설계', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (23, 10, DATE '2026-04-02', 100, '클라우드 마이그레이션', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (26, 11, DATE '2026-04-02', 95, '데이터 플랫폼 구축', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (28, 12, DATE '2026-04-02', 75, '보안관제 고도화', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (31, 13, DATE '2026-04-02', 70, 'VaR 모델 개선', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (33, 14, DATE '2026-04-02', 65, '신용리스크 분석', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (36, 15, DATE '2026-04-02', 60, '시장리스크 한도', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (38, 16, DATE '2026-04-02', 55, '스트레스 테스트', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (41, 17, DATE '2026-04-02', 60, '내부통제 점검', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (43, 18, DATE '2026-04-02', 70, 'AML 시스템 운영', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (46, 19, DATE '2026-04-02', 55, '법규 변경 대응', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (48, 20, DATE '2026-04-02', 45, '컴플라이언스 교육', 'APPROVED', TIMESTAMP '2026-04-15 09:00:00', TIMESTAMP '2026-04-16 10:00:00', 'manager@noaats.com');

-- A few SUBMITTED entries waiting for approval
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at)
VALUES (2, 1, DATE '2026-04-19', 6, '리서치 보조', 'SUBMITTED', TIMESTAMP '2026-04-19 18:00:00');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at)
VALUES (4, 2, DATE '2026-04-19', 8, '문서 정리', 'SUBMITTED', TIMESTAMP '2026-04-19 18:00:00');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at)
VALUES (12, 5, DATE '2026-04-19', 7, '시장 분석', 'SUBMITTED', TIMESTAMP '2026-04-19 18:00:00');

-- A DRAFT
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status)
VALUES (22, 9, DATE '2026-04-20', 8, '설계 검토', 'DRAFT');

-- ===== Sample timesheets for 2025-12 (prior year data) =====
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (1, 1, DATE '2025-12-03', 40, '전년도 경영전략', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (3, 2, DATE '2025-12-04', 45, '사업계획 마감', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (8, 4, DATE '2025-12-05', 40, '조직혁신 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (11, 5, DATE '2025-12-06', 85, '자산배분 분석', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (13, 6, DATE '2025-12-07', 70, '대체투자 마감', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (18, 8, DATE '2025-12-08', 60, '채권 최적화 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (21, 9, DATE '2025-12-09', 110, '코어뱅킹 설계', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (23, 10, DATE '2025-12-10', 95, '클라우드 POC', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (28, 12, DATE '2025-12-11', 70, '보안관제 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (31, 13, DATE '2025-12-12', 60, 'VaR 이전모델', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (36, 15, DATE '2025-12-15', 50, '시장리스크 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (41, 17, DATE '2025-12-16', 55, '내부통제 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (46, 19, DATE '2025-12-17', 50, '법규 대응 전년', 'APPROVED', TIMESTAMP '2025-12-18 09:00:00', TIMESTAMP '2025-12-20 10:00:00', 'manager@noaats.com');

-- ===== Sample timesheets for previous months (2026-03, 2026-02, 2026-01, 2025-12) =====
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (1, 1, DATE '2026-03-05', 60, '경영전략', 'APPROVED', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (11, 5, DATE '2026-03-05', 70, '자산배분', 'APPROVED', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (21, 9, DATE '2026-03-05', 110, '코어뱅킹', 'APPROVED', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (31, 13, DATE '2026-03-05', 65, 'VaR', 'APPROVED', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (41, 17, DATE '2026-03-05', 55, '내부통제', 'APPROVED', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-22 10:00:00', 'manager@noaats.com');

INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (3, 2, DATE '2026-02-05', 55, '사업계획', 'APPROVED', TIMESTAMP '2026-02-20 09:00:00', TIMESTAMP '2026-02-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (13, 6, DATE '2026-02-05', 75, '대체투자', 'APPROVED', TIMESTAMP '2026-02-20 09:00:00', TIMESTAMP '2026-02-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (23, 10, DATE '2026-02-05', 95, '클라우드', 'APPROVED', TIMESTAMP '2026-02-20 09:00:00', TIMESTAMP '2026-02-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (33, 14, DATE '2026-02-05', 60, '신용리스크', 'APPROVED', TIMESTAMP '2026-02-20 09:00:00', TIMESTAMP '2026-02-22 10:00:00', 'manager@noaats.com');

INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (6, 3, DATE '2026-01-05', 50, '예산시스템', 'APPROVED', TIMESTAMP '2026-01-20 09:00:00', TIMESTAMP '2026-01-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (16, 7, DATE '2026-01-05', 65, '글로벌펀드', 'APPROVED', TIMESTAMP '2026-01-20 09:00:00', TIMESTAMP '2026-01-22 10:00:00', 'manager@noaats.com');
INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email)
VALUES (26, 11, DATE '2026-01-05', 85, '데이터플랫폼', 'APPROVED', TIMESTAMP '2026-01-20 09:00:00', TIMESTAMP '2026-01-22 10:00:00', 'manager@noaats.com');

