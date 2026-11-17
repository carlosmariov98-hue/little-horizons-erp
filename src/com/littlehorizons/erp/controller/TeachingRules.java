package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.TeacherStatus;

/**
 * Business rule shared by the controllers that need a teacher to act on a subject
 * (grading, taking attendance): the teacher must be active and teach that subject that year.
 */
public class TeachingRules {

    private final TeacherDao teacherDao;
    private final AssignmentDao assignmentDao;

    public TeachingRules(TeacherDao teacherDao, AssignmentDao assignmentDao) {
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
    }

    /**
     * Checks that a teacher is active and has the subject assigned in the given year.
     *
     * @param teacherId id of the teacher
     * @param subjectId id of the subject
     * @param year      academic year
     * @throws BusinessRuleException if the teacher is not active or does not teach the subject
     * @throws ErpException          if the teacher does not exist or the database fails
     */
    public void requireTeacherOfSubject(int teacherId, int subjectId, int year) throws ErpException {
        Teacher teacher = teacherDao.findById(teacherId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("El profesor " + teacher.getFullName() + " no está activo (estado: "
                    + teacher.getStatus().getLabel() + ").");
        }
        boolean teaches = assignmentDao.findByTeacher(teacherId, year).stream()
                .anyMatch(a -> a.getSubjectId() == subjectId);
        if (!teaches) {
            throw new BusinessRuleException("El profesor " + teacher.getFullName()
                    + " no tiene asignada esa materia en " + year + ".");
        }
    }
}
