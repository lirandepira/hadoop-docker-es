package org.pira.hadoop.filesystem;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.math.BigInteger;
import java.time.LocalDateTime;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

// Data Annotation from Lombok
// avoid to have to define getter and setters
@Data
/**
 * FileMetadata is the class representing 
 * all the metadata that can be retrieved for a given file/ directory
 * It will be fed using the information contained in a listing.txt line
 */
public class FileMetadata {

  private String owner;
  private String group;
  // requires BigInteger instead of Integer
  // as some size might be bigger than 2^31-1
  private BigInteger size;
  // when serializing LocalDateTime class with Java
  // we need to use a specific serializer to make it
  // ISO-9001 compatible (and therefore recognizable by ELK)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  private LocalDateTime lastUpdate;
  private String path;
  private String filename;
  private String fileType;
  /**
   * File permission on UNIX/LINUX are assigned to:
   * - user
   * - group
   * - others
   * It was easier to describe it with a dedicated object that would say:
   * - does the user have read permission : true/ false
   * etc...
   */
  private FilePermission userPermission = new FilePermission();
  private FilePermission groupPermission = new FilePermission();
  private FilePermission othersPermission = new FilePermission();

  /**
   * Setting the path will allow to:
   * - Set the filename (extracted from the path)
   * - Determine the filetype (is it a movie, music, text, or unknown)
   * @param path 
   */
  public void setPath(String path) {
    // retrieve filename
    this.filename = StringUtils.substringAfterLast(path, "/");
    this.path = path;

    // if the file is already set as a directory
    // we do not need to determine its type
    // the only way we have to recognize that
    // the file is a directory
    // is with the permission: dr-xr-x---
    // where the d indicates the directory
    // so it was done before
    if (!FileType.DIRECTORY.equals(fileType)) {
      // retrieve extension
      String extension = StringUtils.substringAfterLast(filename, ".");
      // recognize and assign the filetype per extension
      fileType = FileType.retrieveType(extension);
    }
  }

}
