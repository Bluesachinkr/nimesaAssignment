package com.project.Nimesa.services;

import com.project.Nimesa.data.CloudResource;
import com.project.Nimesa.repository.CloudResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;

import java.util.concurrent.CompletableFuture;

@Service
public class AWSService {

    @Autowired
    CloudResourceRepository cloudResourceRepository;

    @Autowired
    Ec2Client ec2Client;

    @Autowired
    S3Client s3Client;

    @Async
    public CompletableFuture<Void> discoverEC2Instances() {
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
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> discoverS3Buckets() {
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
        return CompletableFuture.completedFuture(null);
    }

    // Method to get the bucket location
    private String getBucketLocation(String bucketName) {
        GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                .bucket(bucketName)
                .build();
        GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);
        return locationResponse.locationConstraintAsString() != null ? locationResponse.locationConstraintAsString() : "us-east-1";
    }
}
