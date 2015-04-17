package vida.phd.tfd;

import com.google.common.io.Files;
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

/**
 * TFD class. Main class of the program
 * @author ehsun7b
 */
public class TFD {

  /**
   * Scoring type for classifying algo
   */
  public enum ScoreType {
    FAM_CLASSIFIER, TFD;
  }

  /**
   * The directory from which all families get loaded
   */
  protected File familiesHome;
  
  /**
   * HashMap to keep all families
   */
  protected volatile HashMap<String, Family> families;
  
  /**
   * Number of threads for initial loading of the families concurrently
   */
  protected static final int THREADS = 10;
  
  /**
   * The new malware which is about to be classified
   */
  protected Malware candidateMalware;
  
  /**
   * Temporary family that a new malware will be assigned to before classification happens
   */
  protected Family candidateFamily;
  
  /**
   * The successfully detected family for the new malware after classification happens
   */
  protected Family detectedFamily;
  
  /**
   * HashMap to keep all families scores for the new malware
   */
  private HashMap<String, Integer> scores;
  
  /**
   * Arbitrary name for the temporary family
   * @see candidateFamily
   */
  private final String CANDIDATE_FAMILY_NAME = "__candidate_family";

  /**
   * Constructor which takes a Directory as the familiesHome
   * @param familiesHome 
   */
  public TFD(File familiesHome) {
    this.familiesHome = familiesHome;
    families = new HashMap<>();
  }

  /**
   * Default constructor
   */
  public TFD() {
    families = new HashMap<>();
  }

  /**
   * Load all families from the familiesHome
   * @throws IOException 
   */
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

  /**
   * Calculates TermFrequencyRatio for all BasicBlocks of all Malware
   */
  public void calculateTermFrequencyRatio() {
    //System.out.println("Calculating term frequency ratio...");
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

  /**
   * Calculates DistributedTermFrequency of all BasicBlocks in all Malware
   */
  public void calculateDistributionTermFrequency() {
    //System.out.println("Calculating distribution term frequency...");
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
        double distributionTermFrequency = termFrequencyRatio - sumOfTermFrequencyRatio;

        basicBlock.setDistributionTermFrequency(distributionTermFrequency);
      }
    }
  }

  /**
   * Find all families which has the given BasicBlock (code)
   * @param code
   * @return 
   */
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

  /**
   * Find the top BasicBlocks in the given family. 
   * @param familyName
   * @param count
   * @return
   * @throws Exception 
   */
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

  /**
   * Sum of the TermFrequencyRatio of all families excluding the given family.
   * @param bbcode
   * @param exclude
   * @return 
   */
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

  /**
   * Sort the given HashMap of BasicBlocks based on TFD
   * @param map
   * @return 
   */
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

  /**
   * 
   * @return familiesHome
   */
  public File getFamiliesHome() {
    return familiesHome;
  }

  /**
   * 
   * @param familiesHome 
   */
  public void setFamiliesHome(File familiesHome) {
    this.familiesHome = familiesHome;
  }

  /**
   * 
   * @return families
   */
  public HashMap<String, Family> getFamilies() {
    return families;
  }

  /**
   * Find the given BasicBlock in all families
   * @param code
   * @return 
   */
  public BasicBlock getBasicBlockByCode(String code) {
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

  /**
   * Prints the current status of the in memory DB. Including count of Families, Malware and BasicBlocks
   * @param details 
   */
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

  /**
   * Find all malware which contain the given BasicBlock
   * @param code
   * @return 
   */
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

  /**
   * Top BasicBlock (based on TFD) in the given Family.
   * @param familyName
   * @return
   * @throws Exception 
   */
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

  /**
   * Find all FamilyBasicBlock occurrences by the given BasicBlock sorted by TermFrequencyDistribution
   * @param code
   * @return 
   */
  public List<FamilyBasicBlock> allOccurencesByHash(String code) {
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

  /**
   * Find all FamilyBasicBlock occurrences by the given BasicBlock sorted by FamilyClassifier
   * @param code
   * @return 
   */
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
          return -1;
        } else {
          return 0;
        }
      }
    });

    Collections.reverse(result);

    return result;
  }

  /**
   * Gets count of all malware in the given family which contain the given BasicBlock
   * @param family
   * @param code
   * @return 
   */
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

  /**
   * FamilyBasicBlock class for detecting occurrences
   */
  public class FamilyBasicBlock implements Comparable<FamilyBasicBlock> {

    /**
     * Family
     */
    private Family family;
    
    /**
     * BasicBlock
     */
    private BasicBlock basicBlock;

    /**
     * Constructor which takes Family and BasicBlock as its parameters
     * @param family
     * @param bb 
     */
    private FamilyBasicBlock(Family family, BasicBlock bb) {
      this.family = family;
      this.basicBlock = bb;
    }

    /**
     * 
     * @return family
     */
    public Family getFamily() {
      return family;
    }

    /**
     * 
     * @param family 
     */
    public void setFamily(Family family) {
      this.family = family;
    }

    /**
     * 
     * @return basicBlock
     */
    public BasicBlock getBasicBlock() {
      return basicBlock;
    }

    /**
     * 
     * @param basicBlock 
     */
    public void setBasicBlock(BasicBlock basicBlock) {
      this.basicBlock = basicBlock;
    }

    /**
     * Compares the FamilyBasicBlock with the given FamilyBasicBlock based on their BasicBlocks
     * @param o
     * @return 
     */
    @Override
    public int compareTo(FamilyBasicBlock o) {
      return basicBlock.compareTo(o.basicBlock);
    }
  }

  /**
   * Calculates the FamilyClassifier
   */
  protected void calculateFCs() {
    //System.out.println("Calculating Family Classifier...");
    Iterator<Map.Entry<String, Family>> iterator = families.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Family> pair = iterator.next();
      Family family = pair.getValue();

      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      Iterator<Map.Entry<String, BasicBlock>> it = basicBlocks.entrySet().iterator();

      while (it.hasNext()) {
        Map.Entry<String, BasicBlock> bbPair = it.next();
        BasicBlock basicBlock = bbPair.getValue();

        double mdf = basicBlock.getMDF();
        double tfd = basicBlock.getDistributionTermFrequency();
        double fc = mdf * tfd;
        //System.out.println("----- FC: " + fc);
        basicBlock.setFC(fc);
      }
    }
  }

  /**
   * Calculates malware distribution frequency
   */
  protected void calculateMDFs() {
    //System.out.println("Calculating MDF...");
    Iterator<Map.Entry<String, Family>> iterator = families.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Family> pair = iterator.next();
      Family family = pair.getValue();

      HashMap<String, BasicBlock> basicBlocks = family.getBasicBlocks();
      Iterator<Map.Entry<String, BasicBlock>> it = basicBlocks.entrySet().iterator();

      while (it.hasNext()) {
        Map.Entry<String, BasicBlock> bbPair = it.next();
        BasicBlock basicBlock = bbPair.getValue();

        int malwaresCount = this.countOfMalwaresInFamilyByBB(family, basicBlock.getCode());
        int countOfMalwares = family.getMalwares().size();
        double mdf = (double) malwaresCount / (double) countOfMalwares;
        //System.out.println("----- MDF: " + mdf);
        basicBlock.setMDF(mdf);
      }
    }
  }

  /**
   * Scores all families for the new malware based on the ScoreType parameter
   * @param type 
   */
  public void score(ScoreType type) {
    scores = new HashMap<>();

    final Iterator<Map.Entry<String, BasicBlock>> itBB = candidateMalware.getBasicBlocks().entrySet().iterator();
    while (itBB.hasNext()) {
      final BasicBlock bb = itBB.next().getValue();

      List<FamilyBasicBlock> familyBasicBlocks = null;

      if (type == ScoreType.TFD) {
        familyBasicBlocks = allOccurencesByHash(bb.getCode());
      } else if (type == ScoreType.FAM_CLASSIFIER) {
        familyBasicBlocks = allOccurancesByHashSortByFC(bb.getCode());
      }

      if (familyBasicBlocks != null && familyBasicBlocks.size() > 0) {
        FamilyBasicBlock familyBasicBlock = familyBasicBlocks.get(0);
        addScore(familyBasicBlock.getFamily().getName());
      }
    }
  }

  /**
   * Based on the scores of families, finds the best matching family
   */
  public void findResultFamily() {
    final Iterator<String> iterator = scores.keySet().iterator();
    Integer max = 0;
    String famKey = null;

    while (iterator.hasNext()) {
      final String next = iterator.next();
      final Integer score = scores.get(next);

      if (score > max) {
        max = score;
        famKey = next;
      }
    }

    detectedFamily = families.get(famKey);
  }

  /**
   * Adds the score of the given family to the scores HashMap
   * @param name 
   */
  private void addScore(String name) {
    Integer score = scores.get(name);

    if (score == null) {
      score = 1;
    } else {
      score++;
    }

    scores.put(name, score);
  }

  /**
   * Prints the families' scores
   */
  public void showScores() {
    Iterator<String> iterator = scores.keySet().iterator();
    while (iterator.hasNext()) {
      String family = iterator.next();
      Integer score = scores.get(family);
      System.out.println("Family: " + family + ": " + score);
    }
  }

  /**
   * Adds the given malware file to the in memory DB based on the ScoringType parameter
   * @param malwareFile
   * @param type
   * @param showInfo
   * @return 
   */
  public int add(String malwareFile, ScoreType type, boolean showInfo) {
    File file = new File(malwareFile);
    int result = 0;
    if (file.isDirectory()) {
      final File[] files = file.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().toLowerCase().endsWith(".txt");
        }
      });

      for (File f : files) {
        //try {
        addCandidate(f.getAbsolutePath(), type, showInfo);
        System.out.println("--------------------------------------------------------------------");
        result++;
        //} catch (Exception ex) {
        //ex.printStackTrace();
        //}
      }

      return result;
    } else if (file.isFile()) {
      addCandidate(malwareFile, type, showInfo);
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Adds a file or all files of a directory to the in memory DB
   * @param malwareFile
   * @param type
   * @param showInfo 
   */
  public void addCandidate(String malwareFile, ScoreType type, boolean showInfo) {
    final long startTime = System.nanoTime();
    detectedFamily = null;
    candidateFamily = Loader.loadMalware(malwareFile, CANDIDATE_FAMILY_NAME);
    candidateMalware = candidateFamily.getMalwares().entrySet().iterator().next().getValue();

    if (showInfo) {
      System.out.println("Updating the database ...");
    }
    calculateTermFrequencyRatio();
    calculateDistributionTermFrequency();

    if (type == ScoreType.FAM_CLASSIFIER) {
      calculateMDFs();
      calculateFCs();
    }

    score(type);
    findResultFamily();
    if (showInfo) {
      final long endTime = System.nanoTime();
      final long timeNano = endTime - startTime;
      final Double timeSec = (double) timeNano / 1000000000.0;
      System.out.println("Duration time: " + timeSec + " seconds");
      showScores();
    }
    //System.out.println("Suggested family is: " + detectedFamily.getName());
    if (detectedFamily == null) {
      System.out.print("Malware can not be classified: ");
      System.out.println(malwareFile);
      System.out.println("");
    } else {
      String directoryName = detectedFamily.getName();
      File sourceFile = new File(malwareFile);
      File destinationFile = new File(this.familiesHome + "/" + directoryName + "/" + sourceFile.getName());
      try {
        Files.copy(sourceFile, destinationFile);
        System.out.println(sourceFile.getName() + " was copied to: " + directoryName + " family.");
      } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error in copying the file: " + sourceFile.getAbsolutePath());
      }
    }
  }
}
