package org.blackprism.tabswitchreborn.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-level persistent settings for the plugin.
 */
@Service(Service.Level.APP)
@State(name = "TabSwitchRebornSettings", storages = @Storage("tabSwitchReborn.xml"))
public final class TabSwitchSettings implements PersistentStateComponent<TabSwitchSettings.State> {

  /** How the Next/Previous tab popup orders open files. */
  public enum Order {
    /** Follow the editor tab bar order (respects the IDE's alphabetical tab sorting). */
    EDITOR_TABS,
    /** Most-recently-used order, Alt+Tab style. */
    RECENTLY_USED
  }

  public static class State {
    public Order order = Order.EDITOR_TABS;
    /** Maximum popup width in (unscaled) pixels; the popup grows up to this to fit the text. */
    public int maxWidthPx = 700;
    /** Maximum number of visible rows; extra files are reachable via the scrollbar. */
    public int maxRows = 15;
  }

  private State state = new State();

  public static TabSwitchSettings getInstance() {
    return ApplicationManager.getApplication().getService(TabSwitchSettings.class);
  }

  @Override
  public @Nullable State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  public Order getOrder() {
    return state.order;
  }

  public void setOrder(Order order) {
    state.order = order;
  }

  public int getMaxWidthPx() {
    return state.maxWidthPx;
  }

  public void setMaxWidthPx(int maxWidthPx) {
    state.maxWidthPx = maxWidthPx;
  }

  public int getMaxRows() {
    return state.maxRows;
  }

  public void setMaxRows(int maxRows) {
    state.maxRows = maxRows;
  }
}
