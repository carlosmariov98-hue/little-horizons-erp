package com.littlehorizons.erp.view;

/**
 * Main menu of the application. It groups the options by module and opens the module menus.
 */
public class MainMenu extends BaseMenu {

    private final StudentMenu studentMenu;
    private final AcademicMenu academicMenu;
    private final TeacherMenu teacherMenu;
    private final ExtracurricularMenu extracurricularMenu;
    private final FinanceMenu financeMenu;
    private final ResourcesMenu resourcesMenu;

    public MainMenu(InputReader input, StudentMenu studentMenu, AcademicMenu academicMenu, TeacherMenu teacherMenu,
                    ExtracurricularMenu extracurricularMenu, FinanceMenu financeMenu,
                    ResourcesMenu resourcesMenu) {
        super(input);
        this.studentMenu = studentMenu;
        this.academicMenu = academicMenu;
        this.teacherMenu = teacherMenu;
        this.extracurricularMenu = extracurricularMenu;
        this.financeMenu = financeMenu;
        this.resourcesMenu = resourcesMenu;
    }

    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println();
            Console.banner("ERP Colegio Little Horizons", "Sistema de Gestión Educativa");
            Console.info("");
            Console.info("1. Gestión de Estudiantes");
            Console.info("2. Gestión Académica");
            Console.info("3. Gestión de Profesores");
            Console.info("4. Actividades Extracurriculares");
            Console.info("5. Gestión Financiera");
            Console.info("6. Gestión de Recursos");
            Console.info("7. Reportes y Estadísticas");
            Console.info("8. Salir");
            Console.info("");
            switch (input.readMenuOption("Seleccione una opción: ", 1, 8)) {
                case 1:
                    studentMenu.run();
                    break;
                case 2:
                    academicMenu.run();
                    break;
                case 3:
                    teacherMenu.run();
                    break;
                case 4:
                    extracurricularMenu.run();
                    break;
                case 5:
                    financeMenu.run();
                    break;
                case 6:
                    resourcesMenu.run();
                    break;
                case 8:
                    exit = true;
                    break;
                default:
                    Console.info("Este módulo estará disponible próximamente.");
                    break;
            }
        }
        Console.info("Gracias por usar el sistema. ¡Hasta pronto!");
    }
}