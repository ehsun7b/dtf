package vida.phd.tfd.entity;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Basic Block class
 * @author Vida
 */
public class BasicBlock implements Comparable<BasicBlock> {

  /**
   * Hash code of the BasicBlock
   */
  private final String code;
  
  /**
   * Count of occurrences of the BasicBlock in the current context (family/malware)
   */
  private int count;
  
  /**
   * Term frequency ratio of the BasicBlock in the current context (family/malware)
   */
  private double termFrequencyRatio;
  
  /**
   * Distributed term frequency of the BasicBlock in the current context (family/malware)
   */
  private double distributionTermFrequency;

  /**
   * Malware distribution frequency
   */
  private double MDF;
  
  /**
   * Family classifier
   */
  private double FC;

  /**
   * Constructor which takes the hash (code) of basic block as the only parameter.
   * @param code
   * @throws Exception 
   */
  public BasicBlock(String code) throws Exception {
    if (code != null && code.trim().length() > 0) {
      this.code = code;
      this.count = 1;
    } else {
      throw new Exception("Code of BB is null or empty!");
    }
  }

  /**
   * 
   * @return hash of the basic block
   */
  public String getCode() {
    return code;
  }

  /**
   * 
   * @return count of occurrences of the basic block in the context (family/malware)
   */
  public int getCount() {
    return count;
  }
  
  /**
   * Increments the count of occurrences by one 
   */
  public void incCound() {
    this.count++;
  }

  /**
   * getter for termFrequencyRatio
   * @return termFrequencyRatio
   */
  public double getTermFrequencyRatio() {
    return termFrequencyRatio;
  }

  /**
   * setter for termFrequencyRatio
   * @param termFrequencyRatio 
   */
  public void setTermFrequencyRatio(double termFrequencyRatio) {
    this.termFrequencyRatio = termFrequencyRatio;
  }

  /**
   * getter for distributionTermFrequency
   * @return distributionTermFrequency
   */
  public double getDistributionTermFrequency() {
    return distributionTermFrequency;
  }

  /**
   * setter for distributionTermFrequency
   * @param distributionTermFrequency 
   */
  public void setDistributionTermFrequency(double distributionTermFrequency) {
    this.distributionTermFrequency = distributionTermFrequency;
  }

  /**
   * 
   * @return unique hash value
   */
  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.code);
    return hash;
  }

  /**
   * Compares the object with the given BasicBlock object based on their hash code
   * @param obj
   * @return boolean comparison result
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final BasicBlock other = (BasicBlock) obj;
    if (!Objects.equals(this.code, other.code)) {
      return false;
    }
    return true;
  }

  /**
   * Displays string representation of the basic block by printing its attributes
   * @return 
   */
  @Override
  public String toString() {
    DecimalFormat df = new DecimalFormat("###,###.############");

    return "BasicBlock{" + "Hash=" + code + ", count: " + count + ", TFR: " + df.format(termFrequencyRatio) + ", TFD: " + df.format(distributionTermFrequency) + "}";
  }

  /**
   * Compares the object with the given BasicBlock object based on their hash code
   * @param bb
   * @return int comparison result
   */
  @Override
  public int compareTo(BasicBlock bb) {
    if (distributionTermFrequency > bb.distributionTermFrequency) {
      return 1;
    } else if (distributionTermFrequency == bb.distributionTermFrequency) {
      return 0;
    } else {
      return -1;
    }
  }

  /**
   * 
   * @return MDF
   */
  public double getMDF() {
    return MDF;
  }

  /**
   * Sets MDF
   * @param MDF 
   */
  public void setMDF(double MDF) {
    this.MDF = MDF;
  }

  /**
   * 
   * @return FC 
   */
  public double getFC() {
    return FC;
  }

  /**
   * Sets FC
   * @param FC 
   */
  public void setFC(double FC) {
    this.FC = FC;
  }
}
