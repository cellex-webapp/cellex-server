package com.example.cellex.services.payment.vnpay;

import com.example.cellex.config.VnpayConfig;
import com.example.cellex.dtos.response.vnpay.VnpayPaymentResponse;
import com.example.cellex.utils.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayConfig vnpayConfig;

    public VnpayPaymentResponse createPaymentUrl(
            String orderId,
            Long amount,
            String orderInfo,
            String ipAddress,
            String locale
    ) {
        try {
            log.info("Creating VNPay payment URL for order: {}", orderId);

            // Build parameters
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnpayConfig.getVersion());
            vnpParams.put("vnp_Command", vnpayConfig.getCommand());
            vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(amount * 100));
            vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrency());
            vnpParams.put("vnp_TxnRef", orderId);
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", vnpayConfig.getOrderType());
            vnpParams.put("vnp_Locale", locale != null ? locale : vnpayConfig.getLocale());
            vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", ipAddress);

            // Add timestamp
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            // Add expire date (10 minutes from now - giống dự án tham khảo)
            cld.add(Calendar.MINUTE, 10);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Sort parameters and build query string
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Build query string with URLEncoding
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                    }
                }
            }

            // IMPORTANT: Hash the ENCODED query string (same as vnpay Node.js library)
            String queryUrl = query.toString();
            String vnpSecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), queryUrl);
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            String paymentUrl = vnpayConfig.getVnpayUrl() + "?" + queryUrl;

            log.info("VNPay payment URL created successfully for order: {}", orderId);
            log.debug("Hash data: {}", queryUrl);
            log.debug("Secure hash: {}", vnpSecureHash);

            return VnpayPaymentResponse.builder()
                    .code("00")
                    .message("Success")
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment URL for order: {}", orderId, e);
            return VnpayPaymentResponse.builder()
                    .code("99")
                    .message("Error creating payment URL: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Verify IPN callback from VNPay
     * @param request HttpServletRequest containing VNPay parameters
     * @return Map containing verification result
     */
    public Map<String, Object> verifyIpnCall(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get all parameters from request
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnpSecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            // Build hash data from sorted parameters (không encode như doc VNPAY)
            String hashData = VnpayUtil.hashAllFields(fields);
            String signValue = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

            log.info("VNPay verification - Hash data: {}", hashData);
            log.info("VNPay verification - Calculated hash: {}", signValue);
            log.info("VNPay verification - Received hash: {}", vnpSecureHash);
            log.info("VNPay verification - Secret key: {}", vnpayConfig.getHashSecret());

            // Verify signature
            if (signValue.equals(vnpSecureHash)) {
                String vnpResponseCode = request.getParameter("vnp_ResponseCode");
                String vnpTxnRef = request.getParameter("vnp_TxnRef");
                String vnpAmount = request.getParameter("vnp_Amount");
                String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
                String vnpBankCode = request.getParameter("vnp_BankCode");
                String vnpPayDate = request.getParameter("vnp_PayDate");

                // Check if payment is successful
                if ("00".equals(vnpResponseCode)) {
                    result.put("isSuccess", true);
                    result.put("message", "Payment successful");
                } else {
                    result.put("isSuccess", false);
                    result.put("message", "Payment failed with code: " + vnpResponseCode);
                }

                result.put("orderId", vnpTxnRef);
                result.put("amount", Long.parseLong(vnpAmount) / 100); // Convert back to VND
                result.put("transactionNo", vnpTransactionNo);
                result.put("bankCode", vnpBankCode);
                result.put("payDate", vnpPayDate);
                result.put("responseCode", vnpResponseCode);

            } else {
                result.put("isSuccess", false);
                result.put("message", "Invalid signature");
            }

        } catch (Exception e) {
            log.error("Error verifying VNPay IPN call", e);
            result.put("isSuccess", false);
            result.put("message", "Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Verify return URL from VNPay
     * @param request HttpServletRequest containing VNPay parameters
     * @return Map containing verification result
     */
    public Map<String, Object> verifyReturnUrl(HttpServletRequest request) {
        return verifyIpnCall(request); // Same logic as IPN verification
    }

    /**
     * Get transaction status message by response code
     */
    public String getTransactionStatusMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65" -> "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch không thành công";
        };
    }
}
