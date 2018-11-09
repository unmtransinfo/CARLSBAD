#!/bin/sh
#
set -x
#
#DBNAME="cb3"
DBNAME="carlsbad"
#
#
RunSQL.sh \
	sql/profile_datasets_01.sql \
	"$DBNAME" \
	carlsbad.health.unm.edu \
	'-H' \
	>data/profile_datasets.html
#
# substance.dataset_id discontinued (?) [April 2012].
#
#RunSQL.sh \
#	sql/profile_datasets_03.sql \
#	"$DBNAME" \
#	carlsbad.health.unm.edu \
#	'-H' \
#	>data/profile_datasets.html
#
RunSQL.sh \
	sql/profile_ids_01.sql \
	"$DBNAME" \
	carlsbad.health.unm.edu \
	'-H' \
	>data/profile_ids.html
#
RunSQL.sh \
	sql/profile_attributes_03.sql \
	"$DBNAME" \
	carlsbad.health.unm.edu \
	'-H' \
	>data/profile_attributes.html
#
