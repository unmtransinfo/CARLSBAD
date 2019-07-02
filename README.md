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
* `unm_biocomp_cytoscape`, `unm_biocomp_kegg`, `unm_biocomp_smarts`, `unm_biocomp_depict`, `unm_biocomp_db`, `unm_biocomp_util`
* ChemAxon JChem (19.3.0 ok)
* Access to [ChemAxon Maven repository](https://hub.chemaxon.com) (see [documentation](https://docs.chemaxon.com/display/docs/Public+Repository)).
  * Requires ChemAxon-Hub Artifactory credentials.


## Compiling

```
mvn clean install
```

## Deploying `CARLSBAD`

```
mvn --projects carlsbad_war tomcat7:deploy
```

or

```
mvn --projects carlsbad_war tomcat7:redeploy
```

## Usage (command-line)

```
mvn --projects unm_biocomp_carlsbad \
	exec:java -Dexec.mainClass="edu.unm.health.biocomp.carlsbad.carlsbadone_app" \
	-Dexec.args="-dbhost carlsbad.health.unm.edu -dbname carlsbad -dbusr batman -dbpw 'guano' -tid 17 -vv -o tid_17.cyjs"
```
