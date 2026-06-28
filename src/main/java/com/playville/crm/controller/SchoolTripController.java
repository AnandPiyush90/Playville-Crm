package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.schooltrip.*;
import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.service.SchoolTripService;
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

@Tag(name = "School Trips", description = "School field trip booking management")
@RestController
@RequestMapping("/school-trips")
@RequiredArgsConstructor
public class SchoolTripController {

    private final SchoolTripService schoolTripService;

    @Operation(summary = "Create a school trip booking")
    @PostMapping
    public ResponseEntity<ApiResponse<SchoolTripResponse>> create(
            @Valid @RequestBody SchoolTripRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("School trip booking created",
                        schoolTripService.createTrip(req, username)));
    }

    @Operation(summary = "List school trips for current branch")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolTripResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                ApiResponse.success(schoolTripService.getBranchTrips(from, to)));
    }

    @Operation(summary = "Get school trip by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolTripResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(schoolTripService.getTripById(id)));
    }

    @Operation(summary = "Update trip status and actual kids count")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SchoolTripResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam BirthdayBooking.BookingStatus status,
            @RequestParam(required = false) Integer actualKids) {
        return ResponseEntity.ok(
                ApiResponse.success(schoolTripService.updateStatus(
                        id, status, actualKids)));
    }
}