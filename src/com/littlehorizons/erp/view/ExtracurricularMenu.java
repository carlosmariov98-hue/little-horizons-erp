package com.littlehorizons.erp.view;

/** Menu of the extracurricular activities module: it opens the clubs and the events. */
public class ExtracurricularMenu extends BaseMenu {

    private final ClubMenu clubMenu;
    private final EventMenu eventMenu;

    public ExtracurricularMenu(InputReader input, ClubMenu clubMenu, EventMenu eventMenu) {
        super(input);
        this.clubMenu = clubMenu;
        this.eventMenu = eventMenu;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("ACTIVIDADES EXTRACURRICULARES");
            Console.info("1. Clubes");
            Console.info("2. Eventos");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 2)) {
                case 1:
                    clubMenu.run();
                    break;
                case 2:
                    eventMenu.run();
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }
}