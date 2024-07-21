package com.project.Nimesa.controllers;

import com.project.Nimesa.services.AWSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String jobId = UUID.randomUUID().toString();
        CompletableFuture<Void> ec2Future = null;
        CompletableFuture<Void> s3Future = null;

        if (services.contains("EC2")) {
            ec2Future = awsService.discoverEC2Instances(jobId);
        }

        if (services.contains("S3")) {
            s3Future = awsService.discoverS3Buckets(jobId);
        }

        CompletableFuture.allOf(ec2Future, s3Future).join();
        return ResponseEntity.ok("JobId: " + jobId);
    }

    @GetMapping("/job/result")
    public ResponseEntity<String> getJobResult(@RequestParam String jobId) {
        String status = awsService.getJobStatus(jobId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/discovery/result")
    public ResponseEntity<List<String>> getDiscoveryResult(@RequestParam String serviceName) {
        List<String> results = awsService.getDiscoveryResult(serviceName);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/bucket/objects")
    public CompletableFuture<ResponseEntity<String>> getS3BucketObjects(@RequestParam String bucketName) {
        return awsService.getS3BucketObjects(bucketName)
                .thenApply(jobId -> ResponseEntity.ok("JobId: " + jobId));
    }

    @GetMapping("/bucket/object-count")
    public ResponseEntity<Long> getS3BucketObjectCount(@RequestParam String bucketName) {
        long count = awsService.getS3BucketObjectCount(bucketName);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/bucket/object-like")
    public ResponseEntity<List<String>> getS3BucketObjectlike(@RequestParam String bucketName, @RequestParam String pattern) {
        List<String> matchingObjects = awsService.getS3BucketObjectlike(bucketName, pattern);
        return ResponseEntity.ok(matchingObjects);
    }
}
