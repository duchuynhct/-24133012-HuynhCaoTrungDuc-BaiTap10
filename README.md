# Bài tập 10: Demo JWT với Spring Boot 3 & Spring Security 6 - Thay thế bằng Nimbus JOSE+JWT

**Sinh viên**: Huỳnh Cao Trung Đức  
**MSSV**: 24133012  
**Môn học**: Lập trình Web (WEBPR330479)  
**Giảng viên**: ThS. Nguyễn Hữu Trung  

---

## 1. Nội dung thực hiện

### Phần 1: Xây dựng ứng dụng JWT theo slide bài giảng
- Cấu hình dự án **Spring Boot 3.3.4** và **Spring Security 6**.
- Sử dụng thư viện **JJWT** (`io.jsonwebtoken` version `0.12.6`).
- Xây dựng đầy đủ các tầng theo 10 bước trong slide:
  - **Entity**: [`User`](src/main/java/vn/iotstar/entity/User.java) implements `UserDetails`.
  - **Models / DTOs**: [`LoginResponse`](src/main/java/vn/iotstar/models/LoginResponse.java), [`LoginUserModel`](src/main/java/vn/iotstar/models/LoginUserModel.java), [`RegisterUserModel`](src/main/java/vn/iotstar/models/RegisterUserModel.java).
  - **Repository & Services**: [`UserRepository`](src/main/java/vn/iotstar/repository/UserRepository.java), [`UserService`](src/main/java/vn/iotstar/services/UserService.java), [`AuthenticationService`](src/main/java/vn/iotstar/services/AuthenticationService.java), [`JwtService`](src/main/java/vn/iotstar/services/JwtService.java).
  - **Bảo mật**: [`ApplicationConfiguration`](src/main/java/vn/iotstar/configs/ApplicationConfiguration.java), [`SecurityConfiguration`](src/main/java/vn/iotstar/configs/SecurityConfiguration.java), [`JwtAuthenticationFilter`](src/main/java/vn/iotstar/filter/JwtAuthenticationFilter.java).
  - **Controllers**: [`AuthenticationController`](src/main/java/vn/iotstar/controllers/AuthenticationController.java) (`/auth/signup`, `/auth/login`), [`UserController`](src/main/java/vn/iotstar/controllers/UserController.java) (`/users/me`, `/users`).
  - **Xử lý ngoại lệ**: [`GlobalExceptionHandler`](src/main/java/vn/iotstar/configs/GlobalExceptionHandler.java) (`@RestControllerAdvice`, trả về chuẩn `ProblemDetail`).
  - **Giao diện Ajax**: [`login.html`](src/main/resources/templates/login.html), [`profile.html`](src/main/resources/templates/profile.html), [`mainjs.js`](src/main/resources/static/js/mainjs.js), [`AuthController`](src/main/java/vn/iotstar/controllers/AuthController.java) (`/login`, `/user/profile`).

### Phần 2: Thay thế JWT bằng thư viện Nimbus JOSE + JWT
- Bổ sung dependency `com.nimbusds:nimbus-jose-jwt:9.37.3` vào [`pom.xml`](pom.xml).
- Tái cấu trúc [`JwtService`](src/main/java/vn/iotstar/services/JwtService.java):
  - Ký token với `SignedJWT`, `JWSHeader(JWSAlgorithm.HS256)` và `MACSigner`.
  - Thiết lập claims bằng `JWTClaimsSet.Builder` (`subject`, `issueTime`, `expirationTime`, custom claims).
  - Xác thực chữ ký token bằng `SignedJWT.parse(token)` và `MACVerifier`.
  - Kiểm tra tính hợp lệ và thời gian hết hạn của token.
- Cập nhật [`GlobalExceptionHandler`](src/main/java/vn/iotstar/configs/GlobalExceptionHandler.java) để bắt và xử lý ngoại lệ Nimbus tương ứng.

---

## 2. Hướng dẫn chạy ứng dụng

### Yêu cầu hệ thống:
- Java JDK 17+ (đã kiểm thử tương thích JDK 26)
- Maven 3.9+
- MySQL 8.0 (Database: `jwt_springboot3`, User: `root`, Mật khẩu cấu hình trong `application.properties`)

### Khởi chạy:
```bash
# Biên dịch và đóng gói
mvn clean package -DskipTests

# Chạy ứng dụng
java -jar target/JWT_springboot3-0.0.1-SNAPSHOT.jar
```
Ứng dụng sẽ chạy tại: `http://localhost:8005`

---

## 3. Kiểm thử API & Web

### 1. Đăng ký tài khoản mới:
```bash
curl -X POST http://localhost:8005/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "trungnh@hcmute.edu.vn", "password": "123456", "fullName": "Nguyễn Hữu Trung"}'
```

### 2. Đăng nhập để nhận Token:
```bash
curl -X POST http://localhost:8005/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "trungnh@hcmute.edu.vn", "password": "123456"}'
```
Kết quả trả về:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3OTA3MzY2OTYsInN1YiI6InRydW5nbmhAaGNtdXRlLmVkdS52biIsImlhdCI6MTc5MDczMzA5Nn0.72zrLEZY3GK7a2X_2zQYSzzCxC2mw8ghGomp80qCHSY",
  "expiresIn": 3600000
}
```

### 3. Truy cập API được bảo vệ với Bearer Token:
```bash
curl -X GET http://localhost:8005/users/me \
  -H "Authorization: Bearer <TOKEN>"

curl -X GET http://localhost:8005/users \
  -H "Authorization: Bearer <TOKEN>"
```

### 4. Truy cập giao diện Web:
- **Đăng nhập**: Mở trình duyệt truy cập `http://localhost:8005/login`
- **Trang thông tin**: Sau khi đăng nhập thành công, giao diện tự động lưu token vào `localStorage` và chuyển sang `http://localhost:8005/user/profile`.
