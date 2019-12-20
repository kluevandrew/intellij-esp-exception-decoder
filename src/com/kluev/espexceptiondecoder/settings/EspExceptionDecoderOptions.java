package com.kluev.espexceptiondecoder.settings;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class EspExceptionDecoderOptions implements Configurable {
    public static final String ESP8266_GDB = "esp8266gdb";
    public static final String ESP32_GDB = "esp32gdb";

    private JPanel settingsPanel;
    private JTextField esp8266gdbTextField;
    private JButton esp8266gdbButton;
    private JTextField esp32gdbTextField;
    private JButton esp32gdbButton;
    private JPanel mainPanel;
    private boolean modified = false;

    EspExceptionDecoderOptions() {
        esp8266gdbButton.addActionListener(e -> chooseESP8266gdb());
        esp32gdbButton.addActionListener(e -> chooseESP32gdb());
        esp8266gdbTextField.addActionListener(e -> modified = true);
        esp32gdbTextField.addActionListener(e -> modified = true);

    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "EspExceptionDecoder";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        esp8266gdbTextField.setText(Settings.get(ESP8266_GDB));
        esp32gdbTextField.setText(Settings.get(ESP32_GDB));

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void reset() {
        esp8266gdbTextField.setText(Settings.get(ESP8266_GDB));
        esp32gdbTextField.setText(Settings.get(ESP32_GDB));
        modified = true;
    }

    @Override
    public void apply() {
        Settings.set(ESP8266_GDB, esp8266gdbTextField.getText());
        Settings.set(ESP32_GDB, esp32gdbTextField.getText());
        modified = false;
    }

    private void chooseESP8266gdb() {
        String path = chooseFile(false);
        esp8266gdbTextField.setText(path);
        modified = true;
    }

    private void chooseESP32gdb() {
        String path = chooseFile(true);
        esp32gdbTextField.setText(path);
        modified = true;
    }

    private String chooseFile(boolean esp32) {
        String home = System.getProperty("user.home");
        String gdbPath = home + "/.platformio/packages/toolchain-xtensa";
        if (esp32) {
            gdbPath += "32/bin/xtensa-esp32-elf-gdb";
        } else {
            gdbPath += "/bin/xtensa-lx106-elf-gdb";
        }

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
        descriptor.withHideIgnored(false);
        descriptor.withShowFileSystemRoots(true);
        descriptor.withShowHiddenFiles(true);
        descriptor.setShowFileSystemRoots(true);

        VirtualFile toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(gdbPath);
        final VirtualFile selected = FileChooser.chooseFile(
                descriptor,
                mainPanel,
                null,
                toSelect
        );

        if (null != selected) {
            return selected.getPath();
        }

        return "";
    }

}
