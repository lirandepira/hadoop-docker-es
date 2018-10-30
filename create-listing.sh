#!/bin/bash
if [ -z "$1" ]
then
  SEARCH_DIR=$HOME
else
  SEARCH_DIR=$1
fi

CURRENT_DIR=`pwd`

find $SEARCH_DIR -type d -exec ls -ld {} \; > $CURRENT_DIR/listing.txt
find $SEARCH_DIR -type f -exec ls -l {} \; >> $CURRENT_DIR/listing.txt