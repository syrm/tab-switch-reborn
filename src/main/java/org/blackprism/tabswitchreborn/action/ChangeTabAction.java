package org.blackprism.tabswitchreborn.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.blackprism.tabswitchreborn.filefetcher.FileFetcher;

import java.util.List;

public abstract class ChangeTabAction extends TabAction {
  private final FileFetcher<VirtualFile> fileFetcher;

  protected ChangeTabAction(FileFetcher<VirtualFile> fileFetcher) {
    this.fileFetcher = fileFetcher;
  }

  @Override
  protected List<VirtualFile> getOpenFiles(Project project) {
    return fileFetcher.getFiles(project);
  }
}
