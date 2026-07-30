package org.blackprism.tabswitchreborn.action;

import org.blackprism.tabswitchreborn.filefetcher.ChangedFilesInVcsFileFetcher;

public class PrevVcsChangeAction extends ChangeTabAction {

  public PrevVcsChangeAction() {
    super(new ChangedFilesInVcsFileFetcher());
  }

  @Override
  protected boolean moveUp() {
    return true;
  }
}
