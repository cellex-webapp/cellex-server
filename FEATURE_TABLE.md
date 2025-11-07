# BẢNG CHỨC NĂNG DỰ ÁN CELLEX - HỆ THỐNG E-COMMERCE

## CHỨC NĂNG ĐÃ TRIỂN KHAI ✅

| **Nhóm Chức Năng** | **Mô Tả Chi Tiết** |
|---------------------|-------------------|
| **🔐 Xác thực & Phân quyền** | • Đăng nhập/Đăng ký (CUSTOMER, VENDOR, ADMIN)<br>• Xác thực JWT với Access Token & Refresh Token<br>• Quên mật khẩu qua OTP Email<br>• Phân quyền theo vai trò (Role-based) |
| **👤 Quản lý người dùng** | • CRUD thông tin user (Admin)<br>• Cập nhật hồ sơ cá nhân (Avatar, địa chỉ)<br>• Khóa/Mở khóa tài khoản (Admin)<br>• Xem danh sách user theo vai trò |
| **🏪 Quản lý cửa hàng** | • Đăng ký trở thành Vendor<br>• Xác minh cửa hàng (Admin approve)<br>• CRUD thông tin cửa hàng<br>• Upload logo, banner<br>• Quản lý trạng thái shop (PENDING/VERIFIED/REJECTED) |
| **📦 Quản lý sản phẩm** | • CRUD sản phẩm với upload ảnh (Vendor)<br>• Phân loại theo danh mục động<br>• Thuộc tính sản phẩm linh hoạt (màu sắc, kích thước...)<br>• Quản lý giá, giảm giá, tồn kho<br>• Tìm kiếm và lọc sản phẩm |
| **📂 Quản lý danh mục** | • CRUD danh mục sản phẩm (Admin)<br>• Quản lý thuộc tính danh mục động<br>• Hỗ trợ nhiều kiểu dữ liệu (TEXT, NUMBER, SELECT, BOOLEAN) |
| **🛒 Giỏ hàng** | • Thêm/Xóa/Cập nhật sản phẩm trong giỏ<br>• Tính toán tổng tiền tự động<br>• Kiểm tra tồn kho real-time |
| **🎟️ Hệ thống Coupon nâng cao** | • **Coupon Campaign**: Tạo chiến dịch khuyến mãi<br>• **Segment Coupon**: Coupon theo phân khúc khách hàng<br>• **User Coupon**: Quản lý coupon của user<br>• Tự động phát coupon khi nâng hạng<br>• Lập lịch phát coupon định kỳ (DAILY/WEEKLY/MONTHLY)<br>• Hỗ trợ giảm giá % hoặc số tiền cố định<br>• Giới hạn số lượng & thời gian sử dụng |
| **👥 Phân khúc khách hàng tự động** | • Tạo phân khúc dựa trên chi tiêu (Kim Cương/Vàng/Bạc/Đồng...)<br>• **Tự động nâng/hạ hạng** khi thay đổi tổng chi tiêu<br>• Lưu lịch sử thay đổi phân khúc<br>• Phát coupon tự động khi nâng hạng |
| **📍 Quản lý địa chỉ** | • Lấy danh sách tỉnh/thành phố<br>• Lấy danh sách quận/huyện theo tỉnh<br>• Tự động format địa chỉ đầy đủ |

---

## CHỨC NĂNG CHƯA TRIỂN KHAI (GỢI Ý CÔNG NGHỆ) ⏳

| **Nhóm Chức Năng** | **Mô Tả Chi Tiết** | **Công Nghệ Đề Xuất** |
|---------------------|-------------------|----------------------|
| **📊 Quản lý đơn hàng** | • Tạo đơn hàng từ giỏ hàng<br>• Cập nhật trạng thái (Processing/Shipping/Completed)<br>• Theo dõi lịch sử đơn hàng<br>• Hủy đơn hàng | Spring Boot + MongoDB Transaction |
| **💳 Thanh toán trực tuyến** | • Tích hợp cổng thanh toán VNPay<br>• Thanh toán MoMo, ZaloPay<br>• Thanh toán COD<br>• Lịch sử giao dịch | **VNPay Sandbox** (Miễn phí test)<br>**Stripe** (International) |
| **⭐ Đánh giá & Bình luận** | • Đánh giá sao (1-5)<br>• Bình luận có ảnh<br>• Vendor trả lời đánh giá<br>• Lọc theo rating | Spring Boot + MongoDB |
| **🤖 Kiểm duyệt bình luận tự động** | • Phát hiện bình luận tiêu cực/spam<br>• Tự động ẩn nội dung xấu<br>• Gửi cảnh báo đến Admin | **OpenAI Moderation API** (Miễn phí)<br>Hoặc **Perspective API** (Google) |
| **💬 Chat realtime** | • Chat giữa Admin-Vendor-Customer<br>• Gửi ảnh, file<br>• Thông báo tin nhắn mới | **Socket.io** hoặc **WebSocket**<br>**Firebase Realtime DB** (Miễn phí) |
| **🔔 Thông báo đẩy** | • Thông báo đơn hàng, nâng hạng<br>• Thông báo khuyến mãi<br>• Push notification mobile | **Firebase Cloud Messaging** (Miễn phí)<br>**OneSignal** (Free tier) |
| **🎤 Tìm kiếm bằng giọng nói** | • Voice-to-text search<br>• Hỗ trợ tiếng Việt | **Web Speech API** (Miễn phí, built-in browser)<br>Hoặc **Google Speech-to-Text** |
| **🧠 Đề xuất sản phẩm thông minh** | • Gợi ý dựa trên lịch sử mua hàng<br>• Sản phẩm tương tự<br>• Người dùng khác cũng mua | **Apache Mahout** (Collaborative Filtering)<br>Hoặc logic đơn giản: "Frequently Bought Together" |
| **📈 Báo cáo & Thống kê (Admin)** | • Số lượng users, vendors, đơn hàng<br>• Doanh thu theo thời gian<br>• Sản phẩm bán chạy<br>• Biểu đồ trực quan | **Spring Boot + Aggregation Pipeline**<br>**Chart.js** (Frontend) |
| **📊 Dashboard Vendor** | • Tổng quan doanh thu<br>• Biểu đồ bán hàng theo thời gian<br>• Lịch sử đơn hàng<br>• Top sản phẩm bán chạy | Spring Boot Aggregation + Chart.js |
| **📦 Quản lý kho thông minh** | • Theo dõi tồn kho realtime<br>• Cảnh báo sắp hết hàng (threshold)<br>• Lịch sử nhập/xuất | Spring Boot + Scheduled Tasks<br>Email Alert với **JavaMailSender** |
| **📧 Hệ thống Email** | • Email xác nhận đơn hàng<br>• Email thông báo nâng hạng<br>• Newsletter marketing | **Spring Boot Mail** + **Thymeleaf Template** |

---

## CÔNG NGHỆ ĐÃ SỬ DỤNG 🛠️

- **Backend**: Spring Boot 3.x, Spring Security, JWT
- **Database**: MongoDB (NoSQL)
- **File Upload**: Cloudinary API
- **Email**: JavaMailSender (Gmail SMTP)
- **Documentation**: Swagger/OpenAPI 3.0
- **Scheduler**: Spring @Scheduled (Phát coupon định kỳ)

---

## ƯU TIÊN TRIỂN KHAI TIẾP THEO 🎯

1. **Quản lý đơn hàng** (Core feature - quan trọng nhất)
2. **Thanh toán VNPay** (Sandbox miễn phí, dễ tích hợp)
3. **Đánh giá sản phẩm** (Tăng tính tương tác)
4. **Chat realtime** với **Socket.io** hoặc **Firebase**
5. **Báo cáo thống kê** cho Admin và Vendor
6. **Kiểm duyệt tự động** bằng **OpenAI Moderation API**

---

## GHI CHÚ 📝

✅ **Điểm mạnh hiện tại:**
- Hệ thống phân quyền rõ ràng
- Coupon & Segment automation hoàn chỉnh
- Upload ảnh/file ổn định (Cloudinary)
- API documentation đầy đủ (Swagger)

⚠️ **Cần cải thiện:**
- Chưa có quản lý đơn hàng (core feature)
- Thiếu tính năng thanh toán
- Chưa có chat và notification realtime
- Chưa có analytics/reporting

💡 **Lưu ý khi mở rộng:**
- Ưu tiên các API miễn phí (OpenAI Moderation, Firebase FCM, Web Speech API)
- Sử dụng Sandbox khi test payment (VNPay, Stripe)
- Tối ưu query MongoDB với index khi có nhiều dữ liệu
- Xem xét cache Redis cho sản phẩm hot

