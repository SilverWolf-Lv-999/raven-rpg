package keystrokesmod.module.impl.world;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoAnswer extends Module {
    private final SliderSetting delay = new SliderSetting("delay", 2.0, 0.0, 20.0, 0.1); // 延迟多少tick

    public AutoAnswer() {
        super("Auto Answer", category.world);
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Utils.nullCheck()) return;
        if (e.message == null) return;
        String stripped = Utils.stripColor(e.message.getUnformattedText());
        if (stripped.isEmpty()) {
            return;
        }
        for (IChatComponent component : e.message.getSiblings()) {

        }
    }
}
