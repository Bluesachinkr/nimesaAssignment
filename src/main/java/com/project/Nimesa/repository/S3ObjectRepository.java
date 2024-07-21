package com.project.Nimesa.repository;

import com.project.Nimesa.data.S3Object;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface S3ObjectRepository extends JpaRepository<S3Object, String> {
    List<S3Object> findByBucketName(String bucketName);
}
