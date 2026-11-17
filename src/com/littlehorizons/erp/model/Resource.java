package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.ResourceStatus;

import java.time.LocalDate;

/** A resource of the school inventory that can be lent (table "recursos"). */
public class Resource {

    private int resourceId;
    private String code;
    private String name;
    private int categoryId;
    private String categoryName;
    private String description;
    private LocalDate acquisitionDate;
    private String location;
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
}