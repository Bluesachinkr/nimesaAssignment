package com.project.Nimesa.repository;

import com.project.Nimesa.data.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {
    Job findByJobId(String jobId);
}
