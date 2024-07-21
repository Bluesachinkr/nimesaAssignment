package com.project.Nimesa.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class S3Object {

    @Id
    @Column(name = "object_key")  // Renamed from 'key' to 'object_key'
    private String objectKey;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "region")
    private String region;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
