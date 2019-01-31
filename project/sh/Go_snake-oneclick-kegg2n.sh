#!/bin/sh
#
# KEGGID2Network (e.g. disease2network)
#
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_kegg.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_rest.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_db.jar
LIBDIR=/home/app/lib
CLASSPATH=$CLASSPATH:$LIBDIR/httpcore-4.2.2.jar
CLASSPATH=$CLASSPATH:$LIBDIR/httpclient-4.2.3.jar
#CLASSPATH=$CLASSPATH:$LIBDIR/org.restlet.jar
#CLASSPATH=$CLASSPATH:$LIBDIR/org.restlet.ext.json.jar
#CLASSPATH=$CLASSPATH:$LIBDIR/org.restlet.ext.xml.jar
CLASSPATH=$CLASSPATH:$LIBDIR/org.json.jar
CLASSPATH=$CLASSPATH:$LIBDIR/opencsv-2.3.jar
CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
CLASSPATH=$CLASSPATH:/usr/share/java/berkeleydb.jar
#
wd=`dirname $0`
. $wd/cb_current_db.sh
#
echo "DBNAME = \"$DBNAME\""
#
#
# Note KID="ds:H00056" is Alzheimer's disease
#KID="ds:H00056"
# Note KID="ds:H00031" is Breast cancer
#KID="ds:H00031"
# Note KID="ds:H00409" is Type II diabetes mellitus
KID="ds:H00409"
#
set -x
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.carlsbad_utils_oneclick \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-kid "$KID" \
	-o $wd/data/kid${KID}_oneclick.xgmml \
	-minweight 40
#
