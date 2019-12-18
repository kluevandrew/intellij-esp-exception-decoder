package com.kluev.espexceptiondecoder;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.sun.scenario.Settings;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class EspExceptionDecoder implements DocumentListener {
    private JPanel windowContent;
    private JTextArea inputArea;
    private JTextPane outputArea;
    private JTextField elfFilePath;
    private JButton chooseElfFileBtn;
    private JRadioButton esp32RadioButton;
    private JRadioButton esp8266RadioButton;
    private String outputText;

    File tool;
    File elf;

    public EspExceptionDecoder(ToolWindow toolWindow) {
        chooseElfFileBtn.addActionListener(e -> chooseElfFile());
        esp32RadioButton.addActionListener(e -> decode());
        esp8266RadioButton.addActionListener(e -> decode());

        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "commit");
        inputArea.getActionMap().put("commit", new CommitAction());
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "commit");
        inputArea.getActionMap().put("commit", new CommitAction());
        inputArea.getDocument().addDocumentListener(this);

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        elfFilePath.setText(project.getBasePath() + "/.pio/build/esp32/firmware.elf");

        elf = new File("/Users/andrew/Documents/Arduino/espx-dancing-led/.pio/build/esp32/firmware.elf");
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        decode();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {

    }

    @Override
    public void changedUpdate(DocumentEvent e) {

    }

    private class CommitAction extends AbstractAction {
        public void actionPerformed(ActionEvent ev) {
            decode();
        }
    }

    public void chooseElfFile() {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                false,
                false, false,
                false,
                false);
        descriptor.setHideIgnored(false);
        descriptor.withFileFilter(virtualFile -> Objects.equals(virtualFile.getExtension(), "elf"));

        VirtualFile toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.getBasePath() + "/.pio/build/esp32/firmware.elf");
        final VirtualFile selected = FileChooser.chooseFile(
                descriptor,
                project,
                toSelect
        );

        try {
            assert selected != null;
            elfFilePath.setText(selected.getPath());
        } catch (NullPointerException e) {
            elfFilePath.setText("");
        }
    }

    public void decode() {
        if (inputArea.getText().isEmpty() || inputArea.getText().equals("Paste your stack trace here")) {
            return;
        }
        elf = new File(elfFilePath.getText());
        if (!elf.exists()) {
            renderError("No .elf file found");
            return;
        }
        tool = getXtensaGdbFile();
        if (!tool.exists()) {
            renderError("No xtensa-gdb file found, please configure it in preferences");
            return;
        }
        runParser();
        outputArea.setText(outputText);
    }

    private void renderError(String error) {
        outputArea.setText("<html><font color=red>" + error + "</font></html>");
    }

    public JPanel getContent() {
        return windowContent;
    }

    private File getXtensaGdbFile() {
        boolean esp8266 = esp8266RadioButton.isSelected();
        boolean esp32 = esp32RadioButton.isSelected() ;

        String esp8266gdb = Settings.get(XtensaGdbOption.OPTION_KEY_GDB8266);
        String esp32gdb = Settings.get(XtensaGdbOption.OPTION_KEY_GDB32);
        if (esp8266 && !esp8266gdb.isEmpty()) {
            return new File(esp32gdb);
        }

        if (esp32 && !esp32gdb.isEmpty()) {
            return new File(esp32gdb);
        }

        String home = System.getProperty("user.home");
        String gccPath = home + "/.platformio/packages/toolchain-xtensa";
        if (esp32) {
            gccPath += "32/bin/xtensa-esp32-elf-gdb";
        } else {
            gccPath += "/bin/xtensa-lx106-elf-gdb";
        }

        return new File(gccPath);
    }

    private static String[] exceptions = {
            "Illegal instruction",
            "SYSCALL instruction",
            "InstructionFetchError: Processor internal physical address or data error during instruction fetch",
            "LoadStoreError: Processor internal physical address or data error during load or store",
            "Level1Interrupt: Level-1 interrupt as indicated by set level-1 bits in the INTERRUPT register",
            "Alloca: MOVSP instruction, if caller's registers are not in the register file",
            "IntegerDivideByZero: QUOS, QUOU, REMS, or REMU divisor operand is zero",
            "reserved",
            "Privileged: Attempt to execute a privileged operation when CRING ? 0",
            "LoadStoreAlignmentCause: Load or store to an unaligned address",
            "reserved",
            "reserved",
            "InstrPIFDataError: PIF data error during instruction fetch",
            "LoadStorePIFDataError: Synchronous PIF data error during LoadStore access",
            "InstrPIFAddrError: PIF address error during instruction fetch",
            "LoadStorePIFAddrError: Synchronous PIF address error during LoadStore access",
            "InstTLBMiss: Error during Instruction TLB refill",
            "InstTLBMultiHit: Multiple instruction TLB entries matched",
            "InstFetchPrivilege: An instruction fetch referenced a virtual address at a ring level less than CRING",
            "reserved",
            "InstFetchProhibited: An instruction fetch referenced a page mapped with an attribute that does not permit instruction fetch",
            "reserved",
            "reserved",
            "reserved",
            "LoadStoreTLBMiss: Error during TLB refill for a load or store",
            "LoadStoreTLBMultiHit: Multiple TLB entries matched for a load or store",
            "LoadStorePrivilege: A load or store referenced a virtual address at a ring level less than CRING",
            "reserved",
            "LoadProhibited: A load referenced a page mapped with an attribute that does not permit loads",
            "StoreProhibited: A store referenced a page mapped with an attribute that does not permit stores"
    };

    // Original code from processing.app.helpers.ProcessUtils.exec()
    // Need custom version to redirect STDERR to STDOUT for GDB processing
    public static Process execRedirected(String[] command) throws IOException {
        ProcessBuilder pb;

        // No problems on linux and mac
        if (false) {//!OSUtils.isWindows()) {
            pb = new ProcessBuilder(command);
        } else {
            // Brutal hack to workaround windows command line parsing.
            // http://stackoverflow.com/questions/5969724/java-runtime-exec-fails-to-escape-characters-properly
            // http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
            // http://bugs.sun.com/view_bug.do?bug_id=6468220
            // http://bugs.sun.com/view_bug.do?bug_id=6518827
            String[] cmdLine = new String[command.length];
            for (int i = 0; i < command.length; i++)
                cmdLine[i] = command[i].replace("\"", "\\\"");
            pb = new ProcessBuilder(cmdLine);
            Map<String, String> env = pb.environment();
            env.put("CYGWIN", "nodosfilewarning");
        }
        pb.redirectErrorStream(true);

        return pb.start();
    }

    private int listenOnProcess(String[] arguments) {
        try {
            final Process p = execRedirected(arguments);
            Thread thread = new Thread() {
                public void run() {
                    try {
                        InputStreamReader reader = new InputStreamReader(p.getInputStream());
                        int c;
                        String line = "";
                        while ((c = reader.read()) != -1) {
                            if ((char) c == '\r')
                                continue;
                            if ((char) c == '\n') {
                                printLine(line);
                                line = "";
                            } else {
                                line += (char) c;
                            }
                        }
                        printLine(line);
                        reader.close();

                        reader = new InputStreamReader(p.getErrorStream());
                        while ((c = reader.read()) != -1)
                            System.err.print((char) c);
                        reader.close();
                    } catch (Exception e) {
                    }
                }
            };
            thread.start();
            int res = p.waitFor();
            thread.join();
            return res;
        } catch (Exception e) {
        }
        return -1;
    }

    private void sysExec(final String[] arguments) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    if (listenOnProcess(arguments) != 0) {
                        renderError("Decode Failed");
                    } else {
                        outputArea.setText(outputText);
                    }
                } catch (Exception e) {
                    renderError("Decode Exception:" + e.getMessage());
                }
            }
        };
        thread.start();
    }

    private String prettyPrintGDBLine(String line) {
        String address = "", method = "", file = "", fileline = "", html = "";

        if (!line.startsWith("0x")) {
            return null;
        }

        address = line.substring(0, line.indexOf(' '));
        line = line.substring(line.indexOf(' ') + 1);

        int atIndex = line.indexOf("is in ");
        if (atIndex == -1) {
            return null;
        }
        try {
            method = line.substring(atIndex + 6, line.lastIndexOf('(') - 1);
            fileline = line.substring(line.lastIndexOf('(') + 1, line.lastIndexOf(')'));
            file = fileline.substring(0, fileline.lastIndexOf(':'));
            line = fileline.substring(fileline.lastIndexOf(':') + 1);
            if (file.length() > 0) {
                int lastfs = file.lastIndexOf('/');
                int lastbs = file.lastIndexOf('\\');
                int slash = (lastfs > lastbs) ? lastfs : lastbs;
                if (slash != -1) {
                    String filename = file.substring(slash + 1);
                    file = file.substring(0, slash + 1) + "<b>" + filename + "</b>";
                }
            }
            html = "<font color=green>" + address + ": </font>" +
                    "<b><font color=blue>" + method + "</font></b> at " +
                    file + " line <b>" + line + "</b>";
        } catch (Exception e) {
            // Something weird in the GDB output format, report what we can
            html = "<font color=green>" + address + ": </font> " + line;
        }

        return html;
    }

    private void printLine(String line) {
        String s = prettyPrintGDBLine(line);
        if (s != null)
            outputText += s + "\n";
    }

    private void parseException() {
        String content = inputArea.getText();
        Pattern p = Pattern.compile("Exception \\(([0-9]*)\\):");
        Matcher m = p.matcher(content);
        if (m.find()) {
            int exception = Integer.parseInt(m.group(1));
            if (exception < 0 || exception > 29) {
                return;
            }
            outputText += "<b><font color=red>Exception " + exception + ": " + exceptions[exception] + "</font></b>\n";
        }
    }

    // Strip out just the STACK lines or BACKTRACE line, and generate the reference log
    private void parseStackOrBacktrace(String regexp, boolean multiLine, String stripAfter) {
        String content = inputArea.getText();

        Pattern strip;
        if (multiLine) strip = Pattern.compile(regexp, Pattern.DOTALL);
        else strip = Pattern.compile(regexp);
        Matcher stripMatch = strip.matcher(content);
        if (!stripMatch.find()) {
            return; // Didn't find it in the text box.
        }

        // Strip out just the interesting bits to make RexExp sane
        content = content.substring(stripMatch.start(), stripMatch.end());

        if (stripAfter != null) {
            Pattern after = Pattern.compile(stripAfter);
            Matcher afterMatch = after.matcher(content);
            if (afterMatch.find()) {
                content = content.substring(0, afterMatch.start());
            }
        }

        // Anything looking like an instruction address, dump!
        Pattern p = Pattern.compile("40[0-2](\\d|[a-f]|[A-F]){5}\\b");
        int count = 0;
        Matcher m = p.matcher(content);
        while (m.find()) {
            count++;
        }
        if (count == 0) {
            return;
        }
        String command[] = new String[7 + count * 2];
        int i = 0;
        command[i++] = tool.getAbsolutePath();
        command[i++] = "--batch";
        command[i++] = elf.getAbsolutePath();
        command[i++] = "-ex";
        command[i++] = "set listsize 1";
        m = p.matcher(content);
        while (m.find()) {
            command[i++] = "-ex";
            command[i++] = "l *0x" + content.substring(m.start(), m.end());
        }
        command[i++] = "-ex";
        command[i++] = "q";
        outputText += "\n<i>Decoding stack results</i>\n";
        sysExec(command);
    }

    // Heavyweight call GDB, run list on address, and return result if it succeeded
    private String decodeFunctionAtAddress(String addr) {
        String command[] = new String[9];
        command[0] = tool.getAbsolutePath();
        command[1] = "--batch";
        command[2] = elf.getAbsolutePath();
        command[3] = "-ex";
        command[4] = "set listsize 1";
        command[5] = "-ex";
        command[6] = "l *0x" + addr;
        command[7] = "-ex";
        command[8] = "q";

        try {
            final Process proc = execRedirected(command);
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            int c;
            String line = "";
            while ((c = reader.read()) != -1) {
                if ((char) c == '\r')
                    continue;
                if ((char) c == '\n' && line != "") {
                    reader.close();
                    return prettyPrintGDBLine(line);
                } else {
                    line += (char) c;
                }
            }
            reader.close();
        } catch (Exception er) {
        }
        // Something went wrong
        return null;
    }

    // Scan and report the last failed memory allocation attempt, if present on the ESP8266
    private void parseAlloc() {
        String content = inputArea.getText();
        Pattern p = Pattern.compile("last failed alloc call: 40[0-2](\\d|[a-f]|[A-F]){5}\\((\\d)+\\)");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String fs = content.substring(m.start(), m.end());
            Pattern p2 = Pattern.compile("40[0-2](\\d|[a-f]|[A-F]){5}\\b");
            Matcher m2 = p2.matcher(fs);
            if (m2.find()) {
                String addr = fs.substring(m2.start(), m2.end());
                Pattern p3 = Pattern.compile("\\((\\d)+\\)");
                Matcher m3 = p3.matcher(fs);
                if (m3.find()) {
                    String size = fs.substring(m3.start() + 1, m3.end() - 1);
                    String line = decodeFunctionAtAddress(addr);
                    if (line != null) {
                        outputText += "Memory allocation of " + size + " bytes failed at " + line + "\n";
                    }
                }
            }
        }
    }

    // Filter out a register output given a regex (ESP8266/ESP32 differ in format)
    private void parseRegister(String regName, String prettyName) {
        String content = inputArea.getText();
        Pattern p = Pattern.compile(regName + "(\\d|[a-f]|[A-F]){8}\\b");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String fs = content.substring(m.start(), m.end());
            Pattern p2 = Pattern.compile("(\\d|[a-f]|[A-F]){8}\\b");
            Matcher m2 = p2.matcher(fs);
            if (m2.find()) {
                String addr = fs.substring(m2.start(), m2.end());
                String line = decodeFunctionAtAddress(addr);
                if (line != null) {
                    outputText += prettyName + ": " + line + "\n";
                } else {
                    outputText += prettyName + ": <font color=\"green\">0x" + addr + "</font>\n";
                }
            }
        }
    }

    private void runParser() {
        outputText = "<html><pre>\n";
        // Main error cause
        parseException();
        // ESP8266 register format
        parseRegister("epc1=0x", "<font color=\"red\">PC</font>");
        parseRegister("excvaddr=0x", "<font color=\"red\">EXCVADDR</font>");
        // ESP32 register format
        parseRegister("PC\\s*:\\s*(0x)?", "<font color=\"red\">PC</font>");
        parseRegister("EXCVADDR\\s*:\\s*(0x)?", "<font color=\"red\">EXCVADDR</font>");
        // Last memory allocation failure
        parseAlloc();
        // The stack on ESP8266, multiline
        parseStackOrBacktrace(">>>stack>>>(.)*", true, "<<<stack<<<");
        // The backtrace on ESP32, one-line only
        parseStackOrBacktrace("Backtrace:(.)*", false, null);
    }

}
