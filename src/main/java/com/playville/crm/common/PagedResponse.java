package com.playville.crm.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedResponse<T> {

    private final List<T>   content;
    private final int       page;
    private final int       size;
    private final long      totalElements;
    private final int       totalPages;
    private final boolean   last;

    public PagedResponse(Page<T> page) {
        this.content       = page.getContent();
        this.page          = page.getNumber();
        this.size          = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages    = page.getTotalPages();
        this.last          = page.isLast();
    }

    // Convenience wrap for controller responses
    public static <T> ApiResponse<PagedResponse<T>> of(Page<T> page) {
        return ApiResponse.success(new PagedResponse<>(page));
    }
}