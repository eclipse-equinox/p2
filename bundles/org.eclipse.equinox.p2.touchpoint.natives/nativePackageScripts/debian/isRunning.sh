#!/bin/sh

ls /etc/debian_version
if [ $? -eq 0 ]; then
  return 0;
fi

return 2;

