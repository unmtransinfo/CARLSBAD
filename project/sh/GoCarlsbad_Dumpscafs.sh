#!/bin/sh
#
#
set -e
#
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
scaffile=data/carlsbad_scafs_all_${DB}.smi
#
#
psql -h $PGHOST -p $PGPORT -U jjyang -q -A -t -F ' ' -d $DB <<__EOF__ >$scaffile
SELECT
	smiles,
	id
FROM
	scaffold
ORDER BY
	id
	;
__EOF__
#
wc -l $scaffile
#
