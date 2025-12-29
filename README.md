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

**Local:**
```
http://localhost:8080/swagger-ui/index.html
```

**Production (EC2):**
```
http://<your-ec2-ip>:8088/swagger-ui.html
```

**Production (Render):**
```
https://your-service.onrender.com/swagger-ui/index.html
```

## 🚀 Deploy lên Render (Khuyến nghị)

Render là một nền tảng cloud deployment đơn giản và miễn phí cho developers. Backend này đã được cấu hình sẵn để deploy với **Docker**.

### 📖 Tài liệu Deployment
- **Docker Guide**: [DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md) - Deploy với Docker
- **Quick Start (5 phút)**: [RENDER_QUICK_START.md](./RENDER_QUICK_START.md)
- **Hướng dẫn đầy đủ**: [RENDER_DEPLOYMENT.md](./RENDER_DEPLOYMENT.md)
- **Checklist chi tiết**: [RENDER_DEPLOYMENT_CHECKLIST.md](./RENDER_DEPLOYMENT_CHECKLIST.md)

### ⚡ Các bước cơ bản
1. Push code lên GitHub/GitLab
2. Tạo Web Service trên [Render](https://render.com)
3. Chọn runtime: **Docker** (tự động detect)
4. Set environment variables
5. Deploy! 🎉

### 🐳 Local Development với Docker
```bash
# Chạy tất cả services (Backend + MongoDB + Mongo Express)
docker-compose up -d

# Xem logs
docker-compose logs -f

# Stop
docker-compose down
```

### 💰 Chi phí
- **Free tier**: $0/tháng (development/testing)
- **Starter**: $7/tháng (production với 24/7 uptime)
- **Standard**: $25/tháng (production với performance cao hơn)

### 🔗 Files deployment
- `Dockerfile` - Multi-stage Docker build
- `.dockerignore` - Optimize Docker build
- `docker-compose.yml` - Local development
- `render.yaml` - Infrastructure as Code
- `.env.example` - Template cho environment variables

Xem chi tiết trong [DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md) và [RENDER_DEPLOYMENT.md](./RENDER_DEPLOYMENT.md)
