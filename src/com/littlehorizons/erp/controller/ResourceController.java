package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.Validator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for the inventory of resources: creating and updating them, changing their status
 * and the inventory statistics.
 */
public class ResourceController {

    private final ResourceDao resourceDao;
    private final ResourceCategoryDao categoryDao;

    public ResourceController(ResourceDao resourceDao, ResourceCategoryDao categoryDao) {
        this.resourceDao = resourceDao;
        this.categoryDao = categoryDao;
    }

    private void validateResource(Resource resource) throws ValidationException {
        resource.setName(Validator.requireText(resource.getName(), "Nombre del recurso", 150));
        resource.setDescription(Validator.optionalText(resource.getDescription(), "Descripción", 500));
        resource.setLocation(Validator.optionalText(resource.getLocation(), "Ubicación", 100));
        if (resource.getAcquisitionDate() != null) {
            Validator.requirePastOrToday(resource.getAcquisitionDate(), "Fecha de adquisición");
        }
    }

    /**
     * Creates a resource, always as available. The inventory code is saved in capital letters.
     *
     * @return the generated id
     * @throws ErpException if the data is invalid, the code is taken, the category does not exist, or the database fails
     */
    public int createResource(Resource resource) throws ErpException {
        String code = Validator.requireText(resource.getCode(), "Código de inventario", 50).toUpperCase();
        if (!code.matches("^[A-Z0-9-]+$")) {
            throw new ValidationException("El código solo admite letras, números y guion (ejemplo: LIB-051).");
        }
        resource.setCode(code);
        validateResource(resource);
        categoryDao.findById(resource.getCategoryId());
        resource.setStatus(ResourceStatus.AVAILABLE);
        return resourceDao.insert(resource);
    }

    /**
     * Updates the data of a resource (not its code or status).
     *
     * @throws ErpException if the data is invalid, the resource or category does not exist, or the database fails
     */
    public void updateResource(Resource resource) throws ErpException {
        validateResource(resource);
        resourceDao.findById(resource.getResourceId());
        categoryDao.findById(resource.getCategoryId());
        resourceDao.update(resource);
    }

    /**
     * Changes the status of a resource by hand. Only "Disponible", "En Mantenimiento" and "Dado de Baja" can be
     * chosen: "Prestado" is set by the loans, and a resource that was retired cannot be used again.
     *
     * @throws ErpException if the change is not allowed, the resource does not exist, or the database fails
     */
    public void changeStatus(int resourceId, ResourceStatus newStatus) throws ErpException {
        Validator.requireNotNull(newStatus, "Estado");
        if (newStatus == ResourceStatus.LOANED) {
            throw new BusinessRuleException("El estado 'Prestado' solo se asigna al registrar un préstamo.");
        }
        Resource resource = resourceDao.findById(resourceId);
        if (resource.getStatus() == ResourceStatus.RETIRED) {
            throw new BusinessRuleException("El recurso está dado de baja y no puede volver a usarse.");
        }
        if (resource.getStatus() == newStatus) {
            throw new BusinessRuleException("El recurso ya se encuentra en estado " + newStatus.getLabel() + ".");
        }
        resourceDao.changeStatus(resourceId, newStatus);
    }

    public Resource getById(int resourceId) throws DatabaseException, EntityNotFoundException {
        return resourceDao.findById(resourceId);
    }

    public List<Resource> listAll() throws DatabaseException {
        return resourceDao.findAll();
    }

    public List<Resource> listByStatus(ResourceStatus status) throws DatabaseException {
        return resourceDao.findByStatus(status);
    }

    /** Resources whose code or name contains the text. */
    public List<Resource> search(String text) throws ErpException {
        return resourceDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public List<ResourceCategory> listCategories() throws DatabaseException {
        return categoryDao.findAll();
    }

    // ---------- statistics (Streams) ----------

    /** Number of resources in each status (all the statuses appear, even with zero). */
    public Map<ResourceStatus, Long> countByStatus() throws DatabaseException {
        Map<ResourceStatus, Long> counts = new EnumMap<>(ResourceStatus.class);
        for (ResourceStatus status : ResourceStatus.values()) {
            counts.put(status, 0L);
        }
        resourceDao.findAll().stream().collect(Collectors.groupingBy(Resource::getStatus, Collectors.counting()))
                .forEach(counts::put);
        return counts;
    }

    /** Number of resources that can be lent right now, for each category. */
    public Map<String, Long> availableByCategory() throws DatabaseException {
        return resourceDao.findByStatus(ResourceStatus.AVAILABLE).stream()
                .collect(Collectors.groupingBy(Resource::getCategoryName, TreeMap::new, Collectors.counting()));
    }

    /** Resources of one category, filtered in memory from the full inventory. */
    public List<Resource> listByCategory(int categoryId) throws DatabaseException {
        return resourceDao.findAll().stream().filter(r -> r.getCategoryId() == categoryId)
                .collect(Collectors.toList());
    }
}