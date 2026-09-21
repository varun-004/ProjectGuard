package com.projectguard.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "project_domains",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_domains_name",
                columnNames = "name"
        )
)
public class ProjectDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Inverse side of the branch_domains ManyToMany.
     */
    @ManyToMany(mappedBy = "domains", fetch = FetchType.LAZY)
    private List<EngineeringBranch> branches = new ArrayList<>();

    /**
     * Which skills are relevant for this domain.
     * Owner side of the domain_skills join table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "domain_skills",
            joinColumns = @JoinColumn(name = "domain_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "domain", fetch = FetchType.LAZY)
    private List<StudentProfile> studentProfiles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_domain_id")
    private ProjectDomain parentDomain;

    @OneToMany(mappedBy = "parentDomain", fetch = FetchType.LAZY)
    private List<ProjectDomain> subDomains = new ArrayList<>();

    @OneToMany(mappedBy = "domain", fetch = FetchType.LAZY)
    private List<Technology> technologies = new ArrayList<>();

    public ProjectDomain() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<EngineeringBranch> getBranches() { return branches; }
    public void setBranches(List<EngineeringBranch> branches) { this.branches = branches; }

    public List<Skill> getSkills() { return skills; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }

    public List<StudentProfile> getStudentProfiles() { return studentProfiles; }
    public void setStudentProfiles(List<StudentProfile> studentProfiles) {
        this.studentProfiles = studentProfiles;
    }

    public ProjectDomain getParentDomain() { return parentDomain; }
    public void setParentDomain(ProjectDomain parentDomain) { this.parentDomain = parentDomain; }

    public List<ProjectDomain> getSubDomains() { return subDomains; }
    public void setSubDomains(List<ProjectDomain> subDomains) { this.subDomains = subDomains; }

    public List<Technology> getTechnologies() { return technologies; }
    public void setTechnologies(List<Technology> technologies) { this.technologies = technologies; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectDomain other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "ProjectDomain{id=" + id + ", name='" + name + "'}";
    }
}