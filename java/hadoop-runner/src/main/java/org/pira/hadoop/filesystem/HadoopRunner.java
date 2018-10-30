package org.pira.hadoop.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.elasticsearch.hadoop.mr.EsOutputFormat;

/**
 * Main Hadoop Runner program
 * Contains the Map Logic
 * Contains the upload to ElasticSearch
 */
public class HadoopRunner {

  /**
   * Mapper class
   */
  public static class TokenizerMapper
          extends Mapper {

    /**
     * Jackson serializer which will be used to convert data into JSON
     * Before sending it to ElasticSearch
     */
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    @Override
    protected void map(Object key, Object value, Context context) throws IOException, InterruptedException {

      // Each single line of the listing.txt will go through that method
      // a line of listing.txt look like this:
      // -r-xr-xr-x  2 root  wheel  1 Jun  2 22:52 /home/example
      // Therefore we start with:
      // - the permissions for user/group/others and the fact the file is a directory or not: position 0
      // - the number of link (ignored): position 1
      // - the owner: position 2
      // - the group: position 3
      // - the size in bytes: position 4
      // - the last updated month: position 5
      // - the last updated day: position 6
      // - the last updated time or year (if not current year): position 7
      // - the full path: starting at position 8 (might contain some spaces so further position are still the same path)
      final int permissionsPosition = 0;
      final int ownerPosition = 2;
      final int groupPosition = 3;
      final int sizePosition = 4;
      final int updateMonthPosition = 5;
      final int updateDayPosition = 6;
      final int updateYearOrTimePosition = 7;
      final int pathPosition = 8;

      // retrieving the update time is a bit tricky
      // because it appears at multiple positions (5-6-7)
      // and position 7 sometimes differs of content
      // Let's first initialize the different elements (year / month.... seconds to the current date... basically now)
      LocalDateTime localUpdateTime = LocalDateTime.now();
      int year = localUpdateTime.getYear();
      int month = localUpdateTime.getMonthValue();
      int day = localUpdateTime.getDayOfMonth();
      int hours = 0;
      int minutes = 0;
      int seconds = 0;

      // path builder will append the path of the file if it has some spaces
      StringBuilder pathBuilder = new StringBuilder();
      
      // a line will in the end be a metadata
      // which we will feed to ElasticSearch ultimately
      FileMetadata metadata = new FileMetadata();
      
      // split the line per spaces
      StringTokenizer itr = new StringTokenizer(value.toString());
      int currentPosition = -1;
      while (itr.hasMoreTokens()) {
        currentPosition++;
        String currentToken = itr.nextToken();
        
        // trigger different process depending on the current position in the line
        switch (currentPosition) {
          case permissionsPosition:
            
            // the first position is the directory definition 
            // along with the permission on the file:
            // ie -r-xr-xr-x:
            // - first character: is it a directory
            // - position 1--> 3: users read/write/execute permissions
            // - position 4--> 6: group read/write/execute permissions
            // - position 7--> 9: others read/write/execute permissions
            FilePermission userPermission = new FilePermission();
            FilePermission groupPermission = new FilePermission();
            FilePermission othersPermission = new FilePermission();

            for (int i = 0; i < currentToken.toCharArray().length; i++) {
              char c = currentToken.toCharArray()[i];
              switch (i) {
                case 0:
                  // directory definition
                  if (c == 'd') {
                    metadata.setFileType(FileType.DIRECTORY);
                  }
                  break;
                case 1:
                  // user read permission
                  if (c == '-') {
                    userPermission.setRead(false);
                  } else {
                    userPermission.setRead(true);
                  }
                  break;
                case 2:
                  // user write permission
                  if (c == '-') {
                    userPermission.setWrite(false);
                  } else {
                    userPermission.setWrite(true);
                  }
                  break;
                case 3:
                  // user execute permission
                  // that's the last permission
                  // we can save the object
                  if (c == '-') {
                    userPermission.setExecute(false);
                  } else {
                    userPermission.setExecute(true);
                  }
                  metadata.setUserPermission(userPermission);
                  break;
                case 4:
                  // group read permission
                  if (c == '-') {
                    groupPermission.setRead(false);
                  } else {
                    groupPermission.setRead(true);
                  }
                  break;
                case 5:
                  // group write permission
                  if (c == '-') {
                    groupPermission.setWrite(false);
                  } else {
                    groupPermission.setWrite(true);
                  }
                  break;
                case 6:
                  // group execute permission
                  // that's the last permission
                  // we can save the object
                  if (c == '-') {
                    groupPermission.setExecute(false);
                  } else {
                    groupPermission.setExecute(true);
                  }
                  metadata.setGroupPermission(groupPermission);
                  break;
                case 7:
                  // others read permission
                  if (c == '-') {
                    othersPermission.setRead(false);
                  } else {
                    othersPermission.setRead(true);
                  }
                  break;
                case 8:
                  // others write permission
                  if (c == '-') {
                    othersPermission.setWrite(false);
                  } else {
                    othersPermission.setWrite(true);
                  }
                  break;
                case 9:
                  // others execute permission
                  // that's the last permission
                  // we can save the object
                  if (c == '-') {
                    othersPermission.setExecute(false);
                  } else {
                    othersPermission.setExecute(true);
                  }
                  metadata.setOthersPermission(othersPermission);
                  break;
              }
            }
            break;
          case ownerPosition:
            metadata.setOwner(currentToken);
            break;
          case groupPosition:
            metadata.setGroup(currentToken);
            break;
          case sizePosition:
            metadata.setSize(new BigInteger(currentToken));
            break;
          case updateMonthPosition:
            // Now here it is a bit tricky
            // the date format on Linux/Unix is always the
            // name of the month on 3 letters: Jan/Feb/,,,/Dec
            // we need to parse it to recognize the month number
            DateFormat fmt = new SimpleDateFormat("MMM", Locale.US);
            Date d;
            try {
              d = fmt.parse(currentToken);
              month = d.getMonth() + 1;
            } catch (ParseException ex) {
              throw new IOException("Unable to parse month [" + currentToken + "]", ex);
            }
            break;
          case updateDayPosition:
            day = Integer.parseInt(currentToken);
            break;
          case updateYearOrTimePosition:
            // Now here it is a bit tricky as well
            // the last date information is either
            // the time of the last update (if the file was updated this year): 11:51
            // or the year of the last update (if the file was updated another year): 2007
            if (currentToken.contains(":")) {
              // if we recognize a time, then we need to parse the hours and the minutes
              try {
                fmt = new SimpleDateFormat("HH:mm", Locale.US);
                d = fmt.parse(currentToken);
                hours = d.getHours();
                minutes = d.getMinutes();
              } catch (ParseException ex) {
                throw new IOException("Unable to parse time [" + currentToken + "]", ex);
              }
            } else {
              // if we recognize a year it's easier
              year = Integer.parseInt(currentToken);
            }
            break;
          case pathPosition:
            pathBuilder.append(currentToken);
            break;
          default:
            // if the path had some spaces we just need to append to the path
            // for the remaining positions
            if (currentPosition>pathPosition) {
              pathBuilder.append(" ");
              pathBuilder.append(currentToken);
            }
            break;
        }
      }
      
      // once we reach the final position we can consider:
      // - that the update date was completely parsed
      // - that the path was completely parsed
      localUpdateTime = LocalDateTime.of(year, month, day, hours, minutes, seconds);
      metadata.setLastUpdate(localUpdateTime);
      metadata.setPath(pathBuilder.toString());
      
      // Insert the json representation as the value
      // of the map function result
      // Note that elatic search does not use any key to store its information
      // so a Null value is perfectly fine for the key
      String json = mapper.writeValueAsString(metadata);
      context.write(NullWritable.get(), json);
    }
  }

  /**
   * Main function
   * To be called with the path to a listing.txt file as argument
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    conf.setBoolean("es.nodes.wan.only", true);
    // specifies that the value is already providing json
    conf.set("es.input.json", "yes");
    // address for elasticsearch (the elasticsearch node will be rightfully named with docker)
    conf.set("es.nodes", "elasticsearch:9200");
    // index definition for elasticsearch: everything will be saved under index filesystem
    conf.set("es.resource", "filesystem/metadata");
    Job job = new Job(conf);
    
    // there is no need of any reduce task
    // as we are not aggregating data there
    job.setNumReduceTasks(0);

    job.setJarByClass(HadoopRunner.class);
    // Define the mapper class
    job.setMapperClass(TokenizerMapper.class);
    
    job.setOutputKeyClass(LongWritable.class);
    // when sending back JSON the Map Output value is Text
    job.setMapOutputValueClass(Text.class);

    // defines the job to output to Elastic Search
    job.setOutputFormatClass(EsOutputFormat.class);
    
    // defines the input listing.txt file that will be sent as a parameter of the run script
    FileInputFormat.addInputPath(job, new Path(args[0]));
    // run the job
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
