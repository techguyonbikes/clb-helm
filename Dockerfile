FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY modules/ ./modules/
RUN mvn clean install

FROM adoptopenjdk/openjdk11:alpine-jre
ARG BUILD_DATE
ARG GIT_FULL_BRANCH
ARG SHORT_COMMIT_HASH

LABEL build_date=$BUILD_DATE
LABEL git_branch=$GIT_FULL_BRANCH
LABEL git_short_commit_hash=$SHORT_COMMIT_HASH

# Create a group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

#Run as non root user
USER appuser

WORKDIR /app

RUN pwd
COPY --from=build /app/modules/clb-api/target/*.jar /app/

ENTRYPOINT java -jar *.jar