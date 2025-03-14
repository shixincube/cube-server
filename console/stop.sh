#!/bin/bash
# -----------------------------------------------------------------------------
# Stop Cube Console
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
CLASSPATH="$CLASSPATH":"$PRGDIR"/../deploy/bin/cell.jar:"$PRGDIR"/../build/cube-console-3.0.jar
# Scan libs path
for file in ` ls "$PRGDIR"/../deploy/libs `
do
  CLASSPATH="$CLASSPATH":"$PRGDIR"/../deploy/libs/$file
done

# echo "$CLASSPATH"
echo -e "---------------------"
echo -e "* Stop Cube Console *"
echo -e "---------------------"

java -Dfile.encoding=UTF-8 -Xmx32m -classpath "$CLASSPATH" cube.console.container.Main stop
