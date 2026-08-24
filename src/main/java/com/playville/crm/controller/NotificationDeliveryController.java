package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.notification.NotificationDeliveryResponse;
import com.playville.crm.service.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/notification-deliveries")
public class NotificationDeliveryController {
    private final NotificationDeliveryService service;
    @GetMapping @PreAuthorize("hasAnyRole('admin','manager')") public ApiResponse<List<NotificationDeliveryResponse>> history(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return ApiResponse.success(service.currentBranchHistory(page,size));}
    @PostMapping("/{id}/retry") @PreAuthorize("hasAnyRole('admin','manager')") public ApiResponse<NotificationDeliveryResponse> retry(@PathVariable Integer id){return ApiResponse.success("Notification queued for retry",service.retry(id));}
}
