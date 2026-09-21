package com.projectguard.repository;

import com.projectguard.entity.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnologyRepository
        extends JpaRepository<Technology, Long> {

    Optional<Technology> findByDomainIdAndName(Long domainId, String name);

    boolean existsByDomainIdAndName(Long domainId, String name);
    
    List<Technology> findByDomainId(Long domainId);
}