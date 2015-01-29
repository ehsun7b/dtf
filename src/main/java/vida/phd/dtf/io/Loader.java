package vida.phd.dtf.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import vida.phd.dtf.entity.BasicBlock;
import vida.phd.dtf.entity.Family;

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
          String code;

          while ((code = reader.readLine()) != null) {
            code = code.trim();
            if (code.length() > 0) {
              BasicBlock basicBlock = family.findByCode(code);
              if (basicBlock != null) {
                basicBlock.setCount(basicBlock.getCount() + 1);
              } else {
                BasicBlock newBB = new BasicBlock(code, malwareName);
                family.getMalwares().put(malwareName, malwareName);
                family.getBasicBlocks().put(code, newBB);
              }
            }
          }
        } catch (IOException ex) {
          Logger.getLogger(FamilyLoader.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
  /*
   public static void main(String[] args) {
   ExecutorService executor = Executors.newFixedThreadPool(10);
   List<Future<Family>> families = new ArrayList<>();
    
   Loader loader = new Loader(new File("/home/ehsun7b/Dropbox/Yas-distros/Families/Agent"), "agent");
   Future<Family> submit = executor.submit(loader);
   families.add(submit);
    
   Loader loader2 = new Loader(new File("/home/ehsun7b/Dropbox/Yas-distros/Families/Dropper"), "dropper");
   Future<Family> submit2 = executor.submit(loader2);
   families.add(submit2);
    
   for (Future<Family> family : families) {
   try {
   String name = family.get().getName();
   int size = family.get().getBasicBlocks().size();
   System.out.println(name + ": " + size);
   } catch (InterruptedException ex) {
   Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
   } catch (ExecutionException ex) {
   Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
   }
      
      
   }
   }*/
}
