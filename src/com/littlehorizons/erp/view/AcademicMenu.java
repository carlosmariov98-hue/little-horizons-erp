package com.littlehorizons.erp.view;

/** Console menu for the academic module. It opens the menu of each academic area. */
public class AcademicMenu extends BaseMenu {

    private final EnrollmentMenu enrollmentMenu;
    private final GradeMenu gradeMenu;
    private final AttendanceMenu attendanceMenu;

    public AcademicMenu(InputReader input, EnrollmentMenu enrollmentMenu, GradeMenu gradeMenu,
                        AttendanceMenu attendanceMenu) {
        super(input);
        this.enrollmentMenu = enrollmentMenu;
        this.gradeMenu = gradeMenu;
        this.attendanceMenu = attendanceMenu;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("GESTIÓN ACADÉMICA");
            Console.info("1. Matrículas e inscripciones");
            Console.info("2. Calificaciones y promedios");
            Console.info("3. Control de asistencia");
            Console.info("0. Volver al menú principal");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 3)) {
                case 1:
                    enrollmentMenu.run();
                    break;
                case 2:
                    gradeMenu.run();
                    break;
                case 3:
                    attendanceMenu.run();
                    break;
                case 0:
                    back = true;
                    break;
                default:
                    break;
            }
        }
    }
}