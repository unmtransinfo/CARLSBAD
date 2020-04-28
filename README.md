# CARLSBAD <img align="right" src="/project/doc/images/carlsbad_logo.png" height="120">

Code for the CarlsbadOne web app and related libs and tools.

See <http://carlsbad.health.unm.edu> for more information.

* The CARLSBAD Database: A Confederated Database of Chemical Bioactivities, S. L.
Mathias, J. Hines-Kay, J. J. Yang, G. Zahoransky-Kohalmi, C. G. Bologa, O. Ursu and
T. I. Oprea, Database, 2013, bat044, DOI: 10.1093/database/bat044,
<http://database.oxfordjournals.org/content/2013/bat044>.

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

Based on Ubuntu 18.04-LTS.
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
