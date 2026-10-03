package com.projectguard.dto.student;

public class EngineeringBranchResponse {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isEngineering;

    public EngineeringBranchResponse() {
    }

    public EngineeringBranchResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public EngineeringBranchResponse(Long id, String name, String description, Boolean isEngineering) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isEngineering = isEngineering;
    }

    public EngineeringBranchResponse(Long id, String name, String displayName, String description, Boolean isEngineering) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.isEngineering = isEngineering;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEngineering() {
        return isEngineering;
    }

    public void setIsEngineering(Boolean isEngineering) {
        this.isEngineering = isEngineering;
    }
}
