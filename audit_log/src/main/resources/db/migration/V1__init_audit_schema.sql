create table audit_log
(
    audit_id        uuid primary key,            -- Klasický Primary Key
    event_time      timestamp with time zone default now() not null,
    user_id         uuid,
    keycloak_id     varchar(100),
    entity_name     varchar(100) not null,
    entity_id       uuid,
    action          varchar(100) not null,
    message         text,
    source_service  varchar(100) not null,
    correlation_id  varchar(100),
    payload         jsonb,                       -- Stále používame JSONB (je super)
    ip_address      varchar(50),
    user_agent      text,
    created_at      timestamp with time zone default now()
);

-- Indexy pre rýchlosť (aj v demo appke chceš, aby to lietalo)
create index idx_audit_time on audit_log(event_time);
create index idx_audit_entity on audit_log(entity_name, entity_id);
create index idx_audit_correlation on audit_log(correlation_id);