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

PRGDIR=`cd "$( dirname "$PRG" )" && pwd`
if [ -z "$PRGDIR" ]; then
	echo "Can NOT resolve the console directory."
	exit 1
fi
# Jetty 的 web 资源根目录是相对路径，必须 cd 到 console/ 下
cd "$PRGDIR" || exit 1

# -----------------------------------------------------------------------------
# 组装 classpath（与 start.sh 保持一致，自动适配发布包/开发仓两种布局）
# -----------------------------------------------------------------------------
CONSOLE_JAR=""
for cand in "$PRGDIR/cube-console.jar" "$PRGDIR"/cube-console-*.jar \
            "$PRGDIR/../build/cube-console-3.0.jar" "$PRGDIR/../build/cube-console.jar"; do
	if [ -f "$cand" ]; then
		CONSOLE_JAR="$cand"
		break
	fi
done

LIBDIR=""
for cand in "$PRGDIR/../server/libs" "$PRGDIR/../deploy/libs" "$PRGDIR/libs"; do
	if [ -d "$cand" ]; then
		LIBDIR="$cand"
		break
	fi
done

CELL_JAR=""
for cand in "$PRGDIR/../server/bin/cell.jar" "$PRGDIR/../deploy/bin/cell.jar" "$PRGDIR/cell.jar"; do
	if [ -f "$cand" ]; then
		CELL_JAR="$cand"
		break
	fi
done

if [ -z "$CONSOLE_JAR" ] || [ -z "$LIBDIR" ]; then
	echo "ERROR: Can NOT locate cube-console.jar or the libs directory."
	echo "  console jar: $CONSOLE_JAR"
	echo "  libs dir   : $LIBDIR"
	exit 1
fi

CLASSPATH="$CONSOLE_JAR"
if [ -n "$CELL_JAR" ]; then
	CLASSPATH="$CLASSPATH:$CELL_JAR"
fi
for file in "$LIBDIR"/*.jar; do
	[ -f "$file" ] && CLASSPATH="$CLASSPATH:$file"
done

# echo "$CLASSPATH"
echo -e "---------------------"
echo -e "* Stop Cube Console *"
echo -e "---------------------"

java -Dfile.encoding=UTF-8 -Xmx64m -classpath "$CLASSPATH" cube.console.container.Main stop

# 清理 start.sh 写入的 pid 文件
# /stop/ 接口返回后 Jetty 才真正退出，这里等进程结束再删 pid 文件
PIDFILE="$PRGDIR/console.pid"
if [ -f "$PIDFILE" ]; then
	PID=`cat "$PIDFILE" 2>/dev/null`
	if [ -n "$PID" ]; then
		for i in `seq 1 10`; do
			kill -0 "$PID" 2>/dev/null || break
			sleep 1
		done
		if kill -0 "$PID" 2>/dev/null; then
			echo "Cube Console process is still alive: $PID"
		else
			rm -f "$PIDFILE"
			echo "Cube Console stopped. (PID $PID)"
		fi
	fi
fi
