package com.littlehorizons.erp.view;

/** Console menu for the academic module. It opens the menu of each academic area. */
public class AcademicMenu extends BaseMenu {

    private final EnrollmentMenu enrollmentMenu;
    private final GradeMenu gradeMenu;

    public AcademicMenu(InputReader input, EnrollmentMenu enrollmentMenu, GradeMenu gradeMenu) {
        super(input);
        this.enrollmentMenu = enrollmentMenu;
        this.gradeMenu = gradeMenu;
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
                case 0:
                    back = true;
                    break;
                default:
                    Console.info("Esta opción estará disponible próximamente.");
                    break;
            }
        }
    }
}