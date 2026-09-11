package com.pharmacyerp.controller;

public class PendingOrdersController extends BaseAgentOrdersController {
    @Override
    public String getPageMode() {
        return "PENDING_ONLY";
    }
}
