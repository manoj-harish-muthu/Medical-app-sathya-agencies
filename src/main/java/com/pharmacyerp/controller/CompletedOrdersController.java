package com.pharmacyerp.controller;

public class CompletedOrdersController extends BaseAgentOrdersController {
    @Override
    public String getPageMode() {
        return "COMPLETED_ONLY";
    }
}
