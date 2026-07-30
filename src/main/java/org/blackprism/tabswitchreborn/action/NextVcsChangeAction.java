package org.blackprism.tabswitchreborn.action;

import org.blackprism.tabswitchreborn.filefetcher.ChangedFilesInVcsFileFetcher;

public class NextVcsChangeAction extends ChangeTabAction {

  public NextVcsChangeAction() {
    super(new ChangedFilesInVcsFileFetcher());
  }

  @Override
  protected boolean moveUp() {
    return false;
  }
}
