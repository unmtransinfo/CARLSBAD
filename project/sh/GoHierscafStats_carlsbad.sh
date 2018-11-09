#!/bin/sh
##############################################################################
### 29 May 2013: Now uses edu.unm.health.biocomp.hscaf.hier_scaffolds_stats
### (formerly used hier_scaffolds_stats.py).
##############################################################################
#DB=cb2
DB=cbdev
#
if [ $# -gt 0 ]; then
	DB=$1
fi
#
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_hscaf.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
#
LIBDIR=/home/app/lib
CLASSPATH=$CLASSPATH:$LIBDIR/jchem.jar
CLASSPATH=$CLASSPATH:$LIBDIR/berkeleydb.jar
CLASSPATH=$CLASSPATH:$LIBDIR/postgresql.jdbc3.jar
#
JAVA=java
#
PREFIX=data/carlsbad_all_${DB}_hscaf
IFILE=${PREFIX}_out.smi
IFILE_SCAF=${PREFIX}_scaf.smi
OFILE_SCAF=${PREFIX}_scaf_stats.smi
#
#
set -x
# 
# --report_mol_lists_ids means CIDs are passed on.
#
$JAVA -classpath $CLASSPATH edu.unm.health.biocomp.hscaf.hier_scaffolds_stats \
	-v \
	-i ${IFILE} \
	-in_scaf ${IFILE_SCAF} \
	-out_scaf ${OFILE_SCAF} \
	-scaflist_append2title \
	-report_mol_lists_ids \
	-sort_by_frequency
#
# Remove scaftree string:
#
cat ${OFILE_SCAF} \
	| awk '{print $1 " " $2 " " $4 " " $5}' \
	> ${PREFIX}_scaf_stats_fixed.smi
#
