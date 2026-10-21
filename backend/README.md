# 🚀 Kiki AI Backend Engine (Java / Spring Boot)

Hệ thống Backend chuyên biệt phục vụ các tính năng Trí tuệ nhân tạo (AI) và Thông báo đẩy (Push Notification) cho ứng dụng di động Kiki.

---

## 🛠️ Công nghệ sử dụng
- **Java 21 LTS**
- **Spring Boot 3.4.3** (Web, Security, Validation, Scheduling)
- **Firebase Admin SDK** (Xác thực JWT token từ Google/Firebase & gửi thông báo FCM)
- **Groq API / Llama 3.3 70B** (Gọi AI tốc độ 300 token/s trên server, bảo mật 100% API Key)
- **Gradle Wrapper**

---

## 📋 Danh sách API đã sẵn sàng

| Phương thức | Đường dẫn (Endpoint) | Quyền | Chức năng |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/health` | Public | Kiểm tra trạng thái máy chủ (Healthcheck) |
| `POST` | `/api/v1/ai/generate-chapter` | JWT (Bearer) | Tự động sinh nội dung chương học + câu hỏi trắc nghiệm/ghép câu/phát âm theo chuẩn JSON |
| `POST` | `/api/v1/ai/mentor-chat` | JWT (Bearer) | Hỏi đáp cùng Gia sư AI Mentor (giải thích ngữ pháp, bóc tách từ vựng IPA) |
| `POST` | `/api/v1/notifications/send-test` | JWT (Bearer) | Bắn thử thông báo đẩy đến FCM Token của thiết bị |
| *(Cron Job)* | Tự động chạy mỗi 20:00 tối | Server | Quét học viên chưa học trong ngày và bắn thông báo giữ Streak |

---

## 🚀 Hướng dẫn chạy thử nghiệm tại Local

### 1. Khởi chạy máy chủ
Di chuyển vào thư mục `backend` và chạy lệnh:
```bash
./gradlew bootRun
```
Máy chủ sẽ khởi động tại địa chỉ: `http://localhost:8080`

### 2. Kiểm tra nhanh (Health Check)
Mở trình duyệt hoặc terminal chạy:
```bash
curl http://localhost:8080/api/v1/health
```
Kết quả trả về:
```json
{
  "service": "kiki-ai-backend",
  "status": "UP",
  "version": "1.0.0",
  "message": "Kiki AI Backend Engine đang hoạt động ổn định!"
}
```

### 3. Cấu hình biến môi trường (Tùy chọn)
Tạo file `.env` hoặc truyền biến khi chạy:
```bash
export GROQ_API_KEY="gsk_your_groq_api_key_here"
export FIREBASE_CREDENTIALS_PATH="path/to/firebase-service-account.json"
./gradlew bootRun
```
*(Nếu chưa có Groq API Key hoặc Firebase Credentials, hệ thống tự động bật chế độ Mock Dev Fallback để không làm gián đoạn việc code/test).*
