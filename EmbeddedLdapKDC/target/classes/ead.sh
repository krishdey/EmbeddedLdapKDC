#!/bin/bash
set -x

PROGRAM_DIR="`dirname \"$0\"`"
[ -z "$EAD_HOME" ] && EAD_HOME="`(cd \"$PROGRAM_DIR/..\" && pwd)`"
if [ -z "$EAD_HOME" ]; then
echo "Unable to detect EAD_HOME, and not specified"
exit 1
fi

HAVE_TTY=0
if [ "`tty`" != "not a tty" ]; then
HAVE_TTY=1
fi


# Checking the parameters
EAD_INSTANCE_NAME=
EAD_ACTION=
if [ $# -eq 1 ]
then
# Using 'default' as default instance name
EAD_INSTANCE_NAME="default"
EAD_ACTION=$1
elif [ $# -eq 2 ]
then
# Getting the instance name from the arguments
EAD_INSTANCE_NAME=$1
EAD_ACTION=$2
else
# Printing usage information
echo "Usage: ead.sh [<instance name>] <action>"
echo "If <instance name> is ommited, 'default' will be used."
echo "<action> is one of start, stop."
exit 1
fi

[ -r "$EAD_HOME/bin/setenv.sh" ] && . "$EAD_HOME/bin/setenv.sh"


[ -z "$EAD_INSTANCES" ] && EAD_INSTANCES="$EAD_HOME/instances"

RUN_JAVA=
if [ -z "$JAVA_HOME" ]; then
RUN_JAVA=$(which java)
else
RUN_JAVA=$JAVA_HOME/bin/java
fi

CLASSPATH=$(JARS=("$EAD_HOME"/lib/*.jar); IFS=:; echo "${JARS[*]}")

EAD_INSTANCE="$EAD_INSTANCES/$EAD_INSTANCE_NAME"

EAD_OUT="$EAD_HOME/log/ead.out"
EAD_PID="$EAD_HOME/run/ead.pid"
touch $EAD_OUT
touch $EAD_PID

if [ $HAVE_TTY -eq 1 ]; then
echo "Using EAD_HOME:    $EAD_HOME"
echo "Using JAVA_HOME:   $JAVA_HOME"
echo ""
fi

if [ "$EAD_ACTION" = "start" ]; then
# Printing instance information
[ $HAVE_TTY -eq 1 ] && echo "Starting EAD instance '$EAD_INSTANCE_NAME'..."

if [ -f $EAD_PID ]; then
PID=`cat $EAD_PID`
if kill -0 $PID > /dev/null 2>&1; then
echo "EAD Server is already running as $PID"
exit 0
fi
fi

# Launching EAD Server
eval "\"$RUN_JAVA\"" \
-Dlog4j.configuration="\"file:$EAD_HOME/conf/log4j.properties\"" \
-Dapacheds.log.dir="\"$EAD_HOME/log\"" \
-Dead.server.port=10389 \
-classpath "\"$CLASSPATH\"" \
com.krish.ead.server.EADServer "\"$EAD_INSTANCE\"" \
> "$EAD_OUT" 2>&1 "&"
echo $! > "$EAD_PID"

elif [ "$EAD_ACTION" = "run" ]; then
# Printing instance information
[ $HAVE_TTY -eq 1 ] && echo "Running EAD instance '$EAD_INSTANCE_NAME'..."
eval exec "\"$RUN_JAVA\"" \
$EAD_JAVA_OPTS \
-Dlog4j.configuration="\"file:$EAD_HOME/log4j.properties\"" \
-Dead.server.port=10389 \
-classpath "\"$CLASSPATH\"" \
com.krish.ead.server.EADServer "\"$EAD_INSTANCE\""

elif [ "$EAD_ACTION" = "status" ]; then
if [ -f $EAD_PID ]; then
PID=`cat $EAD_PID`
if kill -0 $PID > /dev/null 2>&1; then
echo "EAD is running as $PID"
else
echo "EAD is not running"
fi
else
[ $HAVE_TTY -eq 1 ] && echo "EAD is not running"
fi
elif [ "$EAD_ACTION" = "stop" ]; then
# Printing instance information
if [ -f $EAD_PID ]; then
PID=`cat $EAD_PID`
[ $HAVE_TTY -eq 1 ] && echo "Stopping EAD Server instance '$EAD_INSTANCE_NAME' running as $PID"

kill -15 $PID > /dev/null 2>&1

ATTEMPTS_REMAINING=10
while [ $ATTEMPTS_REMAINING > 0 ]; do
kill -0 $PID > /dev/null 2>&1 -gt 0
if [ $? > 0 ]; then
rm -f $EAD_PID > /dev/null 2>&1
[ $HAVE_TTY -eq 1 ] && echo "EAD Server instance '$EAD_INSTANCE_NAME' stopped successfully"
break
fi
sleep 1
ATTEMPTS_REMAINING=`expr $ATTEMPTS_REMAINING - 1`
done
else
[ $HAVE_TTY -eq 1 ] && echo "EAD Server is not running, $EAD_PID does not exist"
ps -ef | grep "JPMISEAD" | grep -v grep | xargs kill -9

fi
fi