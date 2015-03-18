package vida.phd.tfd;

import vida.phd.tfd.entity.BasicBlock;
import vida.phd.tfd.entity.Family;
import vida.phd.tfd.entity.Malware;
import vida.phd.tfd.io.Loader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TFD {

  protected File familiesHome;
  protected volatile HashMap<String, Family> families;
  protected static final int THREADS = 10;

  public TFD(File familiesHome) {
    this.familiesHome = familiesHome;
    families = new HashMap<>();
  }

  public TFD() {
    families = new HashMap<>();
  }

  public void loadFamilies() throws IOException {
    int countOfBB = 0;
    int countOfMalwares = 0;
    int countOfFamilies = 0;

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
          //removeDuplications(family);
          if (families.containsKey(family.getName())) {
            System.out.println("Family " + family.getName() + " already exists. Not loaded again!");
          } else {
            families.put(family.getName(), family);
            System.out.println("Family loaded: " + family.getName() + " " + family.countOfBasicBlocks());
            countOfBB += family.countOfBasicBlocks();
            countOfFamilies++;
            countOfMalwares += family.getMalwares().size();
          }
        } catch (InterruptedException | ExecutionException ex) {
          Logger.getLogger(TFD.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    } else {
      throw new IOException(MessageFormat.format("{0} does not exist or is not a directory!", familiesHome.getAbsolutePath()));
    }

    System.out.println(MessageFormat.format("{0} familes, {1} malwares and {2} basic blocks loaded.", countOfFamilies, countOfMalwares, countOfBB + ""));
    showStatus(false);
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

        /*
         if (sumOfTermFrequencyRatio == 0) {
         int gg = 10;
         }*/
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

  /*
   private void removeDuplications(Family family) {
   HashMap<String, Malware> malwares = family.getMalwares();
    
   Iterator<Map.Entry<String, Malware>> it = malwares.entrySet().iterator();
   while (it.hasNext()) {
   Malware malware = it.next().getValue();
   if (malwareExists(malware, family)) {
   it.remove();
   removeBasicBlocks(family, malware);
   }
   }
   }*/

  /*
   private boolean malwareExists(Malware malware, Family exclude) {
   boolean result = false;

   Collection<Family> values = families.values();
   for (Family family : values) {
   if (!family.equals(exclude)) {
   Malware found = family.getMalwares().get(malware);
   if (found != null) {
   result = true;
   break;
   }
   }
   }

   return result;
   }*/

  /*
   private void removeBasicBlocks(Family family, Malware malware) {    
   Iterator<Map.Entry<String, BasicBlock>> it = family.getBasicBlocks().entrySet().iterator();
   while (it.hasNext()) {
   Map.Entry<String, BasicBlock> next = it.next();
   BasicBlock bb = next.getValue();
   if (bb.getMalwares().contains(malware)) {
   System.out.println("removed: " + bb.toString());
   it.remove();
   }
   }
   }*/
  public File getFamiliesHome() {
    return familiesHome;
  }

  public void setFamiliesHome(File familiesHome) {
    this.familiesHome = familiesHome;
  }

  public HashMap<String, Family> getFamilies() {
    return families;
  }

  BasicBlock getBasicBlockByCode(String code) {
    BasicBlock result = null;
    Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();

    while (it.hasNext()) {
      Family family = it.next().getValue();
      result = family.getBasicBlocks().get(code);

      if (result != null) {
        break;
      }
    }

    return result;
  }

  public void showStatus(boolean details) {
    int countOfBBs = 0;
    int countOfMalwares = 0;
    int countOfDistBBs = 0;

    Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Family> next = it.next();
      Family family = next.getValue();

      Iterator<Map.Entry<String, BasicBlock>> it2 = family.getBasicBlocks().entrySet().iterator();

      while (it2.hasNext()) {
        Map.Entry<String, BasicBlock> next2 = it2.next();
        BasicBlock bb = next2.getValue();
        countOfBBs += bb.getCount();
        countOfDistBBs++;
      }

      countOfMalwares += family.getMalwares().size();
    }

    System.out.println("Current status");
    System.out.println("Total count of families: " + families.size());
    System.out.println("Total count of malwares: " + countOfMalwares);
    System.out.println("Total count of basic blocks: " + countOfBBs);
    System.out.println("Total count of distinguished basic blocks: " + countOfDistBBs);
    System.out.println("");
  }

  public Set<Malware> findMalwaresByBBCode(final String code) {
    Set<Malware> malwares = new HashSet<>();

    Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();

    while (it.hasNext()) {
      Map.Entry<String, Family> nextF = it.next();
      Family family = nextF.getValue();

      Iterator<Map.Entry<String, Malware>> itM = family.getMalwares().entrySet().iterator();
      while (itM.hasNext()) {
        Map.Entry<String, Malware> nextM = itM.next();
        Malware malware = nextM.getValue();

        if (malware.getBasicBlocks().containsKey(code)) {
          malwares.add(malware);
        }
      }
    }

    return malwares;
  }

  public List<BasicBlock> topByFamily(String familyName) throws Exception {
    List<BasicBlock> result = new ArrayList<>();

    Family family = families.get(familyName);

    if (family != null) {
      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      List<BasicBlock> sorted = sortByValue(basicBlocks);

      for (int i = sorted.size() - 1; i >= 0; i--) {
        result.add(sorted.get(i));
      }

    } else {
      throw new Exception("Family " + familyName + " not found.");
    }

    return result;
  }

  public List<FamilyBasicBlock> allOccurancesByHash(String code) {
    List<FamilyBasicBlock> result = new ArrayList<>();

    List<Family> families = findFamiliesByBB(code);

    for (Family family : families) {
      BasicBlock bb = family.getBasicBlocks().get(code);
      if (bb != null) {
        result.add(new FamilyBasicBlock(family, bb));
      }
    }

    Collections.sort(result);
    Collections.reverse(result);

    return result;
  }

  public List<FamilyBasicBlock> allOccurancesByHashSortByFC(String code) {
    List<FamilyBasicBlock> result = new ArrayList<>();

    List<Family> families = findFamiliesByBB(code);

    for (Family family : families) {
      BasicBlock bb = family.getBasicBlocks().get(code);
      if (bb != null) {
        result.add(new FamilyBasicBlock(family, bb));
      }
    }

    Collections.sort(result, new Comparator<FamilyBasicBlock>() {
      @Override
      public int compare(FamilyBasicBlock o1, FamilyBasicBlock o2) {
        if (o1.getBasicBlock().getFC() > o2.getBasicBlock().getFC()) {
          return 1;
        } else if (o1.getBasicBlock().getFC() < o2.getBasicBlock().getFC()) {
          return 0;
        } else {
          return -1;
        }
      }
    });

    Collections.reverse(result);

    return result;
  }

  public int countOfMalwaresInFamilyByBB(Family family, String code) {
    Iterator<Map.Entry<String, Malware>> it = family.getMalwares().entrySet().iterator();
    int result = 0;

    while (it.hasNext()) {
      Malware malware = it.next().getValue();
      Iterator<Map.Entry<String, BasicBlock>> it2 = malware.getBasicBlocks().entrySet().iterator();

      while (it2.hasNext()) {
        BasicBlock bb = it2.next().getValue();
        if (bb.getCode().equals(code)) {
          result++;
        }
      }
    }

    return result;
  }

  public class FamilyBasicBlock implements Comparable<FamilyBasicBlock> {

    private Family family;
    private BasicBlock basicBlock;

    private FamilyBasicBlock(Family family, BasicBlock bb) {
      this.family = family;
      this.basicBlock = bb;
    }

    public Family getFamily() {
      return family;
    }

    public void setFamily(Family family) {
      this.family = family;
    }

    public BasicBlock getBasicBlock() {
      return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
      this.basicBlock = basicBlock;
    }

    @Override
    public int compareTo(FamilyBasicBlock o) {
      return basicBlock.compareTo(o.basicBlock);
    }
  }
}
