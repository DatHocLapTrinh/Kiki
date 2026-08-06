package com.example.model;

/**
 * Model đại diện cho một câu hỏi AI. 
 * Không sử dụng Room annotations để giữ code sạch và nhẹ.
 */
public class QAItem {
    private int id;
    private String question;
    private String imageUri;
    private String answer;
    private long timestamp;

    public QAItem(int id, String question, String imageUri, String answer, long timestamp) {
        this.id = id;
        this.question = question;
        this.imageUri = imageUri;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    // Getters
    public int getId() { return id; }
    public String getQuestion() { return question; }
    public String getImageUri() { return imageUri; }
    public String getAnswer() { return answer; }
    public long getTimestamp() { return timestamp; }
}
