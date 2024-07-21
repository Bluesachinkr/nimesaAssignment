package com.project.Nimesa.services;

import com.project.Nimesa.data.CloudResource;
import com.project.Nimesa.data.Job;
import com.project.Nimesa.data.S3Object;
import com.project.Nimesa.repository.CloudResourceRepository;
import com.project.Nimesa.repository.JobRepository;
import com.project.Nimesa.repository.S3ObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AWSService {

    @Autowired
    CloudResourceRepository cloudResourceRepository;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    S3ObjectRepository s3ObjectRepository;

    @Autowired
    Ec2Client ec2Client;

    @Autowired
    S3Client s3Client;

    // Map to track the status of each job and each service
    private final ConcurrentHashMap<String, String> jobStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> jobServiceCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> jobServiceCompleted = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<Void> discoverEC2Instances(String jobId) {
        updateServiceCount(jobId, "EC2");
        try {
            DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances();
            for (Reservation reservation : describeInstancesResponse.reservations()) {
                for (Instance instance : reservation.instances()) {
                    CloudResource resource = new CloudResource();
                    resource.setType("EC2");
                    resource.setName(instance.instanceId());
                    resource.setRegion(instance.placement().availabilityZone());
                    cloudResourceRepository.save(resource);
                }
            }
            updateServiceStatus(jobId, "EC2", "Success");
        } catch (Exception e) {
            updateServiceStatus(jobId, "EC2", "Failed");
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> discoverS3Buckets(String jobId) {
        updateServiceCount(jobId, "S3");
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            for (Bucket bucket : response.buckets()) {
                String bucketName = bucket.name();
                String region = getBucketLocation(bucketName);
                CloudResource resource = new CloudResource();
                resource.setType("S3");
                resource.setName(bucketName);
                resource.setRegion(region);
                cloudResourceRepository.save(resource);
            }
            updateServiceStatus(jobId, "S3", "Success");
        } catch (Exception e) {
            updateServiceStatus(jobId, "S3", "Failed");
        }
        return CompletableFuture.completedFuture(null);
    }

    private void updateServiceCount(String jobId, String serviceName) {
        jobServiceCounts.put(jobId, jobServiceCounts.getOrDefault(jobId, 0) + 1);
        jobServiceCompleted.putIfAbsent(jobId, 0);
    }

    private void updateServiceStatus(String jobId, String serviceName, String status) {
        jobStatusMap.put(jobId + "-" + serviceName, status);
        int completed = jobServiceCompleted.getOrDefault(jobId, 0) + 1;
        jobServiceCompleted.put(jobId, completed);

        Job job = jobRepository.findById(jobId).orElse(new Job());
        job.setJobId(jobId);

        int totalServices = jobServiceCounts.getOrDefault(jobId, 0);
        if (completed == totalServices) {
            String finalStatus = jobStatusMap.values().contains("Failed") ? "Failed" : "Success";
            job.setStatus(finalStatus);
        } else {
            job.setStatus("In Progress");
        }
        job.setTimestamp(LocalDateTime.now());
        jobRepository.save(job);
    }

    private String getBucketLocation(String bucketName) {
        GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                .bucket(bucketName)
                .build();
        GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);
        return locationResponse.locationConstraintAsString() != null ? locationResponse.locationConstraintAsString() : "us-east-1";
    }

    public String getJobStatus(String jobId) {
        return jobRepository.findById(jobId).map(Job::getStatus).orElse("Not Found");
    }

    public List<String> getDiscoveryResult(String serviceName) {
        if ("S3".equalsIgnoreCase(serviceName)) {
            return cloudResourceRepository.findAll().stream()
                    .filter(resource -> "S3".equalsIgnoreCase(resource.getType()))
                    .map(CloudResource::getName) // Assuming getName() returns the bucket name
                    .distinct()
                    .collect(Collectors.toList());
        } else if ("EC2".equalsIgnoreCase(serviceName)) {
            return cloudResourceRepository.findAll().stream()
                    .filter(resource -> "EC2".equalsIgnoreCase(resource.getType()))
                    .map(CloudResource::getName) // Assuming getName() returns the instance ID
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }
    }



    @Async
    public CompletableFuture<String> getS3BucketObjects(String bucketName) {
        String jobId = UUID.randomUUID().toString();
        updateServiceCount(jobId, "S3 Objects");
        try {
            ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjects);
            listObjectsResponse.contents().forEach(s3Object -> {
                S3Object s3Obj = new S3Object();
                s3Obj.setObjectKey(s3Object.key());
                s3Obj.setBucketName(bucketName);
                s3Obj.setRegion("ap-south-1"); // Example region
                s3ObjectRepository.save(s3Obj);
            });
            updateServiceStatus(jobId, "S3 Objects", "Success");
        } catch (Exception e) {
            updateServiceStatus(jobId, "S3 Objects", "Failed");
        }
        return CompletableFuture.completedFuture(jobId);
    }

    public long getS3BucketObjectCount(String bucketName) {
        return s3ObjectRepository.count();
    }

    public List<String> getS3BucketObjectlike(String bucketName, String pattern) {
        Pattern regexPattern = Pattern.compile(pattern);
        List<S3Object> objects = s3ObjectRepository.findByBucketName(bucketName);
        // Filter objects based on the regex pattern and collect their keys
        List<String> matchingKeys = objects.stream()
                .map(S3Object::getObjectKey)
                .filter(key -> regexPattern.matcher(key).find())
                .collect(Collectors.toList());

        return matchingKeys;
    }

}
