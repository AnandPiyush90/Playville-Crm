package com.playville.crm.auth.service;

import com.playville.crm.entity.Staff;
import com.playville.crm.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetSessionInvalidator {

    private final StaffRepository staffRepository;

    @Transactional
    public void invalidateSessionsForStaff(Staff staff) {
        if (staff == null) {
            return;
        }
        staff.setTokenVersion((staff.getTokenVersion() == null ? 0 : staff.getTokenVersion()) + 1);
        staffRepository.save(staff);
    }
}
