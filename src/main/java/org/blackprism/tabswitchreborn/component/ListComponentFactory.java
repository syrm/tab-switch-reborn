package org.blackprism.tabswitchreborn.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.IconUtil;
import org.blackprism.tabswitchreborn.TabSwitchProjectComponent;

class ListComponentFactory {

  private final Project project;

  ListComponentFactory(Project project) {
    this.project = project;
  }

  JList<VirtualFile> create(JLabel pathLabel) {
    JList<VirtualFile> list = new JBList<>();
    list.setCellRenderer(new ListCellRendererWithColorFactory().create(project));
    list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.getSelectionModel().addListSelectionListener(new ListSelectionListenerWithPathUpdaterFactory().create(list, pathLabel));
    list.addMouseListener(new ListMouseListener(list));
    return list;
  }

  private class ListMouseListener extends MouseAdapter {
    private final JList<VirtualFile> list;

    public ListMouseListener(JList<VirtualFile> list) {
      this.list = list;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      int index = list.locationToIndex(e.getPoint());
      if (index != -1) {
        list.setSelectedIndex(index);
        TabSwitchProjectComponent.getHandler(project).closeAndOpenSelectedFile();
      }
    }
  }

  /**
   * Simple ListCellRenderer factory. This is the default one to render the popped up list.
   */
  private static class ListCellRendererWithColorFactory {

    private ListCellRendererWithColorFactory() {
    }

    ColoredListCellRenderer<VirtualFile> create(final Project project) {
      return new ColoredListCellRenderer<VirtualFile>() {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends VirtualFile> list,
                                             VirtualFile file,
                                             int index,
                                             boolean selected,
                                             boolean hasFocus) {
          setIcon(IconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, project));
          append(EditorTabPresentationUtil.getEditorTabTitle(project, file),
                 SimpleTextAttributes.fromTextAttributes(new TextAttributes(getForegroundColor(file, project),
                                                                                            null,
                                                                                            null,
                                                                                            EffectType.LINE_UNDERSCORE,
                                                                                            Font.PLAIN)));
        }
      };
    }

    private Color getForegroundColor(VirtualFile file, Project project) {
      return FileStatusManager.getInstance(project).getStatus(file).getColor();
    }
  }

  private static class ListSelectionListenerWithPathUpdaterFactory {

    private ListSelectionListenerWithPathUpdaterFactory() {
    }

    ListSelectionListener create(final JList<VirtualFile> list, final JLabel pathLabel) {
      return new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent event) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              updatePath(list, pathLabel);
            }
          });
        }
      };
    }

    private void updatePath(JList<VirtualFile> list, JLabel path) {
      path.setText(getPathTextOrEmptyString(path, list.getSelectedValuesList()));
    }

    private String getPathTextOrEmptyString(JLabel path, List<VirtualFile> selectedValues) {
      return onlyOneFileIsSelected(selectedValues)
             ? getPathTextForSelectedFile(path, selectedValues.get(0).getParent())
             : "";
    }

    private boolean onlyOneFileIsSelected(List<VirtualFile> selectedValues) {
      return selectedValues != null && selectedValues.size() == 1;
    }

    private String getPathTextForSelectedFile(JLabel path, @Nullable VirtualFile parent) {
      if (parent == null) return "";
      String text = parent.getPresentableUrl();
      FontMetrics fontMetrics = path.getFontMetrics(path.getFont());
      while ((fontMetrics.stringWidth(text) > path.getWidth()) && (text.indexOf(File.separatorChar, 4) > 0)) {
        text = "..." + text.substring(text.indexOf(File.separatorChar, 4));
      }
      return text;
    }
  }
}
