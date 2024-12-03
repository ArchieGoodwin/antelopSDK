package com.reactnative.antelop;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.antelop.exposed.AntelopHostApduService;
import fr.antelop.sdk.AntelopError;
import fr.antelop.sdk.AntelopErrorCode;
import fr.antelop.sdk.AsyncRequestType;
import fr.antelop.sdk.EligibilityDenialReason;
import fr.antelop.sdk.Product;
import fr.antelop.sdk.Wallet;
import fr.antelop.sdk.WalletLockReason;
import fr.antelop.sdk.WalletManager;
import fr.antelop.sdk.WalletManagerCallback;
import fr.antelop.sdk.WalletProvisioning;
import fr.antelop.sdk.WalletProvisioningCallback;
import fr.antelop.sdk.authentication.CustomerAuthenticationPattern;
import fr.antelop.sdk.authentication.CustomCustomerAuthenticatedProcessCallback;
import fr.antelop.sdk.authentication.CustomerAuthenticationMethodStatus;
import fr.antelop.sdk.authentication.CustomerScreenUnlockAuthenticationCredentials;
import fr.antelop.sdk.authentication.CustomerAuthenticatedProcess;
import fr.antelop.sdk.authentication.CustomerAuthenticationCredentials;
import fr.antelop.sdk.authentication.CustomerAuthenticationMethod;
import fr.antelop.sdk.authentication.CustomerAuthenticationMethodType;
import fr.antelop.sdk.authentication.CustomerAuthenticationIssuerPasscode;
import fr.antelop.sdk.authentication.CustomerConsentCredentials;
import fr.antelop.sdk.authentication.CustomerCredentialsRequiredReason;
import fr.antelop.sdk.authentication.LocalAuthenticationErrorReason;
import fr.antelop.sdk.authentication.prompt.CustomerAuthenticationFailureReason;
import fr.antelop.sdk.authentication.prompt.CustomerAuthenticationPrompt;
import fr.antelop.sdk.authentication.prompt.CustomerAuthenticationPromptCallback;
import fr.antelop.sdk.authentication.prompt.DeviceBiometricCustomerAuthenticationPromptBuilder;
import fr.antelop.sdk.card.Card;
import fr.antelop.sdk.card.CardDisplay;
import fr.antelop.sdk.exception.WalletValidationException;
import fr.antelop.sdk.sca.CancellationReason;
import fr.antelop.sdk.sca.CustomerAuthenticatedSignature;
import fr.antelop.sdk.sca.PushAuthenticationRequest;
import fr.antelop.sdk.firebase.AntelopFirebaseMessagingUtil;
import fr.antelop.sdk.transaction.hce.HceTransaction;
import fr.antelop.sdk.transaction.hce.HceTransactionStatus;
import fr.antelop.sdk.util.HceHelper;

public class AntelopModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private WalletProvisioning walletProvisioning;

    private Promise onProvisioningPromise;
    private Promise onCheckEligibilityPromise;
    private Promise onAuthenticatePromise;
    private Promise onSynchronizePasscodePromise;
    private Promise onCheckPasscodePromise;
    private Promise onDeleteWalletPromise;
    private Promise onAuthenticateHceTransactionPromise;
    private Promise onActivateBiometricsPromise;
    private Promise onDeactivateBiometricsPromise;
    private Promise onPushAuthenticationRequestPromise;

    private WalletManager mWalletManager;
    private Wallet mWallet;

    private boolean hasDefaultCardChanged = false;

    private boolean isBackgrounded = true;

    private static CustomerAuthenticatedProcess mCustomerAuthenticatedProcess;
    private static final List<PushAuthenticationRequest> pushAuthenticationRequestList = new ArrayList<>();

    public AntelopModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
        this.reactContext = reactContext;
        this.setupAntelopPushAuthentication();
        this.setupWalletNotificationServiceCallback();
        this.setupAntelopHceTransactionListenerCallback();
    }

    @NonNull
    @Override
    public String getName() {
        return "Antelop";
    }

    @ReactMethod
    public void initialize(final Promise promise) {
        Context applicationContext = reactContext.getApplicationContext();

        try {
            walletProvisioning = new WalletProvisioning(applicationContext, new WalletProvisioningCallback() {
                @Override
                public void onProvisioningError(@NonNull AntelopError antelopError, Object o) {
                    onProvisioningPromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                }

                @Override
                public void onProvisioningSuccess(Object o) {
                    onProvisioningPromise.resolve(Objects.isNull(o) ? "" : o.toString());
                }

                @Override
                public void onProvisioningPending(Object o) {

                }

                @Override
                public void onPermissionNotGranted(@NonNull String[] strings, Object o) {
                    promise.reject(AntelopErrorCode.AndroidPermissionNotGranted.toString(), strings[0]);
                }

                @Override
                public void onInitializationError(@NonNull AntelopError antelopError, Object o) {
                    promise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                }

                @Override
                public void onInitializationSuccess(Object o) {
                    promise.resolve("");
                }

                @Override
                public void onDeviceEligible(boolean b, @NonNull List<Product> list, Object o) {
                    onCheckEligibilityPromise.resolve(b);
                }

                @Override
                public void onDeviceNotEligible(@NonNull EligibilityDenialReason eligibilityDenialReason, Object o, @Nullable String s) {
                    onCheckEligibilityPromise.reject(AntelopErrorCode.DeviceArchitectureNotSupported.toString(), eligibilityDenialReason.toString());
                }

                @Override
                public void onCheckEligibilityError(@NonNull AntelopError antelopError, Object o) {
                    onCheckEligibilityPromise.reject(antelopError.getCode().toString(), antelopError.toString());
                }
            });

            walletProvisioning.initialize();
        } catch (WalletValidationException error) {
            promise.reject(error.getCode().toString(), error.getMessage());
        }
    }

    @ReactMethod
    public void setDefaultAppPayment(boolean enabled) {
        if (enabled) {
            HceHelper.enableHceService(reactContext);

            if (!HceHelper.isAppDefaultPaymentApp(reactContext)) {
                Intent intent = new Intent("android.nfc.cardemulation.action.ACTION_CHANGE_DEFAULT");
                intent.putExtra("category", "payment");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("component", new ComponentName(reactContext, AntelopHostApduService.class.getName()));
                reactContext.startActivity(intent);
            }
        } else {
            HceHelper.disableHceService(reactContext);
        }
    }

    @ReactMethod
    public void isDefaultAppPayment(Promise promise) {
        promise.resolve(HceHelper.isAppDefaultPaymentApp(reactContext));
    }

    @ReactMethod
    public void requestNfcActivation() {
        Intent intent = new Intent("android.settings.NFC_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reactContext.startActivity(intent);
    }

    @ReactMethod
    public void registerNfcStatusListener(Promise promise) {
        if (HceHelper.hasNfcFeature(reactContext)) {
            HceHelper.registerNfcStatusListener(reactContext.getApplicationContext(), new HceHelper.NfcStatusListener() {
                @Override
                public void onNfcActivated() {
                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("updateNfcStatus", "enabled");
                }

                @Override
                public void onNfcDeactivated() {
                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("updateNfcStatus", "disabled");
                }
            });

            promise.resolve(HceHelper.isNfcActivated(reactContext) ? "enabled" : "disabled");
        } else {
            promise.resolve("notAvailable");
        }
    }

    @Override
    public void onHostResume() {
        isBackgrounded = false;
        // Request foreground payment priority
        if (HceHelper.hasHceFeature(reactContext) && HceHelper.isForegroundPriorityEnabled(reactContext)) {
            HceHelper.enableHceService(reactContext);
            HceHelper.requestForegroundPaymentPriority(getCurrentActivity());
        }
        // Check if there is a PushAuthenticationRequest waiting
        cleanPushAuthenticationRequestList();
        checkIfPushAuthenticationRequestWaiting();
    }

    @Override
    public void onHostPause() {
        isBackgrounded = true;
        // Release foreground payment priority
        if (HceHelper.hasHceFeature(reactContext) && HceHelper.isForegroundPriorityEnabled(reactContext)) {
            HceHelper.releaseForegroundPaymentPriority(getCurrentActivity());
        }
        // When default card changed, we must disconnect/reconnect the wallet to persist the change
        if (hasDefaultCardChanged) {
            hasDefaultCardChanged = false;
            mWalletManager.disconnect();
            mWalletManager.connect();
        }
    }

    @Override
    public void onHostDestroy() {

    }

    @ReactMethod
    public void setForegroundPaymentPriority(boolean enabled) {
        if (enabled) {
            HceHelper.enableHceService(reactContext);
            HceHelper.requestForegroundPaymentPriority(getCurrentActivity());
        } else {
            HceHelper.releaseForegroundPaymentPriority(getCurrentActivity());
        }
    }

    @ReactMethod
    public void isForegroundPaymentPriorityEnabled(Promise promise) {
        promise.resolve(HceHelper.isForegroundPriorityEnabled(getCurrentActivity()));
    }

    @ReactMethod
    public void hasHceFeature(Promise promise) {
        promise.resolve(HceHelper.hasHceFeature(reactContext));
    }

    @ReactMethod
    public void hasNfcFeature(Promise promise) {
        promise.resolve(HceHelper.hasNfcFeature(reactContext));
    }

    @ReactMethod
    public void checkEligibility(final Promise promise) {
        onCheckEligibilityPromise = promise;
        try {
            walletProvisioning.checkEligibility(true);
        } catch (WalletValidationException error) {
            promise.reject(error.getCode().toString(), error.getMessage());
        }
    }

    @ReactMethod
    public void launch(@Nullable String clientId, @Nullable String walletId, String settingsProfileId, @Nullable String phoneNumber, final Promise promise) {
        onProvisioningPromise = promise;
        try {
            walletProvisioning.launch(clientId, walletId, settingsProfileId, null);
        } catch (WalletValidationException error) {
            promise.reject(error.getCode().toString(), error.getMessage());
        }
    }

    @ReactMethod
    public void launchWithActivationCode(String activationCode, final Promise promise) {
        onProvisioningPromise = promise;
        try {
            walletProvisioning.launch(decodeHexString(activationCode), null);
        } catch (WalletValidationException error) {
            promise.reject(error.getCode().toString(), error.getMessage());
        }
    }

    private int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException(
                "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    public byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    @ReactMethod
    public void clean() {
        walletProvisioning.clean();
    }

    @ReactMethod
    public void connect(@Nullable String passcode, @Nullable String cryptogram, @Nullable String cryptogramData, final Promise promise) {
        try {
            mWalletManager = new WalletManager(reactContext.getApplicationContext(), new WalletManagerCallback() {
                @Override
                public void onConnectionError(@NonNull AntelopError antelopError, Object o) {
                    promise.reject(WalletManagerErrors.CONNECTION_ERROR.toString(), antelopError.getMessage());
                }

                @Override
                public void onConnectionSuccess(@NonNull Wallet wallet, Object o) {
                    mWallet = wallet;
                    HashMap<String, String> resolveData = new HashMap<>();
                    resolveData.put("status", WalletManagerStates.CONNECTION_SUCCESS.toString());
                    resolveData.put("walletId", wallet.getWalletId());

                    promise.resolve(new JSONObject(resolveData).toString());
                }

                @Override
                public void onCredentialsRequired(@NonNull CustomerCredentialsRequiredReason customerCredentialsRequiredReason, AntelopError antelopError, Object o) {
                    switch (customerCredentialsRequiredReason) {

                        case NotSet:
                            promise.reject(WalletManagerErrors.NOT_SET.toString(), "Not set.");
                            break;

                        case ToBeChanged:
                            promise.reject(WalletManagerErrors.TO_BE_CHANGED.toString(), "To be change.");
                            break;

                        case ValidationNeeded:
                            promise.reject(WalletManagerErrors.VALIDATION_NEEDED.toString(), "Validation needed.");
                            break;
                    }

                }

                @Override
                public void onProvisioningRequired(Object o) {
                    promise.reject(WalletManagerErrors.PROVISIONING_REQUIRED.toString(), "Provisioning required.");
                }

                @Override
                public void onAsyncRequestSuccess(AsyncRequestType asyncRequestType, Object o) {
                    switch (asyncRequestType) {
                        case ActivateAuthenticationMethod:
                            if (onActivateBiometricsPromise != null)  {
                                onActivateBiometricsPromise.resolve(true);
                                onActivateBiometricsPromise = null;
                            }
                        case DeactivateAuthenticationMethod:
                            if (onDeactivateBiometricsPromise != null)  {
                                onDeactivateBiometricsPromise.resolve(true);
                                onDeactivateBiometricsPromise = null;
                            }
                        case CheckCredentials:
                            if (onCheckPasscodePromise != null) onCheckPasscodePromise.resolve(true);
                            break;
                        case SynchronizeAuthenticationMethod:
                            if (onSynchronizePasscodePromise != null) onSynchronizePasscodePromise.resolve(true);
                            break;
                        case Delete:
                            if (onDeleteWalletPromise != null) onDeleteWalletPromise.resolve(true);
                            break;
                    }
                }

                @Override
                public void onAsyncRequestError(@NonNull AsyncRequestType asyncRequestType, @NonNull AntelopError antelopError, Object o) {
                    switch (asyncRequestType) {
                        case ActivateAuthenticationMethod:
                            if (onActivateBiometricsPromise != null)  {
                                onActivateBiometricsPromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                                onActivateBiometricsPromise = null;
                            }
                        case DeactivateAuthenticationMethod:
                            if (onDeactivateBiometricsPromise != null)  {
                                onDeactivateBiometricsPromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                                onDeactivateBiometricsPromise = null;
                            }
                        case CheckCredentials:
                            if (onCheckPasscodePromise != null)  onCheckPasscodePromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                            break;
                        case SynchronizeAuthenticationMethod:
                            if (onSynchronizePasscodePromise != null) onSynchronizePasscodePromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                            break;
                        case Delete:
                            if (onDeleteWalletPromise != null) onDeleteWalletPromise.reject(antelopError.getCode().toString(), antelopError.getMessage());
                            break;
                    }
                }

                @Override
                public void onLocalAuthenticationSuccess(@NonNull CustomerAuthenticationMethodType customerAuthenticationMethodType, Object o) {
                    if (onAuthenticateHceTransactionPromise != null) {
                        onAuthenticateHceTransactionPromise.resolve(true);
                        onAuthenticateHceTransactionPromise = null;
                    }
                }

                @Override
                public void onLocalAuthenticationError(@NonNull CustomerAuthenticationMethodType customerAuthenticationMethodType, @NonNull LocalAuthenticationErrorReason localAuthenticationErrorReason, String s, Object o) {
                    if (onAuthenticatePromise != null) {
                        onAuthenticatePromise.reject(AntelopErrorCode.CustomerCredentialsInvalid.toString(), localAuthenticationErrorReason.toString());
                    }
                }
            }, new Object());

            if (passcode == null || cryptogram == null || cryptogramData == null) {
                mWalletManager.connect();
            } else {
                mWalletManager.connect(new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()), null);
            }

        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void logout(Promise promise) {
        try {
            if (mWalletManager != null) {
                mWalletManager.logout();
            }
            promise.resolve(true);
        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void delete(Promise promise) {
        if (mWalletManager != null) {
            mWalletManager.delete();
            onDeleteWalletPromise = promise;
        } else {
            promise.resolve(true);
        }
    }

    private CustomCustomerAuthenticatedProcessCallback generateCustomCallback(final Promise promise, final String passcode, final String cryptogram, final String cryptogramData) {
        return new CustomCustomerAuthenticatedProcessCallback() {

            @Override
            public void onCustomerCredentialsInvalid(@NonNull LocalAuthenticationErrorReason localAuthenticationErrorReason, @NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {
                if (onAuthenticatePromise != null)
                    onAuthenticatePromise.reject(AntelopErrorCode.CustomerCredentialsInvalid.toString(), localAuthenticationErrorReason.toString());

                WritableMap params = Arguments.createMap();
                params.putString("reason", localAuthenticationErrorReason.toString());
                params.putString("patternName", customerAuthenticatedProcess.getCustomerAuthenticationPatternName());
                if (customerAuthenticatedProcess instanceof CustomerAuthenticatedSignature) {
                    params.putString("type", "sign");
                } else {
                    params.putString("type", "push");
                }
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onCustomerCredentialsInvalid", params);

                try {
                    checkMaximumAttempts();
                } catch (WalletValidationException e) {
                    promise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), e.getMessage());
                }
            }

            @Override
            public void onProcessStart(@NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {

            }

            @Override
            public void onProcessSuccess(@NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {
                HashMap<String, String> resolveData = new HashMap<>();
                resolveData.put("status", AuthenticatedProcessCallbackStates.PROCESS_SUCCESS.toString());
                resolveData.put("patternName", customerAuthenticatedProcess.getCustomerAuthenticationPatternName());
                resolveData.put("type", "push");
                if(customerAuthenticatedProcess instanceof CustomerAuthenticatedSignature) {
                    resolveData.put("type", "sign");
                    final String token = ((CustomerAuthenticatedSignature) customerAuthenticatedProcess).getResult();
                    resolveData.put("token", token);
                }

                promise.resolve(new JSONObject(resolveData).toString());
                if (onAuthenticatePromise != null)
                    onAuthenticatePromise.resolve(true);

                if(customerAuthenticatedProcess instanceof PushAuthenticationRequest) {
                    popPushAuthenticationRequest();
                }
            }

            @Override
            public void onError(@NonNull AntelopError antelopError, @NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {
                if (onAuthenticatePromise != null)
                    onAuthenticatePromise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), antelopError.getMessage());
                promise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), antelopError.getMessage());

                if(customerAuthenticatedProcess instanceof PushAuthenticationRequest) {
                    popPushAuthenticationRequest();
                }
            }

            @Override
            public void onAuthenticationDeclined(@NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {
                if (onAuthenticatePromise != null)
                    onAuthenticatePromise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), "onAuthenticationDeclined");
                if(customerAuthenticatedProcess instanceof PushAuthenticationRequest) {
                    try {
                        pushAuthenticationRequestList.get(0).authenticate(reactContext.getApplicationContext(), generateCustomCallback(promise, null, null, null));
                    } catch (WalletValidationException e) {
                        promise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), "onAuthenticationDeclined");
                        popPushAuthenticationRequest();
                    }
                } else {
                    promise.reject(AuthenticatedProcessCallbackErrors.ON_ERROR.toString(), "onAuthenticationDeclined");
                }
            }

            @Override
            public void onCustomerCredentialsRequired(@NonNull List<CustomerAuthenticationMethod> allowedMethods, @NonNull CustomerAuthenticatedProcess customerAuthenticatedProcess) {
                if (passcode != null && cryptogram != null) {
                    try {
                        mCustomerAuthenticatedProcess.setCustomerCredentials(reactContext.getApplicationContext(), new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
                        return ;
                    } catch (WalletValidationException e) {
                        promise.reject(e.getCode().toString(), e.getMessage());
                    }
                }

                if (allowedMethods.get(0).getType() == CustomerAuthenticationMethodType.ScreenUnlock) {
                    try {
                        if (customerAuthenticatedProcess instanceof CustomerAuthenticatedSignature) {
                            mCustomerAuthenticatedProcess.setCustomerCredentials(reactContext.getApplicationContext(), new CustomerScreenUnlockAuthenticationCredentials());
                        } else if(customerAuthenticatedProcess instanceof PushAuthenticationRequest) {
                            pushAuthenticationRequestList.get(0).setCustomerCredentials(reactContext.getApplicationContext(), new CustomerScreenUnlockAuthenticationCredentials());
                        }
                        return ;
                    } catch (WalletValidationException e) {
                        promise.reject(e.getCode().toString(), e.getMessage());
                    }
                }

                WritableMap params = Arguments.createMap();
                WritableArray aAllowedMethods = Arguments.createArray();
                for (CustomerAuthenticationMethod method : allowedMethods) {
                    WritableMap mMethod = Arguments.createMap();
                    mMethod.putString("type", method.getType().toString());
                    mMethod.putInt("duration", method.getAuthenticationDuration());
                    mMethod.putInt("maxAttempts", method.getMaximumAttemptsCount());
                    aAllowedMethods.pushMap(mMethod);
                }
                params.putArray("allowedMethods", aAllowedMethods);
                params.putString("patternName", customerAuthenticatedProcess.getCustomerAuthenticationPatternName());
                if (customerAuthenticatedProcess instanceof CustomerAuthenticatedSignature) {
                    params.putString("type", "sign");
                } else if(customerAuthenticatedProcess instanceof PushAuthenticationRequest) {
                    params.putString("type", "push");
                }
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onCustomerCredentialsRequired", params);
            }
        };
    }

    @ReactMethod
    public void getAuthenticationPatterns(Promise promise) {
        Map<String, CustomerAuthenticationPattern> authenticationPatterns = mWallet.settings().security().getCustomerAuthenticationPatterns().getValue();
        promise.resolve(authenticationPatterns.toString());
    }

    @ReactMethod
    public void runSignature(String patternName, String deviceUuid, String mobilePhone, String appVersion, Promise promise) {
        boolean patternIsReadyForAuthentication = false;

        if (mWallet == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        for (CustomerAuthenticationPattern pattern : mWallet.settings().security().getCustomerAuthenticationPatterns().getValue().values()) {
            if (pattern.isReady() && pattern.getName().equals(patternName)) {
                patternIsReadyForAuthentication = true;
                break;
            }
        }

        if (patternIsReadyForAuthentication) {
            try {
                CustomerAuthenticatedSignature signature = new CustomerAuthenticatedSignature(patternName, "", CustomerAuthenticatedSignature.SignatureType.LocalJws, ("{\"userUuid\":\"" + mWallet.getIssuerClientId() + "\", \"deviceUuid\": \""+deviceUuid+"\", \"appVersion\": \""+appVersion+"\", \"mobilePhone\": \""+mobilePhone+"\" }").getBytes());
                mCustomerAuthenticatedProcess = signature;
                signature.sign(reactContext.getApplicationContext(), generateCustomCallback(promise, null, null, null));
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onSignatureRequestReceived",null);
            } catch (WalletValidationException e) {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), e.getMessage());
            }
        } else {
            promise.reject(AuthenticatedProcessCallbackErrors.PATTERN_NOT_READY_FOR_AUTHENTIFICATION.toString(), "This pattern [" + patternName + "] is not ready for authentication.");
        }
    }

    @ReactMethod
    public void isBiometricsActivated(Promise promise) {
        if (mWallet == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        boolean methodIsReadyForAuthentication = false;

        for (CustomerAuthenticationMethod authenticationMethod : mWallet.settings().security().getCustomerAuthenticationMethods().getValue().values()) {
            if (authenticationMethod.getType().equals(CustomerAuthenticationMethodType.DeviceBiometric) && authenticationMethod.getStatus().equals(CustomerAuthenticationMethodStatus.Activated)) {
                methodIsReadyForAuthentication = true;
                break;
            }
        }

        promise.resolve(methodIsReadyForAuthentication);
    }

    @ReactMethod
    public void isBiometricsSupported(Promise promise) {
        if (mWallet == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        CustomerAuthenticationMethodStatus biometricsStatus = null;

        for (CustomerAuthenticationMethod authenticationMethod : mWallet.settings().security().getCustomerAuthenticationMethods().getValue().values()) {
            if (authenticationMethod.getType().equals(CustomerAuthenticationMethodType.DeviceBiometric)) {
                biometricsStatus = authenticationMethod.getStatus();
                break;
            }
        }

        promise.resolve(biometricsStatus != null && biometricsStatus != CustomerAuthenticationMethodStatus.NotConfigured && biometricsStatus != CustomerAuthenticationMethodStatus.NotSupported);
    }

    @ReactMethod
    public void activateBiometrics(String passcode, String cryptogram, String cryptogramData, Promise promise) {
        if (mWalletManager == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        onActivateBiometricsPromise = promise;
        try {
            mWalletManager.activateAuthenticationMethod(CustomerAuthenticationMethodType.DeviceBiometric, new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
        } catch (WalletValidationException e) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void deactivateBiometrics(String passcode, String cryptogram, String cryptogramData, Promise promise) {
        if (mWalletManager == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        onDeactivateBiometricsPromise = promise;
        try {
            mWalletManager.deactivateAuthenticationMethod(CustomerAuthenticationMethodType.DeviceBiometric, new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
        } catch (WalletValidationException e) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void runStrongSignatureWithPasscode(String passcode, String cryptogram, String cryptogramData, String deviceUuid, String mobilePhone, String appVersion, Promise promise) {
        if (mWallet == null) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Wallet is not connected");
            return ;
        }

        String patternName = "strong";
        boolean patternIsReadyForAuthentication = false;

        for (CustomerAuthenticationPattern pattern : mWallet.settings().security().getCustomerAuthenticationPatterns().getValue().values()) {
            if (pattern.isReady() && pattern.getName().equals(patternName)) {
                patternIsReadyForAuthentication = true;
                break;
            }
        }

        if (patternIsReadyForAuthentication) {
            try {
                CustomerAuthenticatedSignature signature = new CustomerAuthenticatedSignature(patternName, "", CustomerAuthenticatedSignature.SignatureType.LocalJws, ("{\"userUuid\":\"" + mWallet.getIssuerClientId() + "\", \"deviceUuid\": \""+deviceUuid+"\", \"appVersion\": \""+appVersion+"\", \"mobilePhone\": \""+mobilePhone+"\" }").getBytes());
                mCustomerAuthenticatedProcess = signature;
                onAuthenticatePromise = promise;
                signature.sign(reactContext.getApplicationContext(), generateCustomCallback(promise, passcode, cryptogram, cryptogramData));
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onSignatureRequestReceived",null);
            } catch (WalletValidationException e) {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), e.getMessage());
            }
        } else {
            promise.reject(AuthenticatedProcessCallbackErrors.PATTERN_NOT_READY_FOR_AUTHENTIFICATION.toString(), "This pattern [" + patternName + "] is not ready for authentication.");
        }
    }

    @ReactMethod
    public void authenticateByConsent(String scaType, Promise promise) {
        try {
            if (scaType.equals("hce")) {
                mWalletManager.setCustomerCredentialsForTransaction(new CustomerConsentCredentials());
            } else if (scaType.equals("push") && !pushAuthenticationRequestList.isEmpty()) {
                pushAuthenticationRequestList.get(0).setCustomerCredentials(reactContext.getApplicationContext(), new CustomerConsentCredentials());
            } else if (scaType.equals("sign") && mCustomerAuthenticatedProcess != null) {
                mCustomerAuthenticatedProcess.setCustomerCredentials(reactContext.getApplicationContext(), new CustomerConsentCredentials());
            } else {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Variable mCustomerAuthenticatedProcess is null.");
            }
        } catch (WalletValidationException e) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void authenticateByPasscode(String passcode, String cryptogram, String cryptogramData, String scaType, Promise promise) {
        try {
            onAuthenticatePromise = promise;
            if (scaType.equals("hce")) {
                mWalletManager.setCustomerCredentialsForTransaction(new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
            } else if (scaType.equals("push") && !pushAuthenticationRequestList.isEmpty()) {
                pushAuthenticationRequestList.get(0).setCustomerCredentials(reactContext.getApplicationContext(), new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
            } else if (scaType.equals("sign") && mCustomerAuthenticatedProcess != null) {
                mCustomerAuthenticatedProcess.setCustomerCredentials(reactContext.getApplicationContext(), new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
            } else {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Variable mCustomerAuthenticatedProcess is null.");
            }
        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void promptDeviceBiometrics(final boolean silent, final String scaType, final Promise promise) {
        if(isBackgrounded) {
            promise.reject("CANCELLED", "App is in background");
        }

        CustomerAuthenticationPrompt prompt;
        CustomerAuthenticationPromptCallback promptCallback = new CustomerAuthenticationPromptCallback() {

            /**
             * Called when the customer succeeded to authenticate using the Antelop Prompt
             *
             * the correct credentials are passed to the process
             * */
            @Override
            public void onAuthenticationSuccess(CustomerAuthenticationCredentials credentials) {
                try {
                    if (!silent && scaType.equals("hce")) {
                        mWalletManager.setCustomerCredentialsForTransaction(credentials);
                    } else if (!silent && scaType.equals("push") && !pushAuthenticationRequestList.isEmpty()) {
                        pushAuthenticationRequestList.get(0).setCustomerCredentials(reactContext.getApplicationContext(), credentials);
                    } else if (!silent && scaType.equals("sign") && mCustomerAuthenticatedProcess != null) {
                        mCustomerAuthenticatedProcess.setCustomerCredentials(reactContext.getApplicationContext(), credentials);
                    }
                    promise.resolve(true);
                } catch (WalletValidationException e) {
                    promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Error, Authentication Impossible : " + "WalletValidationException : " + e.getCause() + " - " + e.getDomain());
                }
            }

            /**
             * Called when the Antelop Prompt cannot authenticate customer anymore
             * the process is aborted
             * */
            @Override
            public void onAuthenticationFailure(CustomerAuthenticationFailureReason reason) {
                if (Objects.requireNonNull(reason) == CustomerAuthenticationFailureReason.Cancelled) {//customer cancelled the authentication, staying silent
                    promise.reject("CANCELLED", reason.toString());
                } else {//informing customer for any other reason
                    promise.reject(AntelopErrorCode.CustomerCredentialsInvalid.toString(), reason.toString());
                }
            }
        };
        prompt = new DeviceBiometricCustomerAuthenticationPromptBuilder()
            .setTitle(reactContext.getString(R.string.authenticationRequired))
            .setSubtitle("").build();

        try {
            boolean prompted = false;
            for (CustomerAuthenticationMethod method : mWallet.settings().security().getCustomerAuthenticationMethods().getValue().values()) {
                if (method.getType().equals(CustomerAuthenticationMethodType.DeviceBiometric) && method.getStatus().equals(CustomerAuthenticationMethodStatus.Activated)) {
                    method.promptCustomer(reactContext.getApplicationContext(), prompt, promptCallback);
                    prompted = true;
                    break;
                }
            }

            if (!prompted) {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "DeviceBiometrics not activated");
            }
        } catch (WalletValidationException e) {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "Error, Authentication Impossible : " + "WalletValidationException : " + e.getCause() + " - " + e.getDomain());
        }
    }

    @ReactMethod
    public void getWalletId(Promise promise) {
        if (mWallet != null) {
            promise.resolve(mWallet.getWalletId());
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void synchronizePasscode(String passcode, String cryptogram, String cryptogramData, Promise promise) {
        try {
            onSynchronizePasscodePromise = promise;
            mWalletManager.synchronizeAuthenticationMethod(CustomerAuthenticationMethodType.Pin, new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void checkPasscode(String passcode, String cryptogram, String cryptogramData, Promise promise) {
        try {
            onCheckPasscodePromise = promise;
            mWalletManager.checkCredentials(new CustomerAuthenticationIssuerPasscode(passcode.getBytes(), cryptogram.getBytes(), cryptogramData.getBytes()));
        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void authenticateHceTransaction(Promise promise) {
        // On enregistre une Promise pour l'authentification de la Transaction HCE
        if (AntelopHceTransactionListener.getHceCustomerAuthenticationMethods() != null && AntelopHceTransactionListener.getHceTransaction() != null) {
            onAuthenticateHceTransactionPromise = promise;
        } else {
            promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(),"No hce transaction ongoing");
        }
    }

    @ReactMethod
    private void checkIfHceTransactionOngoing() {
        if(AntelopHceTransactionListener.hasOngoingHceTransaction() &&
            Math.abs(new Date().getTime() - AntelopHceTransactionListener.getHceTransaction().getDate().getTime()) < 60000) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceTransactionStart","");

            if(AntelopHceTransactionListener.getHceCustomerAuthenticationMethods() != null && AntelopHceTransactionListener.getHceTransaction() != null) {
                WritableMap params = Arguments.createMap();
                WritableMap transactionData = Arguments.createMap();
                transactionData.putDouble("purchaseAmount", AntelopHceTransactionListener.getHceTransaction().getAmount().doubleValue());
                transactionData.putString("merchantName", AntelopHceTransactionListener.getHceTransaction().getMerchantName());
                transactionData.putString("purchaseCurrency", AntelopHceTransactionListener.getHceTransaction().getCurrency().getSymbol());

                params.putMap("transactionData", transactionData);

                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceCredentialsRequired", params);
            }
        }
    }

    @ReactMethod
    public void cancelOngoingHceTransaction() {
        AntelopHceTransactionListener.cancelOngoingHceTransaction();
    }

    @ReactMethod
    public void setDefaultCard(String cardId, Promise promise) {
        if (mWallet != null) {
            try {
                String defaultCardId = mWallet.getDefaultCardId();
                if (defaultCardId == null || !defaultCardId.equals(cardId)) {
                    hasDefaultCardChanged = true;
                    mWallet.setDefaultCard(cardId);
                }

                promise.resolve(true);
            } catch (WalletValidationException e) {
                e.printStackTrace();
                promise.reject(e.getCode().toString(), e.getMessage());
            }
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void setNextTransactionCard(String cardId, Promise promise) {
        if (mWallet != null) {
            try {
                mWallet.setNextTransactionCard(cardId);
                promise.resolve(true);
            } catch (WalletValidationException e) {
                e.printStackTrace();
                promise.reject(e.getCode().toString(), e.getMessage());
            }
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void resetNextTransactionCard() {
        if (mWallet != null) {
            try {
                mWallet.resetNextTransactionCard();
            } catch (WalletValidationException e) {
                e.printStackTrace();
            }
        }
    }

    @ReactMethod
    public void getEmulatedCards(Promise promise) {
        if (mWallet != null) {
            String defaultCardId = mWallet.getDefaultCardId();
            String nextTransactionCardId = mWallet.getNextTransactionCardId();
            Map<String, Card> cards = mWallet.cards(false);
            WritableMap params = Arguments.createMap();
            WritableArray cardsArray = Arguments.createArray();
            for (Map.Entry<String, Card> entry : cards.entrySet()) {
                Card card = entry.getValue();
                CardDisplay cardDisplay = card.getDisplay();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(card.getExpiryDate());

                WritableMap cardMap = Arguments.createMap();
                cardMap.putString("id", card.getId());
                cardMap.putString("issuerCardId", card.getIssuerCardId());
                cardMap.putString("lastDigits", card.getLastDigits());
                cardMap.putString("label", cardDisplay.getLabel());
                cardMap.putString("bin", card.getBin());
                cardMap.putString("expiryDate", (calendar.get(Calendar.MONTH) + 1 < 10 ? "0"+(calendar.get(Calendar.MONTH)+1) : calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.YEAR)%2000);
                cardMap.putString("graphicResource", cardDisplay.getGraphicResource(reactContext.getApplicationContext()) != null ?
                    getEncoded64ImageStringFromBitmap(drawableToBitmap(cardDisplay.getGraphicResource(reactContext.getApplicationContext()))) : null);
                cardMap.putInt("availablePaymentKeyNumber", card.getAvailablePaymentKeyNumber());
                cardMap.putBoolean("isDefaultCard", defaultCardId!=null && defaultCardId.equals(card.getId()));
                cardMap.putBoolean("isNextTransactionCard", nextTransactionCardId!=null && nextTransactionCardId.equals(card.getId()));
                cardMap.putString("status", card.getStatus().name());
                cardsArray.pushMap(cardMap);
            }
            params.putArray("emulatedCards", cardsArray);

            promise.resolve(params);
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void getEmulatedCardsNumber(Promise promise) {
        if (mWallet != null) {
            promise.resolve(mWallet.cards(false).size());
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void getHceTransactions(int offset, int limit, final Promise promise) {
        if (mWallet != null && getCurrentActivity() != null) {
            final LiveData<List<HceTransaction>> liveDataTransactions = mWalletManager.listHceTransactions((Application) reactContext.getApplicationContext(), offset, limit);

            Handler handler = new Handler(reactContext.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    liveDataTransactions.observe((AppCompatActivity) getCurrentActivity(), new Observer<List<HceTransaction>>() {
                        @Override
                        public void onChanged(List<HceTransaction> transactions) {
                            WritableMap params = Arguments.createMap();
                            WritableArray transactionsArray = Arguments.createArray();
                            for(HceTransaction transaction : transactions) {
                                WritableMap transactionMap = Arguments.createMap();
                                transactionMap.putString("uuid", transaction.getId());
                                transactionMap.putString("merchantName", transaction.getMerchantName());
                                transactionMap.putDouble("amount", transaction.getAmount().doubleValue());
                                transactionMap.putString("transactionDate", transaction.getDate().toString());
                                transactionMap.putBoolean("completed", transaction.getStatus() == HceTransactionStatus.Success);
                                transactionsArray.pushMap(transactionMap);
                            }
                            params.putArray("transactions", transactionsArray);



                            promise.resolve(params);
                            if(getCurrentActivity() != null)
                                liveDataTransactions.removeObservers((AppCompatActivity) getCurrentActivity());
                        }
                    });
                }
            });
        } else {
            promise.reject(WalletManagerErrors.ERROR.toString(), "Wallet is empty.");
        }
    }

    @ReactMethod
    public void authenticatePushRequest(Promise promise) {
        try {
            if (!pushAuthenticationRequestList.isEmpty()) {
                pushAuthenticationRequestList.get(0).authenticate(reactContext.getApplicationContext(), generateCustomCallback(promise, null, null, null));
                onPushAuthenticationRequestPromise = promise;
            } else {
                promise.reject(AuthenticatedProcessCallbackErrors.ERROR.toString(), "No PushAuthenticationRequest found");
            }
        } catch (WalletValidationException e) {
            promise.reject(e.getCode().toString(), e.getMessage());
        }
    }

    @ReactMethod
    public void cancelPushAuthenticationRequest(Promise promise) {
        if(!pushAuthenticationRequestList.isEmpty()) {
            PushAuthenticationRequest pushAuthenticationRequest = pushAuthenticationRequestList.get(0);
            try {
                pushAuthenticationRequest.cancel(reactContext.getApplicationContext());
                onPushAuthenticationRequestPromise.reject("CANCELLED", "CANCELLED");
                popPushAuthenticationRequest();
                promise.resolve(null);
            } catch (WalletValidationException error) {
                promise.reject(error.getCode().toString(), error.getMessage());
            }
        }
    }

    @ReactMethod
    public void onTokenRefresh() {
        AntelopFirebaseMessagingUtil.onTokenRefresh(reactContext.getApplicationContext());
    }

    @ReactMethod
    public void onMessageReceived(ReadableMap remoteMessage) {
        RemoteMessage message = FirebaseMessagingSerializer.remoteMessageFromReadableMap(remoteMessage);

        AntelopFirebaseMessagingUtil.onMessageReceived(
            reactContext.getApplicationContext(),
            message
        );
    }

    private void checkMaximumAttempts() throws WalletValidationException {
        for (CustomerAuthenticationMethod method : mWallet.settings().security().getCustomerAuthenticationMethods().getValue().values()) {
            if (method.getMaximumAttemptsCount() != -1 && method.getCurrentAttemptsCount() >= method.getMaximumAttemptsCount()) {
                // Cancel all pending PushAuthenticationRequest
                if(!pushAuthenticationRequestList.isEmpty()) {
                    try {
                        for(PushAuthenticationRequest pushAuthenticationRequest : pushAuthenticationRequestList) {
                            if(pushAuthenticationRequest.getExpiryTimestamp() > Calendar.getInstance().getTimeInMillis()) {
                                pushAuthenticationRequest.cancel(reactContext.getApplicationContext());
                            }
                        }
                    } catch (WalletValidationException e) {
                        e.printStackTrace();
                    }
                    pushAuthenticationRequestList.clear();
                }
                mWalletManager.lock(WalletLockReason.FraudulentUseSuspected);
                break;
            }
        }
    }

    private void popPushAuthenticationRequest() {
        pushAuthenticationRequestList.remove(0);
        cleanPushAuthenticationRequestList();
    }

    private void cleanPushAuthenticationRequestList() {
        for(PushAuthenticationRequest pushAuthenticationRequest : pushAuthenticationRequestList) {
            if(pushAuthenticationRequest.getExpiryTimestamp() <= Calendar.getInstance().getTimeInMillis()) {
                pushAuthenticationRequestList.remove(pushAuthenticationRequest);
            }
        }
    }

    @ReactMethod
    private void checkIfPushAuthenticationRequestWaiting() {
        if(!pushAuthenticationRequestList.isEmpty()) {
            WritableMap params = Arguments.createMap();
            params.putString("issuerDataHex", encodeHexString(pushAuthenticationRequestList.get(0).getIssuerData()));
            params.putString("expiryTimestamp", String.valueOf(pushAuthenticationRequestList.get(0).getExpiryTimestamp()));
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onPushAuthenticationRequestReceived", params);
        }
    }

    private void setupAntelopPushAuthentication() {
        AntelopPushAuthenticationRequestListener.setCallback(new AntelopPushAuthenticationRequestListener.Callback() {
            @Override
            public void onRequestReceived(PushAuthenticationRequest pushAuthenticationRequest) {
                // Remove all the expired Push
                cleanPushAuthenticationRequestList();

                // Add Push to list if not already in it
                boolean containsPush = false;
                for(PushAuthenticationRequest push : pushAuthenticationRequestList) {
                    if(push.getId().equals(pushAuthenticationRequest.getId())) {
                        containsPush = true;
                        break;
                    }
                }
                if(!containsPush) {
                    pushAuthenticationRequestList.add(pushAuthenticationRequest);
                }

                WritableMap params = Arguments.createMap();
                params.putString("issuerDataHex", encodeHexString(pushAuthenticationRequest.getIssuerData()));
                params.putString("expiryTimestamp", String.valueOf(pushAuthenticationRequest.getExpiryTimestamp()));
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onPushAuthenticationRequestReceived", params);
            }

            @Override
            public void onRequestCancelled(String requestId, CancellationReason cancellationReason) {
                // In our case, PushRequest will be never cancelled by the requestor
            }
        });
    }

    private void setupWalletNotificationServiceCallback() {
        NoelseWalletNotificationServiceCallback.setCallback(new NoelseWalletNotificationServiceCallback.Callback() {
            @Override
            public void onWalletLocked(WalletLockReason walletLockReason) {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onWalletLocked", "");
            }

            @Override
            public void onWalletDeleted() {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onWalletDeleted", "");
            }
        });
    }

    private void setupAntelopHceTransactionListenerCallback() {
        AntelopHceTransactionListener.setCallback(new AntelopHceTransactionListener.Callback() {
            @Override
            public void onTransactionStart(@NonNull Context context) {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceTransactionStart","");
                if(mWallet != null && mWallet.getDefaultCardId() == null) {
                    try {
                        Map<String, Card> cards = mWallet.cards(false);
                        if(!cards.isEmpty() && cards.values().size() == 1) {
                            Card firstCard = cards.entrySet().iterator().next().getValue();
                            mWallet.setDefaultCard(firstCard.getId());
                        }
                    } catch (WalletValidationException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCredentialsRequired(@NonNull List<CustomerAuthenticationMethod> allowedMethods, @NonNull HceTransaction transaction) {
                WritableMap params = Arguments.createMap();
                WritableMap transactionData = Arguments.createMap();
                transactionData.putDouble("purchaseAmount", transaction.getAmount().doubleValue());
                transactionData.putString("merchantName", transaction.getMerchantName());
                transactionData.putString("purchaseCurrency", transaction.getCurrency().getSymbol());

                params.putMap("transactionData", transactionData);

                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceCredentialsRequired", params);
            }

            @Override
            public void onTransactionDone(@NonNull HceTransaction hceTransaction) {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceTransactionDone","");
            }

            @Override
            public void onTransactionError(@NonNull AntelopError antelopError) {
                if (antelopError.getCode() == AntelopErrorCode.NfcDisconnection)
                    return;

                String error = "errorHasOccured";
                if (antelopError.getCode() == AntelopErrorCode.NoCard)
                    error = "errorNoCard";
                else if (antelopError.getCode() == AntelopErrorCode.NoCardSelected)
                    error = "errorNoCardSelected";
                else if (antelopError.getCode() == AntelopErrorCode.MaximumAmountExceeded)
                    error = "errorMaximumAmountExceeded";

                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onHceTransactionError",error);
            }
        });
    }

    private String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    private String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public String getEncoded64ImageStringFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        byte[] byteFormat = stream.toByteArray();
        // get the base 64 string
        String imgString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
            imgString = Base64.encodeToString(byteFormat, Base64.NO_WRAP);
        }
        return imgString;
    }
}
