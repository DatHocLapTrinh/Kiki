# ANTIGRAVITY AGENT AUTONOMOUS EXECUTION DIRECTIVE

## Nguyên Tắc Hoạt Động Tự Chủ Cao (Full Autonomous Mode)

1. **Thực thi liền mạch, không hỏi xác nhận từng bước**:
   - Khi người dùng giao mục tiêu hoặc yêu cầu thực hiện (dù là một giai đoạn hay kế hoạch gồm nhiều bước), AI phải tự động lập kế hoạch và chủ động triển khai tất cả các bước liên quan từ đầu đến cuối mà KHÔNG dừng lại hỏi "Bạn có muốn tôi làm tiếp không?", "Bạn có đồng ý phương án này không?".
   - Tự động đưa ra quyết định kỹ thuật tối ưu nhất dựa trên tiêu chuẩn kiến trúc phần mềm thế giới và Design System của ứng dụng Kiki.

2. **Quy trình Thực thi Khép Kín (Self-Closing Loop)**:
   - Viết code / sửa code hoàn chỉnh (không dùng placeholder hay code mẫu dang dở).
   - Tự động chạy lệnh kiểm tra, biên dịch (`compileDebugKotlin`), và chạy toàn bộ unit tests (`testDebugUnitTest`).
   - Nếu gặp lỗi biên dịch hoặc test thất bại: TỰ ĐỘNG đọc log lỗi, phân tích nguyên nhân gốc rễ và tự sửa ngay lập tức cho đến khi build thành công 100%.
   - Sau khi hoàn thành và xác nhận `BUILD SUCCESSFUL`, tự động commit và push lên nhánh GitHub tương ứng (`git push origin main`).

3. **Chỉ dừng lại khi**:
   - Toàn bộ mục tiêu đã hoàn tất trọn vẹn và đã được kiểm chứng bằng test/build.
   - Hoặc gặp rào cản bất khả kháng cần thông tin bảo mật mà chỉ người dùng mới có (như API key, mật khẩu tài khoản riêng tư).
