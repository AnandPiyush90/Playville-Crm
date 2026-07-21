package com.playville.crm.exception;
public class CustomerPhoneExistsException extends RuntimeException { public CustomerPhoneExistsException(Integer customerId) { super("CUSTOMER_PHONE_EXISTS: Existing customer ID " + customerId); } }
