package com.playville.crm.repository;
import com.playville.crm.entity.OnboardingIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OnboardingIdempotencyRepository extends JpaRepository<OnboardingIdempotency, String> { }
