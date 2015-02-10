package vida.phd.dtf.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import vida.phd.dtf.entity.BasicBlock;
import vida.phd.dtf.entity.Family;
import vida.phd.dtf.entity.Malware;

public class Loader implements Callable<Family> {

  private final File directory;
  private final String familyName;
  private File[] files;

  public Loader(File directory, String familyName) {
    this.directory = directory;
    this.familyName = familyName;
  }

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

  public File getDirectory() {
    return directory;
  }

  public String getFamilyName() {
    return familyName;
  }  
}
