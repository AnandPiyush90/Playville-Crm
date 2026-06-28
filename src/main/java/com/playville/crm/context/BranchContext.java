package com.playville.crm.context;

public final class BranchContext {

    private BranchContext() {}

    private static final ThreadLocal<Integer> BRANCH_ID   = new ThreadLocal<>();
    private static final ThreadLocal<String>  BRANCH_CODE = new ThreadLocal<>();

    public static void set(Integer branchId, String branchCode) {
        BRANCH_ID.set(branchId);
        BRANCH_CODE.set(branchCode);
    }

    public static Integer getBranchId() {
        Integer id = BRANCH_ID.get();
        if (id == null) throw new IllegalStateException(
                "BranchContext not initialised — request missing valid JWT");
        return id;
    }

    public static String getBranchCode() {
        return BRANCH_CODE.get();
    }

    public static void clear() {
        BRANCH_ID.remove();
        BRANCH_CODE.remove();
    }
}