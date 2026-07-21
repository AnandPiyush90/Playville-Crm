package com.playville.crm.repository;
import com.playville.crm.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    boolean existsBySupplierNameIgnoreCase(String supplierName);
    boolean existsBySupplierNameIgnoreCaseAndIdNot(String supplierName, Integer id);
}
