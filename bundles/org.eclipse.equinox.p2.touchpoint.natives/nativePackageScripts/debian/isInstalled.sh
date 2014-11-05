#!/bin/sh
foundVersion=$(dpkg-query -f='${version}' --show $1)

#The package is found, check the version
if [ $? -eq 0 ]; then
  dpkg --compare-versions $foundVersion ge $2
  if [ $? -eq 0 ]; then
    return 0;
  else 
    return 1;
  fi
fi

#We are here because the package is not found
return 2;
