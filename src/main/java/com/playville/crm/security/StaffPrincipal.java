package com.playville.crm.security;

import com.playville.crm.entity.Staff;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class StaffPrincipal implements UserDetails {

    private final Integer id;
    private final Integer branchId;
    private final String  branchCode;
    private final String  username;
    private final String  password;
    private final String  role;
    private final boolean active;
    private final Integer tokenVersion;

    public StaffPrincipal(Staff staff) {
        this.id           = staff.getId();
        this.branchId     = staff.getBranch().getId();
        this.branchCode   = staff.getBranch().getBranchCode();
        this.username     = staff.getUsername();
        this.password     = staff.getPasswordHash();
        this.role         = staff.getRole().name();
        this.active       = staff.isActive();
        this.tokenVersion = staff.getTokenVersion();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}