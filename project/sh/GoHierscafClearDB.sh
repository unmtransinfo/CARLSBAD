#!/bin/sh
#
#PGHOST="agave.health.unm.edu"
PGHOST="carlsbad.health.unm.edu"
PGPORT=5432
PGSU="postgres"
#DB="carlsbad"
DB="cbdev"
SCHEMA="public"
#
cmd="psql -h $PGHOST -p $PGPORT -U jjyang $DB"
#echo $cmd
#
$cmd <<__EOF__
--
UPDATE $SCHEMA.db_properties SET hiers_date_run = NULL ;
--
DELETE FROM $SCHEMA.scafid2cid ;
--
DELETE FROM $SCHEMA.scaffold ;
--
__EOF__
#
