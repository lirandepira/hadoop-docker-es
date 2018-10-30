package org.pira.hadoop.filesystem;

import lombok.Data;

// Data Annotation from Lombok
// avoid to have to define getter and setters
@Data
public class FilePermission {
  private boolean read;
  private boolean write;
  private boolean execute;

}
