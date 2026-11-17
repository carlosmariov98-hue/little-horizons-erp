package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.MembershipStatus;

import java.time.LocalDate;

/** The membership of a student in a club (table "membresias_club"). */
public class ClubMembership {

    private int membershipId;
    private int studentId;
    private String studentName;
    private int clubId;
    private String clubName;
    private LocalDate joinDate;
    private MembershipStatus status;
    private LocalDate leaveDate;
    private String leaveReason;

    public int getMembershipId() { return membershipId; }
    public void setMembershipId(int membershipId) { this.membershipId = membershipId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getClubId() { return clubId; }
    public void setClubId(int clubId) { this.clubId = clubId; }
    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }
    public String getLeaveReason() { return leaveReason; }
    public void setLeaveReason(String leaveReason) { this.leaveReason = leaveReason; }
}
