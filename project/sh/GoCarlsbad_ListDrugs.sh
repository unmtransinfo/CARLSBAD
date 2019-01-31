#!/bin/sh
#
#
DBHOST=carlsbad.health.unm.edu
#DBNAME=cb3
DBNAME=carlsbad
DBUSR=dbc
DBPW='chem!nfo'
TS=`date +'%y%m%d'`
#ofile_csv="data/drugs_${DBNAME}-${TS}.csv"
ofile_csv="data/drugs_${DBNAME}.csv"
ofile_cid="data/drugs_${DBNAME}.cid"
ofile_htm="data/drugs_${DBNAME}.html"
#
set -x
#
time ./carlsbad_utils.py \
	--dbhost $DBHOST \
	--dbname $DBNAME \
	--dbusr $DBUSR \
	--dbpw "$DBPW" \
	--druglist \
	>$ofile_csv
#
csv_utils.py \
	--csv2html \
	--i $ofile_csv \
	--o $ofile_htm
#
# Create file of unique CB CIDs:
#
cat $ofile_csv \
	| sed -e '1d' \
	| awk -F ',' '{print $1}' \
	|sort -nu \
	>$ofile_cid
#
