# Cellex API Service
Dự án backend cho ứng dụng Cellex, được xây dựng bằng Spring Boot. Cung cấp các API để quản lý người dùng, xác thực và các chức năng cốt lõi khác.

## 🚀 Tính năng chính
- Quản lý người dùng (Đăng ký, Đăng nhập)
- Xác thực và phân quyền bằng JWT (JSON Web Token)
- Tài liệu API tự động với Swagger/OpenAPI
- Tích hợp với MongoDB Atlas
- Sử dụng Amazon S3 để lưu trữ file

## 🛠️ Công nghệ sử dụng
- Java 21
- Spring Boot 3.x
- Spring Security 6 (JWT Authentication)
- Spring Data MongoDB
- Maven
- Lombok
- Swagger/OpenAPI 3
- MongoDB Atlas (Cloud Database)
- Amazon S3 (Object Storage)
- Amazon EC2 (Deployment Server)

## ⚙️ Cài đặt môi trường Local

### Yêu cầu
- JDK 21
- Maven 3.8+
- MongoDB Server (chạy local)

### Các bước cài đặt

**Clone repository:**

```bash
git clone https://github.com/your-username/cellex.git
cd cellex
```

**Tạo file .env:**  
Tạo một file có tên `.env` ở thư mục gốc và cấu hình các biến môi trường cần thiết. Bạn có thể sao chép từ file `.env.example`.

```bash
SERVER_PORT=8088
MONGO_URI=mongodb://localhost:27017/cellex_db_dev

# JWT Configuration
application.security.jwt.secret-key=Your-Local-Base64-Secret-Key
application.security.jwt.access-token-expiration=3600000
application.security.jwt.refresh-token-expiration=604800000
```

**Build dự án:**

```bash
mvn clean install
```

**Chạy ứng dụng:**

```bash
java -jar target/cellex-0.0.1-SNAPSHOT.jar
```

Ứng dụng sẽ chạy tại [http://localhost:8088](http://localhost:8088).

## 📦 Triển khai (Deployment) lên AWS EC2

### Yêu cầu
- Một tài khoản AWS
- Một EC2 instance (Amazon Linux 2023) đã được cài đặt Java 21.
- Một S3 bucket đã được tạo.
- Một cluster MongoDB Atlas và chuỗi kết nối.
- Một file key pair (.pem) để truy cập EC2.

### Các bước triển khai

**Tạo file .env.production:**  
Tạo một file `.env.production` trên máy local của bạn với cấu hình cho môi trường production.

```bash
SERVER_PORT=8088
MONGO_URI=mongodb+srv://<user>:<password>@<your-atlas-cluster>
AWS_ACCESS_KEY_ID=YourAccessKey
AWS_SECRET_ACCESS_KEY=YourSecretKey
S3_BUCKET_NAME=your-s3-bucket-name
S3_REGION=your-s3-bucket-region

# Production JWT Configuration
application.security.jwt.secret-key=Your-Production-Base64-Secret-Key
application.security.jwt.access-token-expiration=3600000
application.security.jwt.refresh-token-expiration=604800000
```

**Build dự án:**

```bash
mvn clean package -DskipTests
```

**Dọn dẹp server:**  
Kết nối vào EC2 và dừng các tiến trình cũ.

```bash
ssh -i "your-key.pem" ec2-user@your-ec2-ip
pkill -f java
rm app.jar app.log
```

**Tải file lên server:**  
Mở một terminal mới trên máy của bạn và dùng `scp`.

```bash
# Tải file JAR
scp -i "your-key.pem" target/cellex-0.0.1-SNAPSHOT.jar ec2-user@your-ec2-ip:~/app.jar

# Tải file môi trường
scp -i "your-key.pem" .env.production ec2-user@your-ec2-ip:~/.env
```

**Khởi động ứng dụng:**  
Quay lại terminal SSH và chạy ứng dụng.

```bash
nohup java -jar app.jar > app.log 2>&1 &
```

**Kiểm tra log:**

```bash
tail -f app.log
```

## 📚 Tài liệu API
Sau khi ứng dụng đã khởi động thành công, tài liệu API sẽ có sẵn tại Swagger UI:

```
http://<your-ec2-ip>:8088/swagger-ui.html
```
