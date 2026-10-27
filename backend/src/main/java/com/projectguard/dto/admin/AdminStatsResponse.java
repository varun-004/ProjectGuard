package com.projectguard.dto.admin;

/**
 * Aggregate statistics returned by GET /api/admin/stats.
 */
public class AdminStatsResponse {

    private long totalUsers;
    private long totalStudents;
    private long totalAdmins;
    private long totalProjects;
    private long catalogProjects;
    private long aiGeneratedProjects;
    private long approvedProjects;
    private long totalBranches;
    private long totalDomains;
    private long totalSkills;
    private long totalTechnologies;
    private long totalAssessmentAttempts;
    private long totalAssessmentResults;

    public AdminStatsResponse() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getTotalAdmins() { return totalAdmins; }
    public void setTotalAdmins(long totalAdmins) { this.totalAdmins = totalAdmins; }

    public long getTotalProjects() { return totalProjects; }
    public void setTotalProjects(long totalProjects) { this.totalProjects = totalProjects; }

    public long getCatalogProjects() { return catalogProjects; }
    public void setCatalogProjects(long catalogProjects) { this.catalogProjects = catalogProjects; }

    public long getAiGeneratedProjects() { return aiGeneratedProjects; }
    public void setAiGeneratedProjects(long aiGeneratedProjects) { this.aiGeneratedProjects = aiGeneratedProjects; }

    public long getApprovedProjects() { return approvedProjects; }
    public void setApprovedProjects(long approvedProjects) { this.approvedProjects = approvedProjects; }

    public long getTotalBranches() { return totalBranches; }
    public void setTotalBranches(long totalBranches) { this.totalBranches = totalBranches; }

    public long getTotalDomains() { return totalDomains; }
    public void setTotalDomains(long totalDomains) { this.totalDomains = totalDomains; }

    public long getTotalSkills() { return totalSkills; }
    public void setTotalSkills(long totalSkills) { this.totalSkills = totalSkills; }

    public long getTotalTechnologies() { return totalTechnologies; }
    public void setTotalTechnologies(long totalTechnologies) { this.totalTechnologies = totalTechnologies; }

    public long getTotalAssessmentAttempts() { return totalAssessmentAttempts; }
    public void setTotalAssessmentAttempts(long totalAssessmentAttempts) { this.totalAssessmentAttempts = totalAssessmentAttempts; }

    public long getTotalAssessmentResults() { return totalAssessmentResults; }
    public void setTotalAssessmentResults(long totalAssessmentResults) { this.totalAssessmentResults = totalAssessmentResults; }
}
