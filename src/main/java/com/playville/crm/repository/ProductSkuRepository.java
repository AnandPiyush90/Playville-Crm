package com.playville.crm.repository;

import com.playville.crm.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    boolean existsBySkuCode(String skuCode);
    boolean existsByBarcode(String barcode);
    boolean existsBySkuCodeAndIdNot(String skuCode, Integer id);
    boolean existsByBarcodeAndIdNot(String barcode, Integer id);
    List<ProductSku> findByProductIdOrderByIdAsc(Integer productId);
    Optional<ProductSku> findByBarcodeAndIsActiveTrue(String barcode);
}
