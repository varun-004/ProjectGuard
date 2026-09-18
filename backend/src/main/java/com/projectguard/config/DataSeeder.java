package com.projectguard.config;

import com.projectguard.entity.EngineeringBranch;
import com.projectguard.entity.ProjectDomain;
import com.projectguard.entity.Skill;
import com.projectguard.repository.EngineeringBranchRepository;
import com.projectguard.repository.ProjectDomainRepository;
import com.projectguard.repository.SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EngineeringBranchRepository branchRepository;
    private final ProjectDomainRepository domainRepository;
    private final SkillRepository skillRepository;

    public DataSeeder(EngineeringBranchRepository branchRepository,
                      ProjectDomainRepository domainRepository,
                      SkillRepository skillRepository) {
        this.branchRepository = branchRepository;
        this.domainRepository = domainRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if empty
        if (branchRepository.count() == 0) {
            EngineeringBranch cs = new EngineeringBranch();
            cs.setName("Computer Science");
            branchRepository.save(cs);

            EngineeringBranch it = new EngineeringBranch();
            it.setName("Information Technology");
            branchRepository.save(it);
            
            ProjectDomain web = new ProjectDomain();
            web.setName("Web Development");
            domainRepository.save(web);
            
            ProjectDomain ai = new ProjectDomain();
            ai.setName("Artificial Intelligence");
            domainRepository.save(ai);

            ProjectDomain sec = new ProjectDomain();
            sec.setName("Cybersecurity");
            domainRepository.save(sec);

            // Connect branches to domains
            cs.setDomains(List.of(web, ai, sec));
            branchRepository.save(cs);
            
            it.setDomains(List.of(web, sec));
            branchRepository.save(it);

            // Create Skills
            Skill react = new Skill(); react.setName("React"); skillRepository.save(react);
            Skill java = new Skill(); java.setName("Java"); skillRepository.save(java);
            Skill python = new Skill(); python.setName("Python"); skillRepository.save(python);
            Skill ml = new Skill(); ml.setName("Machine Learning"); skillRepository.save(ml);
            Skill pentesting = new Skill(); pentesting.setName("Penetration Testing"); skillRepository.save(pentesting);

            // Connect domains to skills
            web.setSkills(List.of(react, java, python));
            domainRepository.save(web);

            ai.setSkills(List.of(python, ml));
            domainRepository.save(ai);

            sec.setSkills(List.of(python, pentesting));
            domainRepository.save(sec);
        }
    }
}
