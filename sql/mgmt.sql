-- 1. KATEGÓRIE MAJETKU (Ciselnik, ktory spravuje Admin cez UI)
-- Premenoval som to z "Type“ na "Category“, je to presnejsie pre katalóg
create table asset_categories
(
    category_id   uuid primary key,
    name          varchar(100) unique not null, -- Napr. "Laptop", "Monitor", "Kancelárska stolička"
    is_hardware   boolean default true,         -- Pomocny flag, ci to ma seriove cislo atd.
    description   varchar(255),

    created_at    timestamp with time zone default now()
    -- Tu nepotrebujeme zlozity audit update_at/by, staci kto to vytvoril
);
-- TABULKA ZAMESTNANCOV
-- Status je teraz VARCHAR (Enum: ACTIVE, BLOCKED, TERMINATED)
create table employees
(
    employee_id      uuid primary key,
    keycloak_id      varchar(100) unique not null, -- Prepojenie na IAM
    first_name       varchar(50)  not null,
    last_name        varchar(50)  not null,
    email            varchar(100) unique not null,
    phone            varchar(25),
    -- ENUMS (uložené, ako string)
    status           varchar(50)  not null,
    login_name       varchar(50)  unique not null,
    attendance_login varchar(50)  unique not null,

    started_work     date         not null,
    end_work         date,
    termination_reason varchar(255),
    -- AUDIT
    created_at       timestamp with time zone default now(),
    updated_at       timestamp with time zone,
    created_by       uuid, -- Self-reference, ale bez FK constraintu v inicializacii (kruhova zavislost)
    updated_by       uuid
);

-- 2. TABUĽKA ADRIES
-- Typ je teraz VARCHAR (Enum: HOME, MAILING)
create table addresses
(
    address_id   uuid primary key,
    employee_id  uuid references employees (employee_id) not null,
    type         varchar(50)  not null,
    street       varchar(255) not null,
    city         varchar(100) not null,
    postal_code  varchar(20)  not null,
    country      varchar(100) not null,

    created_at   timestamp with time zone default now(),
    updated_at   timestamp with time zone
);
create index idx_addresses_employee on addresses (employee_id);

-- 3. ODDELENIA
-- Opravené: pridaný NÁZOV a typ ako Enum
create table departments
(
    department_id uuid primary key,
    name          varchar(100) not null, -- Napr. "Vývojové oddelenie"
    code          varchar(50) unique not null, -- Napr. "DEV-BA"
    type          varchar(50) not null, -- Enum: IT, HR, SALES
    manager_id    uuid references employees (employee_id),

    created_at    timestamp with time zone default now(),
    updated_at    timestamp with time zone
);
create index idx_departments_manager on departments (manager_id);

-- 4. VÄZBA ZAMESTNANEC <-> ODDELENIE (M:N)
-- Umožňuje byť zamestnancovi vo viacerých oddeleniach
create table department_employees
(
    department_id uuid references departments (department_id) not null,
    employee_id   uuid references employees (employee_id) not null,
    is_primary    boolean default false not null, -- Ktoré oddelenie je hlavné
    primary key (department_id, employee_id)
);

-- 5. MAJETOK (ASSETS)
create table assets
(
    asset_id      uuid primary key,
    name          varchar(150) not null,
    serial_number varchar(100), -- Nie kazdy majetok musi mat SN (napr. Stol), takze moze byt nullable

    -- VAZBA NA TABULKU (Dynamicka kategoria)
    category_id   uuid references asset_categories(category_id) not null,

    -- STATUS JE STALE ENUM/VARCHAR (Staticka logika)
    -- IN_STOCK, ASSIGNED, BROKEN, RETIRED - toto meni tok programu, ostava v kode
    status        varchar(50) not null,

    purchase_date date,
    warranty_until date,
    price         numeric(10, 2), -- Hodilo by sa evidovat aj cenu

    created_at    timestamp with time zone default now(),
    updated_at    timestamp with time zone
);

-- 6. PRIDELENIE MAJETKU
create table asset_assignments
(
    assignment_id uuid primary key,
    asset_id      uuid references assets (asset_id) not null,
    employee_id   uuid references employees (employee_id) not null,
    assigned_from timestamp with time zone not null,
    assigned_to   timestamp with time zone, -- Null = stále ho má

    created_at    timestamp with time zone default now(),
    updated_at    timestamp with time zone
);
create index idx_asset_assignments_curr on asset_assignments(asset_id) where assigned_to is null;

-- 7. POŽIADAVKY (Obecné: HW, SW, Access)
create table requests
(
    request_id     uuid primary key,
    employee_id    uuid references employees (employee_id) not null,

    type           varchar(50) not null, -- Enum: HARDWARE, ACCESS, REPAIR
    status         varchar(50) not null, -- Enum: PENDING, APPROVED, REJECTED

    subject        varchar(150) not null,
    description    text,

    -- Riešiteľ / Schvaľovateľ
    assigned_to    uuid references employees (employee_id),
    resolution_note text,

    created_at     timestamp with time zone default now(),
    updated_at     timestamp with time zone
);

-- 8. ABSENCIE / DOVOLENKY
-- Oddelené od requests, lebo majú špecifickú logiku (Dátumy od-do)
create table attendance_requests
(
    attendance_request_id uuid primary key,
    employee_id           uuid references employees (employee_id) not null,

    type                  varchar(50) not null, -- Enum: VACATION, SICK_DAY, HO
    status                varchar(50) not null, -- Enum: PENDING, APPROVED

    date_from             date not null,
    date_to               date not null,
    reason                varchar(255),

    approved_by           uuid references employees (employee_id),

    created_at            timestamp with time zone default now(),
    updated_at            timestamp with time zone
);

-- 9. DOCHÁDZKA LOG (Pípanie kartou)
create table attendance_logs
(
    log_id         uuid primary key,
    employee_id    uuid references employees (employee_id) not null,
    check_in       timestamp with time zone not null,
    check_out      timestamp with time zone,

    type           varchar(50) not null, -- Enum: WORK, BREAK, DOCTOR
    note           varchar(255),

    created_at     timestamp with time zone default now()

);
create index idx_attendance_logs_emp_date on attendance_logs(employee_id, check_in);

CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_login_name ON employees (login_name);
CREATE INDEX idx_employees_started_work ON employees (started_work);
CREATE INDEX idx_employees_keycloak_id ON employees (keycloak_id);
CREATE INDEX idx_employees_name ON employees (last_name, first_name);
CREATE INDEX idx_employees_created_by ON employees (created_by);
CREATE INDEX idx_addresses_address_type ON addresses (type);
