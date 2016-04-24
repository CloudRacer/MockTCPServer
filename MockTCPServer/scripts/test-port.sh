#!/bin/sh

if [ -z "$1" ]; then
	echo "Test if a port is listening."
	echo ""
	echo "Usage:"
	echo "    $0 <port>"
	echo "Example:"
	echo "    $0 6789"
else
	nc -zv 127.0.0.1 $1
fi
