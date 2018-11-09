#!/bin/sh
#
#
set -e
#
#PGHOST="agave.health.unm.edu"
PGHOST="habanero.health.unm.edu"
PGPORT=5432
PGSU="postgres"
#DB="carlsbad"
#DB="cb2"
DB="cbdev"
SCHEMA="public"
#
if [ $# -gt 0 ]; then
	DB=$1
fi
#
smifile=data/carlsbad_all_${DB}.smi
#
#
psql -h $PGHOST -p $PGPORT -U jjyang -q -A -t -F ' ' -d $DB <<__EOF__ >$smifile
SELECT smiles,id from compound;
__EOF__
#
wc -l $smifile
#
