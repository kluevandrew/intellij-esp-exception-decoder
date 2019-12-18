package com.kluev.espexceptiondecoder;

import com.intellij.openapi.options.Configurable;
import com.sun.scenario.Settings;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class XtensaGdbOption implements Configurable {
    public static final String OPTION_KEY_GDB8266 = "xtensa-lx106-elf-gdb";
    public static final String OPTION_KEY_GDB32 = "xtensa-esp32-elf-gdb";

    private boolean modified = false;
    private JFilePicker jFilePickerXtensaGdb8266;
    private JFilePicker jFilePickerXtensaGdb32;

    private OptionModifiedListener listener = new OptionModifiedListener(this);

    @Nls
    @Override
    public String getDisplayName() {
        return "EspExceptionDecoder";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel jPanel = new JPanel();

        VerticalLayout verticalLayout = new VerticalLayout(1, 2);
        jPanel.setLayout(verticalLayout);

        jFilePickerXtensaGdb8266 = new JFilePicker("xtensa-lx106-elf-gdb path:", "...");
        jFilePickerXtensaGdb32 = new JFilePicker("xtensa-esp32-elf-gdb path:", "...");

        reset();

        jFilePickerXtensaGdb8266.getTextField().getDocument().addDocumentListener(listener);
        jFilePickerXtensaGdb32.getTextField().getDocument().addDocumentListener(listener);

        jPanel.add(jFilePickerXtensaGdb8266);
        jPanel.add(jFilePickerXtensaGdb32);

        return jPanel;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        Settings.set(OPTION_KEY_GDB8266, jFilePickerXtensaGdb8266.getTextField().getText());
        Settings.set(OPTION_KEY_GDB32, jFilePickerXtensaGdb32.getTextField().getText());
        modified = false;
    }

    @Override
    public void reset() {
        String python = Settings.get(OPTION_KEY_GDB8266);
        jFilePickerXtensaGdb8266.getTextField().setText(python);

        String cpplint = Settings.get(OPTION_KEY_GDB32);
        jFilePickerXtensaGdb32.getTextField().setText(cpplint);

        modified = false;
    }

    @Override
    public void disposeUIResources() {
        jFilePickerXtensaGdb8266.getTextField().getDocument().removeDocumentListener(listener);
        jFilePickerXtensaGdb32.getTextField().getDocument().removeDocumentListener(listener);
    }

    private static class OptionModifiedListener implements DocumentListener {
        private final XtensaGdbOption option;

        public OptionModifiedListener(XtensaGdbOption option) {
            this.option = option;
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            option.setModified(true);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            option.setModified(true);
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            option.setModified(true);
        }
    }
}
