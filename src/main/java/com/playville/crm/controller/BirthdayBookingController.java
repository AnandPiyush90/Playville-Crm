package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.service.BirthdayBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Birthday Bookings", description = "Birthday party booking management")
@RestController
@RequestMapping("/birthday-bookings")
@RequiredArgsConstructor
public class BirthdayBookingController {

    private final BirthdayBookingService bookingService;

    @Operation(summary = "Create a birthday party booking")
    @PostMapping
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> create(
            @Valid @RequestBody BirthdayBookingRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Birthday booking created",
                        bookingService.createBooking(req, username)));
    }

    @Operation(summary = "List birthday bookings for current branch")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BirthdayBookingResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getBranchBookings(from, to)));
    }

    @Operation(summary = "Get birthday booking by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getBookingById(id)));
    }

    @Operation(summary = "Update booking status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam BirthdayBooking.BookingStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.updateStatus(id, status)));
    }
}