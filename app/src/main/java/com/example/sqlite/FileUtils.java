package com.example.sqlite;

import android.content.Context;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * FileUtils hỗ trợ lưu trữ dữ liệu dự phòng dưới dạng file truyền thống.
 */
public class FileUtils {

    /**
     * Lưu dữ liệu vào internal storage
     */
    public static void saveDataToFile(Context context, String filename, String content) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc dữ liệu từ internal storage
     */
    public static String readDataFromFile(Context context, String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
