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
#KEGG_DISEASE_SKIP=894
#
time carlsbad_query.py \
        --kegg_disease_links \
	--dbhost $DBHOST \
	--dbname $DBNAME \
	--dbusr $DBUSR \
	--dbpw $DBPW \
	--o data/kegg2carlsbad.csv
#
#	--vv \
#	--kegg_disease_skip $KEGG_DISEASE_SKIP \
#	--o data/kegg2carlsbad_${KEGG_DISEASE_SKIP}.csv
#
date
#
