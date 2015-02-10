package vida.phd.dtf.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Family {

  private final String name;
  private final HashMap<String, BasicBlock> basicBlocks;
  private final HashMap<String, Malware> malwares;

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

  public HashMap<String, BasicBlock> getBasicBlocks() {
    return basicBlocks;
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

  public void addBasicBlock(String code) throws Exception {
    BasicBlock existingBB = basicBlocks.get(code);
    if (existingBB != null) {
      existingBB.incCound();
    } else {
      basicBlocks.put(code, new BasicBlock(code));
    }
  }
  
  public void addMalware(Malware malware) throws Exception {
    if (malwares.containsKey(malware.getName())) {
      throw new Exception("Malware " + malware.getName() + " already exists.");
    }
    
    malwares.put(malware.getName(), malware);
  }

  public HashMap<String, Malware> getMalwares() {
    return malwares;
  }

  
}
