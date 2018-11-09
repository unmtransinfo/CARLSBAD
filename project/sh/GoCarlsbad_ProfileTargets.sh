#!/bin/sh
#
#
DBHOST=carlsbad.health.unm.edu
#DBNAME=cb3
DBNAME=carlsbad
DBUSR=dbc
DBPW='chem!nfo'
TS=`date +'%y%m%d'`
#ofile_csv="data/profile_targets_${DBNAME}-${TS}.csv"
ofile_csv="data/profile_targets_${DBNAME}.csv"
ofile_htm="data/profile_targets_${DBNAME}.html"
#
set -x
#
time ./carlsbad_utils.py \
	--dbhost $DBHOST \
	--dbname $DBNAME \
	--dbusr $DBUSR \
	--dbpw "$DBPW" \
	--profile_targets \
	>$ofile_csv
#
csv_utils.py \
	--csv2html \
	--i $ofile_csv \
	--o $ofile_htm
#
