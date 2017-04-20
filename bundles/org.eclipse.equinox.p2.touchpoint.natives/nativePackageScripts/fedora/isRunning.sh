#!/bin/sh

file="/etc/os-release"
[ -f "$file" ] && source $file
if [ "$ID" == "fedora" ]; then
	exit 0;
fi
exit 2;

