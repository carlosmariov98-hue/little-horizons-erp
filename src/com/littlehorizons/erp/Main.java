package com.littlehorizons.erp;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.GradeController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.controller.TeachingRules;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.view.AcademicMenu;
import com.littlehorizons.erp.view.AttendanceMenu;
import com.littlehorizons.erp.view.Console;
import com.littlehorizons.erp.view.EnrollmentMenu;
import com.littlehorizons.erp.view.GradeMenu;
import com.littlehorizons.erp.view.InputReader;
import com.littlehorizons.erp.view.MainMenu;
import com.littlehorizons.erp.view.StudentMenu;
import com.littlehorizons.erp.view.TeacherMenu;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Entry point of the ERP. It builds the objects of every layer (DAOs, controllers, views)
 * and connects them, then opens the main menu.
 */
public class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            verifyConnection(database);

            StudentDao studentDao = new StudentDao(database);
            GuardianDao guardianDao = new GuardianDao(database);
            StudentGuardianDao studentGuardianDao = new StudentGuardianDao(database, studentDao, guardianDao);

            GradeDao gradeDao = new GradeDao(database);
            SubjectDao subjectDao = new SubjectDao(database);
            TeacherDao teacherDao = new TeacherDao(database);
            AssignmentDao assignmentDao = new AssignmentDao(database);
            EnrollmentDao enrollmentDao = new EnrollmentDao(database, studentDao);
            SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDao(database);
            AcademicRecordDao academicRecordDao = new AcademicRecordDao(database);
            AttendanceDao attendanceDao = new AttendanceDao(database);

            StudentController studentController = new StudentController(studentDao, studentGuardianDao);
            GuardianController guardianController = new GuardianController(guardianDao, studentGuardianDao);
            TeacherController teacherController = new TeacherController(teacherDao, assignmentDao, subjectDao);
            AcademicController academicController = new AcademicController(studentDao, gradeDao, subjectDao,
                    enrollmentDao, subjectEnrollmentDao);
            TeachingRules teachingRules = new TeachingRules(teacherDao, assignmentDao);
            GradeController gradeController = new GradeController(academicRecordDao, studentDao, teachingRules,
                    subjectEnrollmentDao);
            AttendanceController attendanceController = new AttendanceController(attendanceDao, studentDao,
                    subjectDao, subjectEnrollmentDao, teachingRules);

            InputReader input = new InputReader(new Scanner(System.in, StandardCharsets.UTF_8));
            StudentMenu studentMenu = new StudentMenu(input, studentController, guardianController);
            TeacherMenu teacherMenu = new TeacherMenu(input, teacherController, academicController);
            AcademicMenu academicMenu = new AcademicMenu(input,
                    new EnrollmentMenu(input, academicController, studentController),
                    new GradeMenu(input, gradeController, academicController, studentController, teacherController),
                    new AttendanceMenu(input, attendanceController, academicController, studentController,
                            teacherController));
            new MainMenu(input, studentMenu, academicMenu, teacherMenu).run();
        } catch (ConfigurationException | DatabaseException e) {
            Console.error(e.getMessage());
        } catch (NoSuchElementException e) {
            Console.info("La entrada de datos terminó. Hasta pronto.");
        } catch (RuntimeException e) {
            AppLogger.getInstance().error("Unexpected error stopped the application.", e);
            Console.error("Ocurrió un error inesperado y el programa debe cerrarse. Consulte error_log.txt.");
        }
    }

    // fails fast, with a clear message, when MySQL cannot be reached
    private static void verifyConnection(DatabaseConnection database) throws DatabaseException {
        try {
            database.getConnection().close();
        } catch (SQLException e) {
            AppLogger.getInstance().error("Could not close the verification connection.", e);
        }
    }
}