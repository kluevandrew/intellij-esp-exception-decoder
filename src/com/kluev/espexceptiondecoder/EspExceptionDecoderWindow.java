package com.kluev.espexceptiondecoder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.kluev.espexceptiondecoder.decoder.EspExceptionDecoder;
import com.kluev.espexceptiondecoder.decoder.EspExceptionDecoderError;
import com.kluev.espexceptiondecoder.settings.EspExceptionDecoderOptions;
import com.kluev.espexceptiondecoder.settings.Settings;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

public class EspExceptionDecoderWindow {
    private Project project;
    private JPanel windowContent;
    private JTextArea inputArea;
    private JTextPane outputArea;
    private JTextField elfFilePath;
    private JButton chooseElfFileBtn;
    private JRadioButton esp32RadioButton;
    private JRadioButton esp8266RadioButton;
    private JButton decodeButton;

    public EspExceptionDecoderWindow(Project project) {
        this.project = project;
        chooseElfFileBtn.addActionListener(e -> chooseElfFile());
        decodeButton.addActionListener(e -> decode());
        try {
            this.tryToFindElfFile();
        } catch (IOException e) {
            renderError("No .elf file found");
        }
    }

    public void chooseElfFile() {
        Project project = ProjectManager.getInstance().getDefaultProject();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false
        );
        descriptor.setHideIgnored(false);
        descriptor.withFileFilter(virtualFile -> Objects.equals(virtualFile.getExtension(), "elf"));

        VirtualFile toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.getBasePath() + "/.pio/build/esp32/firmware.elf");
        final VirtualFile selected = FileChooser.chooseFile(
                descriptor,
                project,
                toSelect
        );

        if (null == selected) {
            elfFilePath.setText("");
        } else {
            elfFilePath.setText(selected.getPath());
        }
    }

    public void decode() {
        outputArea.setText("");

        if (elfFilePath.getText().isEmpty()) {
            try {
                this.tryToFindElfFile();
            } catch (IOException e) {
                renderError("No .elf file found");
                return;
            }
        }

        if (inputArea.getText().isEmpty() || inputArea.getText().equals("Paste your stack trace here")) {
            return;
        }

        File elf = new File(elfFilePath.getText());
        if (!elf.exists()) {
            renderError(".elf file does not exists");
            return;
        }
        File gdb = getXtensaGdbFile();
        if (!gdb.exists()) {
            renderError("No xtensa-gdb found, please configure it in preferences");
            return;
        }
        if (!gdb.canExecute()) {
            renderError("Selected xtensa-gdb is not executable");
            return;
        }

        try {
            EspExceptionDecoder decoder = new EspExceptionDecoder(elf, gdb, inputArea.getText());
            outputArea.setText(ApplicationManager.getApplication().runReadAction(decoder));
        } catch (EspExceptionDecoderError e) {
            renderError(e.getMessage());
        }
    }

    private void tryToFindElfFile() throws IOException {
        ArrayList<Path> paths = Util.glob("glob:**/firmware.elf", project.getBasePath() + "/.pio/build");

        if (!paths.isEmpty()) {
            elfFilePath.setText(paths.get(0).toString());
        }
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

        String esp8266gdb = Settings.get(EspExceptionDecoderOptions.ESP8266_GDB);
        String esp32gdb = Settings.get(EspExceptionDecoderOptions.ESP32_GDB);
        if (esp8266 && esp8266gdb != null && !esp8266gdb.isEmpty()) {
            return new File(esp8266gdb);
        }

        if (esp32 && esp32gdb != null && !esp32gdb.isEmpty()) {
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

}
