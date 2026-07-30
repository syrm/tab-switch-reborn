package org.blackprism.tabswitchreborn.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.JBUI;

import org.blackprism.tabswitchreborn.settings.TabSwitchSettings.Order;

/**
 * Settings page (Settings | Tools | Tab Switch Reborn) to pick how the popup orders open files
 * and how large it may grow.
 */
public class TabSwitchConfigurable implements Configurable {

  private JRadioButton editorTabsRadio;
  private JRadioButton recentlyUsedRadio;
  private JSpinner maxWidthSpinner;
  private JSpinner maxRowsSpinner;

  @Override
  public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Tab Switch Reborn";
  }

  @Override
  public @Nullable JComponent createComponent() {
    editorTabsRadio = new JRadioButton("Same as the editor tabs (follows the tab bar order, including alphabetical sorting)");
    recentlyUsedRadio = new JRadioButton("Most recently used (Alt+Tab style)");
    ButtonGroup group = new ButtonGroup();
    group.add(editorTabsRadio);
    group.add(recentlyUsedRadio);

    maxWidthSpinner = new JSpinner(new SpinnerNumberModel(700, 200, 4000, 50));
    maxRowsSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 100, 1));
    // By default a JSpinner only pushes a typed value into its model on Enter, so a value the
    // user types but doesn't confirm is silently dropped by getValue(). Commit on every valid
    // keystroke so typing (the natural way to reach an arbitrary width) actually takes effect.
    commitOnValidEdit(maxWidthSpinner);
    commitOnValidEdit(maxRowsSpinner);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.NONE;

    panel.add(new JLabel("Order of open files shown in the Next/Previous tab popup:"), c);
    c.gridy++;
    c.insets = JBUI.insetsTop(4);
    panel.add(editorTabsRadio, c);
    c.gridy++;
    panel.add(recentlyUsedRadio, c);

    // Labelled spinners: label in column 0, spinner in column 1.
    c.gridy++;
    c.gridwidth = 1;
    c.insets = JBUI.insetsTop(14);
    panel.add(new JLabel("Maximum popup width (px):"), c);
    c.gridx = 1;
    c.insets = JBUI.insets(14, 8, 0, 0);
    panel.add(maxWidthSpinner, c);

    c.gridx = 0;
    c.gridy++;
    c.insets = JBUI.insetsTop(6);
    panel.add(new JLabel("Maximum visible rows (extra files scroll):"), c);
    c.gridx = 1;
    c.insets = JBUI.insets(6, 8, 0, 0);
    panel.add(maxRowsSpinner, c);

    // Filler that soaks up all remaining space so the fields stay packed at the top-left.
    c.gridx = 0;
    c.gridy++;
    c.gridwidth = 2;
    c.weightx = 1.0;
    c.weighty = 1.0;
    c.fill = GridBagConstraints.BOTH;
    c.insets = JBUI.emptyInsets();
    panel.add(new JPanel(), c);

    reset();
    return panel;
  }

  @Override
  public boolean isModified() {
    TabSwitchSettings settings = TabSwitchSettings.getInstance();
    return selectedOrder() != settings.getOrder()
        || maxWidthValue() != settings.getMaxWidthPx()
        || maxRowsValue() != settings.getMaxRows();
  }

  @Override
  public void apply() {
    // Safety net for the case where a value was typed but not yet committed (e.g. the user hit
    // OK straight after typing): flush the editor into the model before reading it.
    commitPendingEdit(maxWidthSpinner);
    commitPendingEdit(maxRowsSpinner);
    TabSwitchSettings settings = TabSwitchSettings.getInstance();
    settings.setOrder(selectedOrder());
    settings.setMaxWidthPx(maxWidthValue());
    settings.setMaxRows(maxRowsValue());
  }

  @Override
  public void reset() {
    TabSwitchSettings settings = TabSwitchSettings.getInstance();
    Order order = settings.getOrder();
    editorTabsRadio.setSelected(order == Order.EDITOR_TABS);
    recentlyUsedRadio.setSelected(order == Order.RECENTLY_USED);
    maxWidthSpinner.setValue(settings.getMaxWidthPx());
    maxRowsSpinner.setValue(settings.getMaxRows());
  }

  private Order selectedOrder() {
    return recentlyUsedRadio.isSelected() ? Order.RECENTLY_USED : Order.EDITOR_TABS;
  }

  private int maxWidthValue() {
    return (Integer) maxWidthSpinner.getValue();
  }

  private int maxRowsValue() {
    return (Integer) maxRowsSpinner.getValue();
  }

  /** Make the spinner push a valid typed value into its model immediately, without needing Enter. */
  private static void commitOnValidEdit(JSpinner spinner) {
    if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
      JFormattedTextField field = editor.getTextField();
      if (field.getFormatter() instanceof DefaultFormatter formatter) {
        formatter.setCommitsOnValidEdit(true);
      }
    }
  }

  /** Flush any typed-but-uncommitted value into the spinner model; ignore invalid input. */
  private static void commitPendingEdit(JSpinner spinner) {
    try {
      spinner.commitEdit();
    } catch (ParseException ignored) {
      // Leave the model at its last valid value.
    }
  }
}
