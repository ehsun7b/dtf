package vida.phd.dtf.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Family {

  private String name;
  private HashMap<String, BasicBlock> basicBlocks;
  private final HashMap<String, String> malwares;

  public Family(String name) {
    basicBlocks = new HashMap<>();
    malwares = new HashMap<>();
    this.name = name;
  }

  public int countOfBasicBlocks() {
    int result = 0;
    Iterator<Map.Entry<String, BasicBlock>> iterator = basicBlocks.entrySet().iterator();

    while (iterator.hasNext()) {
      int count = iterator.next().getValue().getCount();
      result += count;
    }

    return result;
  }

  @Override
  public String toString() {
    return "Family{" + "name: " + name + '}';
  }

  
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public HashMap<String, BasicBlock> getBasicBlocks() {
    return basicBlocks;
  }

  public void setBasicBlocks(HashMap<String, BasicBlock> basicBlocks) {
    this.basicBlocks = basicBlocks;
  }

  public BasicBlock findByCode(String code) {
    BasicBlock basicBlock = basicBlocks.get(code);
    return basicBlock;
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.name);
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
    final Family other = (Family) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    }
    return true;
  }

  public HashMap<String, String> getMalwares() {
    return malwares;
  }

}
