package vida.phd.tfd.commandline;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CommandLine {

    private InputStream is;
    private OutputStream os;
    private String separator;
    private BufferedReader reader;
    private String tempCommand;
    private String prompt, tempPrompt;

    /**
     * Constructor which receive the input stream and the output stream
     *
     * @param is InputStream to read the commands from
     * @param os OutputStream to write the prompt to
     */
    public CommandLine(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
        this.separator = ";";
        this.prompt = "> ";
        this.tempPrompt = "... ";
    }

    /**
     * Constructor which receive the input stream, output stream and the command separator
     *
     * @param is        InputStream to read the commands from
     * @param os        OutputStream to write the prompt to
     * @param separator which is one or more characters to separate batch commands
     */
    public CommandLine(InputStream is, OutputStream os, String separator) {
        this.is = is;
        this.os = os;
        this.separator = separator;
        this.prompt = "> ";
        this.tempPrompt = "... ";
    }

    /**
     * Constructor which receive the input stream, output stream, the command separator and two different prompts
     *
     * @param is         InputStream to read the commands from
     * @param os         OutputStream to write the prompt to
     * @param separator  which is one or more characters to separate batch commands
     * @param prompt     Normal command prompt
     * @param tempPrompt Command prompt which is displayed between lines of a multi line command
     */
    public CommandLine(InputStream is, OutputStream os, String separator, String prompt, String tempPrompt) {
        this.is = is;
        this.os = os;
        this.separator = separator;
        this.prompt = prompt;
        this.tempPrompt = tempPrompt;
    }

    /**
     * Reads one or more commands until the user enters a Separator following by the ENTER key
     *
     * @return List of String as the commands which have been entered by user
     * @throws IOException while reading from OutputStream
     */
    public List<String> read() throws IOException  {
        List<String> result = new ArrayList<String>();
        boolean finished = false;
        while (!finished) {
            os.write((tempCommand == null ? prompt.getBytes() : tempPrompt.getBytes()));
            String line = readLine();
            finished = line.endsWith(separator) || line.length() <= 0;
            List<String> commands = separateLine(line);
            result.addAll(commands);
        }
        return result;
    }

    /**
     * Read one line of input.
     *
     * @return String
     * @throws IOException
     */
    private String readLine() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(is));
        }
        String result = reader.readLine();
        return result.trim();
    }

    /**
     * Processes one line of input and find the complete and incomplete commands based on the number of separators
     *
     * @param line String input
     * @return List of String as commands
     */
    private List<String> separateLine(final String line) {
        List<String> result = new ArrayList<>();
        if (line.length() > 0) {
            String[] parts = line.split(separator);

            if (line.startsWith(separator)) {
                result.add(tempCommand);
                tempCommand = null;
            } else if (tempCommand != null) {
                parts[0] = tempCommand.concat(" ").concat(parts[0]);
                tempCommand = null;
            }

            int length = parts.length;
            if (!line.endsWith(separator)) {
                tempCommand = parts[parts.length - 1];
                length--;
            }

            for (int i = 0; i < length; i++) {
                result.add(parts[i]);
            }
        }
        return result;
    }

    public InputStream getIs() {
        return is;
    }

    public void setIs(InputStream is) {
        this.is = is;
    }

    public OutputStream getOs() {
        return os;
    }

    public void setOs(OutputStream os) {
        this.os = os;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getTempPrompt() {
        return tempPrompt;
    }

    public void setTempPrompt(String tempPrompt) {
        this.tempPrompt = tempPrompt;
    }
}
