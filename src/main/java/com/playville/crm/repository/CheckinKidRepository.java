package com.playville.crm.repository;

import com.playville.crm.entity.CheckinKid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckinKidRepository extends JpaRepository<CheckinKid, Integer> {
    List<CheckinKid> findByCheckinId(Integer checkinId);
    List<CheckinKid> findByCheckinIdAndSessionUsedFalse(Integer checkinId);
}