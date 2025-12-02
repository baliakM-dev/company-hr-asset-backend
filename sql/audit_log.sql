create table audit_log
(
    audit_id        uuid primary key,
    event_time      timestamp with time zone default now() not null,
    user_id         uuid,                                        -- referencia na employees.employee_id ak existuje
    keycloak_id     varchar(100),                                -- fallback a aj pre používateľov bez DB účtu (log in/out)
    entity_name     varchar(100) not null,                       -- EMPLOYEE, REQUEST, ASSET...
    entity_id       uuid,                                        -- konkrétny objekt
    action          varchar(50) not null,                        -- CREATED, UPDATED, DELETED, LOGIN...
    source_service  varchar(100) not null,                       -- HR_SERVICE, ASSET_SERVICE, AUDIT_SERVICE
    correlation_id  varchar(100),                                -- na sledovanie requestov
    payload         jsonb,                                       -- diff alebo celé dáta
    ip_address      varchar(50),                                 -- ak vieš poslať z gateway
    user_agent      varchar(255),
    created_at      timestamp with time zone default now()
);

create index idx_audit_time on audit_log(event_time);
create index idx_audit_entity on audit_log(entity_name, entity_id);
create index idx_audit_user on audit_log(user_id);
