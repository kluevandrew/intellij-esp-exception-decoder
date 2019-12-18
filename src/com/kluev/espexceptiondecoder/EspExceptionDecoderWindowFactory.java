package com.kluev.espexceptiondecoder;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class EspExceptionDecoderWindowFactory implements ToolWindowFactory {
    // Create the tool window content.
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        EspExceptionDecoder window = new EspExceptionDecoder(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(window.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
