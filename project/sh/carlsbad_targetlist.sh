#!/bin/sh
#
if [ "`uname -s`" = "Darwin" ]; then
	APPDIR="/Users/app"
elif [ "`uname -s`" = "Linux" ]; then
	APPDIR="/home/app"
else
	APPDIR="/home/app"
fi
#
#LIBDIR=/home/app/lib
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_http.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_db.jar
#CLASSPATH=$CLASSPATH:$APPDIR/ChemAxon/JChem/lib/jchem.jar
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.TargetList $*
#
