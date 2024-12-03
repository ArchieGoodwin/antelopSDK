package com.reactnative.antelop;

enum WalletManagerErrors {
    CONNECTION_ERROR("CONNECTION_ERROR"),
    PROVISIONING_REQUIRED("PROVISIONING_REQUIRED"),
    NOT_SET("NOT_SET"),
    VALIDATION_NEEDED("VALIDATION_NEEDED"),
    TO_BE_CHANGED("TO_BE_CHANGED"),
    ASYNC_REQUEST_ERROR("ASYNC_REQUEST_ERROR"),
    ERROR("ERROR"),
    LOCAL_AUTHENTIFICATION_ERROR("LOCAL_AUTHENTIFICATION_ERROR");

    private final String text;

    WalletManagerErrors(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

enum WalletManagerStates {
    ASYNC_REQUEST_SUCCESS("ASYNC_REQUEST_SUCCESS"),
    LOCAL_AUTHENTIFICATION_SUCCESS("LOCAL_AUTHENTIFICATION_SUCCESS"),
    CONNECTION_SUCCESS("CONNECTION_SUCCESS");

    private final String text;

    WalletManagerStates(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

enum AuthenticatedProcessCallbackErrors {
    CUSTOMER_CREDENTIALS_INVALID("CUSTOMER_CREDENTIALS_INVALID"),
    AUTHENTIFICATION_DECLINED("AUTHENTIFICATION_DECLINED"),
    CASE_PIN_ERROR("CASE_PIN_ERROR"),
    CASE_CONSENT_ERROR("CASE_CONSENT_ERROR"),
    CASE_SCREEN_UNLOCK_ERROR("CASE_SCREEN_UNLOCK_ERROR"),
    CASE_DEVICE_BIOMETRIC_ERROR("CASE_DEVICE_BIOMETRIC_ERROR"),
    CASE_DEVICE_NONE_ERROR("CASE_DEVICE_NONE_ERROR"),
    ON_ERROR("ON_ERROR"),
    ERROR("ERROR"),
    CREDENTIALS_REQUIRED_ERROR("CREDENTIALS_REQUIRED_ERROR"),
    PATTERN_NOT_READY_FOR_AUTHENTIFICATION("PATTERN_IS_NOT_READY_FOR_AUTHENTIFICATION");

    private final String text;

    AuthenticatedProcessCallbackErrors(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

enum AuthenticatedProcessCallbackStates {
    PROCESS_SUCCESS("PROCESS_SUCCESS");

    private final String text;

    AuthenticatedProcessCallbackStates(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

