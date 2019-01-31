#!/bin/sh
#
#
LIBDIR=$HOME/src/java/lib
#CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$LIBDIR/unm_biocomp_cytoscape.jar
LIBDIR=/home/app/lib
#CLASSPATH=$CLASSPATH:$LIBDIR/opencsv-2.3.jar
#CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
CLASSPATH=$CLASSPATH:/home/app/Cytoscape_v2.8.2/cytoscape.jar
#
JAVA_OPTS="-Djava.awt.headless=true"
#
set -x
#
#
java \
	$JAVA_OPTS \
	-classpath $CLASSPATH edu.unm.health.biocomp.cytoscape.cytoscape_utils \
	-i ~/Download/snake_oneclick_CARLSBAD_SubNet_Ketorolac_20130416.xgmml \
	-describe
#
