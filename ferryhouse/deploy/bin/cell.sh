#!/bin/sh
# -----------------------------------------------------------------------------
# Start/Stop Script for the Cell
#
# Environment Variable Prequisites
#
#   CC_HOME      May point at your Cell Cloud "build" directory.
#
#   CC_BASE      (Optional) Base directory for resolving dynamic portions
#                of a Cell Cloud installation.  If not present, resolves to
#                the same directory that CC_HOME points to.
#
#   JAVA_HOME    Must point at your Java Development Kit installation.
#                Required to run the with the "debug" or "javac" argument.
#
#   JRE_HOME     Must point at your Java Development Kit installation.
#                Defaults to JAVA_HOME if empty.
#
#   JAVA_OPTS    (Optional) Java runtime options used when the "start",
#                "stop", or "run" command is executed.
#
#   CC_PID       (Optional) Path of the file which should contains the pid
#                of Cell Cloud startup java process, when start (fork) is used
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Parse tag
ARGVIDX=1
TAG=""
for argv in "$@"
do
  if [ "$argv" = "-tag" ]; then
    TAG="${@:$ARGVIDX+1:1}"
  fi
  let ARGVIDX+=1
done

# Only set CC_HOME if not already set
[ -z "$CC_HOME" ] && CC_HOME=`cd "$PRGDIR/.." ; pwd`

if [ -r "$CC_BASE"/bin/setenv.sh ]; then
  . "$CC_BASE"/bin/setenv.sh
elif [ -r "$CC_HOME"/bin/setenv.sh ]; then
  . "$CC_HOME"/bin/setenv.sh
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JRE_HOME" ] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [ -n "$CC_HOME" ] && CC_HOME=`cygpath --unix "$CC_HOME"`
  [ -n "$CC_BASE" ] && CC_BASE=`cygpath --unix "$CC_BASE"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

# Get standard Java environment variables
if $os400; then
  # -r will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  BASEDIR="$CC_HOME"
  . "$CC_HOME"/bin/setclasspath.sh 
else
  if [ -r "$CC_HOME"/bin/setclasspath.sh ]; then
    BASEDIR="$CC_HOME"
    . "$CC_HOME"/bin/setclasspath.sh
  else
    echo "Cannot find $CC_HOME/bin/setclasspath.sh"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# Add on extra jar files to CLASSPATH
CLASSPATH="$CLASSPATH":"$CC_HOME"/bin/cell.jar
# Scan libs path
for file in ` ls "$CC_HOME"/libs `
do
  CLASSPATH="$CLASSPATH":"$CC_HOME"/libs/$file
done

# Option for server
#JAVA_OPTS="-server -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -Xmx512m -Xms512m -Xss256k -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=7 -XX:GCTimeRatio=19 -XX:+DisableExplicitGC -XX:+CMSClassUnloadingEnabled -XX:-CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+PrintClassHistogram -XX:+PrintGCDetails -Xloggc:logs/gc_$TAG.log"
JAVA_OPTS="-server -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -Xmx512m -Xms512m -Xss256k -XX:+PrintClassHistogram -XX:+PrintGCDetails -Xloggc:logs/gc_$TAG.log"

if [ -z "$CC_BASE" ] ; then
  CC_BASE="$CC_HOME"
fi

# Define PID file
CC_PID="$CC_HOME"/bin/pid

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  CC_HOME=`cygpath --absolute --windows "$CC_HOME"`
  CC_BASE=`cygpath --absolute --windows "$CC_BASE"`
  CC_TMPDIR=`cygpath --absolute --windows "$CC_TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JSSE_HOME" ] && JSSE_HOME=`cygpath --absolute --windows "$JSSE_HOME"`
  JAVA_ENDORSED_DIRS=`cygpath --path --windows "$JAVA_ENDORSED_DIRS"`
fi

# ----- Execute The Requested Command -----------------------------------------

# Bugzilla 37848: only output this if we have a TTY
if [ $have_tty -eq 1 ]; then
  echo "Welcome to Cell (Genie)"
  echo "Copyright (C) 2020 Cell"
  echo "--------------------------------------------------------------------"
  echo "Using CC_BASE:     $CC_BASE"
  echo "Using CC_HOME:     $CC_HOME"
  if [ "$1" = "debug" -o "$1" = "javac" ] ; then
    echo "Using JAVA_HOME:   $JAVA_HOME"
  else
    echo "Using JRE_HOME:    $JRE_HOME"
  fi
  echo "Working ..."
  echo
fi


if [ "$1" = "debug" ] ; then
  if $os400; then
    echo "Debug command not available on OS400"
    exit 1
  else
    exec "$_RUNJDB" $JAVA_OPTS \
      cell.carpet.Cell debug "$@"
  fi

elif [ "$1" = "run" ]; then

  shift
  exec "$_RUNJAVA" $JAVA_OPTS \
    cell.carpet.Cell start "$@"

elif [ "$1" = "start" ] ; then

  shift
  touch "$CC_BASE"/logs/cell_"$TAG".out

  "$_RUNJAVA" $JAVA_OPTS \
    -classpath "$CLASSPATH" \
    cell.carpet.Cell start "$@" -console false \
    > "$CC_BASE"/logs/cell_"$TAG".out 2>&1 &

    if [ ! -z "$CC_PID" ]; then
      echo $! > $CC_PID
    fi

elif [ "$1" = "stop" ] ; then

  shift
  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

#  "$_RUNJAVA" $JAVA_OPTS \
#    -classpath "$CLASSPATH" \
  "$_RUNJAVA" \
      -classpath "$CC_HOME/bin/cell.jar" \
    cell.carpet.Cell stop "$@"

  if [ $FORCE -eq 1 ]; then
    if [ ! -z "$CC_PID" ]; then
       echo "Killing: `cat $CC_PID`"
       kill -9 `cat $CC_PID`
    else
       echo "Kill failed: \$CC_PID not set"
    fi
  fi

elif [ "$1" = "version" ] ; then

    "$_RUNJAVA" \
      -classpath "$CC_HOME/bin/cell.jar" \
      cell.carpet.CellVersion

else

  echo "Usage: cell.sh ( commands ... )"
  echo "commands:"
  if $os400; then
    echo "  debug             Start cell in a debugger (not available on OS400)"
  else
    echo "  debug             Start cell in a debugger"
  fi
  echo "  run               Start cell in the current window"
  echo "  start             Start cell in a separate window"
  echo "  stop              Stop cell"
  echo "  stop -force       Stop cell (followed by kill -KILL)"
  echo "  version           What version of tomcat are you running?"
  exit 1

fi
