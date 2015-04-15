package vida.phd.tfd;

import vida.phd.tfd.commandline.CommandLine;
import vida.phd.tfd.entity.BasicBlock;
import vida.phd.tfd.entity.Family;
import vida.phd.tfd.entity.Malware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static final String version = "3.3.0";
  private CommandLine getter;
  private boolean running;
  private TFD tfd;
  private File initFile;

  private void run() {
    showVersion();
    running = true;
    tfd = new TFD();
    tfd.showStatus(false);

    if (checkInitFile()) {
      System.out.println("Init file found!");
      String initLoadPath = checkInitLoad();
      if (initLoadPath != null) {
        System.out.println("Init load from: " + initLoadPath);
        try {
          loadFamiles(initLoadPath);
        } catch (IOException ex) {
          Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    getter = new CommandLine(System.in, System.out, ";", "TFD> ", "-> ");
    while (running) {
      try {
        List<String> commands = getter.read();
        for (String command : commands) {
          if (command != null && command.trim().length() > 0) {
            command = command.trim();
            if (command.equalsIgnoreCase("exit")) {
              running = false;
              System.out.print("Bye\n");
            } else if (command.equalsIgnoreCase("time") || command.equalsIgnoreCase("date") || command.equalsIgnoreCase("now")) {
              timeCommand(command);
            } else if (command.equalsIgnoreCase("help")) {
              showHelp();
            } else if (command.equalsIgnoreCase("status")) {
              statusCommand();
            } else if (command.equalsIgnoreCase("families")) {
              familiesCommand();
            } else if (command.startsWith("query")) {
              queryCommand(command);
            } else if (command.startsWith("family")) {
              familyCommand(command);
            } else if (command.startsWith("load")) {
              loadCommand(command);
            } else if (command.startsWith("classify")) {
              addCommand(command);
            } else {
              System.out.println("Unknown Command!");
              System.out.println("");
              showHelp();
            }
          }
        }
      } catch (IOException e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
  }

  private void addCommand(String command) {
    if (command.equals("classify")) {
      showAddHelp();
    } else {
      String[] parts = splitCommand(command);
      if (parts.length == 3 || (parts.length == 4 && parts[3].equals("scoring"))) {
        boolean showInfo = parts.length == 4 && parts[3].equals("scoring");
        try {
          String typeStr = parts[1].trim().toLowerCase();
          String malwareFilePath = parts[2];
          TFD.ScoreType type = typeStr.equals("fc") ? TFD.ScoreType.FAM_CLASSIFIER : (typeStr.equals("tfd") ? TFD.ScoreType.TFD : null);
          final int count = tfd.add(malwareFilePath, type, showInfo);

          final String home = tfd.getFamiliesHome().getAbsolutePath();
          tfd = new TFD();
          loadFamiles(home);
          System.out.println(count + " new malware files added");
        } catch (Exception ex) {
          System.out.println("Error: " + ex.getMessage());
          showAddHelp();
        }
      }
    }
  }

  private void showAddHelp() {
    System.out.println("classify command:");
    System.out.println("classify tfd/fc path [scoring]");
  }

  private void showVersion() {
    System.out.println("TFD ".concat(version).concat("\n"));
  }

  private void timeCommand(String command) {
    Calendar calendar = new GregorianCalendar();
    DateFormat format = DateFormat.getInstance();
    System.out.println(format.format(calendar.getTime()));
  }

  private void showHelp() {
    System.out.println("Available commands:");
    System.out.println("query");
    System.out.println("exit");
    System.out.println("help");
    System.out.println("load");
  }

  private void queryCommand(String command) {
    if (command.equals("query")) {
      showQueryHelp();
    } else {
      String[] parts = splitCommand(command);
      if (parts.length == 3 && parts[1].equals("tfd")) {
        try {
          String familyName = parts[2];
          if (tfd != null) {
            try {
              List<BasicBlock> bbs = tfd.topByFamily(familyName);

              int i = 1;
              for (BasicBlock bb : bbs) {
                DecimalFormat df = new DecimalFormat("###,###.############");
                System.out.println(df.format(bb.getDistributionTermFrequency()));
              }
            } catch (Exception ex) {
              Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
          } else {
            System.out.println("Please load the families first.");
          }

        } catch (Exception ex) {
          System.out.println("Error: " + ex.getMessage());
          showQueryHelp();
        }
      } else if (parts.length == 4 && parts[1].equals("top")) {
        try {
          int top = Integer.parseInt(parts[2]);
          String familyName = parts[3];
          if (tfd != null) {
            try {
              List<BasicBlock> bbs = tfd.topByFamily(familyName, top);

              int i = 1;
              for (BasicBlock bb : bbs) {
                System.out.println(MessageFormat.format("{0}. {1}", String.valueOf(i++), bb));
              }
            } catch (Exception ex) {
              Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
          } else {
            System.out.println("Please load the families first.");
          }

        } catch (NumberFormatException ex) {
          System.out.println("Top should be an integer.");
          showQueryHelp();
        }
      } else if (parts.length == 5 && parts[1].equals("top") && parts[4].equals("more")) {
        try {
          int top = Integer.parseInt(parts[2]);
          String familyName = parts[3];
          if (tfd != null) {
            try {
              List<BasicBlock> bbs = tfd.topByFamily(familyName, top);

              int i = 1;
              for (BasicBlock bb : bbs) {
                System.out.println(MessageFormat.format("{0}. {1}", String.valueOf(i++), bb));

                List<Family> commonFamilies = tfd.findFamiliesByBB(bb.getCode());
                int j = 0;

                for (Family commonFamily : commonFamilies) {
                  if (!familyName.equals(commonFamily.getName())) {
                    Iterator<Map.Entry<String, BasicBlock>> it = commonFamily.getBasicBlocks().entrySet().iterator();

                    while (it.hasNext()) {
                      BasicBlock commonBB = it.next().getValue();

                      if (commonBB.equals(bb)) {
                        j++;
                        System.out.println("\t" + j + ". " + commonFamily + " " + commonBB);
                      }
                    }
                  }
                }
              }
            } catch (Exception ex) {
              Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
          } else {
            System.out.println("Please load the families first.");
          }

        } catch (NumberFormatException ex) {
          System.out.println("Top should be an integer.");
          showQueryHelp();
        }
      } else if (parts.length == 3 && parts[1].equals("bb")) {
        String hash = parts[2].trim();
        System.out.println("Query basic block " + hash);
        List<TFD.FamilyBasicBlock> occurances = tfd.allOccurancesByHash(hash);

        DecimalFormat df = new DecimalFormat("###,###.############");
        System.out.println(occurances.size() + " families: ");
        for (TFD.FamilyBasicBlock occurance : occurances) {
          System.out.print(occurance.getFamily().getName());
          System.out.println(": TFD:"
                  + df.format(occurance.getBasicBlock().getDistributionTermFrequency()) + ", TFR:"
                  + df.format(occurance.getBasicBlock().getTermFrequencyRatio()) + ", Count:"
                  + occurance.getBasicBlock().getCount() + ", Malwares Count:"
                  + tfd.countOfMalwaresInFamilyByBB(occurance.getFamily(), hash));
        }

        Set<Malware> malwares = tfd.findMalwaresByBBCode(hash);
        Iterator<Malware> itM = malwares.iterator();
        System.out.println(malwares.size() + " malwares: ");
        while (itM.hasNext()) {
          Malware malware = itM.next();
          System.out.println(malware);
        }
      } else if (parts.length == 3 && parts[1].equals("malware")) {
        String malwareName = parts[2].trim();

        Iterator<Map.Entry<String, Family>> itF = tfd.getFamilies().entrySet().iterator();
        while (itF.hasNext()) {
          Map.Entry<String, Family> nextF = itF.next();
          Family family = nextF.getValue();

          Iterator<Map.Entry<String, Malware>> itM = family.getMalwares().entrySet().iterator();
          while (itM.hasNext()) {
            Map.Entry<String, Malware> nextM = itM.next();
            Malware malware = nextM.getValue();

            if (malware.getName().equals(malwareName)) {
              System.out.println(malware);

              int countOfBBs = 0;

              HashMap<String, BasicBlock> basicBlocks = malware.getBasicBlocks();
              Iterator<Map.Entry<String, BasicBlock>> itB = basicBlocks.entrySet().iterator();
              while (itB.hasNext()) {
                Map.Entry<String, BasicBlock> nextB = itB.next();
                BasicBlock bbM = nextB.getValue();
                System.out.println(bbM.getCode());
                
                DecimalFormat df = new DecimalFormat("###,###.############");
                List<TFD.FamilyBasicBlock> occurances = tfd.allOccurancesByHash(bbM.getCode());
                System.out.println("Families: ");
                for (TFD.FamilyBasicBlock occurance : occurances) {
                  System.out.print(occurance.getFamily().getName());
                  System.out.println(": TFD:"
                          + df.format(occurance.getBasicBlock().getDistributionTermFrequency()) + ", TFR:"
                          + df.format(occurance.getBasicBlock().getTermFrequencyRatio()) + ", Count:"
                          + occurance.getBasicBlock().getCount() + ", Malwares Count: "
                          + tfd.countOfMalwaresInFamilyByBB(occurance.getFamily(), occurance.getBasicBlock().getCode()));
                }

                System.out.println("\n ---------------------------");
                countOfBBs += bbM.getCount();
              }

              System.out.println("\nCount of basic blocks: " + countOfBBs);
              System.out.println("Count of distinguished basic blocks: " + malware.getBasicBlocks().size());

              break;
            }
          }
        }

      } else {
        showQueryHelp();
      }
    }
  }

  private String[] splitCommand(String command) {
    return command.split("\\s+");
  }

  private void showQueryHelp() {
    System.out.println("Query command is not valid!");
    System.out.println("e.g.");
    System.out.println("query top 10");
    System.out.println("query top 40 Dropper");
    System.out.println("query sort Dropper");
    System.out.println("query bb 636ceb71b4eb15e8170c1ac8d2b7a9ecf8fcc7d4");
    System.out.println("query malware c9c6f8f844c82a45624dfedc6e76144e.txt");
  }

  private void showLoadHelp() {
    System.out.println("Load command is not valid!");
    System.out.println("e.g.");
    System.out.println("load c:\\families");
    System.out.println("load /home/user/families");
  }

  private void loadCommand(String command) throws IOException {
    if (command.equals("load")) {
      showLoadHelp();
    } else {
      String[] parts = splitCommand(command);
      if (parts.length == 2) {
        loadFamiles(parts[1]);
      } else {
        showLoadHelp();
      }
    }
  }

  private void loadFamiles(String path) throws IOException {
    File file = new File(path);
    tfd.setFamiliesHome(file);
    tfd.loadFamilies();
    tfd.calculateTermFrequencyRatio();
    tfd.calculateDistributionTermFrequency();
    //tfd.calculateMDFs();
    //tfd.calculateFCs();
  }

  private void familiesCommand() {
    if (tfd != null) {
      HashMap<String, Family> families = tfd.getFamilies();
      Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();

      while (it.hasNext()) {
        Family family = it.next().getValue();
        Set<Map.Entry<String, BasicBlock>> basicBlocks = family.getBasicBlocks().entrySet();

        System.out.println(family.getName() + " malwares: " + family.getMalwares().size());
      }

    } else {
      System.out.println("Please load the families first.");
    }
  }

  private void familyCommand(String command) {
    if (command.equals("family")) {
      showFamilyHelp();
    } else {
      String[] parts = splitCommand(command);
      if (parts.length == 2) {
        Family family = tfd.getFamilies().get(parts[1]);

        if (family != null) {
          Iterator<Map.Entry<String, BasicBlock>> it = family.getBasicBlocks().entrySet().iterator();

          while (it.hasNext()) {
            BasicBlock basicBlock = it.next().getValue();
            System.out.println(basicBlock);
          }

          System.out.println("Malwares: " + family.getMalwares().size());
          System.out.println("Basic blocks: " + family.countOfBasicBlocks());
        } else {
          System.out.println("family " + parts[1] + " not found!");
        }
      } else {
        showFamilyHelp();
      }
    }
  }

  private void showFamilyHelp() {
    System.out.println("Family command is not valid!");
    System.out.println("e.g.");
    System.out.println("family Agent");
  }

  public static void main(String[] args) {
    Main main = new Main();
    main.run();
  }

  private void statusCommand() {
    tfd.showStatus(false);
  }

  private boolean checkInitFile() {
    try {
      String path = getJarFilePath();
      path += "init";
      initFile = new File(path);
      return initFile.exists() && initFile.isFile();
    } catch (URISyntaxException ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }

    return false;
  }

  private String checkInitLoad() {
    Properties initProperties = new Properties();
    try {
      initProperties.load(new FileInputStream(initFile));
      String result = initProperties.getProperty("load");
      return result;
    } catch (FileNotFoundException ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  private String getJarFilePath() throws URISyntaxException {
    String jarFilePath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
     jarFilePath = jarFilePath.substring(0, jarFilePath.lastIndexOf(getFileSeparator()) + 1);
     return jarFilePath.replaceAll("%20", "\\ ");
  }
  
  public static String getFileSeparator() {
    Properties sysProperties = System.getProperties();
    String fileSeparator = sysProperties.getProperty("file.separator");
    return fileSeparator;
  }

}
