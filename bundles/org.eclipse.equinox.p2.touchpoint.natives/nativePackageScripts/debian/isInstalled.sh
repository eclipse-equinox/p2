#!/bin/sh

# Check if a package whose name and version are specified as input is installed.
# Return 0 if the package is installed, 1 if the version is not correct, 2 if the package is not installed
# 
# Usage:
#  $ ./isInstalled param1 [param2, param3]
# * param1: packageId
# * param2: version comparator
# * param3: version of the package

foundVersion=$(dpkg-query -f='${version}' --show $1)

#The package is found, check the version
if [ $? -eq 0 ]; then
  if [ "$#" -eq 1 ]; then
  	 return 0;
  fi
  dpkg --compare-versions $foundVersion $2 $3
  if [ $? -eq 0 ]; then
    return 0;
  else 
    return 1;
  fi
fi

#We are here because the package is not found
return 2;
