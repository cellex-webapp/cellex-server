# Hướng dẫn cấu hình Cloudinary

## Vấn đề: "cloud_name is disabled"

Lỗi này xảy ra khi Cloudinary không được cấu hình đúng cách trong file `.env`.

## Giải pháp

Có **2 cách** để cấu hình Cloudinary trong file `.env`:

### Cách 1: Sử dụng CLOUDINARY_URL (Khuyến nghị - Đơn giản nhất)

```env
CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME
```

**Ví dụ:**
```env
CLOUDINARY_URL=cloudinary://123456789012345:AbCdEfGhIjKlMnOpQrStUvWxYz@your-cloud-name
```

### Cách 2: Sử dụng các biến riêng lẻ

```env
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=AbCdEfGhIjKlMnOpQrStUvWxYz
CLOUDINARY_FOLDER_PREFIX=cellex
```

## ⚠️ LƯU Ý QUAN TRỌNG VỀ CLOUDINARY SETTINGS

### 1. 🔒 Kiểm tra API Restrictions trên Cloudinary Dashboard

**Cloudinary có thể chặn IP từ máy khác nếu bạn đã bật các chế độ bảo mật:**

#### Cách kiểm tra và sửa:

1. Đăng nhập vào https://cloudinary.com/
2. Vào **Settings** (⚙️) → **Security**
3. Kiểm tra các mục sau:

#### a) **Allowed IP Addresses**
- ❌ **Nếu có whitelist IP:** Cloudinary chỉ cho phép upload từ những IP được list
- ✅ **Giải pháp:** 
  - Thêm IP của máy bạn bạn vào whitelist
  - HOẶC xóa hết whitelist để cho phép mọi IP (phù hợp cho dev)

#### b) **Unsigned Upload Prevention**
- ❌ **Nếu bật:** Chỉ cho phép upload có chữ ký (signed upload)
- ✅ **Giải pháp:** Tắt tùy chọn này nếu bạn đang dùng signed upload (app đang dùng API Secret để sign)

#### c) **API Rate Limits**
- Cloudinary Free plan có giới hạn:
  - **500 transformations/month**
  - **25 credits/month**
- Nếu vượt quá, API sẽ trả về lỗi

### 2. 🌐 Network & Firewall Issues

#### Có thể bị chặn bởi:
- **Firewall công ty/trường học:** Chặn kết nối đến Cloudinary API
- **VPN/Proxy:** Cloudinary có thể chặn một số IP của VPN
- **Antivirus:** Một số phần mềm chặn upload file

#### Cách test:
```bash
# Test kết nối đến Cloudinary
curl https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/image/upload
```

Nếu không kết nối được → vấn đề network/firewall

### 3. 🔑 API Key vs API Secret

- **API Key**: Public, có thể share
- **API Secret**: PRIVATE, KHÔNG ĐƯỢC commit lên Git

**Quan trọng:** Backend đang dùng **Signed Upload** (có API Secret) → an toàn hơn

### 4. 📂 Folder Permissions

Nếu set `CLOUDINARY_FOLDER_PREFIX=cellex`:
- Kiểm tra folder `cellex` có tồn tại trên Cloudinary không
- Hoặc Cloudinary sẽ tự tạo folder mới

### 5. 🔄 Environment Variables Priority

Ứng dụng đọc theo thứ tự:
1. **CLOUDINARY_URL** (ưu tiên cao nhất)
2. **CLOUDINARY_CLOUD_NAME, API_KEY, API_SECRET** (fallback)

→ Nếu có cả 2, chỉ cần 1 cái đúng là được

## Lấy thông tin Cloudinary ở đâu?

1. Đăng nhập vào https://cloudinary.com/
2. Vào **Dashboard**
3. Tìm phần **Account Details** hoặc **API Keys**
4. Copy thông tin:
   - **Cloud Name**
   - **API Key**
   - **API Secret**

## Kiểm tra cấu hình

Sau khi cấu hình, khi khởi động ứng dụng bạn sẽ thấy log:

✅ **Thành công:**
```
✅ Cloudinary: Using CLOUDINARY_URL configuration
```
hoặc
```
✅ Cloudinary: Using individual configuration (cloud_name, api_key, api_secret)
```

❌ **Thất bại:**
```
❌ Cloudinary: No valid configuration found!
   Please set either CLOUDINARY_URL or all of: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
```

## Lưu ý khi làm việc nhóm

- **KHÔNG** commit file `.env` lên Git
- File `.env` phải nằm ở **thư mục gốc** của project (cùng cấp với `pom.xml`)
- Sau khi sửa file `.env`, cần **restart** ứng dụng
- Kiểm tra file `.env` không có khoảng trắng thừa trước/sau dấu `=`
- **Mỗi người phải có file `.env` riêng** trên máy mình

## Ví dụ file .env hoàn chỉnh

```env
# JWT Configuration
APPLICATION_SECURITY_JWT_SECRET_KEY=your-secret-key-here
APPLICATION_SECURITY_JWT_ACCESS_TOKEN_EXPIRATION=86400000
APPLICATION_SECURITY_JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Server Configuration
SERVER_PORT=8080

# MongoDB
MONGO_URI=mongodb://localhost:27017/cellex

# Cloudinary (Chọn 1 trong 2 cách)
# Cách 1: URL đầy đủ (Khuyến nghị)
CLOUDINARY_URL=cloudinary://123456789012345:AbCdEfGhIjKlMnOpQrStUvWxYz@your-cloud-name

# Cách 2: Các biến riêng lẻ (comment nếu dùng cách 1)
# CLOUDINARY_CLOUD_NAME=your-cloud-name
# CLOUDINARY_API_KEY=123456789012345
# CLOUDINARY_API_SECRET=AbCdEfGhIjKlMnOpQrStUvWxYz
# CLOUDINARY_FOLDER_PREFIX=cellex

# Email
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REFRESH_TOKEN=your-refresh-token
```

## Test upload ảnh

Sau khi cấu hình xong, test bằng cách:
1. Start ứng dụng
2. Kiểm tra log có thông báo Cloudinary success
3. Thử upload ảnh qua API (ví dụ: tạo shop, upload product image)
4. Kiểm tra ảnh có xuất hiện trên Cloudinary Dashboard

## Troubleshooting

### Vẫn bị lỗi "cloud_name is disabled"?

1. **Kiểm tra file .env có tồn tại không:**
   ```
   dir .env
   ```

2. **Kiểm tra nội dung file .env:**
   ```
   type .env
   ```

3. **Kiểm tra không có khoảng trắng thừa:**
   ```
   CLOUDINARY_URL=cloudinary://...  ❌ (có dấu cách ở cuối)
   CLOUDINARY_URL=cloudinary://...  ✅ (không có dấu cách)
   ```

4. **Restart lại ứng dụng hoàn toàn** (không dùng hot reload)

5. **Kiểm tra log khi khởi động** - xem có load được biến CLOUDINARY_URL không

### Lỗi upload từ máy khác nhưng máy của bạn OK?

1. **Kiểm tra Cloudinary Security Settings:**
   - Settings → Security → Allowed IP Addresses
   - Xóa whitelist HOẶC thêm IP của máy khác

2. **Kiểm tra Network:**
   ```bash
   # Test từ máy bị lỗi
   curl -X POST https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/image/upload
   ```

3. **Kiểm tra API Key giống nhau:**
   - So sánh file `.env` của 2 máy
   - Đảm bảo cloud_name, api_key, api_secret giống hệt nhau

4. **Kiểm tra VPN/Proxy:**
   - Tắt VPN thử
   - Thử đổi mạng (4G, WiFi khác)

5. **Kiểm tra Firewall/Antivirus:**
   - Tắt tạm thời để test
   - Thêm exception cho Java/IntelliJ

### Lỗi "Invalid API Key" hoặc "Invalid signature"?

- ✅ Kiểm tra API_KEY và API_SECRET có đúng không
- ✅ Không có khoảng trắng thừa trong `.env`
- ✅ CLOUD_NAME phải là tên chính xác (lowercase, no spaces)
- ✅ Restart ứng dụng sau khi sửa

### Lỗi "Upload preset required"?

- Không áp dụng cho app này vì đang dùng signed upload (có API Secret)
- Nếu gặp lỗi này → check lại code có đang gửi API Secret không

### Cần hỗ trợ thêm?

Gửi screenshot của:
1. Console log khi khởi động ứng dụng (phần load .env)
2. Phần cấu hình Cloudinary trong log (đã ẩn secret)
3. Error message chi tiết khi upload ảnh
4. Cloudinary Dashboard → Settings → Security (screenshot phần Allowed IPs)

## 📚 Tài liệu tham khảo

- Cloudinary Upload API: https://cloudinary.com/documentation/image_upload_api_reference
- Security Settings: https://cloudinary.com/documentation/security_considerations
- Java SDK: https://cloudinary.com/documentation/java_integration
