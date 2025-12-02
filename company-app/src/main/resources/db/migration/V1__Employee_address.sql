-- TABULKA ZAMESTNANCOV
-- Status je teraz VARCHAR (Enum: ACTIVE, BLOCKED, TERMINATED)
create table employees
(
    employee_id        uuid primary key,
    keycloak_id        varchar(100) unique not null, -- Prepojenie na IAM
    first_name         varchar(50)         not null,
    last_name          varchar(50)         not null,
    email              varchar(100) unique not null,
    phone              varchar(25),
    -- ENUMS (uložené, ako string)
    status             varchar(50)         not null,
    keycloak_name         varchar(50) unique  not null,
    started_work       date                not null,
    end_work           date,
    termination_reason varchar(255),
    version            bigint              not null default 0,
    -- AUDIT
    created_at         timestamp with time zone     default now(),
    updated_at         timestamp with time zone,
    created_by         uuid,                         -- Self-reference, ale bez FK constraintu v inicializacii (kruhova zavislost)
    updated_by         uuid
);

-- 2. TABUĽKA ADRIES
-- Typ je teraz VARCHAR (Enum: HOME, MAILING)
create table addresses
(
    address_id  uuid primary key,
    employee_id uuid references employees (employee_id) not null,
    type        varchar(50)                             not null,
    street      varchar(255)                            not null,
    city        varchar(100)                            not null,
    postal_code varchar(20)                             not null,
    country     varchar(100)                            not null,
    version     bigint                                  not null default 0,

    created_at  timestamp with time zone                         default now(),
    updated_at  timestamp with time zone
);
-- indexy pre employee
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_login_name ON employees (keycloak_name);
CREATE INDEX idx_employees_started_work ON employees (started_work);
CREATE INDEX idx_employees_keycloak_id ON employees (keycloak_id);
CREATE INDEX idx_employees_name ON employees (last_name, first_name);
-- indexy pre address
CREATE INDEX idx_addresses_address_type ON addresses (type);
create index idx_addresses_employee on addresses (employee_id);


-- DODATOČNÉ CONSTRAINTY PRE AUDIT (Self-reference)
-- Pridávame až tu, aby sme sa vyhli chybe pri vytváraní tabuľky
ALTER TABLE employees
    ADD CONSTRAINT fk_emp_created_by FOREIGN KEY (created_by) REFERENCES employees(employee_id);
ALTER TABLE employees
    ADD CONSTRAINT fk_emp_updated_by FOREIGN KEY (updated_by) REFERENCES employees(employee_id);