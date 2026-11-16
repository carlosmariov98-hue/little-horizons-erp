package com.littlehorizons.erp.model;

/** A school grade, from Preescolar to Once (table "grados"). */
public class Grade {

    private int gradeId;
    private String name;
    private int level;

    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}