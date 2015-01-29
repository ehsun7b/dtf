package vida.phd.dtf;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import vida.phd.dtf.entity.BasicBlock;
import vida.phd.dtf.entity.Family;
import vida.phd.dtf.io.Loader;

public class DTF {

  private File familiesHome;
  private volatile HashMap<String, Family> families;
  private static final int THREADS = 10;

  public DTF(File familiesHome) {
    this.familiesHome = familiesHome;
    families = new HashMap<>();
  }

  public void loadFamilies() throws IOException {
    int countOfBB = 0;

    if (familiesHome.exists() && familiesHome.isDirectory()) {
      File[] directories = familiesHome.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
          return file.isDirectory();
        }
      });

      ExecutorService executor = Executors.newFixedThreadPool(THREADS);
      List<Future<Family>> futures = new ArrayList<>();

      for (File directory : directories) {
        Loader loader = new Loader(directory, directory.getName());
        Future<Family> submit = executor.submit(loader);
        futures.add(submit);
      }

      executor.shutdown();

      for (Future<Family> future : futures) {
        try {
          Family family = future.get();
          removeDuplications(family);
          families.put(family.getName(), family);
          System.out.println("Family loaded: " + family.getName() + " " + family.countOfBasicBlocks());
          countOfBB += family.countOfBasicBlocks();
        } catch (InterruptedException | ExecutionException ex) {
          Logger.getLogger(DTF.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    } else {
      throw new IOException(MessageFormat.format("{0} does not exist or is not a directory!", familiesHome.getAbsolutePath()));
    }

    System.out.println(MessageFormat.format("{0} familes and {1} basic blocks loaded.", families.size(), countOfBB + ""));
  }

  public void calculateTermFrequencyRatio() {
    System.out.println("Calculating term frequency ratio...");
    Iterator<Map.Entry<String, Family>> iterator = families.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Family> pair = iterator.next();
      Family family = pair.getValue();

      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      Iterator<Map.Entry<String, BasicBlock>> it = basicBlocks.entrySet().iterator();

      int totalBasicBlocks = family.countOfBasicBlocks();

      while (it.hasNext()) {
        Map.Entry<String, BasicBlock> bbPair = it.next();
        BasicBlock basicBlock = bbPair.getValue();

        int count = basicBlock.getCount();
        double tfr = (double) count / (double) totalBasicBlocks;
        
        if (tfr == 0) {
          System.out.println("!");
        }

        basicBlock.setTermFrequencyRatio(tfr);
        //System.out.println("bb: " + basicBlock.getMalwareName() + " " + basicBlock.getCode() + " : " + tfr);
      }
    }
  }

  public void calculateDistributionTermFrequency() {
    System.out.println("Calculating distribution term frequency...");
    Iterator<Map.Entry<String, Family>> iterator = families.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Family> pair = iterator.next();
      Family family = pair.getValue();

      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      Iterator<Map.Entry<String, BasicBlock>> it = basicBlocks.entrySet().iterator();

      while (it.hasNext()) {
        Map.Entry<String, BasicBlock> bbPair = it.next();
        BasicBlock basicBlock = bbPair.getValue();

        double termFrequencyRatio = basicBlock.getTermFrequencyRatio();
        double sumOfTermFrequencyRatio = sumOfTermFrequencyRatio(basicBlock.getCode(), family);
        
        if (sumOfTermFrequencyRatio == 0) {
          int gg = 10;
        }
        
        double distributionTermFrequency = termFrequencyRatio - sumOfTermFrequencyRatio;

        basicBlock.setDistributionTermFrequency(distributionTermFrequency);
      }
    }
  }

  public List<Family> findFamiliesByBB(String code) {
    List<Family> result = new ArrayList<>();

    Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();

    while (it.hasNext()) {
      Family family = it.next().getValue();
      if (family.getBasicBlocks().containsKey(code)) {
        result.add(family);
      }
    }

    return result;
  }

  public List<BasicBlock> topByFamily(String familyName, int count) throws Exception {
    List<BasicBlock> result = new ArrayList<>(count);

    Family family = families.get(familyName);

    if (family != null) {
      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      List<BasicBlock> sorted = sortByValue(basicBlocks);

      for (int i = sorted.size() - 1; i > sorted.size() - (count + 1); i--) {
        result.add(sorted.get(i));
      }

    } else {
      throw new Exception("Family " + familyName + " not found.");
    }

    return result;
  }

  protected double sumOfTermFrequencyRatio(String bbcode, Family exclude) {
    double result = 0;
    List<Family> famList = findFamiliesByBB(bbcode);

    for (Family family : famList) {
      if (!family.equals(exclude)) {
        double termFrequencyRatio = family.getBasicBlocks().get(bbcode).getTermFrequencyRatio();
        result += termFrequencyRatio;
      }
    }

    return result;
  }

  public static List<BasicBlock> sortByValue(Map<String, BasicBlock> map) {
    List<BasicBlock> list = new LinkedList<>();

    Iterator<Map.Entry<String, BasicBlock>> it = map.entrySet().iterator();

    while (it.hasNext()) {
      BasicBlock bb = it.next().getValue();
      list.add(bb);
    }

    Collections.sort(list);
    return list;
  }

  private void removeDuplications(Family family) {
    HashMap<String, String> malwares = family.getMalwares();
/*
    Collection<String> values = malwares.values();
    for (String malware : values) {
      if (malwareExists(malware, family)) {
        malwares.remove(malware);
        removeBasicBlocks(family, malware);
      }
    }*/
    
    Iterator<Map.Entry<String, String>> it = malwares.entrySet().iterator();
    while (it.hasNext()) {
      String malware = it.next().getValue();
      if (malwareExists(malware, family)) {
        it.remove();
        removeBasicBlocks(family, malware);
      }
    }
  }

  private boolean malwareExists(String malware, Family exclude) {
    boolean result = false;

    Collection<Family> values = families.values();
    for (Family family : values) {
      if (!family.equals(exclude)) {
        String found = family.getMalwares().get(malware);
        if (found != null) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private void removeBasicBlocks(Family family, String malware) {
    /*
    HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
    for (BasicBlock bb : basicBlocks.values()) {
      if (bb.getMalwareName().equals(malware)) {
        //basicBlocks.remove(bb.getCode());
      }
    }*/
    
    Iterator<Map.Entry<String, BasicBlock>> it = family.getBasicBlocks().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, BasicBlock> next = it.next();      
      BasicBlock bb = next.getValue();
      if (bb.getMalwareName().equals(malware)) {
        it.remove();
      }
    }
  }

  public File getFamiliesHome() {
    return familiesHome;
  }

  public void setFamiliesHome(File familiesHome) {
    this.familiesHome = familiesHome;
  }

  public HashMap<String, Family> getFamilies() {
    return families;
  }

  /*
   public static void main(String[] args) {
   DTF dtf = new DTF(new File("/home/ehsun7b/Dropbox/Yas-distros/Families"));
   try {
   dtf.loadFamilies();
   } catch (IOException ex) {
   Logger.getLogger(DTF.class.getName()).log(Level.SEVERE, null, ex);
   }

   System.out.println("calculating TFR...");
   dtf.calculateTermFrequencyRatio();
   System.out.println("calculating DTF...");
   dtf.calculateDistributionTermFrequency();
    
   Iterator<Map.Entry<String, Family>> iterator = dtf.getFamilies().entrySet().iterator();
   while (iterator.hasNext()) {
   Map.Entry<String, Family> pair = iterator.next();
   Family family = pair.getValue();

   HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
   Iterator<Map.Entry<String, BasicBlock>> it = basicBlocks.entrySet().iterator();

   int totalBasicBlocks = basicBlocks.size();

   while (it.hasNext()) {
   Map.Entry<String, BasicBlock> bbPair = it.next();
   BasicBlock basicBlock = bbPair.getValue();

   System.out.println("bb: " + basicBlock.getMalwareName() + " " + basicBlock.getCode() + " : " + basicBlock.getTermFrequencyRatio() + " - " + basicBlock.getDistributionTermFrequency());
   }
   }
   try {
   List<BasicBlock> top = dtf.topByFamily("Agent", 10);
   for (BasicBlock bb : top) {
   System.out.println(bb);

   }
   } catch (Exception ex) {
   Logger.getLogger(DTF.class.getName()).log(Level.SEVERE, null, ex);
   }

   }*/
}
