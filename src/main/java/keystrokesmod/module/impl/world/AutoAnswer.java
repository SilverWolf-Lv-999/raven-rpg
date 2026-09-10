package keystrokesmod.module.impl.world;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.QuestionUtility;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.question.QuestionEntry;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.List;

public class AutoAnswer extends Module {
    private SliderSetting delay;
    private List<QuestionEntry> pendingQuestions = Collections.emptyList();
    private IChatComponent pendingClickComponent;
    private double pendingTicks;
    private int optionTicks;

    public AutoAnswer() {
        super("Auto Answer", category.world);
        this.registerSetting(delay = new SliderSetting("Delay", " tick", 2.0, 0.0, 20.0, 0.1));
    }

    @Override
    public void onDisable() {
        clearPending();
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            clearPending();
            return;
        }
        if (pendingQuestions.isEmpty()) {
            return;
        }
        if (isClickQuestion() && pendingClickComponent == null) {
            if (--optionTicks <= 0) {
                clearPending();
            }
            return;
        }
        if (--pendingTicks > 0) {
            return;
        }
        executePending();
        clearPending();
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Utils.nullCheck() || e.message == null) {
            return;
        }
        String stripped = Utils.stripColor(e.message.getUnformattedText());
        if (stripped.isEmpty()) {
            return;
        }

        List<QuestionEntry> questions = QuestionUtility.find(stripped);
        if (!questions.isEmpty()) {
            pendingQuestions = questions;
            pendingClickComponent = findClickComponent(e.message, questions);
            pendingTicks = delay.getInput();
            optionTicks = 20;
            return;
        }
        if (pendingQuestions.isEmpty() || !isClickQuestion() || pendingClickComponent != null) {
            return;
        }
        pendingClickComponent = findClickComponent(e.message, pendingQuestions);
    }

    private IChatComponent findClickComponent(IChatComponent message, List<QuestionEntry> questions) {
        for (IChatComponent component : message) {
            if (component == null || component.getChatStyle() == null) {
                continue;
            }
            ClickEvent clickEvent = component.getChatStyle().getChatClickEvent();
            if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND) {
                continue;
            }
            String componentText = Utils.stripColor(component.getUnformattedText());
            for (QuestionEntry question : questions) {
                if (question.isClick() && componentText.contains(question.getAnswer())) {
                    return component;
                }
            }
        }
        return null;
    }

    private boolean isClickQuestion() {
        for (QuestionEntry question : pendingQuestions) {
            if (question.isClick()) {
                return true;
            }
        }
        return false;
    }

    private void executePending() {
        if (pendingClickComponent != null) {
            ClickEvent clickEvent = pendingClickComponent.getChatStyle().getChatClickEvent();
            if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                mc.thePlayer.sendChatMessage(clickEvent.getValue());
            }
            return;
        }
        for (QuestionEntry question : pendingQuestions) {
            if (question.isSend()) {
                mc.thePlayer.sendChatMessage(question.getAnswer());
                return;
            }
        }
    }

    private void clearPending() {
        pendingQuestions = Collections.emptyList();
        pendingClickComponent = null;
        pendingTicks = 0;
        optionTicks = 0;
    }
}
