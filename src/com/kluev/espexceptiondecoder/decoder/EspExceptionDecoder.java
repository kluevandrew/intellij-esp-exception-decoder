package com.kluev.espexceptiondecoder.decoder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;

public class EspExceptionDecoder implements Computable<String> {
    File elf;
    File gdb;
    String input;
    String output = "";

    public EspExceptionDecoder(File elf, File gdb, String input) {
        this.elf = elf;
        this.gdb = gdb;
        this.input = input;
    }

    @Override
    public String compute() {
        output = "<html><pre>\n";

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

        return output;
    }

    private void parseException() {
        Pattern p = Pattern.compile("Exception \\(([0-9]*)\\):");
        Matcher m = p.matcher(input);
        if (m.find()) {
            int exception = Integer.parseInt(m.group(1));
            if (exception < 0 || exception > 29) {
                return;
            }
            output += "<b><font color=red>Exception " + exception + ": " + EspExceptions.messages[exception] + "</font></b>\n";
        }
    }

    // Filter out a register output given a regex (ESP8266/ESP32 differ in format)
    private void parseRegister(String regName, String prettyName) {
        Pattern p = Pattern.compile(regName + "(\\d|[a-f]|[A-F]){8}\\b");
        Matcher m = p.matcher(input);
        if (m.find()) {
            String fs = input.substring(m.start(), m.end());
            Pattern p2 = Pattern.compile("(\\d|[a-f]|[A-F]){8}\\b");
            Matcher m2 = p2.matcher(fs);
            if (m2.find()) {
                String addr = fs.substring(m2.start(), m2.end());
                String line = decodeFunctionAtAddress(addr);
                if (line != null) {
                    output += prettyName + ": " + line + "\n";
                } else {
                    output += prettyName + ": <font color=\"green\">0x" + addr + "</font>\n";
                }
            }
        }
    }

    // Heavyweight call GDB, run list on address, and return result if it succeeded
    private String decodeFunctionAtAddress(String addr) {
        String command[] = new String[9];
        command[0] = gdb.getAbsolutePath();
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

    private static Process execRedirected(String[] command) throws IOException {
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

    // Scan and report the last failed memory allocation attempt, if present on the ESP8266
    private void parseAlloc() {
        Pattern p = Pattern.compile("last failed alloc call: 40[0-2](\\d|[a-f]|[A-F]){5}\\((\\d)+\\)");
        Matcher m = p.matcher(input);
        if (m.find()) {
            String fs = input.substring(m.start(), m.end());
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
                        output += "Memory allocation of " + size + " bytes failed at " + line + "\n";
                    }
                }
            }
        }
    }

    // Strip out just the STACK lines or BACKTRACE line, and generate the reference log
    private void parseStackOrBacktrace(String regexp, boolean multiLine, String stripAfter) {
        Pattern strip;
        if (multiLine) strip = Pattern.compile(regexp, Pattern.DOTALL);
        else strip = Pattern.compile(regexp);
        Matcher stripMatch = strip.matcher(input);
        if (!stripMatch.find()) {
            return; // Didn't find it in the text box.
        }

        // Strip out just the interesting bits to make RexExp sane
        input = input.substring(stripMatch.start(), stripMatch.end());

        if (stripAfter != null) {
            Pattern after = Pattern.compile(stripAfter);
            Matcher afterMatch = after.matcher(input);
            if (afterMatch.find()) {
                input = input.substring(0, afterMatch.start());
            }
        }

        // Anything looking like an instruction address, dump!
        Pattern p = Pattern.compile("40[0-2](\\d|[a-f]|[A-F]){5}\\b");
        int count = 0;
        Matcher m = p.matcher(input);
        while (m.find()) {
            count++;
        }
        if (count == 0) {
            return;
        }
        String command[] = new String[7 + count * 2];
        int i = 0;
        command[i++] = gdb.getAbsolutePath();
        command[i++] = "--batch";
        command[i++] = elf.getAbsolutePath();
        command[i++] = "-ex";
        command[i++] = "set listsize 1";
        m = p.matcher(input);
        while (m.find()) {
            command[i++] = "-ex";
            command[i++] = "l *0x" + input.substring(m.start(), m.end());
        }
        command[i++] = "-ex";
        command[i++] = "q";
        output += "\n<i>Decoding stack results</i>\n";
        sysExec(command);
    }

    private void sysExec(final String[] arguments) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if (listenOnProcess(arguments) != 0) {
                        throw new EspExceptionDecoderError("Decode Failed");
                    }
                } catch (Exception e) {
                    throw new EspExceptionDecoderError("Decode Exception:" + e.getMessage());
                }
            }
        });
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

    private void printLine(String line) {
        String s = prettyPrintGDBLine(line);
        if (s != null)
            output += s + "\n";
    }

}
