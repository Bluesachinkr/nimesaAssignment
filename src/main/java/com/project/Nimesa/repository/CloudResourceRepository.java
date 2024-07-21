package com.project.Nimesa.repository;

import com.project.Nimesa.data.CloudResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {

    long countByNameAndType(String name, String type);

    List<CloudResource> findByNameAndType(String name, String type);

    List<CloudResource> findByType(String type);
}
