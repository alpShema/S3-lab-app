# S3-lab-app

Java Spring Boot photo gallery application deployed on Amazon ECS Fargate via GitHub Actions and CodeDeploy blue/green deployment.

## What the app does

- Displays uploaded photos with descriptions in a web gallery
- Allows users to upload new photos with descriptions
- Stores images in Amazon S3, served securely via CloudFront
- Stores metadata (description, upload time) in Amazon RDS PostgreSQL
- Supports photo deletion (removes from S3 and RDS)

## Repository structure

```
S3-lab-app/
├── src/
│   └── main/
│       ├── java/com/ecslab/
│       │   ├── EcsLabApplication.java        # Spring Boot entry point
│       │   ├── Photo.java                    # JPA entity — maps to photos table in RDS
│       │   ├── PhotoDto.java                 # Data transfer object for the view
│       │   ├── PhotoRepository.java          # Spring Data JPA — queries RDS
│       │   ├── PhotoController.java          # HTTP endpoints: gallery, upload, delete
│       │   ├── StorageService.java           # Interface: upload / getUrl / delete
│       │   ├── S3StorageService.java         # prod — stores files in S3, serves via CloudFront
│       │   └── LocalStorageService.java      # dev — stores files on local disk, no AWS needed
│       └── resources/
│           ├── templates/index.html          # Thymeleaf gallery UI
│           ├── application.properties        # Base config: port, JPA, multipart limits
│           ├── application-prod.properties   # prod: RDS + S3 + CloudFront (values from env vars)
│           └── application-dev.properties    # dev: H2 in-memory DB + local file storage
├── .github/
│   └── workflows/
│       └── build-push.yml                   # CI/CD: build image, push to ECR, trigger pipeline
├── Dockerfile                               # Multi-stage build (Maven → JRE 21)
├── appspec.yaml                             # CodeDeploy blue/green deployment spec
├── taskdef.json                             # ECS task definition template (placeholders substituted at deploy time)
└── pom.xml                                  # Spring Boot 3 + Java 21 + AWS SDK S3
```

## How it works

Every push to `main` that touches application files triggers the GitHub Actions workflow:

1. **OIDC authentication** — exchanges a GitHub JWT for temporary AWS credentials via IAM role, no long-lived secrets stored in GitHub
2. **Build and push** — builds the Docker image and pushes it to ECR with two tags: `:latest` and `:<git-sha>`
3. **Resolve infrastructure values** — reads CloudFormation stack outputs (RDS endpoint, CloudFront URL, secret ARN, role ARNs) and substitutes them into `taskdef.json` placeholders at runtime
4. **Upload artifacts** — uploads `taskdef.json`, `appspec.yaml`, and `imageDetail.json` bundled as `artifacts.zip` to the S3 artifact bucket
5. **Trigger pipeline** — explicitly starts the CodePipeline execution, which runs a CodeDeploy blue/green deployment to ECS Fargate

## Required GitHub variables

Set these under **Settings → Secrets and variables → Actions → Variables** in this repo:

| Variable | Where to get it |
|----------|----------------|
| `AWS_REGION` | `eu-central-1` |
| `ECR_REPOSITORY` | `s3-lab-app` |
| `AWS_ROLE_ARN` | CloudFormation → `s3-lab-main` stack → Outputs → `GitHubActionsRoleArn` |
| `PIPELINE_ARTIFACT_BUCKET` | CloudFormation → `s3-lab-main` stack → Outputs → `ArtifactBucketName` |

## Local development

```bash
# Run locally — uses H2 in-memory DB and local file storage, no AWS credentials needed
mvn spring-boot:run

# App available at
http://localhost:8080

# H2 database console (inspect tables)
http://localhost:8080/h2-console

# Build and run as Docker container locally
docker build -t s3-lab-app .
docker run -p 8080:8080 s3-lab-app
```

## Spring profiles

| Profile | Storage | Database | When used |
|---------|---------|----------|-----------|
| `dev` (default) | Local disk (`./uploads/`) | H2 in-memory | Local development |
| `prod` | Amazon S3 + CloudFront | RDS PostgreSQL | ECS Fargate in AWS |

## Infrastructure

All AWS infrastructure is managed in the **S3-lab-infra** repo via a single CloudFormation stack (`s3-lab-main`) deployed through CloudFormation GitSync.
