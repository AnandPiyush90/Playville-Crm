package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.enquiry.EnquiryRequest;
import com.playville.crm.dto.enquiry.EnquiryResponse;
import com.playville.crm.dto.enquiry.EnquiryStatusRequest;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Customer;
import com.playville.crm.entity.Enquiry;
import com.playville.crm.entity.Kid;
import com.playville.crm.entity.enums.EnquiryStatus;
import com.playville.crm.entity.enums.LeadSource;
import com.playville.crm.exception.ApiConflictException;
import com.playville.crm.exception.BranchAccessDeniedException;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.BranchRepository;
import com.playville.crm.repository.CustomerRepository;
import com.playville.crm.repository.EnquiryRepository;
import com.playville.crm.repository.KidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final KidRepository kidRepository;

    @Transactional
    public EnquiryResponse create(EnquiryRequest request, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        var existing = enquiryRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replayExisting(existing.get(), request);
        }

        Branch branch = currentBranch();
        Enquiry enquiry = Enquiry.builder()
                .branch(branch)
                .parentName(request.getParentName().trim())
                .phoneNumber(normalizePhone(request.getPhoneNumber()))
                .email(blankToNull(request.getEmail()))
                .leadSource(request.getLeadSource())
                .status(initialStatus(request))
                .visitScheduledAt(request.getVisitScheduledAt())
                .childName(blankToNull(request.getChildName()))
                .childDob(request.getChildDob())
                .notes(blankToNull(request.getNotes()))
                .idempotencyKey(key)
                .build();

        try {
            return toResponse(enquiryRepository.saveAndFlush(enquiry));
        } catch (DataIntegrityViolationException ex) {
            return enquiryRepository.findByIdempotencyKey(key)
                    .map(found -> replayExisting(found, request))
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<EnquiryResponse> list(int page, int size, EnquiryStatus status, String search) {
        Integer branchId = BranchContext.getBranchId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (search != null && !search.isBlank()) {
            return enquiryRepository.search(branchId, status, search.trim(), pageable).map(this::toResponse);
        }
        if (status != null) {
            return enquiryRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status, pageable)
                    .map(this::toResponse);
        }
        return enquiryRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EnquiryResponse get(Integer id) {
        return toResponse(findBranchEnquiry(id));
    }

    @Transactional
    public EnquiryResponse update(Integer id, EnquiryRequest request) {
        Enquiry enquiry = findBranchEnquiry(id);
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED_TO_CUSTOMER) {
            throw new BusinessRuleException("Converted enquiries cannot be edited");
        }

        enquiry.setParentName(request.getParentName().trim());
        enquiry.setPhoneNumber(normalizePhone(request.getPhoneNumber()));
        enquiry.setEmail(blankToNull(request.getEmail()));
        enquiry.setLeadSource(request.getLeadSource());
        enquiry.setVisitScheduledAt(request.getVisitScheduledAt());
        enquiry.setChildName(blankToNull(request.getChildName()));
        enquiry.setChildDob(request.getChildDob());
        enquiry.setNotes(blankToNull(request.getNotes()));
        if (enquiry.getStatus() == EnquiryStatus.NEW && request.getVisitScheduledAt() != null) {
            enquiry.setStatus(EnquiryStatus.VISIT_SCHEDULED);
        }

        return toResponse(enquiryRepository.save(enquiry));
    }

    @Transactional
    public EnquiryResponse updateStatus(Integer id, EnquiryStatusRequest request) {
        Enquiry enquiry = findBranchEnquiry(id);
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED_TO_CUSTOMER
                && request.getStatus() != EnquiryStatus.CONVERTED_TO_CUSTOMER) {
            throw new BusinessRuleException("Converted enquiries cannot be moved back to another status");
        }
        if (request.getStatus() == EnquiryStatus.CONVERTED_TO_CUSTOMER) {
            throw new BusinessRuleException("Use the convert endpoint to convert an enquiry to a customer");
        }

        enquiry.setStatus(request.getStatus());
        return toResponse(enquiryRepository.save(enquiry));
    }

    @Transactional
    public EnquiryResponse convert(Integer id) {
        Enquiry enquiry = findBranchEnquiry(id);
        if (enquiry.getConvertedCustomer() != null) {
            return toResponse(enquiry);
        }

        Customer customer = customerRepository.findByPhoneNumber(enquiry.getPhoneNumber())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .phoneNumber(enquiry.getPhoneNumber())
                        .parentName(enquiry.getParentName())
                        .email(enquiry.getEmail())
                        .leadSource(enquiry.getLeadSource() != null ? enquiry.getLeadSource() : LeadSource.Walk_in)
                        .homeBranch(enquiry.getBranch())
                        .firstVisitBranch(enquiry.getBranch())
                        .notes(enquiry.getNotes())
                        .build()));

        if (enquiry.getChildName() != null && enquiry.getChildDob() != null) {
            boolean childExists = kidRepository.findByCustomerIdAndIsActiveTrue(customer.getId()).stream()
                    .anyMatch(kid -> kid.getKidName().equalsIgnoreCase(enquiry.getChildName())
                            && kid.getDob().equals(enquiry.getChildDob()));
            if (!childExists) {
                kidRepository.save(Kid.builder().branch(enquiry.getBranch()).customer(customer)
                        .kidName(enquiry.getChildName()).dob(enquiry.getChildDob()).build());
            }
        }

        enquiry.setConvertedCustomer(customer);
        enquiry.setStatus(EnquiryStatus.CONVERTED_TO_CUSTOMER);
        return toResponse(enquiryRepository.save(enquiry));
    }

    private Enquiry findBranchEnquiry(Integer id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", id));
        if (!enquiry.getBranch().getId().equals(BranchContext.getBranchId())) {
            throw new BranchAccessDeniedException();
        }
        return enquiry;
    }

    private EnquiryResponse replayExisting(Enquiry enquiry, EnquiryRequest request) {
        if (!enquiry.getBranch().getId().equals(BranchContext.getBranchId())) {
            throw new BranchAccessDeniedException();
        }
        if (!sameCreatePayload(enquiry, request)) {
            throw new ApiConflictException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used for a different enquiry");
        }
        return toResponse(enquiry);
    }

    private boolean sameCreatePayload(Enquiry enquiry, EnquiryRequest request) {
        return Objects.equals(enquiry.getParentName(), request.getParentName().trim())
                && Objects.equals(enquiry.getPhoneNumber(), normalizePhone(request.getPhoneNumber()))
                && Objects.equals(enquiry.getEmail(), blankToNull(request.getEmail()))
                && Objects.equals(enquiry.getLeadSource(), request.getLeadSource())
                && Objects.equals(enquiry.getVisitScheduledAt(), request.getVisitScheduledAt())
                && Objects.equals(enquiry.getChildName(), blankToNull(request.getChildName()))
                && Objects.equals(enquiry.getChildDob(), request.getChildDob())
                && Objects.equals(enquiry.getNotes(), blankToNull(request.getNotes()));
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to create an enquiry");
        }
        String key = idempotencyKey.trim();
        if (key.length() > 100) {
            throw new BusinessRuleException("Idempotency-Key must be 100 characters or fewer");
        }
        return key;
    }

    private Branch currentBranch() {
        Integer branchId = BranchContext.getBranchId();
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    private EnquiryStatus initialStatus(EnquiryRequest request) {
        return request.getVisitScheduledAt() != null ? EnquiryStatus.VISIT_SCHEDULED : EnquiryStatus.NEW;
    }

    private String normalizePhone(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");
        if (normalized.length() < 10 || normalized.length() > 15) {
            throw new BusinessRuleException("Phone number must contain 10 to 15 digits");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EnquiryResponse toResponse(Enquiry enquiry) {
        return EnquiryResponse.builder()
                .id(enquiry.getId())
                .branchId(enquiry.getBranch().getId())
                .convertedCustomerId(enquiry.getConvertedCustomer() != null ? enquiry.getConvertedCustomer().getId() : null)
                .parentName(enquiry.getParentName())
                .phoneNumber(enquiry.getPhoneNumber())
                .email(enquiry.getEmail())
                .leadSource(enquiry.getLeadSource())
                .status(enquiry.getStatus())
                .visitScheduledAt(enquiry.getVisitScheduledAt())
                .childName(enquiry.getChildName())
                .childDob(enquiry.getChildDob())
                .notes(enquiry.getNotes())
                .createdAt(enquiry.getCreatedAt())
                .updatedAt(enquiry.getUpdatedAt())
                .build();
    }
}
