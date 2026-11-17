package com.littlehorizons.erp.view;

/** Menu of the resources module: it opens the inventory and the loans. */
public class ResourcesMenu extends BaseMenu {

    private final InventoryMenu inventoryMenu;
    private final LoanMenu loanMenu;

    public ResourcesMenu(InputReader input, InventoryMenu inventoryMenu, LoanMenu loanMenu) {
        super(input);
        this.inventoryMenu = inventoryMenu;
        this.loanMenu = loanMenu;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("GESTIÓN DE RECURSOS");
            Console.info("1. Inventario");
            Console.info("2. Préstamos");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 2)) {
                case 1:
                    inventoryMenu.run();
                    break;
                case 2:
                    loanMenu.run();
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }
}