#!/bin/sh
# Extracts all translatable strings into messages.po

SOURCEDIR=../src
OUTNAME=FOPMessages

xgettext -k_ --from-code=utf-8 `find $SOURCEDIR -name '*.java'` -o$OUTNAME.po

