#!/bin/sh

setScriptFilename() {
	SCRIPT_FILE="`basename \"$0\"`"
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		echo "ERR: $RESULT: Error encountered while determining the name of the current script."
		return $RESULT;
	fi

	echo SCRIPT_FILE:$SCRIPT_FILE.

	return 0
}

setScriptFolderName() {
	SCRIPT_FOLDER="`dirname \"$0\"`";
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		echo "ERR: $RESULT: Error encountered while determining the name of the folder containing the current script."
		return $RESULT;
	fi
	
	if [ "$SCRIPT_FOLDER" = "" ] || [ "$SCRIPT_FOLDER" = "." ] || [ -z "$SCRIPT_FOLDER" ]; then
		SCRIPT_FOLDER=`pwd`
	fi
	
	echo SCRIPT_FOLDER:$SCRIPT_FOLDER.
	
	return 0
}

setParentFoldername() {
	PARENT_FOLDER="`dirname \"$SCRIPT_FOLDER\"`";
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		echo "ERR: $RESULT: Error encountered while determining the name of the parent of the current script."
		return $RESULT;
	fi
	
	if [ "$PARENT_FOLDER" = "" ] || [ "$PARENT_FOLDER" = "." ] || [ -z "$PARENT_FOLDER" ]; then
		PARENT_FOLDER=`pwd`
	fi
	
	echo PARENT_FOLDER:$PARENT_FOLDER.
	
	return 0
}

initialiseEnvironment() {	
	setScriptFilename
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		return $RESULT
	fi
	
	setScriptFolderName
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		return $RESULT
	fi
	
	setParentFoldername
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		return $RESULT
	fi
	
	return 0
}

main() {
	initialiseEnvironment
	RESULT=$?
	if [ $RESULT -ne 0 ]; then
		return $RESULT
	fi
	
	return 0
}

main
RESULT=$?
if [ $RESULT -ne 0 ]; then
	return $RESULT
fi

LOG_FOLDER=$PARENT_FOLDER/logs
LOG_FILE=$LOG_FOLDER/$SCRIPT_FILE.log

echo LOG_FOLDER:$LOG_FOLDER.
echo LOG_FILE:$LOG_FILE

if [ ! -d "$LOG_FOLDER" ]; then
    mkdir "$LOG_FOLDER"
fi

pushd "$PARENT_FOLDER"

java -jar "$PARENT_FOLDER/MockTCPServer-Standalone-1.3.0.jar" $1 $2 | tee $LOG_FILE