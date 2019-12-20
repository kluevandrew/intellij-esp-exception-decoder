package com.kluev.espexceptiondecoder;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;

public class EspExceptionDecoderComponent implements ProjectComponent {
    Project project;

    protected EspExceptionDecoderComponent(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        this.initToolWindow();
    }

    public void initToolWindow() {
        final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);

        EspExceptionDecoderWindow window = new EspExceptionDecoderWindow(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(window.getContent(), "", false);

        ToolWindow toolWindow = manager.registerToolWindow("EspExceptionDecoder",
                false, ToolWindowAnchor.BOTTOM, project, true
        );
        toolWindow.getContentManager().addContent(content);
        toolWindow.setIcon(IconLoader.getIcon("/intellij-esp-exception-decoder/plus.png"));

        Disposer.register(project, () -> toolWindow.getContentManager().removeAllContents(true));
    }
}
