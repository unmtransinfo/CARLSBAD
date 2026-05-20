# CARLSBAD <img align="right" src="/project/doc/images/carlsbad_logo.png" height="120">

Code for the CarlsbadOne web app and related libs and tools.

See <https://datascience.unm.edu/carlsbad/> for more information.

* [The CARLSBAD Database: A Confederated Database of Chemical Bioactivities](https://database.oxfordjournals.org/content/2013/bat044), S. L.  Mathias, J. Hines-Kay, J. J. Yang, G. Zahoransky-Kohalmi, C. G. Bologa, O. Ursu and T. I. Oprea, Database, 2013, 10.1093/database/bat044.

## Dependencies
* Java 8
* Maven 3.5+
* [`unm_biocomp_util`](https://github.com/unmtransinfo/unm_biocomp_util),
[`unm_biocomp_text`](https://github.com/unmtransinfo/unm_biocomp_text),
[`unm_biocomp_smarts`](https://github.com/unmtransinfo/unm_biocomp_smarts),
[`unm_biocomp_depict`](https://github.com/unmtransinfo/unm_biocomp_depict),
[`unm_biocomp_cytoscape`](https://github.com/unmtransinfo/unm_biocomp_cytoscape),
[`unm_biocomp_kegg`](https://github.com/unmtransinfo/unm_biocomp_kegg)
* ChemAxon JChem (19.3.0 ok)
* Access to [ChemAxon Maven repository](https://hub.chemaxon.com) (see [documentation](https://docs.chemaxon.com/display/docs/Public+Repository)).
  * Requires ChemAxon-Hub Artifactory credentials.


## Developer Setup / Quick Start

### 1. Set Up Proprietary Dependencies (`libs/`)
Because proprietary ChemAxon and UNM SNAPSHOT `.jar` dependency files are not tracked in Git, the `libs/` folder must be populated manually before compiling or building images:
* Secure the `libs/` folder containing the required `.jar` files (e.g., copied from a secure team share or backup) and place it directly in the root of your local workspace.
* Alternatively, configure your local `~/.m2/settings.xml` with ChemAxon Hub Artifactory credentials to download dependencies.

### 2. Start Development Server (Containerized - Easiest)
This is the recommended and fastest way to start the development environment:
1. Ensure **Docker Desktop** (or the Docker daemon) is running.
2. In the repository root, start the containers in development mode:
   ```bash
   ./sh/deploy.sh dev
   ```
   This will spin up both the **CARLSBAD Database** and the **Web UI** containers.
3. Access the Web UI in your browser at:
   👉 [http://localhost:8080/carlsbad/carlsbadone](http://localhost:8080/carlsbad/carlsbadone)
4. Press `Ctrl + C` in the terminal to gracefully stop the containers.

---


## Compiling

```
mvn clean install
```

## Testing with Jetty

<http://localhost:8081/carlsbad/carlsbadone>

```
mvn --projects carlsbad_war jetty:run
```

## Deploying `CARLSBAD`

```
mvn --projects carlsbad_war tomcat7:deploy
```

or

```
mvn --projects carlsbad_war tomcat7:redeploy
```

## Containerized Deployment (Recommended)

A unified Docker Compose orchestration system is provided to spin up both the **CARLSBAD Database** and **Web UI** containers automatically. This is the fastest and most reliable way to run the application locally.

### Orchestration Command

Run the deployment script located in the `sh/` directory with the desired mode:

```bash
# Start in DEVELOPMENT mode (interactive logs, remote debugging, direct DB access)
./sh/deploy.sh dev

# Start in PRODUCTION mode (detached background, secure)
./sh/deploy.sh prod
```

> [!IMPORTANT]
> **Local Dependency Directory (`libs/`):**
> To build the Docker images locally, the `libs/` directory **must** be present in your local workspace containing the proprietary ChemAxon and UNM SNAPSHOT `.jar` dependency files. 
> Because these files are very large, the `libs/` directory is untracked via `.gitignore` to keep GitHub pushes lightweight and fast. **Do not delete `libs/` from your local workspace.**


### Environment Overview

#### Development Mode (`dev`)
* **Web UI:** Accessible on the host at [http://localhost:8080/carlsbad/carlsbadone](http://localhost:8080/carlsbad/carlsbadone).
* **Remote Debugging:** Port `8000` is exposed on the host with JPDA transport enabled (`-agentlib:jdwp`), allowing you to attach an IDE debugger to the servlets.
* **Database Connection:** Port `5432` is exposed on the host for direct administration via external database clients (e.g., DBeaver, pgAdmin, or local `psql`).
* **Logs & Lifecycle:** Runs in the foreground, showing live interleaved container outputs. Press `Ctrl + C` to gracefully stop.

#### Production Mode (`prod`)
* **Detached Execution:** Runs in the background (detached `-d` mode).
* **Port Mapping:** Exposes port `8080` for the Web UI.
* **Hardened Security:** Remote debugging (port `8000`) and external database ports (port `5432`) are kept internal to the Docker bridge network to protect the environment.


## Db configuration

```
sudo -u postgres createdb carlsbad
sudo -u postgres psql -d carlsbad -c "CREATE ROLE batman WITH LOGIN PASSWORD 'foobar'"
CREATE ROLE
sudo -u postgres psql -d carlsbad -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO batman"
ALTER DEFAULT PRIVILEGES
sudo -u postgres psql -d carlsbad -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO batman"
ALTER DEFAULT PRIVILEGES
sudo -u postgres psql -d carlsbad -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO batman"
gunzip -c carlsbad-pgdump.sql.gz |sudo -u postgres psql -d carlsbad
```

## Usage (command-line)

```
mvn --projects unm_biocomp_carlsbad exec:java -Dexec.mainClass="edu.unm.health.biocomp.carlsbad.carlsbadone_app" -Dexec.args="-dbhost localhost -dbname carlsbad -dbusr batman -dbpw 'foobar' -cid 5442 -v -o cid_5442.cyjs"
mvn --projects unm_biocomp_carlsbad exec:java -Dexec.mainClass="edu.unm.health.biocomp.carlsbad.carlsbadone_app" -Dexec.args="-dbhost localhost -dbname carlsbad -dbusr batman -dbpw 'foobar' -tid 17 -v -o tid_17.cyjs"
java -jar
unm_biocomp_carlsbad/target/unm_biocomp_carlsbad-0.0.1-SNAPSHOT-jar-with-dependencies.jar -dbhost localhost -dbname carlsbad -dbusr batman -dbpw 'foobar' -tid 1855 -v -o tid_1855.cyjs
```

## Etc.

```
mvn javadoc:javadoc
```

## Docker

* [DockerHub:carlsbad_db](https://hub.docker.com/repository/docker/unmtransinfo/carlsbad_db)
* [DockerHub:carlsbad_ui](https://hub.docker.com/repository/docker/unmtransinfo/carlsbad_ui)

Based on Ubuntu 20.04-LTS.
In accordance with the guideline ___one service per container___, CARLSBAD is organized
into images `carlsbad_db` and `carlsbad_ui`, built from separate
[Dockerfile\_Db](Dockerfile_Db) and [Dockerfile\_UI](Dockerfile_UI).
The running containers communicate via
[user-defined bridge network](https://docs.docker.com/network/bridge/), which
allows private communication between the containers, via container names
as hostnames: `carlsbad_db_container` and `carlsbad_ui_container`.

From the Docker engine host, the application is accessible at 
<http://localhost:9091/carlsbad/carlsbadone>.

[Dockerfile\_Db](Dockerfile_Db) currently takes about 1h:45m, mostly to load the 
database.

See:

* [Dockerfile\_Db](DockerFile_Db)
* [Dockerfile\_UI](DockerFile_UI)
* [Go\_DockerBuild\_Db.sh](sh/Go_DockerBuild_Db.sh)
* [Go\_DockerBuild\_UI.sh](sh/Go_DockerBuild_UI.sh)
* [Go\_DockerRun.sh](sh/Go_DockerRun.sh)
* [Go\_DockerNetwork.sh](sh/Go_DockerNetwork.sh)
* [Go\_DockerClean.sh](sh/Go_DockerClean.sh)

Docker\_UI container may be served via Apache-proxy configured thus:

```
ProxyPass /carlsbad http://localhost:9091/carlsbad
ProxyPassReverse /carlsbad http://localhost:9091/carlsbad
```

## GitHub Actions CI/CD

An automated GitHub Actions workflow is set up to build and push the `carlsbad_ui` Docker image to Docker Hub whenever changes are merged into the `master` branch.

The workflow is defined in [.github/workflows/docker-publish.yml](file:///Users/bivek/Desktop/CARLSBAD/.github/workflows/docker-publish.yml).

### Required GitHub Secrets

To allow the workflow to successfully build the Docker image and push to Docker Hub, you must configure the following **Repository Secrets** in your GitHub repository (`Settings > Secrets and variables > Actions > Repository secrets`):

#### 1. Docker Hub Credentials
* `DOCKERHUB_USERNAME`: Your Docker Hub username or organization name.
* `DOCKERHUB_TOKEN`: A Personal Access Token (PAT) created under your Docker Hub Account settings.

#### 2. Proprietary Dependencies (`libs/`) (One of the following is required)
Because the `libs/` directory containing ChemAxon and UNM SNAPSHOT libraries is untracked by Git, you must supply it in the CI runner:

* **Option A (Recommended - Base64 Secret):**
  Compress your local `libs/` directory into a tarball, base64-encode it, and save the resulting string as `LIBS_TGZ_BASE64` in GitHub Secrets.
  
  You can generate this base64 string using:
  ```bash
  tar -czf - libs/ | base64 | pbcopy
  ```
  *(Paste the copied string directly into the secret value)*

* **Option B (Secure Download URL):**
  Upload a compressed archive of `libs/` to a secure/private cloud storage bucket (AWS S3, Google Cloud Storage, etc.) and save the secure signed URL as a secret named `LIBS_DOWNLOAD_URL`.

