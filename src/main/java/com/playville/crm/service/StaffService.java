package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.staff.*;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Staff;
import com.playville.crm.entity.enums.StaffRole;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository  staffRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder  passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffDto> getStaffForCurrentBranch() {
        return staffRepository
                .findByBranchIdAndIsActiveTrue(BranchContext.getBranchId())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public StaffDto createStaff(CreateStaffRequest req) {
        StaffRole requestedRole = req.getRole() != null ? req.getRole() : StaffRole.staff;
        validateRoleAssignment(requestedRole);

        if (staffRepository.existsByUsername(req.getUsername()))
            throw new DuplicateResourceException(
                    "Username already taken: " + req.getUsername());
        if (req.getEmail() != null && staffRepository.existsByEmail(req.getEmail()))
            throw new DuplicateResourceException(
                    "Email already registered: " + req.getEmail());

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        Staff staff = Staff.builder()
                .branch(branch)
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(requestedRole)
                .build();

        return toDto(staffRepository.save(staff));
    }

    @Transactional
    public void deactivateStaff(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
        if (!staff.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        staff.setActive(false);
        staffRepository.save(staff);
    }

    private void validateRoleAssignment(StaffRole requestedRole) {
        if (requestedRole == StaffRole.staff || currentUserIsAdmin()) {
            return;
        }
        throw new AccessDeniedException("Only admins can create manager or admin users");
    }

    private boolean currentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
    }

    private StaffDto toDto(Staff s) {
        return StaffDto.builder()
                .id(s.getId())
                .branchId(s.getBranch().getId())
                .branchCode(s.getBranch().getBranchCode())
                .fullName(s.getFullName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .username(s.getUsername())
                .role(s.getRole())
                .isActive(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
