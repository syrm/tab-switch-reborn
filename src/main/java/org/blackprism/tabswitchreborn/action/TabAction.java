/*
 * Copyright (c) 2008-2009 by Fuhrer Engineering AG, CH-2504 Biel/Bienne, Switzerland & Bas Leijdekkers
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

import java.awt.event.KeyEvent;
import java.util.List;


import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.blackprism.tabswitchreborn.TabSwitchProjectComponent;

public abstract class TabAction extends AnAction implements DumbAware {

  protected abstract List<VirtualFile> getOpenFiles(Project project);

  /**
   * @return true if this action moves up the list (previous tab), false to move down (next tab)
   */
  protected abstract boolean moveUp();

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (canShowTabSwitchPopup(event, project)) {
      TabSwitchProjectComponent.getHandler(project).show(getKeyEvent(event), moveUp(), getOpenFiles(project));
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

  private boolean canShowTabSwitchPopup(AnActionEvent event, Project project) {
    return project != null && event.getInputEvent() instanceof KeyEvent;
  }

  private KeyEvent getKeyEvent(AnActionEvent event) {
    return (KeyEvent) event.getInputEvent();
  }
}