#!/bin/sh
#
#
DBHOST="habanero.health.unm.edu"
DBNAME="carlsbad"
DBUSR="jjyang"
DBPW="assword"
#
SCAFMIN="0.5"
#
#"Acute myeloid leukemia (AML)","Neuroblastoma","Small cell lung cancer","Type II diabetes mellitus" 
KIDS="ds:H00003 ds:H00043 ds:H00013 ds:H00409"
#"Loratadine","Ketorolac","Ketotifen","Marinol","Methotrexate","Midazolam","Rofecoxib","Sildenafil","Tamoxifen"
CIDS="16563 55042 8797 5442 6070 70123 874 1550 14856"
#"GABA A Alpha4Beta3Gamma2","NADP-dependent malic enzyme","Rhodopsin kinase"
TIDS="1730 3072 3274"
#
for kid in $KIDS ; do
	carlsbadone.sh \
		-kid "$kid" \
		-scaf_min $SCAFMIN \
		-rgtp \
		-o "data/cb1_KID`echo ${kid} |sed -e 's/://'`_rgtp.xgmml" \
		-vv
done
#
for cid in $CIDS ; do
	carlsbadone.sh \
		-cid "$cid" \
		-scaf_min $SCAFMIN \
		-rgtp \
		-o "data/cb1_CID${cid}_rgtp.xgmml" \
		-vv
done
#
for tid in $TIDS ; do
	carlsbadone.sh \
		-tid "$tid" \
		-scaf_min $SCAFMIN \
		-rgtp \
		-o "data/cb1_TID${tid}_rgtp.xgmml" \
		-vv
done
#
