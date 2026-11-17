package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.MembershipStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the extracurricular clubs: the clubs themselves, the memberships of the students
 * and the occupation report.
 */
public class ClubController {

    private static final int MAX_CAPACITY = 200;

    private final ClubDao clubDao;
    private final ClubMembershipDao membershipDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ClubController(ClubDao clubDao, ClubMembershipDao membershipDao, StudentDao studentDao,
                          TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // validates the data of a club and checks that the supervisor, when there is one, is an active teacher
    private void validateClub(Club club) throws ErpException {
        club.setName(Validator.requireText(club.getName(), "Nombre del club", 100));
        club.setDescription(Validator.optionalText(club.getDescription(), "Descripción", 500));
        club.setSchedule(Validator.optionalText(club.getSchedule(), "Horario", 100));
        Validator.requireIntInRange(club.getMaxCapacity(), 1, MAX_CAPACITY, "Capacidad máxima");
        if (club.getSupervisorId() != 0) {
            Teacher supervisor = teacherDao.findById(club.getSupervisorId());
            if (supervisor.getStatus() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("El profesor " + supervisor.getFullName()
                        + " no está activo y no puede supervisar un club.");
            }
        }
    }

    /**
     * Creates a club. A club without supervisor has supervisorId 0.
     *
     * @return the generated id
     * @throws ErpException if the data is invalid, the name is taken, or the database fails
     */
    public int createClub(Club club) throws ErpException {
        validateClub(club);
        club.setActive(true);
        return clubDao.insert(club);
    }

    /**
     * Updates a club. The capacity cannot be lower than the number of active members.
     *
     * @throws ErpException if the data is invalid, the club does not exist, or the database fails
     */
    public void updateClub(Club club) throws ErpException {
        validateClub(club);
        Club current = clubDao.findById(club.getClubId());
        if (club.getMaxCapacity() < current.getActiveMembers()) {
            throw new BusinessRuleException("El club tiene " + current.getActiveMembers()
                    + " miembros activos; la capacidad no puede ser menor.");
        }
        clubDao.update(club);
    }

    /**
     * Activates or deactivates a club. A club with active members cannot be deactivated.
     *
     * @throws ErpException if the club does not exist, already has that status, has members, or the database fails
     */
    public void changeActive(int clubId, boolean active) throws ErpException {
        Club club = clubDao.findById(clubId);
        if (club.isActive() == active) {
            throw new BusinessRuleException("El club ya se encuentra " + (active ? "activo" : "inactivo") + ".");
        }
        if (!active && club.getActiveMembers() > 0) {
            throw new BusinessRuleException("No se puede desactivar: el club tiene " + club.getActiveMembers()
                    + " miembros activos. Retírelos primero.");
        }
        clubDao.changeActive(clubId, active);
    }

    /**
     * Enrolls a student in a club.
     *
     * @return the id of the membership
     * @throws ErpException if the student is not active, the club is full or inactive, or the database fails
     */
    public int joinClub(int studentId, int clubId, LocalDate date) throws ErpException {
        Validator.requirePastOrToday(date, "Fecha de ingreso");
        Student student = studentDao.findById(studentId);
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Solo los estudiantes activos pueden inscribirse en un club "
                    + "(estado actual: " + student.getStatus().getLabel() + ").");
        }
        return membershipDao.join(studentId, clubId, date);
    }

    /**
     * Withdraws a student from a club.
     *
     * @throws ErpException if the membership is not active, the date is invalid, or the database fails
     */
    public void withdraw(int membershipId, LocalDate date, String reason) throws ErpException {
        Validator.requirePastOrToday(date, "Fecha de retiro");
        String cleanReason = Validator.requireText(reason, "Motivo del retiro", 300);
        ClubMembership membership = membershipDao.findById(membershipId);
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessRuleException("Esa membresía ya está retirada.");
        }
        if (date.isBefore(membership.getJoinDate())) {
            throw new ValidationException("La fecha de retiro no puede ser anterior a la fecha de ingreso ("
                    + membership.getJoinDate() + ").");
        }
        membershipDao.withdraw(membershipId, date, cleanReason);
    }

    public Club getClub(int clubId) throws DatabaseException, EntityNotFoundException {
        return clubDao.findById(clubId);
    }

    public List<Club> listClubs() throws DatabaseException {
        return clubDao.findAll();
    }

    public List<Club> listActiveClubs() throws DatabaseException {
        return clubDao.findActive();
    }

    public List<ClubMembership> getMembers(int clubId) throws ErpException {
        clubDao.findById(clubId);
        return membershipDao.findByClub(clubId);
    }

    /** The active membership of a student in a club, found to withdraw the student. */
    public ClubMembership getActiveMembership(int studentId, int clubId) throws ErpException {
        return membershipDao.findActiveOf(studentId, clubId).stream().findFirst().orElseThrow(
                () -> new EntityNotFoundException("El estudiante no es miembro activo de ese club."));
    }

    public List<ClubMembership> getClubsOfStudent(int studentId) throws ErpException {
        studentDao.findById(studentId);
        return membershipDao.findByStudent(studentId);
    }

    // ---------- statistics (Streams) ----------

    /** Percentage of the capacity of a club that is taken by active members. */
    public static int occupancyPercent(Club club) {
        return club.getMaxCapacity() == 0 ? 0 : Math.round(club.getActiveMembers() * 100f / club.getMaxCapacity());
    }

    /** Active clubs ordered from the most to the least occupied. */
    public List<Club> getOccupancyReport() throws DatabaseException {
        return clubDao.findActive().stream()
                .sorted(Comparator.comparingInt(ClubController::occupancyPercent).reversed()
                        .thenComparing(Club::getName))
                .collect(Collectors.toList());
    }

    /** Total number of active memberships in all the clubs. */
    public static int totalMembers(List<Club> clubs) {
        return clubs.stream().mapToInt(Club::getActiveMembers).sum();
    }
}