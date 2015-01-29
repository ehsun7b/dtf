package vida.phd.dtf.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import vida.phd.dtf.entity.BasicBlock;
import vida.phd.dtf.entity.Family;

public class FamilyLoader extends Observable implements Runnable {

  private final File directory;
  private final String familyName;
  private File[] files;
  private List<File> failedFiles;

  public FamilyLoader(File directory, String familyName) {
    this.directory = directory;
    this.familyName = familyName;
  }

  @Override
  public void run() {

    if (directory.isDirectory()) {
      files = directory.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
          return file.isFile() && file.getName().toLowerCase().endsWith(".txt");
        }
      });

      failedFiles = new ArrayList<>();

      Family family = new Family(familyName);

      for (File file : files) {
        String malwareName = file.getName();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
          String code;

          while ((code = reader.readLine()) != null) {
            code = code.trim();
            if (code.length() > 0) {
              BasicBlock basicBlock = family.findByCode(code);
              if (basicBlock != null) {
                basicBlock.setCount(basicBlock.getCount() + 1);
              } else {
                BasicBlock newBB = new BasicBlock(code, malwareName);
                family.getBasicBlocks().put(code, newBB);
              }
            }
          }
        } catch (IOException ex) {
          Logger.getLogger(FamilyLoader.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
      }

      setChanged();
      notifyObservers(family);
    }
  }

  public File getDirectory() {
    return directory;
  }

  public String getFamilyName() {
    return familyName;
  }

  /*
  public static void main(String[] args) {
    FamilyLoader loader = new FamilyLoader(new File("/home/ehsun7b/Dropbox/Yas-distros/Families/Agent"), "Agent");
    loader.addObserver(new Observer() {

      @Override
      public void update(Observable o, Object arg) {
        if (arg instanceof Family) {
          Family fam = (Family) arg;
          HashMap<String, BasicBlock> basicBlocks = fam.getBasicBlocks();
          System.out.println(basicBlocks.size());

          Iterator it = basicBlocks.entrySet().iterator();
          while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());            
          }
        }
      }
    });
    Thread thread = new Thread(loader);
    try {
      thread.join();
    } catch (InterruptedException ex) {
      Logger.getLogger(FamilyLoader.class.getName()).log(Level.SEVERE, null, ex);
    }

    thread.start();
  }*/
}
