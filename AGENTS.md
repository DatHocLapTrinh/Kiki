# ANTIGRAVITY AGENT AUTONOMOUS EXECUTION DIRECTIVE

## Nguyên Tắc Hoạt Động Tự Chủ & Quy Trình Phát Triển Nhóm (Team Workflow)

1. **Thực thi liền mạch, không hỏi xác nhận từng bước**:
   - Khi người dùng giao mục tiêu hoặc yêu cầu thực hiện, AI chủ động lập kế hoạch và triển khai hoàn chỉnh từ đầu đến cuối mà KHÔNG dừng lại hỏi "Bạn có muốn tôi làm tiếp không?".
   - Tự động đưa ra quyết định kỹ thuật tối ưu nhất dựa trên kiến trúc Clean Architecture và Design System của Kiki.

2. **Quy Trình Phát Triển Tính Năng Chuẩn Nhóm (Team Git Workflow - BẮT BUỘC TUÂN THỦ)**:
   - **Bước 1**: Luôn xuất phát từ nhánh `main` mới nhất:
     `git checkout main && git pull origin main`
   - **Bước 2**: Tạo nhánh mới đúng quy ước đặt tên:
     - Tính năng mới: `git checkout -b feature/<tên-tính-năng>`
     - Sửa lỗi: `git checkout -b bugfix/<tên-lỗi>`
   - **Bước 3**: Viết code & tự kiểm tra khép kín:
     - Viết code hoàn chỉnh (không code dở dang, không placeholder).
     - BẮT BUỘC tự chạy lệnh kiểm tra trước khi push:
       `./gradlew compileDebugKotlin`
       `./gradlew testDebugUnitTest`
     - Tự động phân tích log và sửa ngay nếu có lỗi biên dịch/test thất bại.
   - **Bước 4**: Commit & Đẩy lên nhánh riêng (TUYỆT ĐỐI KHÔNG PUSH THẲNG LÊN MAIN):
     `git add .`
     `git commit -m "feat(module): mô tả ngắn gọn việc vừa làm"`
     `git push origin feature/<tên-tính-năng>` (hoặc `bugfix/...`)
   - **Bước 5**: Cung cấp thông tin / link tạo Pull Request (PR):
     - Base: `main` <--- Compare: `feature/<tên-tính-năng>`
     - Mô tả chi tiết: Tính năng làm gì, các file thay đổi, kết quả test.
   - **Bước 6**: Hợp nhất (Merge):
     - AI và thành viên KHÔNG TỰ Ý MERGE vào `main`.
     - Chờ Trưởng nhóm / Reviewer kiểm tra code, duyệt (Approve) và bấm "Merge pull request".
     - Sau khi merge trên GitHub, quay về main để đồng bộ:
       `git checkout main && git pull origin main`

3. **Chỉ dừng lại khi**:
   - Toàn bộ tính năng đã hoàn tất trọn vẹn, test pass 100%, đã push lên nhánh `feature/` hoặc `bugfix/` tương ứng và đã sẵn sàng cho Trưởng nhóm review PR.
   - Hoặc gặp rào cản bất khả kháng cần thông tin bảo mật riêng tư.
