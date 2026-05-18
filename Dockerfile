# Build Stage
FROM maven:3.8-openjdk-8-slim AS build
WORKDIR /home/app

# --- STEP 1: Install all dependency JARs from libs/ ---
# This includes:
#   - ChemAxon commercial libraries (*-19.3.0.jar)
#   - Pre-built unm_biocomp_* JARs from private repos (*-0.0.1-SNAPSHOT.jar)
# All sourced from habanero:/opt/tomcat/webapps/carlsbad/WEB-INF/lib/
# Note: libs/ is also covered by the later COPY . . but we copy it early here
# so Maven can install JARs before the main source copy (better layer caching).
COPY libs/ libs/

# Install ChemAxon JARs with explicit com.chemaxon coordinates
RUN for jar in libs/*.jar; do \
        filename=$(basename "$jar") && \
        case "$filename" in \
            unm_biocomp_*) \
                artifact=$(echo "$filename" | sed 's/-0.0.1-SNAPSHOT.jar//') && \
                echo "Installing UNM dependency: $artifact" && \
                mvn -q install:install-file \
                    -Dfile="$jar" \
                    -DgroupId=unmbiocomputing \
                    -DartifactId="$artifact" \
                    -Dversion=0.0.1-SNAPSHOT \
                    -Dpackaging=jar ;; \
            *-19.3.0.jar) \
                artifact=$(echo "$filename" | sed 's/-19.3.0.jar//') && \
                echo "Installing ChemAxon: $artifact" && \
                mvn -q install:install-file \
                    -Dfile="$jar" \
                    -DgroupId=com.chemaxon \
                    -DartifactId="$artifact" \
                    -Dversion=19.3.0 \
                    -Dpackaging=jar ;; \
            *) \
                echo "Skipping unknown JAR: $filename" ;; \
        esac; \
    done

# --- STEP 2: Clone and install open-source unm_biocomp_util ---
# (Only util is needed from GitHub; smarts, depict, text, kegg come from libs/)
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*
ENV GIT_TERMINAL_PROMPT=0
RUN git clone --depth 1 https://github.com/unmtransinfo/unm_biocomp_util.git && \
    cd unm_biocomp_util && \
    mvn install -DskipTests -B && \
    cd .. && \
    rm -rf unm_biocomp_util

# --- STEP 3: Build Main Project ---
COPY pom.xml .
COPY unm_biocomp_carlsbad/pom.xml unm_biocomp_carlsbad/
COPY carlsbad_war/pom.xml carlsbad_war/
COPY . .
RUN mvn package -DskipTests -B

# Run Stage
FROM tomcat:9-jdk8-openjdk-slim
WORKDIR /usr/local/tomcat
RUN apt-get update && apt-get install -y postgresql-client libfreetype6 fontconfig && rm -rf /var/lib/apt/lists/*
COPY --from=build /home/app/carlsbad_war/target/carlsbad_war-0.0.1-SNAPSHOT.war webapps/carlsbad.war
ENV DBHOST=db DBNAME=carlsbad DBUSR=batman DBPW=foobar DBPORT=5432
EXPOSE 8080
CMD ["catalina.sh", "run"]
