#!/bin/sh

# Check if a package whose name and version are specified as input is installed.
# Return 0 if the package is installed, 1 if the version is not correct, 2 if the package is not installed
# 
# Usage:
#  $ ./isInstalled param1 [param2, param3]
# * param1: packageId
# * param2: version comparator
# * param3: version of the package

is_equal () {
	if [ "$1" = "$2" ]; then 
		echo 0
	else 
    	echo 1
	fi
}

is_greater () {
	if [ "$1" \> "$2" ]; then 
		echo 0
	else 
    	echo 1
	fi
}

foundVersion=$(rpm -q --qf '%{version}' $1)

#The package is found, check the version
if [ $? -eq 0 ]; then
	if [ "$#" -eq 1 ]; then
		exit 0
	fi
	
case "$2" in 
	"gt")
	great=$(is_greater $foundVersion $3)
	exit $great
	;;
	"ge")
	equal=$(is_equal $foundVersion $3)
	if [ "$equal" = "0" ]; then
		exit 0
	else
		great=$(is_greater $foundVersion $3)
		exit $great
	fi
	;;
	"eq")
	equal=$(is_equal $foundVersion $3)
	exit $equal
	;;
	"le")
	equal=$(is_equal $foundVersion $3)
	if [ "$equal" = "0" ]; then
		exit 0
	else
		great=$(is_greater $3 $foundVersion)
		exit $great
	fi
	;;
	"lt")
	great=$(is_greater $3 $foundVersion )
	exit $great
	;;
esac

fi


#We are here because the package is not found
exit 2;
