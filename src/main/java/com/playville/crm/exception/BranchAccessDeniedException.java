package com.playville.crm.exception;

// Thrown when a staff member tries to access data outside their branch
public class BranchAccessDeniedException extends RuntimeException {
    public BranchAccessDeniedException() {
        super("Access denied: resource does not belong to your branch");
    }
    public BranchAccessDeniedException(String message) {
        super(message);
    }
}