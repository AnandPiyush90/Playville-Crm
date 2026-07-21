package com.playville.crm.repository;

import com.playville.crm.entity.CheckinSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CheckinSessionItemRepository extends JpaRepository<CheckinSessionItem, Integer> {
    List<CheckinSessionItem> findByCheckinIdAndStatusOrderByCreatedAtAsc(Integer checkinId, CheckinSessionItem.Status status);
    Optional<CheckinSessionItem> findByIdAndCheckinId(Integer id, Integer checkinId);
    Optional<CheckinSessionItem> findByCheckinIdAndSkuIdAndStatus(Integer checkinId, Integer skuId, CheckinSessionItem.Status status);
}
