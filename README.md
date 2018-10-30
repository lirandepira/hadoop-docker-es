## Run Hadoop Cluster and ELK (without the L) within Docker Containers

* Adapted from https://github.com/kiwenlau/hadoop-cluster-docker

## Goal

The purpose of the software is to run an analysis of a filesystem of a host machine using Hadoop to process the data and ElasticSearch/Kibana to represent the resulting process

The input data is a file listing the metadata of all the files on a host system. It can be generated using a utility script. It contains every information that are accessible through a ls -l command.
Once given as an input to the hadoop java process, the java process will rely on the Hadoop/ES connector (https://www.elastic.co/products/hadoop) in order to push the retrieved information into Elastic Search.
Once in Elastic Search, the easiest way to consult it is using the Kibana UI providing easy search and dashboard based on the data that was fed into Elastic Search.

Each line of the listing file
```
-rw-r--r--  2 root  wheel  586 Jul  25 2017 /Applications/QuickTime Player.app/Contents/Resources/Dutch.lproj/YouTube.strings
```
Will therefore be transformed into a JSON document that will be stored into Elastic Search
```
{
    "owner": "root",
    "group": "wheel",
    "size": 586,
    "lastUpdate": "2017-07-25T00:00:00",
    "path": "/Applications/QuickTime Player.app/Contents/Resources/Dutch.lproj/YouTube.strings",
    "filename": "YouTube.strings",
    "fileType": "UNKNOWN",
    "userPermission": {
      "read": true,
      "write": true,
      "execute": false
    },
    "groupPermission": {
      "read": true,
      "write": false,
      "execute": false
    },
    "othersPermission": {
      "read": true,
      "write": false,
      "execute": false
    }
}
```

That transformation is handled using the Map function of Hadoop and therefore done on multiple nodes simultaneously.

## Architecture

The overall architecture is the following:
![architecture](/screenshots/0-architecture.png)

First of all one will have to generate a listing text file of its host machine folders content.

Once this step is done, the user can create the whole docker environment infrastructure which will consist in:
* n nodes of hadoop (including one master)
  * the master has a docker volume defined so that we can exchange files between the host and the master container
  * master exposes ports 8088 and 50070 for accessing the Hadoop UI
  * nodes are named hadoop-master or hadoop-slaven on the network so that they can communicate with each others
* 1 instance of Elastic Search
  * exposes the 9200 port so that other software can communicate with it over that port
  * is named elasticsearch on the network so that other software can reach it through that name
* 1 instance of Kibana
  * exposes the 5601 port so that users can access its UI
  * have a link to ElasticSearch on the port 9200 so that it can access the information stored into elasticsearch

Every container runs under a common docker network named hadoop which enables containers to reach each others.

There is a utility script which will create the whole infrastructure on the fly so that everything is made ready immediately.
The same utility script compiles also the java software so that the resulting jar file (which will be used into hadoop master) is copied into the shared docker volume

In order to provide hadoop with a listing file, the listing file has to be manually copied into the shared docker volume.

## Prerequisites

* Java 8
* Maven 3.x
* If you want to follow the progress on the Hadoop UI while using the script, it is recommended to edit your /etc/hosts file in order to make the hadoop-master hostname recognized as being localhost. In order to do so edit your /etc/hosts file (requires root access):
```
sudo vi /etc/hosts
```
And insert the following line in it before to save it
```
127.0.0.1       hadoop-master
```

### Build the hadoop docker image

```
docker build -t kiwenlau/hadoop:1.0 .
```

### Prepare a listing file
##### Listing file description

The listing file will be the input of the hadoop java process. It is basically the concatenation of ls -l command which will contain the following metadata:
* file permission: -r-xr-xr-x
* number of link (unused)
* owner name: root
* group name: wheel
* size in byte: 256
* month of last modification: Apr
* day of last modification: 24
* time of last modification if current year or year of last modification: 11:51 or 2006
* full path to the file: /root/example.txt

```
dr-xr-xr-x  2 root  wheel  1 Jun  2 22:52 /home
drwxr-xr-x 10 root  wheel  320 Jan 13 11:01 /usr
drwxr-xr-x  978 root  wheel  31296 Jun  1 20:59 /usr/bin
```

##### Prepare the listing file

In order to ease the generation of the listing file, a utility has been made and needs to be executed on the host (file analysis will run on the metadata of files of the host):

```
./create-listing.sh <optional-input-directory>
```

* if no directory is given, the script will use the user home directory by default
* if you want to analys your whole computer, use / as an input directory
* note you might encounter some permission restriction when executing the script on other directory than your home directory as your user might not have read access to it --> please ignore these


### Start the Cluster and ELK (without the L)

##### 1. start containers

```
./start-container.sh <number-of-hadoop-nodes>
```

* If no node number is give, default will go to 1 master / 2 slaves
* The process will automatically:
  * Create the hadoop master container
  * Create the hadoop slave containers
  * Create the ElasticSearch node container
  * Create the Kibana node container
  * Compile the java Hadoop filesystem analysis project
* Each container will be strongly named and belonging to the same network
* The master container has a share volume (/root/share on the container --> ./share on the host) so that files can be exchanged between the host and the master hadoop container
* At the end of the process a docker exec command gives automatically access to the hadoop master container

**output:**

```
start hadoop-master container...
start hadoop-slave1 container...
start hadoop-slave2 container...
```
* start 3 containers with 1 master and 2 slaves

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 19.702 s
[INFO] Finished at: 2018-06-02T18:38:07+01:00
[INFO] Final Memory: 90M/984M
[INFO] ------------------------------------------------------------------------
start hadoop-elasticsearch container if not already started...
hadoop-elasticsearch
start hadoop-kibana container if not already started...
hadoop-kibana
root@hadoop-master:~# 
```
* you will get into the /root directory of hadoop-master container

##### 2. start hadoop

```
./start-hadoop.sh
```

##### 3. run the filesystem analysis

**Note that the related java project is available in the java directory. It uses Lombok as a maven dependency which is only recognized properly by decent IDE such as Netbeans or IntelliJ but of course the mvn command line will not be an issue**

```
./run-filesystem-analysis.sh <path-to-a-filelisting>
```

* note that you can access the file listing you have prepared before by copying it into the share directory in your host. It will be made available into /root/share of the master hadoop node

**output**

```
18/06/03 08:20:20 INFO mapreduce.Job:  map 8% reduce 0%
18/06/03 08:20:28 INFO mapreduce.Job:  map 9% reduce 0%
18/06/03 08:20:31 INFO mapreduce.Job:  map 10% reduce 0%
..........
File System Counters
		FILE: Number of bytes read=0
		FILE: Number of bytes written=469760
		FILE: Number of read operations=0
		FILE: Number of large read operations=0
		FILE: Number of write operations=0
		HDFS: Number of bytes read=541566522
		HDFS: Number of bytes written=0
		HDFS: Number of read operations=8
		HDFS: Number of large read operations=0
		HDFS: Number of write operations=0
	Job Counters 
		Launched map tasks=4
		Data-local map tasks=3
		Rack-local map tasks=1
		Total time spent by all maps in occupied slots (ms)=2323266
		Total time spent by all reduces in occupied slots (ms)=0
		Total time spent by all map tasks (ms)=2323266
		Total vcore-milliseconds taken by all map tasks=2323266
		Total megabyte-milliseconds taken by all map tasks=2379024384
	Map-Reduce Framework
		Map input records=2816561
		Map output records=2816561
		Input split bytes=480
		Spilled Records=0
		Failed Shuffles=0
		Merged Map outputs=0
		GC time elapsed (ms)=46605
		CPU time spent (ms)=605200
		Physical memory (bytes) snapshot=895582208
		Virtual memory (bytes) snapshot=7960616960
		Total committed heap usage (bytes)=477102080
	File Input Format Counters 
		Bytes Read=541566042
	File Output Format Counters 
		Bytes Written=0
	Elasticsearch Hadoop Counters
		Bulk Retries=0
		Bulk Retries Total Time(ms)=0
		Bulk Total=2819
		Bulk Total Time(ms)=1253340
		Bytes Accepted=1392679446
		Bytes Received=11276390
		Bytes Retried=0
		Bytes Sent=1392679446
		Documents Accepted=2816561
		Documents Received=0
		Documents Retried=0
		Documents Sent=2816561
		Network Retries=0
		Network Total Time(ms)=1258093
		Node Retries=0
		Scroll Total=0
		Scroll Total Time(ms)=0
```

* the process will clear the existing index in elasticsearch if it exist already
* it will execute the file analysis on a listing file and load the individual json information through the MAP function into elastic search, allowing search to be performed
* Note that the REDUCE function is not utilized here as we do not want to aggregate any result

##### 4. follow progress on hadoop UI

**Note the you'll need to have the prerequisite with the /etc/hosts modification setup to do so**

* You can access the hadoop UI here: http://localhost:8088
* On the cluster application menu, your map process should be running or accepted:
![applications](/screenshots/9-hadoop-applications.png)
* You can follow its progress in the application details clicking on the tracking url:
![tracking](/screenshots/10-application-details.png)
* Drill-down to the job details:
![job details](/screenshots/11-job-details.png)
* Which gives you access to the map/reduce job. You can get the progress per nodes here:
![map reduce](/screenshots/12-map-reduce.png)
* And finally the node execution (node that there is only 4 Map node at max on that configuration by default):
![node job](/screenshots/13-node-execution.png)

**Note that some of the hadoop UI screens are linked to the slaves and might not be accessible as the docker containers does not expose the ports properly for the slaves**

### Accessing and configuring Kibana
##### Accessing Kibana

Kibana is accessible here: http://localhost:5601

##### Configuring the index

Once in Kibana you can configure the "filesystem" index following these steps:

* Reach the configure index page:
![Index Pattern](/screenshots/1-index-pattern.png)

* After having run the filesystem analysis script, your elastic search should have some data. Create the filesystem index:
![Filesystem Index](/screenshots/2-filesystem.png)

* Ignore the timestamp parameter and click Create Index Pattern
![Ignore timestamp](/screenshots/3-no-timestamp.png)

You're ready to go now !

##### Importing the dashboard

A dashboard and search example can be imported to give immediate access to the data. 

* Reach the saved object page
![Saved Objects](/screenshots/4-saved-objects.png)

* Click the import button and import the file kibana/config.json
![Import](/screenshots/5-import.png)

* Assign your newly created index to it by forcing to be prompted
![Prompt](/screenshots/6-prompt.png)

* When prompted, choose your index
![Index](/screenshots/7-index-choose.png)

* You can now access your dashboard

![Index](/screenshots/8-dashboard.png)
