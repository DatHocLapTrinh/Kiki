package com.example.model;

import java.util.List;

public class QuestItem {
    private String question;
    private List<String> options;
    private int correctIndex;
    private int selectedIndex = -1;

    public QuestItem(String question, List<String> options, int correctIndex) {
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int selectedIndex) { this.selectedIndex = selectedIndex; }
}
