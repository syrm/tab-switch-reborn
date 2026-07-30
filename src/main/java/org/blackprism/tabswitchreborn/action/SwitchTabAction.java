/*
 * Copyright 2008-2011 Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.blackprism.tabswitchreborn.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Toggles back to the previously active file without any popup: repeated invocations alternate
 * between the two most recent files (like a "switch to last tab").
 */
public class SwitchTabAction extends AnAction implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) return;
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    VirtualFile file = findPreviousFile(project, fileEditorManager);
    if (file != null) {
      fileEditorManager.openFile(file, true, true);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    event.getPresentation().setEnabled(event.getProject() != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  /**
   * Walks the editor history backwards from the current file to the most recent suitable one,
   * skipping invalid (e.g. deleted) files.
   */
  @Nullable
  private VirtualFile findPreviousFile(Project project, FileEditorManager fileEditorManager) {
    boolean acceptRecentFiles = UISettings.getInstance().getEditorTabLimit() > 0;
    VirtualFile[] recentFiles = EditorHistoryManager.getInstance(project).getFiles();
    // The last entry is the current file, so start one before it.
    for (int i = recentFiles.length - 2; i >= 0; i--) {
      VirtualFile file = recentFiles[i];
      if (file.isValid() && (acceptRecentFiles || fileEditorManager.isFileOpen(file))) {
        return file;
      }
    }
    return null;
  }
}