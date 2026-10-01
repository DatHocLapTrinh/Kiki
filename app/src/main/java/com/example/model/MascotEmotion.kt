package com.example.model

/**
 * Các trạng thái cảm xúc của linh vật Kiki
 */
enum class MascotEmotion {
    IDLE,        // Thảnh thơi, thở nhẹ, chớp mắt tự nhiên
    THINKING,    // Suy nghĩ lâu (>15s), nghiêng đầu, bóng đèn gợi ý
    CHEER,       // Trả lời đúng, nhảy cẫng, sao vàng lấp lánh
    STREAK,      // Combo đúng >= 3 câu, bùng cháy hào quang lửa ngầu lòi
    EMPATHY,     // Trả lời sai, tai cụp nhẹ, an ủi động viên người học
    LISTENING,   // Đang trong thử thách luyện nói hoặc thu âm micro
    PETTED       // Người học chạm/xoa đầu Kiki, cười khúc khích, tim bay
}

/**
 * Dữ liệu thoại động viên theo ngữ cảnh cảm xúc của Kiki
 */
object MascotQuotes {
    fun getQuoteForEmotion(emotion: MascotEmotion, isEnglish: Boolean, streak: Int = 0): String {
        return when (emotion) {
            MascotEmotion.IDLE -> if (isEnglish) {
                listOf(
                    "Take your time, you've got this! ✨",
                    "Ready whenever you are! 🚀",
                    "Learning English is a superpower! 🌟"
                ).random()
            } else {
                listOf(
                    "Cứ bình tĩnh suy nghĩ nhé, bạn làm được mà! ✨",
                    "Kiki luôn sẵn sàng đồng hành cùng bạn! 🚀",
                    "Mỗi ngày một chút, tiếng Anh sẽ lên như diều gặp gió! 🌟"
                ).random()
            }

            MascotEmotion.THINKING -> if (isEnglish) {
                listOf(
                    "Need a hint? Read the keywords carefully! 💡",
                    "Hmm, take a deep breath and trust your instincts! 🧠",
                    "Tricky question? You can conquer it! 🔍"
                ).random()
            } else {
                listOf(
                    "Cần Kiki gợi ý không? Hãy đọc kỹ từ khóa nhé! 💡",
                    "Hmm, hít một hơi thật sâu và tin vào trực giác của bạn! 🧠",
                    "Câu này hơi lắt léo chút thôi, bạn giải được mà! 🔍"
                ).random()
            }

            MascotEmotion.CHEER -> if (isEnglish) {
                listOf(
                    "Bingo! Spot on! 🎉",
                    "Awesome job! Your English is shining! ⭐",
                    "You nailed it! Keep the momentum! 🚀"
                ).random()
            } else {
                listOf(
                    "Chính xác tuyệt đối! Giỏi quá đi! 🎉",
                    "Tuyệt cú mèo! Phản xạ của bạn quá đỉnh! ⭐",
                    "Chuẩn không cần chỉnh! Tiến lên nào! 🚀"
                ).random()
            }

            MascotEmotion.STREAK -> if (isEnglish) {
                listOf(
                    "UNSTOPPABLE! $streak in a row! 🔥",
                    "You're on absolute FIRE with $streak streak! Keep blazing! ⚡",
                    "Combo x$streak! Native speaker mode unlocked! 👑"
                ).random()
            } else {
                listOf(
                    "KHÔNG THỂ CẢN PHÁ! Chuỗi $streak câu liên tiếp! 🔥",
                    "Bạn đang bùng cháy rực rỡ với chuỗi $streak câu! Tiến lên nào! ⚡",
                    "Combo x$streak! Đẳng cấp đỉnh cao xuất hiện! 👑"
                ).random()
            }

            MascotEmotion.EMPATHY -> if (isEnglish) {
                listOf(
                    "Mistakes are proof you're trying! Keep going! 💖",
                    "Don't worry, even native speakers stumble! You'll get the next one! 💪",
                    "Every error is a lesson in disguise! Kiki believes in you! 🌱"
                ).random()
            } else {
                listOf(
                    "Không sao đâu bạn ơi! Sai là bước đệm để nhớ lâu hơn! 💖",
                    "Đừng lo, câu sau mình làm lại ngon lành ngay thôi! 💪",
                    "Kiki luôn ở đây cổ vũ bạn, tiếp tục chinh phục nào! 🌱"
                ).random()
            }

            MascotEmotion.LISTENING -> if (isEnglish) {
                listOf(
                    "Kiki is listening carefully! Speak clearly into the mic! 🎙️",
                    "I hear your voice! Let your English flow naturally! 🎧",
                    "Speak with confidence, you sound great! 🔊"
                ).random()
            } else {
                listOf(
                    "Kiki đang lắng nghe rất kỹ đây! Hãy phát âm to rõ nhé! 🎙️",
                    "Hãy tự tin nói tự nhiên như người bản xứ nhé! 🎧",
                    "Cố lên, âm điệu tiếng Anh của bạn rất hay đấy! 🔊"
                ).random()
            }

            MascotEmotion.PETTED -> if (isEnglish) {
                listOf(
                    "Hehe, that tickles! Thank you for the headpat! 🥰",
                    "Yay! Kiki is energized and ready to cheer you on! 💖",
                    "High five! Let's conquer this quest together! 🐾"
                ).random()
            } else {
                listOf(
                    "Hihi, nhột quá đi! Cảm ơn bạn đã xoa đầu Kiki nhé! 🥰",
                    "Yay! Kiki được tiếp thêm 100% năng lượng rồi nè! 💖",
                    "Đập tay nào! Chúng mình cùng về đích xuất sắc nhé! 🐾"
                ).random()
            }
        }
    }
}
