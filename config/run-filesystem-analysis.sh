#!/bin/bash

if [ -z "$1" ]
then
  echo "Usage is ./run-filesystem-analysis.sh <path-to-a-filelisting>"
  exit -1
fi

FILE=$1

if [ ! -f $FILE ]
then
  echo File $FILE cannot be found
  exit -1
fi

# Clear the existing index
echo "Clear existing filesystem index in elasticsearch if existing"
curl -X DELETE "elasticsearch:9200/filesystem" &> /dev/null

FILE=$1
FILENAME=$(basename -- "$FILE")

# remove listing directory if existing
hadoop fs -rm listing/*

# create listing directory on HDFS
hadoop fs -mkdir -p listing

# put the listing file into HDFS
hdfs dfs -put $FILE listing/$FILENAME

# run wordcount 
hadoop jar /root/share/hadoop-runner-1.0.0-SNAPSHOT.jar listing/$FILENAME

