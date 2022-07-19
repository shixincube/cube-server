#!/bin/bash
# -----------------------------------------------------------------------------
# Start Cube App Server
# -----------------------------------------------------------------------------

# Better OS/400 detection
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
DIRLEN=${#PRGDIR}
if [ $DIRLEN -eq 0 ]; then
	PRGDIR="."
fi

# Add on extra jar files to CLASSPATH
CLASSPATH="$CLASSPATH":"$PRGDIR"/../deploy/bin/cell.jar:"$PRGDIR"/libs/kaptcha-2.3.2.jar:"$PRGDIR"/../build/cube-server-app-3.0.jar
# Scan libs path
for file in ` ls "$PRGDIR"/../deploy/libs `
do
  CLASSPATH="$CLASSPATH":"$PRGDIR"/../deploy/libs/$file
done

# echo "$CLASSPATH"
echo -e "================================================================"
echo -e "Cube App Server"
echo -e "---------------"

java -Dfile.encoding=UTF-8 -Xmx512m -classpath "$CLASSPATH" cube.app.server.Main start

echo -e "================================================================"
