package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.EventType;

import java.time.LocalDate;

/** An event of the school (table "eventos"). */
public class SchoolEvent {

    private int eventId;
    private String name;
    private String description;
    private EventType type;
    private LocalDate date;
    private String place;
    private boolean competition;
    private int organizerId;
    private String organizerName;
    private boolean active = true;
    private int participantCount;

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    /** True when the event is a competition and its competitors receive a position. */
    public boolean hasCompetition() { return competition; }
    public void setCompetition(boolean competition) { this.competition = competition; }
    /** Id of the organizing teacher, or 0 when there is none. */
    public int getOrganizerId() { return organizerId; }
    public void setOrganizerId(int organizerId) { this.organizerId = organizerId; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }
}