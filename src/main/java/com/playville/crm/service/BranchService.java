package com.playville.crm.service;

import com.playville.crm.dto.branch.BranchDto;
import com.playville.crm.dto.branch.UpdateBranchRequest;
import com.playville.crm.entity.Branch;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchDto> getAllActiveBranches() {
        return branchRepository.findAllByIsActiveTrue()
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Integer id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public BranchDto updateBranch(Integer id, UpdateBranchRequest req) {
        Branch branch = findOrThrow(id);
        if (req.getBranchName()        != null) branch.setBranchName(req.getBranchName());
        if (req.getAddress()           != null) branch.setAddress(req.getAddress());
        if (req.getPhone()             != null) branch.setPhone(req.getPhone());
        if (req.getOpenTime()          != null) branch.setOpenTime(req.getOpenTime());
        if (req.getCloseTime()         != null) branch.setCloseTime(req.getCloseTime());
        if (req.getClosedDay()         != null) branch.setClosedDay(req.getClosedDay());
        if (req.getNotificationEmail() != null) branch.setNotificationEmail(req.getNotificationEmail());
        return toDto(branchRepository.save(branch));
    }

    private Branch findOrThrow(Integer id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private BranchDto toDto(Branch b) {
        return BranchDto.builder()
                .id(b.getId())
                .branchCode(b.getBranchCode())
                .branchName(b.getBranchName())
                .address(b.getAddress())
                .city(b.getCity())
                .phone(b.getPhone())
                .openTime(b.getOpenTime())
                .closeTime(b.getCloseTime())
                .closedDay(b.getClosedDay())
                .settlementRate(b.getSettlementRate())
                .notificationEmail(b.getNotificationEmail())
                .isActive(b.isActive())
                .build();
    }
}
