package vida.phd.tfd.entity;

import java.text.DecimalFormat;
import java.util.Objects;

public class BasicBlock implements Comparable<BasicBlock> {

  private final String code;
  private int count;
  private double termFrequencyRatio;
  private double distributionTermFrequency;

  private double MDF;
  private double FC;

  public BasicBlock(String code) throws Exception {
    if (code != null && code.trim().length() > 0) {
      this.code = code;
      this.count = 1;
    } else {
      throw new Exception("Code of BB is null or empty!");
    }
  }

  public String getCode() {
    return code;
  }

  public int getCount() {
    return count;
  }
  
  public void incCound() {
    this.count++;
  }

  public double getTermFrequencyRatio() {
    return termFrequencyRatio;
  }

  public void setTermFrequencyRatio(double termFrequencyRatio) {
    this.termFrequencyRatio = termFrequencyRatio;
  }

  public double getDistributionTermFrequency() {
    return distributionTermFrequency;
  }

  public void setDistributionTermFrequency(double distributionTermFrequency) {
    this.distributionTermFrequency = distributionTermFrequency;
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.code);
    return hash;
  }

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

  @Override
  public String toString() {
    DecimalFormat df = new DecimalFormat("###,###.############");

    return "BasicBlock{" + "Hash=" + code + ", count: " + count + ", TFR: " + df.format(termFrequencyRatio) + ", TFD: " + df.format(distributionTermFrequency) + "}";
  }

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

  public double getMDF() {
    return MDF;
  }

  public void setMDF(double MDF) {
    this.MDF = MDF;
  }

  public double getFC() {
    return FC;
  }

  public void setFC(double FC) {
    this.FC = FC;
  }
}
