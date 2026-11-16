package com.littlehorizons.erp.model;

/** A subject of one grade (table "materias"). */
public class Subject {

    private int subjectId;
    private String code;
    private String name;
    private String description;
    private int gradeId;
    private String gradeName;
    private int weeklyHours;
    private boolean active = true;

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }
    public String getGradeName() { return gradeName; }
    public void setGradeName(String gradeName) { this.gradeName = gradeName; }
    public int getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}