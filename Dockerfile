# STAGE 1 - BUILD
FROM openjdk:8 as build
ARG version=latest
ARG nextVersion=latest
WORKDIR /workspace
# Install chrome for puppeteer tests
# (see https://github.com/puppeteer/puppeteer/blob/master/docs/troubleshooting.md#running-puppeteer-in-docker)
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-unstable fonts-ipafont-gothic fonts-wqy-zenhei fonts-thai-tlwg fonts-kacst fonts-freefont-ttf --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
# Download/cache gradle + npm dependencies
# (by copying over only the build scripts and attempting to run a build)
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY ui/package.json ui/package-lock.json ./ui/
RUN ./gradlew resolveDependencies npmInstall
# Run gradle build
COPY . .
RUN echo "$version"
RUN if [ "$version" = "latest" ]; then \
      ./gradlew build --info; \
    else \
      ./gradlew release --info -Prelease.releaseVersion=$version -Prelease.newVersion=$nextVersion -Prelease.useAutomaticVersion=true; \
    fi

# STAGE 2 - ARTIFACTS
FROM scratch as artifacts
COPY --from=build /workspace/build /

# STAGE 3 - RUN
FROM openjdk:8-jre
WORKDIR /app
COPY --from=build workspace/build/libs/bcl-ndelius-um*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK CMD wget --quiet --tries=1 --spider http://localhost:8080/umt/actuator/health
ENV PROFILE=default
CMD ["java","-jar","/app/app.jar","--spring.profiles.active=${PROFILE}"]
