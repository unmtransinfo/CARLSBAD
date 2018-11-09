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
LIBDIR=/home/app/lib
#LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_kegg.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_rest.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_http.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
CLASSPATH=$CLASSPATH:$APPDIR/ChemAxon/JChemSuite/lib/jchem.jar
LIBDIR=$APPDIR/lib
CLASSPATH=$CLASSPATH:$LIBDIR/berkeleydb.jar
CLASSPATH=$CLASSPATH:$LIBDIR/httpclient-4.2.3.jar
CLASSPATH=$CLASSPATH:$LIBDIR/httpcore-4.2.2.jar
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.carlsbadone_app $*
#
