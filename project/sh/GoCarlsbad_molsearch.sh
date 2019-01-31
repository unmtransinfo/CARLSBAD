#!/bin/sh
#
set -e
#
PGHOST="habanero.health.unm.edu"
PGPORT=5432
PGSU="postgres"
DB="carlsbad"
SCHEMA="public"
#USR="jjyang"
USR="dbc"
#
if [ $# -lt 1 ]; then
	printf "ERROR: Syntax: %s SMARTSFILE\n" `basename $0`
	exit
fi
#
SMARTSFILE=$1
#
cmd="psql -h $PGHOST -p $PGPORT -U $USR $DB"
#
smarts=`cat $SMARTSFILE |head -1`
#
$cmd <<__EOF__
SELECT
	compound.id,
	compound.smiles
FROM
	compound
WHERE
	gnova.matches(compound.smiles,'${smarts}')
LIMIT 100
;
__EOF__
#
