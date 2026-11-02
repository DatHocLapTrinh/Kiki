package com.example.model

/**
 * Danh sách liên kết máy chủ lưu trữ trực tuyến toàn bộ 5 hoạt ảnh GIF của linh vật Kiki AI.
 * Đã chuyển sang CDN GitHub Releases v3.0 tải cực nhanh, dung lượng nén siêu nhẹ (~1MB),
 * ổn định tuyệt đối và loại bỏ hoàn toàn hiện tượng văng lag hay bộ nhớ quá tải.
 */
object KikiAnimationUrls {
    /**
     * 1. Chào khi vào app và khi click vào nhân vật Kiki
     */
    const val GREETING = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/chao.gif"

    /**
     * 2. Đọc đề bài câu hỏi
     */
    const val READING_QUESTION = "https://github.com/dungpham280306nd-dot/BPS_asm1/releases/download/v3.0/doc_de.gif"

    /**
     * 3. Vỗ tay nhẹ khi trả lời đúng 1 câu
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
     */
    fun getAnimationUrl(emotion: MascotEmotion): String? {
        return when (emotion) {
            MascotEmotion.GREETING -> GREETING
            MascotEmotion.THINKING -> READING_QUESTION
            MascotEmotion.CHEER -> CLAP_NORMAL
            MascotEmotion.STREAK -> CLAP_STREAK
            MascotEmotion.EMPATHY -> SAD
            MascotEmotion.SPEAKING -> READING_QUESTION
            MascotEmotion.PETTED -> null
            MascotEmotion.IDLE -> null
            MascotEmotion.LISTENING -> null
        }
    }
}
