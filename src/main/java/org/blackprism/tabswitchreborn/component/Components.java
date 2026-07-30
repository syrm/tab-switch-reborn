package org.blackprism.tabswitchreborn.component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class Components {

  private Components() {
  }

  public static JList<VirtualFile> newList(Project project, JLabel pathLabel) {
    return new ListComponentFactory(project).create(pathLabel);
  }

  public static JComponent newListFooter(JLabel pathLabel) {
    return new FooterComponentFactory().create(pathLabel);
  }

  public static JLabel newPathLabel() {
    return new PathLabelComponentFactory().create();
  }
}
