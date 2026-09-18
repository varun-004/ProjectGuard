package com.projectguard.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "engineering_branches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_engineering_branches_name",
                columnNames = "name"
        )
)
public class EngineeringBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Which domains are relevant for this branch.
     * Owner side of the branch_domains join table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "branch_domains",
            joinColumns = @JoinColumn(name = "branch_id"),
            inverseJoinColumns = @JoinColumn(name = "domain_id")
    )
    private List<ProjectDomain> domains = new ArrayList<>();

    @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY)
    private List<StudentProfile> studentProfiles = new ArrayList<>();

    public EngineeringBranch() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ProjectDomain> getDomains() { return domains; }
    public void setDomains(List<ProjectDomain> domains) { this.domains = domains; }

    public List<StudentProfile> getStudentProfiles() { return studentProfiles; }
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
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "EngineeringBranch{id=" + id + ", name='" + name + "'}";
    }
}