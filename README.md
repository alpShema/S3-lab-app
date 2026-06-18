# ecs-lab-app

Java Spring Boot application for the ECS CI/CD lab.
Containerised and deployed to Amazon ECS Fargate via GitHub Actions and CodeDeploy blue/green.

## Repository structure

```
ecs-lab-app/
├── src/
│   └── main/
│       ├── java/com/ecslab/
│       │   ├── EcsLabApplication.java   # Spring Boot entry point
│       │   └── LabController.java       # Serves / and /health endpoints
│       └── resources/
│           ├── static/index.html        # UI — shows your name and lab name
│           └── application.properties   # Port 8080, actuator config
├── .github/
│   └── workflows/
│       └── build-push.yml              # CI/CD workflow — build, push, upload artifacts
├── Dockerfile                          # Multi-stage build (Maven → JRE)
├── appspec.yaml                        # CodeDeploy blue/green deployment spec
└── pom.xml                             # Spring Boot 3 + Java 21 dependencies
```

## How it works

Every push to `main` triggers the GitHub Actions workflow which:

1. Authenticates to AWS using **OIDC** — no long-lived secrets stored in GitHub
2. Builds the Docker image using a multi-stage Dockerfile
3. Pushes the image to Amazon ECR with an immutable git SHA tag
4. Uploads `imagedefinitions.json` and `appspec.yaml` to S3

From there, EventBridge detects the ECR image push and triggers CodePipeline, which runs a CodeDeploy blue/green deployment to ECS Fargate automatically.

## Required GitHub secrets

Set these under **Settings → Secrets and variables → Actions** in this repo:

| Secret | Where to get it |
|--------|----------------|
| `AWS_ROLE_ARN` | CloudFormation → `ecs-lab-pipeline` stack → Outputs → `GitHubActionsRoleArn` |
| `PIPELINE_ARTIFACT_BUCKET` | CloudFormation → `ecs-lab-pipeline` stack → Outputs → `ArtifactBucketName` |

## Local development

```bash
# Build and run locally
mvn spring-boot:run

# App available at
http://localhost:8080

# Build the Docker image locally
docker build -t ecs-lab-app .
docker run -p 8080:8080 ecs-lab-app
```

## Infrastructure

All AWS infrastructure is managed separately in the **ecs-lab-infra** repo via CloudFormation GitSync.
