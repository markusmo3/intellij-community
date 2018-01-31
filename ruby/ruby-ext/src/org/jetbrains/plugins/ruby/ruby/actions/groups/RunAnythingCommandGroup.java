package org.jetbrains.plugins.ruby.ruby.actions.groups;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.actions.*;

public class RunAnythingCommandGroup extends RunAnythingGroup {
  private static final int MAX_COMMANDS = 5;

  @NotNull
  @Override
  public String getTitle() {
    return "Recent commands";
  }

  @NotNull
  @Override
  protected String getKey() {
    return "run.anything.undefined.commands";
  }

  @Override
  protected int getMax() {
    return MAX_COMMANDS;
  }

  public RunAnythingAction.SearchResult getItems(@NotNull Project project,
                                                 @Nullable Module module,
                                                 @NotNull RunAnythingSearchListModel listModel,
                                                 @NotNull String pattern,
                                                 boolean isMore,
                                                 @NotNull Runnable check) {
    RunAnythingAction.SearchResult result = new RunAnythingAction.SearchResult();

    check.run();
    for (String command : ContainerUtil.iterateBackward(RunAnythingCache.getInstance(project).getState().undefinedCommands)) {
      if (addToList(listModel, result, pattern, new RunAnythingUndefinedItem(project, module, command), command, isMore)) break;
      check.run();
    }
    return result;
  }

  @NotNull
  @Override
  public WidgetID getWidget() {
    return WidgetID.COMMANDS;
  }

  @Override
  public boolean isRecent() {
    return true;
  }
}