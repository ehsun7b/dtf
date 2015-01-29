package vida.phd.dtf;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vida.phd.dtf.commandline.CommandLine;
import vida.phd.dtf.entity.BasicBlock;
import vida.phd.dtf.entity.Family;

public class Main {

  private static final String version = "1.2.5";
  private CommandLine getter;
  private boolean running;
  private DTF dtf;

  private void run() {
    showVersion();
    running = true;
    getter = new CommandLine(System.in, System.out, ";", "DTF> ", "-> ");
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
            } else if (command.equalsIgnoreCase("families")) {
              familiesCommand();
            } else if (command.startsWith("query")) {
              queryCommand(command);
            } else if (command.startsWith("family")) {
              familyCommand(command);
            } else if (command.startsWith("load")) {
              loadCommand(command);
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

  private void showVersion() {
    System.out.println("DTF ".concat(version).concat("\n"));
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
      if (parts.length == 4 && parts[1].equals("top")) {
        try {
          int top = Integer.parseInt(parts[2]);
          String familyName = parts[3];
          if (dtf != null) {
            try {
              List<BasicBlock> bbs = dtf.topByFamily(familyName, top);

              int i = 1;
              for (BasicBlock bb : bbs) {
                //System.out.println(MessageFormat.format("{0}.\t{1} Malware: {2}, TFR: {3}, DTF: {4}",
                //        String.valueOf(i++), bb.getCode(), bb.getMalwareName(), bb.getTermFrequencyRatio(), bb.getDistributionTermFrequency()));
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
          if (dtf != null) {
            try {
              List<BasicBlock> bbs = dtf.topByFamily(familyName, top);

              int i = 1;
              for (BasicBlock bb : bbs) {
               // System.out.println(MessageFormat.format("{0}.\t{1} Malware: {2}, TFR: {3}, DTF: {4}",
                //       String.valueOf(i++), bb.getCode(), bb.getMalwareName(), bb.getTermFrequencyRatio(), bb.getDistributionTermFrequency()));
                System.out.println(MessageFormat.format("{0}. {1}", String.valueOf(i++), bb));

                List<Family> commonFamilies = dtf.findFamiliesByBB(bb.getCode());
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
    System.out.println("query top 40 in Dropper");
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
    dtf = new DTF(file);
    dtf.loadFamilies();
    dtf.calculateTermFrequencyRatio();
    dtf.calculateDistributionTermFrequency();
  }

  private void familiesCommand() {
    if (dtf != null) {
      HashMap<String, Family> families = dtf.getFamilies();
      Iterator<Map.Entry<String, Family>> it = families.entrySet().iterator();
      int countOfFamilies = families.size();
      int countOfBasicBlocks = 0;

      while (it.hasNext()) {
        Family family = it.next().getValue();
        Set<Map.Entry<String, BasicBlock>> basicBlocks = family.getBasicBlocks().entrySet();
        countOfBasicBlocks += family.countOfBasicBlocks();

        Iterator<Map.Entry<String, BasicBlock>> it2 = basicBlocks.iterator();

        while (it2.hasNext()) {
          BasicBlock basicBlock = it2.next().getValue();
          //System.out.println(basicBlock);
        }
      }

      System.out.println("\n Families: " + countOfFamilies + " BasicBlocks: " + countOfBasicBlocks);
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
        Family family = dtf.getFamilies().get(parts[1]);

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

}
