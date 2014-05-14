#!/bin/sh



JAVA="/usr/lib/jvm/java-7-oracle/bin/java"
if [ $# -ne 2 ]; then
    echo "Usage: ./run -file <casefile.xml>"
    exit 1
fi


$JAVA -Xmx1024m -Xms512m -Dit.polito.appeal.traci.tcp_nodelay=true -javaagent:lib/continuations-java/dist/Continuations_full.jar -Dfile.encoding=UTF-8 -classpath ./jar:./lib/mason/lib/gluegen-rt-natives-linux-amd64.jar:./lib/mason/lib/gluegen-rt.jar:./lib/mason/lib/itext-1.2.jar:./lib/mason/lib/j3dcore.jar:./lib/mason/lib/j3dutils.jar:./lib/mason/lib/jcommon-1.0.16.jar:./lib/mason/lib/jfreechart-1.0.13.jar:./lib/mason/lib/jmf.jar:./lib/mason/lib/joal-natives-linux-amd64.jar:./lib/mason/lib/joal.jar:./lib/mason/lib/jogl-all-natives-linux-amd64.jar:./lib/mason/lib/jogl-all.jar:./lib/mason/lib/portfolio.jar:./lib/mason/lib/vecmath.jar:./lib/mason/jar/mason.jar:./lib/TraCI4J/bin/TraCI4J.jar:./lib/TraCI4J/lib/junit.jar:./lib/TraCI4J/lib/log4j.jar:./lib/TraCI4J/lib/org.hamcrest.core_1.1.0.v20090501071000.jar:./lib/TraCI4J/lib/xercesImpl.jar:./lib/continuations-java/dist/Continuations.jar:./lib/continuations-java/lib/ant.jar:./lib/continuations-java/lib/asm-all-4.0.jar:./ urbansim.UrbanSimWithUI -file $2



