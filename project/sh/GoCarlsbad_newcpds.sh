#!/bin/sh
#
### Note: It is not yet possible to incrementally add scaffolds for new compounds to
### Carlsbad (except with significant manual effort).
#
#
set -e
#
PGHOST="habanero.health.unm.edu"
PGPORT=5432
PGSU="postgres"
#DB="carlsbad"
DB="cbdev"
#DB="cb2"
SCHEMA="public"
#
if [ $# -gt 0 ]; then
	DB=$1
fi
#
thisdir=`dirname $0`
#
prefix=$thisdir/data/carlsbad_new
smifile=${prefix}_${DB}.smi
#
#
psql -h $PGHOST -p $PGPORT -U jjyang -q -A -t -F ' ' -d $DB <<__EOF__ >$smifile
SELECT
	smiles,id
FROM
	compound
WHERE
	compound.id > 896526
;
__EOF__
#
wc -l $smifile
#
##############################################################################
#
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_hscaf.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
LIBDIR=/home/app/lib
CLASSPATH=$CLASSPATH:$LIBDIR/jchem.jar
CLASSPATH=$CLASSPATH:$LIBDIR/berkeleydb.jar
#
#
java -classpath $CLASSPATH edu.unm.health.biocomp.hscaf.hier_scaffolds \
        -i $smifile \
        -o ${prefix}_hscaf_out.smi \
        -out_scaf ${prefix}_hscaf_scaf.smi \
        -inc_mol \
        -inc_scaf \
        -inc_link \
        -inc_chain \
        -scaflist_append2title \
	-maxmol 100 \
	-maxrings 5 \
        -vv
#
#
carlsbad_query.py \
	--lookup_scafs ${prefix}_hscaf_scaf.smi \
	--vv
#
