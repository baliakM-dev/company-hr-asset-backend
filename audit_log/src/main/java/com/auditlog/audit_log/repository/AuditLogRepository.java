package com.auditlog.audit_log.repository;

import com.auditlog.audit_log.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends
        JpaRepository<AuditLogEntity, UUID>,
        JpaSpecificationExecutor<AuditLogEntity> {
}