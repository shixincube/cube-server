#!/bin/bash
# -----------------------------------------------------------------------------
# Start Cube Console
# -----------------------------------------------------------------------------
#
# 用法:
#   ./start.sh              后台启动（nohup，退出终端后继续运行）
#   ./start.sh -f           前台启动（Ctrl+C 退出）
#   ./start.sh -h           显示帮助
#
# 后台模式产生的文件:
#   console.pid             Java 进程号
#   logs/console.log        标准输出与错误输出
#
# Better OS/400 detection
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

usage() {
	echo "Usage: $0 [-f|--foreground] [-h|--help]"
	echo "  无参数      后台启动（nohup），退出终端后继续运行"
	echo "  -f          前台启动，Ctrl+C 退出"
	echo "  -h          显示本帮助"
}

# 解析参数
FOREGROUND=false
for arg in "$@"; do
	case "$arg" in
	-f|--foreground) FOREGROUND=true;;
	-h|--help) usage; exit 0;;
	*) echo "Unknown argument: $arg"; usage; exit 1;;
	esac
done

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

# 取脚本所在目录的绝对路径并切换过去：
# Jetty 的 ResourceHandler 以相对路径 "web" 作为资源根目录，
# 只有工作目录等于 console/ 时前端页面才能被访问到。
PRGDIR=`cd "$( dirname "$PRG" )" && pwd`
if [ -z "$PRGDIR" ]; then
	echo "Can NOT resolve the console directory."
	exit 1
fi
cd "$PRGDIR" || exit 1

PIDFILE="$PRGDIR/console.pid"
LOGDIR="$PRGDIR/logs"
LOGFILE="$LOGDIR/console.log"

# 控制台监听端口，与 cube.console.container.Main 里的取值保持一致
PORT=7080

# 已在运行则退出，避免重复启动
if [ -f "$PIDFILE" ]; then
	PID=`cat "$PIDFILE" 2>/dev/null`
	if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
		echo "Cube Console is already running. (PID $PID)"
		echo "Please run ./stop.sh first if you want to restart it."
		exit 1
	fi
	# 残留的 pid 文件，清理后继续
	rm -f "$PIDFILE"
fi

# 端口预检：pid 文件丢失时也能拦住重复启动（不依赖 lsof/nc）
if (exec 3<>/dev/tcp/127.0.0.1/$PORT) 2>/dev/null; then
	echo "Port $PORT is already in use. Cube Console may be running in another way."
	echo "Please stop it first, or check with: lsof -nP -iTCP:$PORT -sTCP:LISTEN"
	exit 1
fi

# -----------------------------------------------------------------------------
# 组装 classpath
# 自动适配两种目录布局（原来硬编码开发仓库路径，直接拷到发布包上会 ClassNotFound）：
#   发布包 : <root>/console/{cube-console.jar,web,console.properties} + <root>/server/{bin/cell.jar,libs/}
#   开发仓 : <repo>/console/ + <repo>/{deploy/bin/cell.jar,deploy/libs,build/cube-console-3.0.jar}
# -----------------------------------------------------------------------------
# 1) console 自身的 jar：优先脚本同目录（发布包），退回 ../build（开发仓）
CONSOLE_JAR=""
for cand in "$PRGDIR/cube-console.jar" "$PRGDIR"/cube-console-*.jar \
            "$PRGDIR/../build/cube-console-3.0.jar" "$PRGDIR/../build/cube-console.jar"; do
	if [ -f "$cand" ]; then
		CONSOLE_JAR="$cand"
		break
	fi
done

# 2) 三方依赖目录
LIBDIR=""
for cand in "$PRGDIR/../server/libs" "$PRGDIR/../deploy/libs" "$PRGDIR/libs"; do
	if [ -d "$cand" ]; then
		LIBDIR="$cand"
		break
	fi
done

# 3) cell.jar（cell.* 工具类只在这里，不在 cube-common 中）
CELL_JAR=""
for cand in "$PRGDIR/../server/bin/cell.jar" "$PRGDIR/../deploy/bin/cell.jar" "$PRGDIR/cell.jar"; do
	if [ -f "$cand" ]; then
		CELL_JAR="$cand"
		break
	fi
done

if [ -z "$CONSOLE_JAR" ]; then
	echo "ERROR: Can NOT find cube-console.jar."
	echo "  Looked in: $PRGDIR/cube-console.jar"
	echo "             $PRGDIR/../build/cube-console-3.0.jar"
	exit 1
fi
if [ -z "$LIBDIR" ]; then
	echo "ERROR: Can NOT find the dependency libs directory."
	echo "  Looked in: $PRGDIR/../server/libs"
	echo "             $PRGDIR/../deploy/libs"
	exit 1
fi
if [ -z "$CELL_JAR" ]; then
	echo "ERROR: Can NOT find cell.jar."
	echo "  Looked in: $PRGDIR/../server/bin/cell.jar"
	echo "             $PRGDIR/../deploy/bin/cell.jar"
	exit 1
fi

CLASSPATH="$CONSOLE_JAR:$CELL_JAR"
for file in "$LIBDIR"/*.jar; do
	[ -f "$file" ] && CLASSPATH="$CLASSPATH:$file"
done

echo "Console jar : $CONSOLE_JAR"
echo "Cell jar    : $CELL_JAR"
echo "Libs dir    : $LIBDIR"

JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -Xmx1024m"

# echo "$CLASSPATH"
echo -e "================================================================"
echo -e "Cube Console"
echo -e "------------"

if [ "$FOREGROUND" = "true" ]; then
	echo $$ > "$PIDFILE"
	trap 'rm -f "$PIDFILE"' EXIT INT TERM
	java $JAVA_OPTS -classpath "$CLASSPATH" cube.console.container.Main start
	echo -e "================================================================"
else
	mkdir -p "$LOGDIR"
	echo "===== `date '+%Y-%m-%d %H:%M:%S'` start cube console =====" >> "$LOGFILE"

	nohup java $JAVA_OPTS -classpath "$CLASSPATH" cube.console.container.Main start >> "$LOGFILE" 2>&1 < /dev/null &
	PID=$!
	echo "$PID" > "$PIDFILE"

	# 确认进程存活：避免"启动即失败"却报告成功
	sleep 2
	if kill -0 "$PID" 2>/dev/null; then
		echo "Cube Console started. (PID $PID)"
		echo "Log file : $LOGFILE"
		echo "Open     : http://<server-ip>:$PORT in your browser to login Cube Console."
		echo "Stop     : ./stop.sh"
	else
		echo "Cube Console FAILED to start. Please check log: $LOGFILE"
		rm -f "$PIDFILE"
		exit 1
	fi
fi
