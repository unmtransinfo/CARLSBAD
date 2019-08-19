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
* [`unm_biocomp_cytoscape`](https://github.com/unmtransinfo/unm_biocomp_cytoscape), [`unm_biocomp_kegg`](https://github.com/unmtransinfo/unm_biocomp_kegg), [`unm_biocomp_smarts`](https://github.com/unmtransinfo/unm_biocomp), [`unm_biocomp_depict`](https://github.com/unmtransinfo/unm_biocomp_depict), [`unm_biocomp_util`](https://github.com/unmtransinfo/unm_biocomp_util)
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
```

## Etc.

```
mvn javadoc:javadoc
```

## Docker

In accordance with the guideline ___one service per container___, CARLSBAD is organized
into `carlsbad_db` and `carlsbad_ui` from separate
[Dockerfile\_Db](Dockerfile_Db) and [Dockerfile\_UI](Dockerfile_UI).
These containers communicate via
[user-defined bridge network](https://docs.docker.com/network/bridge/).

[Dockerfile\_Db](Dockerfile_Db) currently takes about 1h:45m, mostly to load the 
database.
