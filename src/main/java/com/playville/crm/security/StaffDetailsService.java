package com.playville.crm.security;

import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return staffRepository.findByUsernameAndIsActiveTrue(username)
                .map(StaffPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Staff not found or inactive: " + username));
    }
}