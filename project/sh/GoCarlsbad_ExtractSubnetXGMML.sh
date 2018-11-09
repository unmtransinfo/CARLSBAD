#!/bin/sh
#
#LIBDIR=/home/app/lib
LIBDIR=$HOME/dev/java/lib
#
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_threads.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_db.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
#
CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
#
set -x
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.carlsbad_utils \
	-dbhost 'habanero.health.unm.edu' \
	-dbusr 'dbc' -dbpw 'chem!nfo' \
	-tids2xgmml '1015,2367,2368,2369' \
	-o data/test_subnet.xgmml \
	-v
#
