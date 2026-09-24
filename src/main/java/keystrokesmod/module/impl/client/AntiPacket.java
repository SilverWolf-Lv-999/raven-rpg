package keystrokesmod.module.impl.client;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiPacket extends Module {
    private final ButtonSetting particle;

    public AntiPacket() {
        super("Anti Packet", category.client);
        this.registerSetting(particle = new ButtonSetting("Particle", true));
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (particle.isToggled() && event.getPacket() instanceof S2APacketParticles) {
            event.setCanceled(true);
        }
    }
}
