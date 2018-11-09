#!/bin/sh
#
DBHOST="habanero.health.unm.edu"
DBPORT=5432
DBNAME="carlsbad"
DBUSR="jjyang"
DBPW="assword"
#
date
#
time carlsbad_query.py \
        --vv \
        --kegg_disease_links \
	--dbhost $DBHOST \
	--dbname $DBNAME \
	--dbusr $DBUSR \
	--dbpw $DBPW \
	--o data/kegg2carlsbad.csv
#
date
#
