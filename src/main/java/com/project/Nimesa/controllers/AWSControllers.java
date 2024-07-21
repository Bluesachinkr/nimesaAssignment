package com.project.Nimesa.controllers;

import com.project.Nimesa.services.AWSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/aws")
public class AWSControllers {

    @Autowired
    AWSService awsService;

    @PostMapping("/discover")
    public ResponseEntity<String> discoverServices(@RequestBody List<String> services) {
        CompletableFuture<Void> ec2Future = null;
        CompletableFuture<Void> s3Future = null;

        if (services.contains("EC2")) {
            ec2Future = awsService.discoverEC2Instances();
        }

        if (services.contains("S3")) {
            s3Future = awsService.discoverS3Buckets();
        }

        CompletableFuture.allOf(ec2Future, s3Future).join();
        return ResponseEntity.ok("JobId: " + UUID.randomUUID().toString());
    }
}
