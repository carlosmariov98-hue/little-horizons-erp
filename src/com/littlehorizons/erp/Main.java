package com.littlehorizons.erp;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.GradeController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.controller.TeachingRules;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.InvoiceFileWriter;
import com.littlehorizons.erp.util.ReportFileWriter;
import com.littlehorizons.erp.view.AcademicMenu;
import com.littlehorizons.erp.view.AttendanceMenu;
import com.littlehorizons.erp.view.ClubMenu;
import com.littlehorizons.erp.view.Console;
import com.littlehorizons.erp.view.EnrollmentMenu;
import com.littlehorizons.erp.view.EventMenu;
import com.littlehorizons.erp.view.ExtracurricularMenu;
import com.littlehorizons.erp.view.FinanceMenu;
import com.littlehorizons.erp.view.GradeMenu;
import com.littlehorizons.erp.view.InputReader;
import com.littlehorizons.erp.view.InventoryMenu;
import com.littlehorizons.erp.view.LoanMenu;
import com.littlehorizons.erp.view.MainMenu;
import com.littlehorizons.erp.view.PaymentConceptMenu;
import com.littlehorizons.erp.view.ReportMenu;
import com.littlehorizons.erp.view.ResourcesMenu;
import com.littlehorizons.erp.view.StudentMenu;
import com.littlehorizons.erp.view.TeacherMenu;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
            AppLogger.getInstance().info("Application started. Database connection verified.");

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
            PaymentConceptDao conceptDao = new PaymentConceptDao(database);
            PaymentDao paymentDao = new PaymentDao(database);
            InvoiceDao invoiceDao = new InvoiceDao(database);
            ClubDao clubDao = new ClubDao(database);
            ClubMembershipDao clubMembershipDao = new ClubMembershipDao(database);
            EventDao eventDao = new EventDao(database);
            EventParticipantDao eventParticipantDao = new EventParticipantDao(database);
            ResourceCategoryDao resourceCategoryDao = new ResourceCategoryDao(database);
            ResourceDao resourceDao = new ResourceDao(database);
            LoanDao loanDao = new LoanDao(database);

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
            PaymentConceptController conceptController = new PaymentConceptController(conceptDao);
            PaymentController paymentController = new PaymentController(paymentDao, invoiceDao, conceptDao,
                    studentDao, studentGuardianDao, guardianDao, new InvoiceFileWriter(Paths.get("invoices")));
            ClubController clubController = new ClubController(clubDao, clubMembershipDao, studentDao, teacherDao);
            EventController eventController = new EventController(eventDao, eventParticipantDao, studentDao,
                    teacherDao);
            ResourceController resourceController = new ResourceController(resourceDao, resourceCategoryDao);
            LoanController loanController = new LoanController(loanDao, resourceDao, studentDao, teacherDao);
            ReportController reportController = new ReportController(studentDao, teacherDao, gradeDao,
                    enrollmentDao, academicRecordDao, attendanceDao, clubDao, paymentController, resourceController,
                    loanController);

            InputReader input = new InputReader(new Scanner(System.in, StandardCharsets.UTF_8));
            StudentMenu studentMenu = new StudentMenu(input, studentController, guardianController);
            TeacherMenu teacherMenu = new TeacherMenu(input, teacherController, academicController);
            AcademicMenu academicMenu = new AcademicMenu(input,
                    new EnrollmentMenu(input, academicController, studentController),
                    new GradeMenu(input, gradeController, academicController, studentController, teacherController),
                    new AttendanceMenu(input, attendanceController, academicController, studentController,
                            teacherController));
            ExtracurricularMenu extracurricularMenu = new ExtracurricularMenu(input,
                    new ClubMenu(input, clubController, studentController, teacherController),
                    new EventMenu(input, eventController, studentController, teacherController));
            FinanceMenu financeMenu = new FinanceMenu(input, paymentController, conceptController, studentController,
                    guardianController, new PaymentConceptMenu(input, conceptController));
            ResourcesMenu resourcesMenu = new ResourcesMenu(input, new InventoryMenu(input, resourceController),
                    new LoanMenu(input, loanController, resourceController, studentController, teacherController));
            new MainMenu(input, studentMenu, academicMenu, teacherMenu, extracurricularMenu, financeMenu,
                    resourcesMenu, new ReportMenu(input, reportController,
                    new ReportFileWriter(Paths.get("reports")))).run();
            AppLogger.getInstance().info("Application closed by the user.");
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