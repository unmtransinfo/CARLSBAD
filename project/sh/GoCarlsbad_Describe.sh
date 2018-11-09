#!/bin/sh
#
set -e
#
#PGHOST="agave.health.unm.edu"
PGHOST="habanero.health.unm.edu"
PGPORT=5432
PGSU="postgres"
DB="carlsbad"
#DB="cb1"
SCHEMA="public"
#USR="jjyang"
USR="dbc"
#
if [ $# -gt 0 ]; then
	DB=$1
fi
#
#
cmd="psql -h $PGHOST -p $PGPORT -U $USR $DB"
echo $cmd
$cmd <<__EOF__
SELECT table_name FROM information_schema.tables WHERE table_schema='$SCHEMA';
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='scaffold';
SELECT count(*) AS scaffold_count FROM $SCHEMA.scaffold;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='scafid2cid';
SELECT count(*) AS scafid2cid_count FROM $SCHEMA.scafid2cid;
SELECT count(*) AS cpd_count FROM $SCHEMA.compound;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='compound';
SELECT count(*) AS sbs_count FROM $SCHEMA.substance;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='substance';
SELECT count(*) AS target_count FROM $SCHEMA.target;
SELECT count(*) AS cpd_w_assdata FROM $SCHEMA.compound WHERE nass_tested IS NOT NULL;
SELECT count(*) AS cpd_wo_assdata FROM $SCHEMA.compound WHERE nass_tested IS NULL;
SELECT count(*) AS synonym_count FROM $SCHEMA.synonym;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='synonym';
SELECT count(*) AS identifier_count FROM $SCHEMA.identifier;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='identifier';
SELECT * FROM $SCHEMA.db_properties;
SELECT id,name FROM $SCHEMA.attr_type;
SELECT name,version,to_char(load_date,'YYYY-MM-DD') AS load_date FROM $SCHEMA.dataset;
__EOF__
#
