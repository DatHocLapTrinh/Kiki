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
        title = "Túi 100 Sao Vũ Trụ",
        periodLabel = "Nạp nhanh dùng ngay",
        priceText = "19.000 đ",
        originalPriceText = null,
        monthlyEquivalent = "Mua 1 lần",
        savingsBadge = "NẠP NHANH",
        isBestValue = false,
        bonusDescription = "Hồi đầy 5 Tim ngay lập tức hoặc mua vật phẩm tại Cửa hàng"
    );

    fun getTitle(isEnglish: Boolean): String = if (isEnglish) when (this) {
        ANNUAL -> "1-Year Cosmic Plan"
        MONTHLY -> "1-Month Flexible Plan"
        STAR_BUNDLE -> "100 Cosmic Stars Pack"
    } else title

    fun getPeriodLabel(isEnglish: Boolean): String = if (isEnglish) when (this) {
        ANNUAL -> "Billed annually"
        MONTHLY -> "Billed monthly"
        STAR_BUNDLE -> "Instant top-up"
    } else periodLabel

    fun getMonthlyEquivalent(isEnglish: Boolean): String = if (isEnglish) when (this) {
        ANNUAL -> "59,000 VND / mo"
        MONTHLY -> "99,000 VND / mo"
        STAR_BUNDLE -> "One-time"
    } else monthlyEquivalent

    fun getSavingsBadge(isEnglish: Boolean): String? = if (isEnglish) when (this) {
        ANNUAL -> "SAVE 45% • MOST POPULAR"
        MONTHLY -> null
        STAR_BUNDLE -> "QUICK PACK"
    } else savingsBadge

    fun getBonusDescription(isEnglish: Boolean): String = if (isEnglish) when (this) {
        ANNUAL -> "Includes 10 Shields + Gold Avatar + 7-day free trial"
        MONTHLY -> "Includes 2 Shields • Flexible renewal monthly"
        STAR_BUNDLE -> "Instant full refill (5 Hearts) or shop items"
    } else bonusDescription
}

data class CosmicPassBenefit(
    val iconEmoji: String,
    val title: String,
    val description: String,
    val freeLimitation: String,
    val vipAdvantage: String
) {
    fun getDisplayTitle(isEnglish: Boolean): String = if (isEnglish) when (title) {
        "Trái Tim Học Tập" -> "Learning Hearts"
        "Kiki AI Super Tutor 24/7" -> "Kiki AI Super Tutor 24/7"
        "Bảo Vệ Chuỗi Tự Động" -> "Auto Streak Protection"
        else -> title
    } else title

    fun getDisplayDescription(isEnglish: Boolean): String = if (isEnglish) when (title) {
        "Trái Tim Học Tập" -> "Practice speaking & reflexes without interruptions"
        "Kiki AI Super Tutor 24/7" -> "Free conversation & deep pronunciation analysis with Gemini AI"
        "Bảo Vệ Chuỗi Tự Động" -> "Cosmic shield automatically saves streak when you are busy"
        else -> description
    } else description

    fun getDisplayFreeLimitation(isEnglish: Boolean): String = if (isEnglish) when (title) {
        "Trái Tim Học Tập" -> "Fixed 5 Hearts (wait 4h to refill)"
        "Kiki AI Super Tutor 24/7" -> "Limited to 3-5 questions/day"
        "Bảo Vệ Chuỗi Tự Động" -> "Requires manual shields"
        else -> freeLimitation
    } else freeLimitation

    fun getDisplayVipAdvantage(isEnglish: Boolean): String = if (isEnglish) when (title) {
        "Trái Tim Học Tập" -> "UNLIMITED HEARTS (Learn freely)"
        "Kiki AI Super Tutor 24/7" -> "UNLIMITED QUESTIONS"
        "Bảo Vệ Chuỗi Tự Động" -> "AUTO STREAK PROTECTION"
        else -> vipAdvantage
    } else vipAdvantage
}

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
