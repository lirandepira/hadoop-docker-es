package org.pira.hadoop.filesystem;

import java.util.Arrays;
import java.util.List;


public class FileType {

  /**
   * Static String description for filetypes
   */
  public static String PICTURES="PICTURES";
  public static String MOVIES="MOVIES";
  public static String TEXT="TEXT";
  public static String DIRECTORY="DIRECTORY";
  public static String CODE="CODE";
  public static String MUSIC="MUSIC";
  public static String DOCUMENTS="DOCUMENTS";
  public static String ARCHIVES="ARCHIVES";
  public static String UNKNOWN="UNKNOWN";
  
  /**
   * ArrayList containing the list of extensions
   * relative to a known filetype:
   * ie if .mov is in moviesExtension then the type is MOVIES
   */
  private static final List<String> picturesExtensions= Arrays.asList("jpg", "png", "bmp");
  private static final List<String> moviesExtensions= Arrays.asList("mov", "m4v", "mkv", "avi");
  private static final List<String> textExtensions= Arrays.asList("txt", "log", "xml", "json", "html");
  private static final List<String> codeExtensions= Arrays.asList("c", "java", "py");
  private static final List<String> musicExtensions= Arrays.asList("mp3", "m4a");
  private static final List<String> documentsExtensions= Arrays.asList("doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "pages", "number");
  private static final List<String> archivesExtensions= Arrays.asList("zip", "tar","gz","7z");
  
  /**
   * Retrieve the type against the extension
   * @param extension
   * @return 
   */
  public static String retrieveType(String extension){
    // ensure we compare only lower case extension
    extension= extension.toLowerCase();
    /**
     * Browse all the arraylist:
     * - if the extension is part of one, then return the related type
     * - else return UNKNOWN as it is not handled (at least yet) in the program
     */
    if (picturesExtensions.contains(extension)){
      return PICTURES;
    }
    else if (moviesExtensions.contains(extension)){
      return MOVIES;
    }
    else if (textExtensions.contains(extension)){
      return TEXT;
    }
    else if (codeExtensions.contains(extension)){
      return CODE;
    }
    else if (musicExtensions.contains(extension)){
      return MUSIC;
    }
    else if (documentsExtensions.contains(extension)){
      return DOCUMENTS;
    }
    else if (archivesExtensions.contains(extension)){
      return ARCHIVES;
    }
    return UNKNOWN;
  } 
}
