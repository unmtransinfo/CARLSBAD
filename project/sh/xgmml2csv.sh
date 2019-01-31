#!/bin/sh
#
#
LIBDIR=/home/app/lib
CLASSPATH=$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:/home/app/Cytoscape_v2.8.2/cytoscape.jar
#
help() {
cat <<__EOF__
syntax: `basename $0` [-i ifile] [-o ofile]
	ifile = input XGMML network file
	ofile = output CSV file
path: $0
__EOF__
exit 1
}

#
while getopts i:o: opt ; do
	case "$opt"
	in
	i)      ifile=$OPTARG ;;
	o)      ofile=$OPTARG ;;
	\?)     help
		exit 1 ;;
	esac
done
#
if [ ! "$ifile" ]; then
        echo "Input file must be specified."
        help
fi
if [ ! "$ofile" ]; then
        echo "Output file must be specified."
        help
fi
#
java -classpath $CLASSPATH edu.unm.health.biocomp.cytoscape.cytoscape_utils \
	-v \
	-xgmml2csv \
	-i $ifile \
	-o $ofile
#
