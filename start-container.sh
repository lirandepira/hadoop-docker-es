#!/bin/bash

CURRENT_DIR=`pwd`
mkdir -p share

# the default node number is 3
N=${1:-3}

# change slaves file
i=1
rm config/slaves
while [ $i -lt $N ]
do
	echo "hadoop-slave$i" >> config/slaves
	((i++))
done 

echo ""

# create the network if not existing
docker network create --driver=bridge hadoop &> /dev/null

# start hadoop master container
# add a volume share to copy files from the host
docker rm -f hadoop-master &> /dev/null
echo "start hadoop-master container..."
docker run -itd \
                --net=hadoop \
                -p 50070:50070 \
                -p 8088:8088 \
                --name hadoop-master \
                -v $CURRENT_DIR/share:/root/share \
                --hostname hadoop-master \
                kiwenlau/hadoop:1.0 &> /dev/null


# start hadoop slave container
i=1
while [ $i -lt $N ]
do
	docker rm -f hadoop-slave$i &> /dev/null
	echo "start hadoop-slave$i container..."
	docker run -itd \
	                --net=hadoop \
	                --name hadoop-slave$i \
	                --hostname hadoop-slave$i \
	                kiwenlau/hadoop:1.0 &> /dev/null
	i=$(( $i + 1 ))
done 

# Compile the java software
cd java/hadoop-runner
mvn clean install
# Copy the resulting jar file in the share directory
cp target/hadoop-runner-*.jar $CURRENT_DIR/share

# start hadoop related elastic search instance
# it needs to be on the same network as the hadoop nodes
# and to be named specifically so that other instances
# can call it
echo "start hadoop-elasticsearch container if not already started..."
docker create  -it \
           --net=hadoop \
           -p 9200:9200 \
           -p 9300:9300 \
           -e ES_JAVA_OPTS="-Xmx256m -Xms256m" \
           --name hadoop-elasticsearch \
           --network-alias elasticsearch \
           --hostname elasticsearch \
           docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4 &> /dev/null
docker start hadoop-elasticsearch

# start hadoop related kibana instance
# it needs to be on the same network as the hadoop nodes
echo "start hadoop-kibana container if not already started..."
docker create -it \
           --net=hadoop \
           --name hadoop-kibana \
           --hostname kibana \
           --network-alias kibana \
           -p 5601:5601 \
           -e ES_JAVA_OPTS="-Xmx256m -Xms256m" \
           -e ELASTICSEARCH_URL="http://elasticsearch:9200" \
           docker.elastic.co/kibana/kibana-oss:6.2.4 &> /dev/null
docker start hadoop-kibana
	       

# get into hadoop master container
docker exec -it hadoop-master bash
