package com.pharmacyerp.controller;

public class FailedOrdersController extends BaseAgentOrdersController {
    @Override
    public String getPageMode() {
        return "FAILED_ONLY";
    }
}
