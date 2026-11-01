package com.example.model

/**
 * Direct URLs tới các bộ animation GIF trong suốt (Transparent 0 Alpha) của linh vật Kiki.
 * Lưu trữ vĩnh viễn trên GitHub Release CDN tốc độ cao.
 */
object KikiAnimationUrls {
    /**
     * 1. Chào khi vào app & Chào khi người dùng click vào Kiki
     */
    const val GREETING = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/chao_khi_vao_app.gif"

    /**
     * 2. Đọc đề khi người học ở trong câu hỏi quá lâu (>5s)
     */
    const val READING_QUESTION = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/doc_de.gif"

    /**
     * 3. Vỗ tay thường khi trả lời đúng 1 câu
     */
    const val CLAP_NORMAL = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/vay_tay_thuong.gif"

    /**
     * 4. Vỗ tay liên tục ăn mừng khi trả lời đúng 5 câu liên tiếp
     */
    const val CLAP_STREAK = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/vo_tay_khi_dung_lien_tiep.gif"

    /**
     * 5. Buồn khi trả lời sai câu hỏi
     */
    const val SAD = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/buon.gif"

    /**
     * Ánh xạ trạng thái cảm xúc MascotEmotion sang Animation URL tương ứng.
     * Lưu ý: MascotEmotion.IDLE và PETTED trong lúc học trả về null để giữ ảnh tĩnh mặc định.
     * Chỉ ở tab khởi động (IntroScreen) hoặc tương tác chào trực tiếp mới phát hoạt ảnh GREETING.
     */
    fun getAnimationUrl(emotion: MascotEmotion): String? {
        return when (emotion) {
            MascotEmotion.GREETING -> GREETING
            MascotEmotion.THINKING -> READING_QUESTION
            MascotEmotion.CHEER -> CLAP_NORMAL
            MascotEmotion.STREAK -> CLAP_STREAK
            MascotEmotion.EMPATHY -> SAD
            MascotEmotion.PETTED -> null
            MascotEmotion.IDLE -> null
            MascotEmotion.LISTENING -> null
        }
    }
}
