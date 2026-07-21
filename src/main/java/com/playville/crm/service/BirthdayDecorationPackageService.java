package com.playville.crm.service;
import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor
public class BirthdayDecorationPackageService {
    private final BirthdayDecorationPackageRepository repository;
    private final BranchRepository branchRepository;
    @Transactional(readOnly = true) public List<BirthdayDecorationPackageResponse> active() { return repository.findByBranchIdAndActiveTrueOrderByPackageNameAsc(BranchContext.getBranchId()).stream().map(this::response).toList(); }
    @Transactional(readOnly = true) public List<BirthdayDecorationPackageResponse> all() { return repository.findByBranchIdOrderByPackageNameAsc(BranchContext.getBranchId()).stream().map(this::response).toList(); }
    @Transactional public BirthdayDecorationPackageResponse create(BirthdayDecorationPackageRequest request) { Integer branchId = BranchContext.getBranchId(); BirthdayDecorationPackage value = BirthdayDecorationPackage.builder().branch(branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))).packageCode(request.getPackageCode().trim()).packageName(request.getPackageName().trim()).description(request.getDescription()).price(request.getPrice()).taxRate(request.getTaxRate()).active(request.getActive() == null || request.getActive()).build(); return response(repository.save(value)); }
    @Transactional public BirthdayDecorationPackageResponse update(Integer id, BirthdayDecorationPackageRequest request) { BirthdayDecorationPackage value = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BirthdayDecorationPackage", id)); if (!value.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException(); value.setPackageCode(request.getPackageCode().trim()); value.setPackageName(request.getPackageName().trim()); value.setDescription(request.getDescription()); value.setPrice(request.getPrice()); value.setTaxRate(request.getTaxRate()); if (request.getActive() != null) value.setActive(request.getActive()); return response(repository.save(value)); }
    @Transactional public BirthdayDecorationPackageResponse deactivate(Integer id) { BirthdayDecorationPackage value = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BirthdayDecorationPackage", id)); if (!value.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException(); value.setActive(false); return response(repository.save(value)); }
    private BirthdayDecorationPackageResponse response(BirthdayDecorationPackage value) { return BirthdayDecorationPackageResponse.builder().id(value.getId()).packageCode(value.getPackageCode()).packageName(value.getPackageName()).description(value.getDescription()).price(value.getPrice()).taxRate(value.getTaxRate()).active(value.getActive()).build(); }
}
