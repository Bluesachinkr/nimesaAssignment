# Nimesa Project

## Overview

The Nimesa project is a Spring Boot application designed to interact with AWS EC2 and S3 services. It provides functionality for discovering resources, managing S3 objects, and querying data from a database.

## Features

- Discover AWS EC2 instances and S3 buckets.
- Fetch and manage S3 objects based on patterns.
- Query and manage data stored in the database.

## Prerequisites

- Java 17+
- Spring Boot
- Maven or Gradle
- H2/MySQL/PostgreSQL database

Configuration
===============

**AWS Credentials**
----------------

Update the AWS credentials in your environment variables:

### Unix/Linux/MacOS

```bash
export AWS_ACCESS_KEY_ID=your_access_key_id
export AWS_SECRET_ACCESS_KEY=your_secret_access_key
export AWS_REGION=your_aws_region
```

### Windows
```
set AWS_ACCESS_KEY_ID=your_access_key_id
set AWS_SECRET_ACCESS_KEY=your_secret_access_key
set AWS_REGION=your_aws_region
```
Replace your_access_key_id, your_secret_access_key, and your_aws_region with your actual AWS credentials and region.


## Database Configuration

Configure your database connection in `src/main/resources/application.properties`:

```properties
# H2 Database Configuration (In-Memory)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# For MySQL or PostgreSQL, replace the above configuration with the appropriate values:
# spring.datasource.url=jdbc:mysql://<host>:<port>/<database>
# spring.datasource.username=<username>
# spring.datasource.password=<password>
# spring.jpa.database-platform=org.hibernate.dialect.MySQL5Dialect
# OR
# spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

Database Schema
=====================

The database schema consists of three tables: `s3object`, `cloud_resource`, and `job`.

### s3object Table
```
CREATE TABLE s3object ( bucket_name VARCHAR(255), object_key VARCHAR(255) NOT NULL, region VARCHAR(255), PRIMARY KEY (object_key) );
```

### cloud_resource Table
```
CREATE TABLE cloud_resource ( id UUID PRIMARY KEY, type VARCHAR(255), name VARCHAR(255), region VARCHAR(255) );
```
### cloud_resource Table

```
CREATE TABLE job ( job_id VARCHAR(255) PRIMARY KEY, status VARCHAR(255), timestamp TIMESTAMP );
```


**API Endpoints**
===============

The API provides the following endpoints:

### Get S3 Buckets

* **URL:** `/s3/buckets`
* **Method:** `GET`
* **Response:** List of S3 bucket names.

### Get EC2 Instances

* **URL:** `/ec2/instances`
* **Method:** `GET`
* **Response:** List of EC2 instance IDs.

### Get S3 Bucket Objects

* **URL:** `/s3/bucket/objects`
* **Method:** `GET`
* **Parameters:**
   + `bucketName`: The name of the S3 bucket.
* **Response:** List of object keys in the specified bucket.

### Get S3 Bucket Objects by Pattern

* **URL:** `/s3/bucket/object-like`
* **Method:** `GET`
* **Parameters:**
   + `bucketName`: The name of the S3 bucket.
   + `pattern`: The regex pattern to match object keys.
* **Response:** List of object keys matching the pattern.

**Running the Application**
==========================

To run the application, follow these steps:

1. Clone the repository: ```git clone <repository-url> cd <project-directory>```
2. Build the project: ```./mvnw clean install```
3. Run the application: ```./mvnw spring-boot:run```

