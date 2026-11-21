package com.kiki.backend.dto;

/**
 * Phản hồi đàm thoại & đánh giá thông minh từ Kiki Backend
 */
public record CosmicCallResponse(
    int score,                  // Điểm số 60 - 100
    String spokenFeedbackEn,    // Câu Kiki cất giọng đọc bằng tiếng Anh (ngắn gọn 10-25 từ)
    String feedbackVi,          // Nhận xét sư phạm & lời động viên bằng tiếng Việt hiển thị trên UI
    String grammarCorrection,   // Câu chuẩn ngữ pháp gợi ý để người học hoàn thiện
    String nextQuestionEn,      // Câu hỏi tiếp theo bằng tiếng Anh đúng ngữ cảnh hội thoại
    String nextHintVi,          // Gợi ý tiếng Việt cho câu hỏi tiếp theo
    String suggestedStarterEn   // Mẫu câu gợi ý mở đầu (Starter) đúng theo ngữ cảnh câu hỏi tiếp theo
) {}

