package com.example.cellex.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // General Errors
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi chưa xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Khóa message không hợp lệ", HttpStatus.BAD_REQUEST),

    // Authentication & Authorization Errors
    USER_EXISTED(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Tên đăng nhập phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Mật khẩu phải có ít nhất 8 ký tự", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "Không tìm thấy email hoặc mật khẩu", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền truy cập chức năng này", HttpStatus.FORBIDDEN),

    // Sign Up Errors
    PASSWORDS_DO_NOT_MATCH(1008, "Mật khẩu nhập lại không khớp", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1009, "Email này đã được đăng ký", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1010, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1011, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED(1012, "Mã OTP đã được sử dụng", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(1013, "Gửi email thất bại. Vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),

    // Domain Specific Errors
    PRODUCT_NOT_FOUND(2001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(2002, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXISTED(2003, "Danh mục không tồn tại", HttpStatus.NOT_FOUND),
    SHOP_NOT_FOUND(2004, "Không tìm thấy cửa hàng", HttpStatus.NOT_FOUND),
    SHOP_ALREADY_EXISTS(2005, "Bạn đã có cửa hàng, không thể đăng ký thêm", HttpStatus.BAD_REQUEST),
    SHOP_NOT_VERIFIED(2006, "Cửa hàng chưa được duyệt", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND_OR_NOT_VERIFIED(2007, "Không tìm thấy cửa hàng hoặc cửa hàng chưa được duyệt", HttpStatus.BAD_REQUEST),

    // Category Attribute Errors
    CATEGORY_ATTRIBUTE_NOT_FOUND(2008, "Không tìm thấy thuộc tính danh mục", HttpStatus.NOT_FOUND),
    ATTRIBUTE_KEY_EXISTED(2009, "Khóa thuộc tính đã tồn tại trong danh mục này", HttpStatus.BAD_REQUEST),
    SELECT_OPTIONS_REQUIRED(2010, "Thuộc tính dạng chọn bắt buộc phải có danh sách lựa chọn", HttpStatus.BAD_REQUEST),
    REQUIRED_ATTRIBUTE_MISSING(2011, "Thiếu thuộc tính bắt buộc", HttpStatus.BAD_REQUEST),
    INVALID_ATTRIBUTE_VALUE(2012, "Giá trị thuộc tính không hợp lệ", HttpStatus.BAD_REQUEST),

    // File Upload Errors
    INVALID_INPUT(3001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(3002, "Tải lên file thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(3003, "Xóa file thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_FORMAT(3004, "Định dạng file không hợp lệ", HttpStatus.BAD_REQUEST),
    FILE_SIZE_TOO_LARGE(3005, "Kích thước file quá lớn", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND(3006, "Không tìm thấy file", HttpStatus.NOT_FOUND),

    // Access Control Errors
    ACCESS_DENIED(4001, "Truy cập bị từ chối", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_OWNED(4002, "Bạn không sở hữu tài nguyên này", HttpStatus.FORBIDDEN),
    OPERATION_NOT_ALLOWED(4003, "Thao tác không được phép", HttpStatus.FORBIDDEN),

    // Validation Errors
    FIELD_REQUIRED(5001, "Thiếu trường bắt buộc", HttpStatus.BAD_REQUEST),
    FIELD_TOO_LONG(5002, "Giá trị trường quá dài", HttpStatus.BAD_REQUEST),
    FIELD_TOO_SHORT(5003, "Giá trị trường quá ngắn", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(5004, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT(5005, "Định dạng số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST),
    DUPLICATE_VALUE(5006, "Giá trị bị trùng lặp không được phép", HttpStatus.BAD_REQUEST),

    // Account Ban Errors
    ACCOUNT_BANNED(6001, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    ACCOUNT_ALREADY_BANNED(6002, "Tài khoản đã bị khóa trước đó", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_BANNED(6003, "Tài khoản chưa bị khóa", HttpStatus.BAD_REQUEST),
    CANNOT_BAN_ADMIN(6004, "Không thể khóa tài khoản admin", HttpStatus.FORBIDDEN),
    CANNOT_BAN_SELF(6005, "Không thể tự khóa tài khoản của mình", HttpStatus.FORBIDDEN),

    // Customer Segment Errors
    SEGMENT_NOT_FOUND(7001, "Không tìm thấy phân khúc khách hàng", HttpStatus.NOT_FOUND),
    SEGMENT_ALREADY_EXISTS(7002, "Phân khúc khách hàng đã tồn tại", HttpStatus.BAD_REQUEST),

    // Coupon Errors
    COUPON_NOT_FOUND(8001, "Không tìm thấy mã giảm giá", HttpStatus.NOT_FOUND),
    COUPON_EXPIRED(8002, "Mã giảm giá đã hết hạn", HttpStatus.BAD_REQUEST),
    COUPON_ALREADY_USED(8003, "Mã giảm giá đã được sử dụng", HttpStatus.BAD_REQUEST),
    COUPON_NOT_APPLICABLE(8004, "Mã giảm giá không áp dụng cho đơn hàng này", HttpStatus.BAD_REQUEST),

    // Campaign Errors
    CAMPAIGN_NOT_FOUND(8100, "Không tìm thấy chiến dịch", HttpStatus.NOT_FOUND),
    CAMPAIGN_NOT_ACTIVE(8101, "Chiến dịch chưa hoạt động", HttpStatus.BAD_REQUEST),
    CAMPAIGN_ALREADY_DISTRIBUTED(8102, "Chiến dịch đã được phân phối", HttpStatus.BAD_REQUEST),

    // General Errors
    INVALID_REQUEST(9001, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(9002, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_EXISTED(9003, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),

    // Cart Errors
    CART_NOT_FOUND(10001, "Không tìm thấy giỏ hàng", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(10002, "Không tìm thấy sản phẩm trong giỏ hàng", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK(10003, "Sản phẩm đã hết hàng", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(10004, "Số lượng sản phẩm không đủ", HttpStatus.BAD_REQUEST),
    CART_EMPTY(10005, "Giỏ hàng trống", HttpStatus.BAD_REQUEST),

    // Address Errors
    ADDRESS_NOT_FOUND(11001, "Không tìm thấy địa chỉ", HttpStatus.NOT_FOUND),
    ADDRESS_REQUIRED(11002, "Vui lòng thêm địa chỉ giao hàng", HttpStatus.BAD_REQUEST),

    // Order Errors
    ORDER_NOT_FOUND(12001, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_ALREADY_PROCESSED(12002, "Đơn hàng đã được xử lý", HttpStatus.BAD_REQUEST),
    ORDER_NOT_CONFIRMED(12003, "Đơn hàng chưa được xác nhận", HttpStatus.BAD_REQUEST),
    ORDER_NOT_SHIPPING(12004, "Đơn hàng chưa ở trạng thái vận chuyển", HttpStatus.BAD_REQUEST),
    CANNOT_CANCEL_ORDER(12005, "Không thể hủy đơn hàng này", HttpStatus.BAD_REQUEST),
    CANNOT_APPLY_COUPON_TO_THIS_ORDER(12006, "Không thể áp dụng mã giảm giá cho đơn hàng này", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_THIS_ORDER(12007, "Không thể chỉnh sửa đơn hàng này", HttpStatus.BAD_REQUEST),
    COUPON_NOT_BELONG_TO_USER(12008, "Mã giảm giá không thuộc về bạn", HttpStatus.BAD_REQUEST),
    COUPON_NOT_ACTIVE(12009, "Mã giảm giá không còn hiệu lực", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_PUBLISHED(12010, "Sản phẩm chưa được xuất bản", HttpStatus.BAD_REQUEST),
    NO_PRODUCTS_SELECTED(12011, "Vui lòng chọn ít nhất một sản phẩm", HttpStatus.BAD_REQUEST),
    PRODUCTS_MUST_BE_FROM_SAME_SHOP(12012, "Các sản phẩm phải cùng một cửa hàng", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_ORDER(12013, "Không thể xóa đơn hàng này", HttpStatus.BAD_REQUEST),

    // Payment Errors
    PAYMENT_ERROR(13001, "Lỗi thanh toán", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(13002, "Thanh toán thất bại", HttpStatus.BAD_REQUEST),
    PAYMENT_CANCELLED(13003, "Thanh toán bị hủy", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
