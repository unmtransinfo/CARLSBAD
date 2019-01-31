#!/bin/sh
#
#
set -x
#
cidfile="data/pc_mlp.cid"
sidfile="data/pc_mlp.sid"
sdffile="data/pc_mlp.sdf"
aidfile="data/pc_mlp_4cb.aid"
assfile="data/pc_assaysummary.csv"
actfile="data/pc_activity.csv"
#
set -e
#
if [ ! -e "$cidfile" ]; then
	entrez_compound_search.pl \
		-v \
		-mlp \
		-out_cids $cidfile
else
	printf "File exists, not overwritten: %s\n" $cidfile
fi
#
if [ ! -e "$sidfile" ]; then
	entrez_substance_search.pl \
		-v \
		-mlp \
		-out_sids $sidfile
else
	printf "File exists, not overwritten: %s\n" $sidfile
fi
#
if [ ! -e "$sdffile" ]; then
	pug_rest_query.py \
		--v \
		--sids2sdf \
		--i $sidfile \
		--o $sdffile
else
	printf "File exists, not overwritten: %s\n" $sdffile
fi
#
if [ ! -e "$aidfile" ]; then
	entrez_assay_search.pl \
		-v \
		-mlp \
		-active_concentration_specified \
		-pcassay_protein_target \
		-out_aids $aidfile \
		-out_summaries_csv $assfile
else
	printf "File exists, not overwritten: %s\n" $aidfile
fi
#
if [ ! -e "$actfile" ]; then
	pug_rest_assaydata.py \
		--v \
		--cidfile $cidfile \
		--aidfile $aidfile \
		--o $actfile
else
	printf "File exists, not overwritten: %s\n" $actfile
fi
#
