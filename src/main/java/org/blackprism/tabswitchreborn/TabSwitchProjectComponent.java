/*
 * Copyright (c) 2008-2011 by Fuhrer Engineering AG, CH-2504 Biel/Bienne, Switzerland & Bas Leijdekkers
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
package org.blackprism.tabswitchreborn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;

import org.blackprism.tabswitchreborn.component.Components;
import org.blackprism.tabswitchreborn.settings.TabSwitchSettings;

/**
 * Project-level service that owns the Tab Switch Reborn popup and drives it from raw key events.
 */
@Service(Service.Level.PROJECT)
public final class TabSwitchProjectComponent implements KeyEventDispatcher {

  private final Project project;
  private final BitSet modifiers = new BitSet();
  private final JList<VirtualFile> list;
  private final JBScrollPane scrollPane;
  private final JComponent content;

  private JBPopup popup;
  private int trigger = 0;
  private boolean movingUp;
  private boolean isTriggeredWithShift;

  public TabSwitchProjectComponent(Project project) {
    this.project = project;

    JLabel pathLabel = Components.newPathLabel();
    this.list = Components.newList(project, pathLabel);

    this.scrollPane = new JBScrollPane(list);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    // We grow the width to fit the text (up to the configured maximum) but never scroll it
    // horizontally: this also avoids a spurious horizontal scrollbar flickering in when the
    // selected cell is measured a hair wider than the reserved width while cycling.
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.add(Components.newListFooter(pathLabel), BorderLayout.SOUTH);
    this.content = panel;
  }

  public static TabSwitchProjectComponent getHandler(Project project) {
    return project.getService(TabSwitchProjectComponent.class);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    boolean consumed = true;
    if (popup != null && popup.isDisposed()) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
      consumed = false;
    } else if ((event.getID() == KeyEvent.KEY_RELEASED) && modifiers.get(event.getKeyCode())) {
      closeAndOpenSelectedFile();
    } else if (event.getID() == KeyEvent.KEY_PRESSED) {
      int keyCode = event.getKeyCode();
      if (keyCode == trigger) {
        moveOnTrigger(event.isShiftDown());
      } else {
        switch (keyCode) {
          case KeyEvent.VK_UP:
            moveUp();
            break;
          case KeyEvent.VK_DOWN:
            moveDown();
            break;
          case KeyEvent.VK_ENTER:
            closeAndOpenSelectedFile();
            break;
          case KeyEvent.VK_SHIFT:
          case KeyEvent.VK_CONTROL:
          case KeyEvent.VK_ALT:
          case KeyEvent.VK_ALT_GRAPH:
          case KeyEvent.VK_META:
            break;
          default:
            close();
            break;
        }
      }
    }
    return consumed;
  }

  public void show(KeyEvent event, boolean moveUp, List<VirtualFile> files) {
    if (cannotShow(files)) return;
    if (popup != null) popup.dispose();
    int currentIndex = indexOfCurrentFile(files);
    prepareListWithFiles(files);
    popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(content, list)
        .setTitle("Open files")
        .setMovable(true)
        .setResizable(false)
        .setRequestFocus(false)
        .setCancelOnClickOutside(true)
        .setCancelOnWindowDeactivation(true)
        .createPopup();
    trigger = event.getKeyCode();
    modifiers.set(KeyEvent.VK_CONTROL, event.isControlDown());
    modifiers.set(KeyEvent.VK_META, event.isMetaDown());
    modifiers.set(KeyEvent.VK_ALT, event.isAltDown());
    modifiers.set(KeyEvent.VK_ALT_GRAPH, event.isAltGraphDown());
    movingUp = moveUp;
    isTriggeredWithShift = event.isShiftDown();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    popup.showCenteredInCurrentWindow(project);
    // Select the current file AFTER the popup is realized (showing it resets the list
    // selection). The trigger key press is then caught by the KeyEventDispatcher, which
    // performs the single step to the next/previous tab — so we must NOT step here too,
    // otherwise the popup would jump two tabs at once.
    list.setSelectedIndex(currentIndex);
    list.ensureIndexIsVisible(currentIndex);
    // Re-fit to the now-realized content. On the very first open the pre-show estimate uses the
    // renderer's row height, which is smaller than what the list actually renders, so the scroll
    // pane ends up a few pixels short and shows a spurious vertical scrollbar. Recomputing from
    // the realized list and packing again removes it.
    refitToRealizedContent(files.size());
    popup.pack(true, true);
  }

  private boolean cannotShow(List<VirtualFile> files) {
    return files.isEmpty() || popup != null && popup.isVisible();
  }

  private void prepareListWithFiles(final List<VirtualFile> files) {
    list.setModel(new AbstractListModel<VirtualFile>() {
      @Override
      public int getSize() {
        return files.size();
      }

      @Override
      public VirtualFile getElementAt(int index) {
        return files.get(index);
      }
    });
    sizePopup(files.size());
  }

  /**
   * Rough size before the popup is shown, measured through the renderer (the list isn't realized
   * yet). {@link #refitToRealizedContent} corrects it once the real row metrics are known.
   */
  private void sizePopup(int fileCount) {
    Dimension cell = measureWidestCell();
    applyPreferredSize(fileCount, cell.height, cell.width);
  }

  /**
   * Re-measures from the realized list (real, laid-out row height and width) and resizes the popup.
   * Before realization the renderer underestimates the row height, which reserves too little
   * vertical space and shows a spurious scrollbar on the very first open.
   */
  private void refitToRealizedContent(int fileCount) {
    if (fileCount <= 0) return;
    Rectangle firstRow = list.getCellBounds(0, 0);
    int rowHeight = firstRow != null ? firstRow.height : measureWidestCell().height;
    applyPreferredSize(fileCount, rowHeight, list.getPreferredSize().width);
  }

  /**
   * Grows the width to fit the widest title (capped at the configured maximum) and limits the
   * visible height to the configured number of rows; extra files are reachable via the scrollbar.
   */
  private void applyPreferredSize(int fileCount, int rowHeight, int contentWidth) {
    TabSwitchSettings settings = TabSwitchSettings.getInstance();
    int rowCount = Math.max(1, Math.min(fileCount, settings.getMaxRows()));
    list.setVisibleRowCount(rowCount);

    int verticalScrollBar = fileCount > rowCount ? scrollPane.getVerticalScrollBar().getPreferredSize().width : 0;
    // Small margin so the widest title is never clipped by a pixel of measurement rounding.
    int width = Math.min(contentWidth + verticalScrollBar + JBUIScale.scale(6),
                         JBUIScale.scale(settings.getMaxWidthPx()));
    // +2px slack absorbs sub-pixel rounding so a scrollbar never shows when the rows already fit;
    // it is far smaller than a row, so it never hides the need for a scrollbar when there are more.
    int height = rowCount * rowHeight + JBUIScale.scale(2);

    scrollPane.setPreferredSize(new Dimension(width, height));
  }

  private Dimension measureWidestCell() {
    ListModel<VirtualFile> model = list.getModel();
    ListCellRenderer<? super VirtualFile> renderer = list.getCellRenderer();
    int width = 0;
    int rowHeight = JBUIScale.scale(20);
    for (int i = 0; i < model.getSize(); i++) {
      Component cell = renderer.getListCellRendererComponent(list, model.getElementAt(i), i, false, false);
      Dimension pref = cell.getPreferredSize();
      width = Math.max(width, pref.width);
      rowHeight = Math.max(rowHeight, pref.height);
    }
    return new Dimension(width, rowHeight);
  }

  private int indexOfCurrentFile(List<VirtualFile> files) {
    VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selected.length > 0) {
      int index = files.indexOf(selected[0]);
      if (index >= 0) return index;
    }
    return 0;
  }

  private void moveOnTrigger(boolean isShiftDown) {
    boolean reverse = isTriggeredWithShift != isShiftDown;
    boolean up = reverse ^ movingUp;
    move(up);
  }

  private void moveUp() {
    move(true);
  }

  private void moveDown() {
    move(false);
  }

  private void move(boolean up) {
    int offset = up ? -1 : 1;
    int size = list.getModel().getSize();
    list.setSelectedIndex((list.getSelectedIndex() + size + offset) % size);
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public void closeAndOpenSelectedFile() {
    close();
    openSelectedFile();
  }

  private void close() {
    disposePopup();
    removeMouseListeners();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
  }

  private void openSelectedFile() {
    VirtualFile file = list.getSelectedValue();
    if (file != null && file.isValid()) {
      FileEditorManager.getInstance(project).openEditor(new OpenFileDescriptor(project, file), true);
    }
  }

  private void disposePopup() {
    if (popup != null) {
      popup.cancel();
      popup.dispose();
      popup = null;
    }
  }

  /**
   * Workaround for MouseListener leak added in the popup builder.
   */
  private void removeMouseListeners() {
    for (MouseListener listener : list.getMouseListeners()) {
      removeMouseListener(listener);
    }
  }

  private void removeMouseListener(MouseListener listener) {
    if (listener.getClass().getName().startsWith("com.intellij.openapi.ui.popup.PopupChooserBuilder")) {
      list.removeMouseListener(listener);
    }
  }
}
