package com.playville.crm.repository;

import com.playville.crm.entity.CronLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CronLogRepository extends JpaRepository<CronLog, Integer> {}