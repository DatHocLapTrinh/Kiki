package com.example.model

enum class CosmicPassTier(
    val id: String,
    val title: String,
    val periodLabel: String,
    val priceText: String,
    val originalPriceText: String?,
    val monthlyEquivalent: String,
    val savingsBadge: String?,
    val isBestValue: Boolean,
    val bonusDescription: String
) {
    ANNUAL(
        id = "cosmic_pass_annual",
        title = "Gói 1 Năm Vũ Trụ",
        periodLabel = "Thanh toán theo năm",
        priceText = "708.000 đ / năm",
        originalPriceText = "1.188.000 đ",
        monthlyEquivalent = "59.000 đ / tháng",
        savingsBadge = "TIẾT KIỆM 45% • PHỔ BIẾN NHẤT",
        isBestValue = true,
        bonusDescription = "Tặng 10 Khiên Chuỗi + Avatar Vàng + Dùng thử 7 ngày miễn phí"
    ),
    MONTHLY(
        id = "cosmic_pass_monthly",
        title = "Gói 1 Tháng Linh Hoạt",
        periodLabel = "Thanh toán từng tháng",
        priceText = "99.000 đ / tháng",
        originalPriceText = null,
        monthlyEquivalent = "99.000 đ / tháng",
        savingsBadge = null,
        isBestValue = false,
        bonusDescription = "Tặng 2 Khiên Chuỗi • Gia hạn linh hoạt mỗi tháng"
    ),
    STAR_BUNDLE(
        id = "cosmic_stars_100",
        title = "Túi 100 Đá Quý Vũ Trụ",
        periodLabel = "Nạp nhanh dùng ngay",
        priceText = "19.000 đ",
        originalPriceText = null,
        monthlyEquivalent = "Mua 1 lần",
        savingsBadge = "NẠP NHANH",
        isBestValue = false,
        bonusDescription = "Hồi đầy 5 Tim ngay lập tức hoặc mua vật phẩm tại Cửa hàng"
    )
}

data class CosmicPassBenefit(
    val iconEmoji: String,
    val title: String,
    val description: String,
    val freeLimitation: String,
    val vipAdvantage: String
)

object CosmicPassConfig {
    val benefits = listOf(
        CosmicPassBenefit(
            iconEmoji = "❤️",
            title = "Trái Tim Học Tập",
            description = "Thử thách phát âm và phản xạ không sợ bị gián đoạn",
            freeLimitation = "Cố định 5 Tim (hết phải chờ 4h)",
            vipAdvantage = "VÔ HẠN TIM (Học thả ga)"
        ),
        CosmicPassBenefit(
            iconEmoji = "🤖",
            title = "Kiki AI Super Tutor 24/7",
            description = "Đàm thoại tự do và phân tích phát âm từng âm tiết bằng Gemini AI",
            freeLimitation = "Giới hạn 3-5 câu hỏi/ngày",
            vipAdvantage = "KHÔNG GIỚI HẠN LƯỢT HỎI"
        ),
        CosmicPassBenefit(
            iconEmoji = "🛡️",
            title = "Bảo Vệ Chuỗi Tự Động",
            description = "Khiên vũ trụ tự động cứu chuỗi ngày học dù bạn bận rộn hay đi du lịch",
            freeLimitation = "Cần nạp khiên thủ công",
            vipAdvantage = "TỰ ĐỘNG BẢO VỆ STREAK"
        ),
        CosmicPassBenefit(
            iconEmoji = "⚡",
            title = "Năng Lượng Hành Tinh & EXP",
            description = "Duy trì năng lượng hành tinh 100% không suy giảm và nhân đôi điểm kinh nghiệm",
            freeLimitation = "Giảm xuống 70% sau 3 ngày",
            vipAdvantage = "LUÔN ĐẦY 100% + NHÂN ĐÔI EXP"
        ),
        CosmicPassBenefit(
            iconEmoji = "👑",
            title = "Huy Hiệu & Avatar Hoàng Gia",
            description = "Biểu tượng Phi hành gia Vàng lấp lánh trên Bảng Xếp Hạng Thiên Hà",
            freeLimitation = "Huy hiệu thông thường",
            vipAdvantage = "AVATAR VÀNG ĐỘC QUYỀN"
        )
    )
}
