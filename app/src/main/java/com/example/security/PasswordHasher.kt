package com.example.security

import java.security.MessageDigest

/**
 * Hỗ trợ băm và kiểm tra mật khẩu sử dụng thuật toán mật mã học SHA-256 kèm Salt bí mật.
 * Giúp ngăn chặn tấn công Rainbow Table và bảo vệ an toàn dữ liệu người dùng trong Room Database.
 */
object PasswordHasher {
    private const val SALT = "kiki_hihi_secure_study_salt_v1_#@99!"

    /**
     * Băm mật khẩu người dùng với chuỗi salt cố định.
     */
    fun hash(password: String): String {
        val input = "$SALT$password$SALT"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Xác thực mật khẩu nhập vào với mã hash đã lưu.
     */
    fun verify(password: String, storedHash: String): Boolean {
        return hash(password).equals(storedHash, ignoreCase = true)
    }
}
