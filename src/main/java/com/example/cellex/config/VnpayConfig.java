package com.example.cellex.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnpayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command:pay}")
    private String command;

    @Value("${vnpay.currency:VND}")
    private String currency;

    @Value("${vnpay.locale:vn}")
    private String locale;

    @Value("${vnpay.order-type:other}")
    private String orderType;

    @Value("${frontend.payment.success-url:http://localhost:3000/payment/success}")
    private String frontendSuccessUrl;

    @Value("${frontend.payment.failure-url:http://localhost:3000/payment/failure}")
    private String frontendFailureUrl;

    // VNPay API Version
    public static final String VERSION = "2.1.0";

    // VNPay Command
    public static final String COMMAND = "pay";

    // Currency Code
    public static final String CURRENCY_CODE = "VND";

    // VNPay Locale
    public static final String LOCALE_VN = "vn";
    public static final String LOCALE_EN = "en";

    // Order Type
    public static final String ORDER_TYPE = "other";

    // Response Codes
    public static final String SUCCESS_CODE = "00";
    public static final String TRANSACTION_SUCCESS_CODE = "00";
}
