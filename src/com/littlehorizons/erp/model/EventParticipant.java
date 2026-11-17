package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.EventPosition;
import com.littlehorizons.erp.model.enums.ParticipationRole;

/** The participation of a student in an event (table "participantes_evento"). */
public class EventParticipant {

    private int participationId;
    private int eventId;
    private String eventName;
    private int studentId;
    private String studentName;
    private ParticipationRole role;
    private EventPosition position;
    private String notes;

    public int getParticipationId() { return participationId; }
    public void setParticipationId(int participationId) { this.participationId = participationId; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public ParticipationRole getRole() { return role; }
    public void setRole(ParticipationRole role) { this.role = role; }
    /** Result obtained in a competition, or null when no position has been given. */
    public EventPosition getPosition() { return position; }
    public void setPosition(EventPosition position) { this.position = position; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
