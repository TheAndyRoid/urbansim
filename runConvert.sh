#!/bin/sh

JAVA="/usr/lib/jvm/java-7-oracle/bin/java"
if [ $# -ne 4 ]; then
    echo "Usage: ./runConvert <OSM File> <SUMO Network File> <Device Type> <Output File>"
    exit 1
fi

$JAVA -classpath ./tools/convert/bin Convert $1 $2 $3 $4
