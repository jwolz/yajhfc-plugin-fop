#!/bin/sh
# Starts YajHFC including the FO plugin

echo "Starting YajHFC..."

DIR=`dirname $0`

YAJHFC="$DIR/yajhfc.jar"
FOPLUGIN="$DIR/FOPPlugin.jar"

JAVA=`which java`

if [ ! -f "$YAJHFC" ]; then
	echo "$YAJHFC not found!"
	exit 1 ;
fi

if [ ! -f "$FOPLUGIN" ]; then
	echo "$FOPLUGIN not found!"
	exit 1 ;
fi

if [ -z "$JAVA" ]; then
	echo "Java executable not found in path."
	exit 1 ;
fi

for JAR in $DIR/lib/*.jar ; do
	CLASSPATH="$CLASSPATH:$JAR" ;
done

export CLASSPATH="$CLASSPATH:$YAJHFC"

exec $JAVA -Xmx512m yajhfc.Launcher "--loadplugin=$FOPLUGIN" "$@"

