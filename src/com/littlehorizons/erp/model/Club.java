package com.littlehorizons.erp.model;

/** An extracurricular club (table "clubes"). */
public class Club {

    private int clubId;
    private String name;
    private String description;
    private int supervisorId;
    private String supervisorName;
    private int maxCapacity;
    private String schedule;
    private boolean active = true;
    private int activeMembers;

    public int getClubId() { return clubId; }
    public void setClubId(int clubId) { this.clubId = clubId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    /** Id of the supervising teacher, or 0 when the club has none. */
    public int getSupervisorId() { return supervisorId; }
    public void setSupervisorId(int supervisorId) { this.supervisorId = supervisorId; }
    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getActiveMembers() { return activeMembers; }
    public void setActiveMembers(int activeMembers) { this.activeMembers = activeMembers; }

    public int getAvailableSlots() { return Math.max(0, maxCapacity - activeMembers); }
}