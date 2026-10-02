package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.customer.*;
import com.playville.crm.dto.kid.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KidRepository      kidRepository;
    private final BranchRepository   branchRepository;

    @Transactional(readOnly = true)
    public Page<CustomerSummaryDto> getCustomers(int page, int size, String search) {
        Integer branchId = BranchContext.getBranchId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return customerRepository
                    .searchByNameOrPhoneInBranch(branchId, search, pageable)
                    .map(this::toSummaryDto);
        }
        return customerRepository
                .findByHomeBranchIdAndIsActiveTrue(branchId, pageable)
                .map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(Integer id) {
        Customer c = findCustomerOrThrow(id);
        assertBranchAccess(c);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public CustomerDto findByPhone(String phone) {
        Customer c = customerRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer found with phone: " + phone));
        return toDto(c);
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest req) {
        if (customerRepository.existsByPhoneNumber(req.getPhoneNumber()))
            throw new DuplicateResourceException(
                    "Customer already registered with phone: " + req.getPhoneNumber());

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        Customer customer = Customer.builder()
                .phoneNumber(req.getPhoneNumber())
                .parentName(req.getParentName())
                .email(req.getEmail())
                .leadSource(req.getLeadSource())
                .homeBranch(branch)
                .firstVisitBranch(branch)
                .disclaimerAccepted(false)
                .build();

        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto updateCustomer(Integer id, UpdateCustomerRequest req) {
        Customer c = findCustomerOrThrow(id);
        assertBranchAccess(c);
        if (req.getParentName()  != null) c.setParentName(req.getParentName());
        if (req.getEmail()       != null) c.setEmail(req.getEmail());
        if (req.getLeadSource()  != null) c.setLeadSource(req.getLeadSource());
        if (req.getNotes()       != null) c.setNotes(req.getNotes());
        return toDto(customerRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<KidDto> getKidsForCustomer(Integer customerId) {
        Customer c = findCustomerOrThrow(customerId);
        assertBranchAccess(c);
        return kidRepository.findByCustomerIdAndIsActiveTrue(customerId)
                .stream().map(this::toKidDto).toList();
    }

    @Transactional
    public KidDto addKid(Integer customerId, CreateKidRequest req) {
        Customer customer = findCustomerOrThrow(customerId);
        assertBranchAccess(customer);

        int age = Period.between(req.getDob(), LocalDate.now()).getYears();
        if (age > 8)
            throw new BusinessRuleException(
                    "Kid must be 8 years or younger. Provided DOB gives age: " + age);
        if (age < 0)
            throw new BusinessRuleException("Date of birth cannot be in the future");

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        Kid kid = Kid.builder()
                .branch(branch)
                .customer(customer)
                .kidName(req.getKidName())
                .dob(req.getDob())
                .gender(req.getGender())
                .specialNotes(req.getSpecialNotes())
                .build();

        return toKidDto(kidRepository.save(kid));
    }

    @Transactional
    public void deactivateKid(Integer customerId, Integer kidId) {
        Customer c = findCustomerOrThrow(customerId);
        assertBranchAccess(c);
        Kid kid = kidRepository.findById(kidId)
                .orElseThrow(() -> new ResourceNotFoundException("Kid", kidId));
        if (!kid.getCustomer().getId().equals(customerId))
            throw new BusinessRuleException("Kid does not belong to this customer");
        kid.setActive(false);
        kidRepository.save(kid);
    }

    private Customer findCustomerOrThrow(Integer id) {
        return customerRepository.findByIdWithDetails(id)
                .or(() -> customerRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private void assertBranchAccess(Customer c) {
        if (!c.getHomeBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
    }

    private CustomerSummaryDto toSummaryDto(Customer c) {
        return CustomerSummaryDto.builder()
                .id(c.getId())
                .phoneNumber(c.getPhoneNumber())
                .parentName(c.getParentName())
                .globalSessionBalance(c.getGlobalSessionBalance())
                .currentPackageName(c.getCurrentPackage() != null
                        ? c.getCurrentPackage().getPackageName() : null)
                .totalVisits(c.getTotalVisits())
                .disclaimerAccepted(c.isDisclaimerAccepted())
                .build();
    }

    private CustomerDto toDto(Customer c) {
        List<KidDto> kids = c.getKids() == null
                ? List.of()
                : c.getKids().stream()
                        .filter(Kid::isActive)
                        .map(this::toKidDto)
                        .toList();
        return CustomerDto.builder()
                .id(c.getId())
                .phoneNumber(c.getPhoneNumber())
                .parentName(c.getParentName())
                .email(c.getEmail())
                .leadSource(c.getLeadSource())
                .globalSessionBalance(c.getGlobalSessionBalance())
                .currentPackageName(c.getCurrentPackage() != null
                        ? c.getCurrentPackage().getPackageName() : null)
                .homeBranchId(c.getHomeBranch().getId())
                .homeBranchCode(c.getHomeBranch().getBranchCode())
                .disclaimerAccepted(c.isDisclaimerAccepted())
                .totalVisits(c.getTotalVisits())
                .notes(c.getNotes())
                .isActive(c.isActive())
                .kids(kids)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private KidDto toKidDto(Kid k) {
        return KidDto.builder()
                .id(k.getId())
                .kidName(k.getKidName())
                .dob(k.getDob())
                .ageInYears(k.getAgeInYears())
                .gender(k.getGender())
                .specialNotes(k.getSpecialNotes())
                .isActive(k.isActive())
                .eligible(k.isEligible())
                .createdAt(k.getCreatedAt())
                .build();
    }
}
