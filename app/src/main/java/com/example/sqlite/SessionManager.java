package com.example.sqlite;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager quản lý phiên đăng nhập của người dùng.
 * Sử dụng SharedPreferences để lưu trữ thông tin session.
 */
public class SessionManager {

    private static final String PREF_NAME = "StudyMentorSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context _context;

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Tạo phiên đăng nhập
     */
    public void createLoginSession(String username) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.commit();
    }

    /**
     * Kiểm tra trạng thái đăng nhập
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Lấy tên người dùng đang đăng nhập
     */
    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    /**
     * Xóa phiên đăng nhập (Logout)
     */
    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
