package vida.phd.tfd.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Family class
 * @author ehsun7b
 */
public class Family {

  /**
   * Name of the family
   */
  private final String name;
  
  /**
   * HashMap to keep all the BasicBlocks of the family
   */
  private final HashMap<String, BasicBlock> basicBlocks;
  
  /**
   * HashMap to keep all the Malware of the family
   */
  private final HashMap<String, Malware> malwares;

  /**
   * Constructor which takes family name is the only parameter
   * @param name 
   */
  public Family(String name) {
    basicBlocks = new HashMap<>();
    malwares = new HashMap<>();
    this.name = name;
  }

  /**
   * Count of all BasicBlocks in the family
   * @return 
   */
  public int countOfBasicBlocks() {
    int result = 0;
    Iterator<Map.Entry<String, BasicBlock>> iterator = basicBlocks.entrySet().iterator();

    while (iterator.hasNext()) {
      int count = iterator.next().getValue().getCount();
      result += count;
    }

    return result;
  }

  /**
   * String representation of the family by its name
   * @return 
   */
  @Override
  public String toString() {
    return "Family{" + "name: " + name + '}';
  }

  /**
   * 
   * @return name 
   */
  public String getName() {
    return name;
  }

  /**
   * 
   * @return basicBlocks 
   */
  public HashMap<String, BasicBlock> getBasicBlocks() {
    return basicBlocks;
  }

  /**
   * Find a BasicBlock in the HashMap by the basicBlock code
   * @param code
   * @return 
   */
  public BasicBlock findByCode(String code) {
    BasicBlock basicBlock = basicBlocks.get(code);
    return basicBlock;
  }

  /**
   * 
   * @return unique hash value
   */
  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.name);
    return hash;
  }

  /**
   * Compare the family with the given family based on their names
   * @param obj
   * @return 
   */
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

  /**
   * Adds the given BasicBlock to the HashMap; if it is a new BasicBlock it will be added. <br/>But if it as already added, its count will get incremented.
   * @param code
   * @throws Exception 
   */
  public void addBasicBlock(String code) throws Exception {
    BasicBlock existingBB = basicBlocks.get(code);
    if (existingBB != null) {
      existingBB.incCound();
    } else {
      basicBlocks.put(code, new BasicBlock(code));
    }
  }
  
  /**
   * Adds the given Malware to the HashMap; if it is a new Malware it will be added. <br/> But if it is already added the method will raise an Excaption.
   * @param malware
   * @throws Exception 
   */
  public void addMalware(Malware malware) throws Exception {
    if (malwares.containsKey(malware.getName())) {
      throw new Exception("Malware " + malware.getName() + " already exists.");
    }
    
    malwares.put(malware.getName(), malware);
  }

  /**
   * 
   * @return malwares
   */
  public HashMap<String, Malware> getMalwares() {
    return malwares;
  }

  
}
