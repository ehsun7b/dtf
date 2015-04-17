package vida.phd.tfd.io;

import vida.phd.tfd.entity.Family;
import vida.phd.tfd.entity.Malware;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to load a family. It will load all *.txt files from the given directory.
 * @author Vida
 */
public class Loader implements Callable<Family> {

  /**
   * Directory which contains all *.txt files
   */
  private final File directory;
  
  /**
   * Family name
   */
  private final String familyName;
  
  /**
   * Array of all *.txt files
   */
  private File[] files;

  /**
   * Constructor of the class which takes two parameters.
   * @param directory which contains all malware files.
   * @param familyName 
   */
  public Loader(File directory, String familyName) {
    this.directory = directory;
    this.familyName = familyName;
  }

  /**
   * Overridden call method which contains the main logic of family loading.
   * @return loaded family
   * @throws Exception 
   */
  @Override
  public Family call() throws Exception {
    Family family = new Family(familyName);

    if (directory.isDirectory()) {
      files = directory.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
          return file.isFile() && file.getName().toLowerCase().endsWith(".txt");
        }
      });

      for (File file : files) {
        String malwareName = file.getName();        
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
          Malware malware = new Malware(malwareName, family);
          
          String code;

          while ((code = reader.readLine()) != null) {
            code = code.trim();
            if (code.length() > 0) {
              malware.addBasicBlock(code);
              family.addBasicBlock(code);
            }
          }
          
          family.addMalware(malware);
        } catch (IOException ex) {
          Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    }

    return family;
  }

  /**
   * 
   * @return directory
   */
  public File getDirectory() {
    return directory;
  }

  /**
   * 
   * @return familyName
   */
  public String getFamilyName() {
    return familyName;
  }

  /**
   * Utility method to load a family from the given path.
   * @param path in which the malware files located
   * @param familyName family name
   * @return Family
   */
  public static Family loadMalware(String path, String familyName) {
    Family result = new Family(familyName);
    Malware malware;

    File file = new File(path);
    String malwareName = file.getName();

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      malware = new Malware(malwareName, result);

      String code;

      while ((code = reader.readLine()) != null) {
        code = code.trim();
        if (code.length() > 0) {
          malware.addBasicBlock(code);
          result.addBasicBlock(code);
        }
      }

      result.addMalware(malware);
    } catch (Exception ex) {
      Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
    }

    return result;
  }
}
