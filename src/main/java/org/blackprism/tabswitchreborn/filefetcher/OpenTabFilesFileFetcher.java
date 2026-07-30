package org.blackprism.tabswitchreborn.filefetcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;

import org.blackprism.tabswitchreborn.settings.TabSwitchSettings;

/**
 * Creates a list of {@link VirtualFile} of the files open in tabs in the current project, ordered
 * either like the editor tab bar or by most-recent use, depending on {@link TabSwitchSettings}.
 */
public class OpenTabFilesFileFetcher implements FileFetcher<VirtualFile> {

  @Override
  public List<VirtualFile> getFiles(Project project) {
    if (TabSwitchSettings.getInstance().getOrder() == TabSwitchSettings.Order.RECENTLY_USED) {
      return getFilesInRecentlyUsedOrder(project);
    }
    return getFilesInTabOrder(project);
  }

  /** Files in the order of the current editor tab bar, matching the IDE's alphabetical sorting when enabled. */
  private List<VirtualFile> getFilesInTabOrder(Project project) {
    FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
    EditorWindow window = fileEditorManager.getCurrentWindow();
    List<VirtualFile> files = new ArrayList<>(
        window != null ? window.getFileList() : List.of(fileEditorManager.getOpenFiles()));
    if (UISettings.getInstance().getSortTabsAlphabetically()) {
      // Sort by the actual tab title (what the tab bar shows), not VirtualFile.getName(),
      // so special tabs like diffs land where the IDE puts them.
      files.sort(Comparator.comparing(
          file -> EditorTabPresentationUtil.getEditorTabTitle(project, file),
          String.CASE_INSENSITIVE_ORDER));
    }
    return files;
  }

  /** Open files in most-recently-used order, Alt+Tab style. */
  private List<VirtualFile> getFilesInRecentlyUsedOrder(Project project) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    VirtualFile[] recentFiles = ArrayUtil.reverseArray(EditorHistoryManager.getInstance(project).getFiles());
    List<VirtualFile> openFiles = new LinkedList<>();
    int editorTabLimit = UISettings.getInstance().getEditorTabLimit();
    for (VirtualFile file : recentFiles) {
      if (openFiles.size() <= editorTabLimit && fileEditorManager.isFileOpen(file) && !openFiles.contains(file)) {
        openFiles.add(file);
      }
    }
    return openFiles;
  }
}
