CREATE SCHEMA IF NOT EXISTS actors_learners;
CREATE SCHEMA IF NOT EXISTS actors_instructors;
CREATE SCHEMA IF NOT EXISTS actors_parameterization;
CREATE SCHEMA IF NOT EXISTS actors_companies;

CREATE TABLE actors_learners.learner (
    id UUID PRIMARY KEY
);

CREATE TABLE actors_instructors.instructor (
    id UUID PRIMARY KEY,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE actors_parameterization.status_category (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE actors_parameterization.status (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    status_category_id UUID NOT NULL REFERENCES actors_parameterization.status_category(id),
    is_terminal BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE actors_companies.company (
    id UUID PRIMARY KEY,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE actors_companies.productive_stage (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL REFERENCES actors_learners.learner(id) ON DELETE RESTRICT,
    company_id UUID NOT NULL REFERENCES actors_companies.company(id) ON DELETE RESTRICT,
    supervisor_instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id) ON DELETE RESTRICT,
    status_id UUID NOT NULL REFERENCES actors_parameterization.status(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    planned_end_date DATE,
    total_hours_required INTEGER NOT NULL,
    total_hours_completed INTEGER NOT NULL,
    interruption_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    row_version INTEGER NOT NULL,
    CONSTRAINT ck_productive_stage_dates CHECK (planned_end_date IS NULL OR planned_end_date >= start_date),
    CONSTRAINT ck_productive_stage_hours_required CHECK (total_hours_required > 0),
    CONSTRAINT ck_productive_stage_hours_completed CHECK (total_hours_completed >= 0)
);

CREATE TABLE actors_companies.company_visit (
    id UUID PRIMARY KEY,
    productive_stage_id UUID NOT NULL REFERENCES actors_companies.productive_stage(id) ON DELETE CASCADE,
    instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id) ON DELETE RESTRICT,
    visit_number INTEGER NOT NULL,
    visit_date DATE NOT NULL,
    visit_type VARCHAR(20) NOT NULL,
    learner_performance VARCHAR(20) NOT NULL,
    company_satisfaction VARCHAR(20),
    has_improvement_plan BOOLEAN NOT NULL DEFAULT false,
    first_visit_late BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT ck_company_visit_number CHECK (visit_number >= 1),
    CONSTRAINT ck_company_visit_type CHECK (visit_type IN ('IN_PERSON', 'VIRTUAL', 'PHONE')),
    CONSTRAINT ck_company_visit_performance CHECK (learner_performance IN ('EXCELLENT', 'GOOD', 'ACCEPTABLE', 'DEFICIENT')),
    CONSTRAINT ck_company_visit_satisfaction CHECK (company_satisfaction IS NULL OR company_satisfaction IN ('HIGH', 'MEDIUM', 'LOW'))
);
