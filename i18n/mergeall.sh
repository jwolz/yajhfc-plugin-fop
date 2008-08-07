#!/bin/bash
# Builds all po files

OUTNAME=FOPMessages

echo 'Extracting Strings ...'

. ./jxgettext.sh

echo 'Merging language files ...'

for PO in ${OUTNAME}_*.po ; do
	echo $PO
	msgmerge -N -U $PO $OUTNAME.po
done

