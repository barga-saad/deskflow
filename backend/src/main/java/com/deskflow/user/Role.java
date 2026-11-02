package com.deskflow.user;

public enum Role {
    CUSTOMER,
    AGENT,
    ADMIN;

    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
