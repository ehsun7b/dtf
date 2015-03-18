package vida.phd.tfd.classify;

import vida.phd.tfd.TFD;
import vida.phd.tfd.entity.BasicBlock;
import vida.phd.tfd.entity.Family;
import vida.phd.tfd.entity.Malware;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Classify extends TFD {

  private static final String TEMP_FAMILY = "--temp--family";
  private Malware malware;
  private Family resultFamily, tempFamily;
  private HashMap<String, Integer> scores;

  public Classify(File familiesHome, Malware malware) {
    super(familiesHome);
    this.malware = malware;
  }

  public void init() throws Exception {
    this.loadFamilies();

    tempFamily = createTempFamily();
    malware.setFamily(tempFamily);
    families.put(TEMP_FAMILY, tempFamily);

    this.calculateTermFrequencyRatio();
    this.calculateDistributionTermFrequency();
    this.calculateMDFs();
    this.calculateFCs();

    showStatus(false);
  }

  private Family createTempFamily() throws Exception {
    Family result = new Family(TEMP_FAMILY);

    result.addMalware(malware);

    Iterator<Map.Entry<String, BasicBlock>> iterator = malware.getBasicBlocks().entrySet().iterator();
    while (iterator.hasNext()) {
      BasicBlock bb = iterator.next().getValue();

      for (int i = 0; i < bb.getCount(); ++i) { // to have a correct count
        result.addBasicBlock(bb.getCode());
      }
    }

    return result;
  }

  private void calculateFCs() {
    System.out.println("Calculating Family Classifier...");
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

  private void calculateMDFs() {
    System.out.println("Calculating MDF...");
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

  public void score() {
    scores = new HashMap<>();

    Iterator<Map.Entry<String, Family>> itF = getFamilies().entrySet().iterator();
    while (itF.hasNext()) {
      Map.Entry<String, Family> nextF = itF.next();
      Family family = nextF.getValue();

      Iterator<Map.Entry<String, Malware>> itM = family.getMalwares().entrySet().iterator();
      while (itM.hasNext()) {
        Map.Entry<String, Malware> nextM = itM.next();
        Malware malware = nextM.getValue();

        if (malware.getName().equals(this.malware.getName())) {
          HashMap<String, BasicBlock> basicBlocks = malware.getBasicBlocks();
          Iterator<Map.Entry<String, BasicBlock>> itB = basicBlocks.entrySet().iterator();
          while (itB.hasNext()) {
            Map.Entry<String, BasicBlock> nextB = itB.next();
            BasicBlock bbM = nextB.getValue();

            List<FamilyBasicBlock> occurances = allOccurancesByHashSortByFC(bbM.getCode());

            if (occurances.size() > 0) {
              FamilyBasicBlock familyBasicBlock = occurances.get(0);
              addScore(familyBasicBlock.getFamily().getName());
            }

            System.out.println(bbM.getCode() + " - ");
          }
          break;
        }
      }
    }
  }

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

    resultFamily = families.get(famKey);
  }

  public void showScores() {
    Iterator<String> iterator = scores.keySet().iterator();
    while (iterator.hasNext()) {
      String family = iterator.next();
      Integer score = scores.get(family);
      System.out.println("Family: " + family + ": " + score);
    }
  }

  private void addScore(String name) {
    Integer score = scores.get(name);

    if (score == null) {
      score = 1;
    } else {
      score++;
    }

    scores.put(name, score);
  }

  public Family getResultFamily() {
    return resultFamily;
  }
}
