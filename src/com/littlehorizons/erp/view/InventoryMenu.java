package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.DateUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Console menu for the inventory of resources (books, laboratory equipment, sports equipment...). */
public class InventoryMenu extends BaseMenu {

    // the statuses a user can choose by hand: "Prestado" is set only by the loans
    private static final List<ResourceStatus> MANUAL_STATUSES = Arrays.asList(ResourceStatus.AVAILABLE,
            ResourceStatus.MAINTENANCE, ResourceStatus.RETIRED);

    private final ResourceController resources;

    public InventoryMenu(InputReader input, ResourceController resources) {
        super(input);
        this.resources = resources;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("INVENTARIO DE RECURSOS");
            Console.info("1. Listar todos los recursos");
            Console.info("2. Listar recursos por estado");
            Console.info("3. Buscar un recurso por código o nombre");
            Console.info("4. Registrar un recurso nuevo");
            Console.info("5. Editar un recurso");
            Console.info("6. Cambiar el estado de un recurso (mantenimiento, baja, disponible)");
            Console.info("7. Resumen del inventario");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 7)) {
                case 1:
                    runSafely(() -> printResources(resources.listAll()));
                    break;
                case 2:
                    runSafely(this::listByStatus);
                    break;
                case 3:
                    runSafely(this::search);
                    break;
                case 4:
                    runSafely(this::createResource);
                    break;
                case 5:
                    runSafely(this::editResource);
                    break;
                case 6:
                    runSafely(this::changeStatus);
                    break;
                case 7:
                    runSafely(this::summary);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void printResources(List<Resource> list) {
        Console.title("RECURSOS (" + list.size() + ")");
        if (list.isEmpty()) {
            Console.info("No hay recursos para mostrar.");
            return;
        }
        Console.info(String.format("%3s | %-9s | %-44s | %-22s | %-16s | %s", "ID", "Código", "Nombre", "Categoría",
                "Estado", "Ubicación"));
        for (Resource r : list) {
            Console.info(String.format("%3d | %-9s | %-44s | %-22s | %-16s | %s", r.getResourceId(), r.getCode(),
                    r.getName().length() > 44 ? r.getName().substring(0, 41) + "..." : r.getName(),
                    r.getCategoryName(), r.getStatus().getLabel(), dash(r.getLocation())));
        }
    }

    private void listByStatus() throws ErpException {
        ResourceStatus status = input.readChoice("Estado:", Arrays.asList(ResourceStatus.values()),
                ResourceStatus::getLabel);
        printResources(resources.listByStatus(status));
    }

    private void search() throws ErpException {
        String text = input.readValid("Texto a buscar (código o nombre): ", t -> t);
        printResources(resources.search(text));
    }

    private ResourceCategory chooseCategory() throws ErpException {
        return input.readChoice("Categoría:", resources.listCategories(), ResourceCategory::getName);
    }

    private LocalDate readOptionalDate(String prompt, LocalDate current) throws ErpException {
        return input.readValid(prompt, text -> text.isEmpty() ? current : DateUtils.parse(text));
    }

    private void createResource() throws ErpException {
        Console.title("REGISTRAR UN RECURSO");
        Resource resource = new Resource();
        resource.setCategoryId(chooseCategory().getCategoryId());
        resource.setCode(input.readValid("Código de inventario (ejemplo: LIB-051): ", text -> text));
        resource.setName(input.readValid("Nombre: ", text -> text));
        resource.setDescription(input.readLine("Descripción (Enter = ninguna): "));
        resource.setAcquisitionDate(readOptionalDate("Fecha de adquisición dd/MM/yyyy (Enter = ninguna): ", null));
        resource.setLocation(input.readLine("Ubicación física (Enter = ninguna): "));
        int id = resources.createResource(resource);
        Console.success("Recurso registrado con ID " + id + " (disponible).");
    }

    private void editResource() throws ErpException {
        Resource resource = resources.getById(readId("ID del recurso: "));
        Console.title("EDITAR: " + resource.getCode() + " (Enter conserva el valor actual, '-' borra un dato opcional)");
        resource.setName(readKeepText("Nombre", resource.getName(), text -> text, false));
        resource.setDescription(readKeepText("Descripción", resource.getDescription(), text -> text, true));
        resource.setLocation(readKeepText("Ubicación", resource.getLocation(), text -> text, true));
        resource.setAcquisitionDate(readOptionalDate("Fecha de adquisición [" + (resource.getAcquisitionDate() == null
                ? "-" : DateUtils.format(resource.getAcquisitionDate())) + "]: ", resource.getAcquisitionDate()));
        if (input.readYesNo("Categoría actual: " + resource.getCategoryName() + ". ¿Cambiarla? (s/n): ")) {
            resource.setCategoryId(chooseCategory().getCategoryId());
        }
        resources.updateResource(resource);
        Console.success("Recurso actualizado.");
    }

    private void changeStatus() throws ErpException {
        Resource resource = resources.getById(readId("ID del recurso: "));
        Console.info("Recurso: " + resource.getName() + " (estado actual: " + resource.getStatus().getLabel() + ")");
        ResourceStatus status = input.readChoice("Nuevo estado:", MANUAL_STATUSES, ResourceStatus::getLabel);
        if (status == ResourceStatus.RETIRED
                && !input.readYesNo("Dar de baja es definitivo: el recurso no podrá volver a usarse. ¿Continuar? (s/n): ")) {
            Console.info("No se realizó ningún cambio.");
            return;
        }
        resources.changeStatus(resource.getResourceId(), status);
        Console.success("Estado actualizado a " + status.getLabel() + ".");
    }

    private void summary() throws ErpException {
        Console.title("RESUMEN DEL INVENTARIO");
        Map<ResourceStatus, Long> byStatus = resources.countByStatus();
        byStatus.forEach((status, count) -> Console.info(String.format("%-18s %d", status.getLabel(), count)));
        Console.info(String.format("%-18s %d", "TOTAL", byStatus.values().stream().mapToLong(Long::longValue).sum()));
        Console.info("");
        Console.info("Recursos disponibles por categoría:");
        resources.availableByCategory().forEach((name, count) -> Console.info(String.format("  %-26s %d", name, count)));
    }
}
