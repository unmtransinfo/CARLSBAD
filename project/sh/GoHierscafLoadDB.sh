#!/bin/sh
#############################################################################
# gNova Chord version.
#
# Jeremy J Yang
# 29 May 2013
#############################################################################
#set -e
set -x
#
#DBHOST="habanero.health.unm.edu"
DBHOST="carlsbad.health.unm.edu"
DBPORT=5432
DBUSR="jjyang"
#DB="carlsbad"
DB="cbdev"
SCHEMA="public"
#
if [ $# -gt 0 ]; then
	DB=$1
fi
#
PREFIX=data/carlsbad_all_${DB}_hscaf
#
date
#
### This is the -out_scafs file from hier_scaffolds_stats.py.  The fields are:
### <SMILES> <ID> <NMOL>;<NTOTAL> <MOLLIST>
### --report_mol_lists_names means MOLLIST contains CIDs.
###
IFILE=${PREFIX}_scaf_stats_fixed.smi
#
./scafstats_2inserts.py \
	$SCHEMA \
	$IFILE \
	${PREFIX}_scaf_inserts.sql \
	${PREFIX}_scafid2cid_inserts.sql
#
cmd="psql -q -h $DBHOST -p $DBPORT -U $DBUSR $DB"
echo $cmd
#
#exit	#DEBUG
#
$cmd < ${PREFIX}_scaf_inserts.sql
#
$cmd < ${PREFIX}_scafid2cid_inserts.sql
#
$cmd <<__EOF__
UPDATE $SCHEMA.db_properties SET hiers_date_run = CURRENT_TIMESTAMP ;
__EOF__
#
date
#
