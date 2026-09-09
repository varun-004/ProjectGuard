package com.projectguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "engineering_branches",
        uniqueConstraints = @UniqueConstraint(name = "uk_engineering_branches_name", columnNames = "name")
)
public class EngineeringBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY)
    private List<StudentProfile> studentProfiles = new ArrayList<>();

    public EngineeringBranch() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<StudentProfile> getStudentProfiles() {
        return studentProfiles;
    }

    public void setStudentProfiles(List<StudentProfile> studentProfiles) {
        this.studentProfiles = studentProfiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EngineeringBranch other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "EngineeringBranch{id=" + id + ", name='" + name + "'}";
    }
}