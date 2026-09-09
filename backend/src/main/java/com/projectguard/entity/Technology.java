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
        name = "technologies",
        uniqueConstraints = @UniqueConstraint(name = "uk_technologies_name", columnNames = "name")
)
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "technology", fetch = FetchType.LAZY)
    private List<ProjectTechnology> projectTechnologies = new ArrayList<>();

    public Technology() {
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

    public List<ProjectTechnology> getProjectTechnologies() {
        return projectTechnologies;
    }

    public void setProjectTechnologies(List<ProjectTechnology> projectTechnologies) {
        this.projectTechnologies = projectTechnologies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Technology other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Technology{id=" + id + ", name='" + name + "'}";
    }
}