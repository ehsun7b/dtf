package vida.phd.dtf.entity;

import java.text.DecimalFormat;
import java.util.Objects;

public class BasicBlock implements Comparable<BasicBlock> {

  private String code;
  private String malwareName;

  private int count;
  private double termFrequencyRatio;
  private double distributionTermFrequency;

  public BasicBlock(String code, String malwareName) {
    this.code = code;
    this.malwareName = malwareName;
    this.count = 1;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMalwareName() {
    return malwareName;
  }

  public void setMalwareName(String malwareName) {
    this.malwareName = malwareName;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
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
    return "BasicBlock{" + "Hash=" + code + ", malware: " + malwareName + ", count: " + count + ", TFR: " + df.format(termFrequencyRatio) + ", TFD: " + df.format(distributionTermFrequency) + '}';
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

}
