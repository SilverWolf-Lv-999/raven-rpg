package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.AutoTracking;

import java.util.Arrays;
import java.util.List;

public class TrackingLiving extends Command {
    public TrackingLiving() {
        super("trackingLiving");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() != 1) {
            syntaxError();
            return;
        }
        AutoTracking autoTracking = ModuleManager.autoTracking;
        if (autoTracking == null) {
            replyWithHeader("&cAuto Tracking is unavailable.");
            return;
        }
        String action = input.getArgument(0);
        if ("start".equalsIgnoreCase(action)) {
            if (!autoTracking.isEnabled()) {
                autoTracking.enable();
            }
            autoTracking.setTracking(true);
            replyWithHeader("&7Living entity tracking &aenabled&7.");
            return;
        }
        if ("stop".equalsIgnoreCase(action)) {
            autoTracking.setTracking(false);
            if (autoTracking.isEnabled()) {
                autoTracking.disable();
            }
            replyWithHeader("&7Living entity tracking &cdisabled&7.");
            return;
        }
        syntaxError();
    }

    @Override
    public List<String> suggest(CommandInput input) {
        return filterSuggestions(input, Arrays.asList("start", "stop"));
    }

    @Override
    public int getSuggestionArgumentStart(CommandInput input) {
        return 0;
    }
}
