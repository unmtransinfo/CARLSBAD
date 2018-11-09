#!/bin/sh
##############################################################################
# GoHierscaf_carlsbad.sh
# 
# Jeremy J Yang
##############################################################################
#
HOST=`hostname |sed -e 's/\..*$//'`
#
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_hscaf.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_db.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
LIBDIR=/home/app/lib
CLASSPATH=$CLASSPATH:$LIBDIR/jchem.jar
CLASSPATH=$CLASSPATH:$LIBDIR/berkeleydb.jar
CLASSPATH=$CLASSPATH:$LIBDIR/postgresql.jdbc3.jar
#
if [ "$HOST" = "habanero" ]; then
	JAVA=java
	JAVA_OPTS='-Xmx22G -Xms22G'
else
	JAVA=java
	JAVA_OPTS=''
fi
#
#
#DB=cb1
DB=cbdev
#
IFILE=data/carlsbad_all_${DB}.smi
#
PREFIX=data/carlsbad_all_${DB}_hscaf
OFILE=${PREFIX}_out.smi
OFILE_SCAF=${PREFIX}_scaf.smi
#
DBHOST="localhost"
DBPORT="5432"
DBNAME="hscafscratch"
DBSCHEMA="public"
#DBTABLEPREFIX="cb"
DBTABLEPREFIX=""
DBUSER="jjyang"
DBPW="assword"
#
if [ ! -e "$IFILE" ]; then
	echo "ERROR: $IFILE not found."
	exit
fi
#
echo "HOSTNAME: $HOST"
date
#
NMOL=`cat $IFILE |wc -l`
#
echo "NMOL($IFILE) = $NMOL"
#
# application options:
opts="-v"
opts="$opts -maxmol 100"
opts="$opts -maxrings 5"
opts="$opts -inc_mol"
opts="$opts -scaflist_append2title"
opts="$opts -i $IFILE"
opts="$opts -o $OFILE"
opts="$opts -out_scaf $OFILE_SCAF"
opts="$opts -rdb"
opts="$opts -rdb_host $DBHOST"
opts="$opts -rdb_port $DBPORT"
opts="$opts -rdb_name $DBNAME"
opts="$opts -rdb_schema $DBSCHEMA"
if [ "$DBTABLEPREFIX" ]; then
	opts="$opts -rdb_tableprefix $DBTABLEPREFIX"
fi
opts="$opts -rdb_user $DBUSER"
opts="$opts -rdb_pw $DBPW"
opts="$opts -rdb_keep"
#
opts="$opts -rdb_predelete"
#
set -x
#
$JAVA $JAVA_OPTS \
	-classpath $CLASSPATH edu.unm.health.biocomp.hscaf.hier_scaffolds \
	$opts
#
date
#

