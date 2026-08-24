package com.playville.crm.dto.disclaimer;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;
@Getter @Setter
public class DraftRequest {
     @NotBlank private String parentName;
     @NotBlank private String phoneNumber;
     private String email;
     @NotBlank private String visitPurpose;
     @NotEmpty private List<Map<String,Object>> children; 
    }
