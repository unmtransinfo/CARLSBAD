#!/bin/sh
#
set -x
#
#LIBDIR=/home/app/lib
LIBDIR=$HOME/dev/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
CLASSPATH=$CLASSPATH:/usr/share/java/berkeleydb.jar
#
. cb_current_db.sh
echo "DBNAME = \"$DBNAME\""
#
#
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-vv \
	-cpd_query 'c1ccc(cc1)CNc2c(=O)[#7]c(cn2)c3ccccc3' -substruct \
	-o data/z.xgmml
#
# -cpd_query 'CC(c1ccccc1)Nc2c(=O)n(c(c(n2)Cl)c3ccc(c(c3)OC)OC)c4ccccc4' \
#
