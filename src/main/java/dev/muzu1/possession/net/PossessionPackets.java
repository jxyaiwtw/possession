package dev.muzu1.possession.net;

import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.session.PossessionManager;
import dev.muzu1.possession.session.PossessionReason;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class PossessionPackets {
    public static final Identifier C2S_ATTACH_REQUEST = PossessionMod.id("attach_request");
    public static final Identifier C2S_DETACH_REQUEST = PossessionMod.id("detach_request");
    public static final Identifier C2S_CONTROL_UPDATE = PossessionMod.id("control_update");
    public static final Identifier S2C_ATTACH_ACCEPTED = PossessionMod.id("attach_accepted");
    public static final Identifier S2C_ATTACH_REJECTED = PossessionMod.id("attach_rejected");
    public static final Identifier S2C_FORCE_EXIT = PossessionMod.id("force_exit");

    private PossessionPackets() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ATTACH_REQUEST, (server, player, handler, buf, responseSender) -> {
            int targetEntityId = buf.readInt();
            server.execute(() -> PossessionManager.getInstance().handleAttachRequest(player, targetEntityId));
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_DETACH_REQUEST, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> PossessionManager.getInstance().handleDetachRequest(player, PossessionReason.MANUAL));
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_CONTROL_UPDATE, (server, player, handler, buf, responseSender) -> {
            float forward = buf.readFloat();
            float sideways = buf.readFloat();
            boolean jump = buf.readBoolean();
            boolean sneak = buf.readBoolean();
            boolean sprint = buf.readBoolean();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            boolean ability = buf.readBoolean();
            boolean attack = buf.readBoolean();
            boolean use = buf.readBoolean();
            int focusTargetEntityId = buf.readInt();
            int attackTargetEntityId = buf.readInt();
            boolean hasBlockTarget = buf.readBoolean();
            BlockPos blockTargetPos = hasBlockTarget ? buf.readBlockPos() : BlockPos.ORIGIN;
            Direction blockTargetSide = hasBlockTarget ? buf.readEnumConstant(Direction.class) : Direction.UP;
            server.execute(() -> PossessionManager.getInstance().handleControlUpdate(player, forward, sideways, jump, sneak, sprint, yaw, pitch, ability, attack, use, focusTargetEntityId, attackTargetEntityId, hasBlockTarget, blockTargetPos, blockTargetSide));
        });
    }

    public static void sendAttachAccepted(ServerPlayerEntity player, int targetEntityId, float boostedMaxHealth, String displayName) {
        var buf = PacketByteBufs.create();
        buf.writeInt(targetEntityId);
        buf.writeFloat(boostedMaxHealth);
        buf.writeString(displayName);
        ServerPlayNetworking.send(player, S2C_ATTACH_ACCEPTED, buf);
    }

    public static void sendReject(ServerPlayerEntity player, String reasonKey) {
        var buf = PacketByteBufs.create();
        buf.writeString(reasonKey);
        ServerPlayNetworking.send(player, S2C_ATTACH_REJECTED, buf);
    }

    public static void sendForceExit(ServerPlayerEntity player, String reasonKey) {
        var buf = PacketByteBufs.create();
        buf.writeString(reasonKey);
        ServerPlayNetworking.send(player, S2C_FORCE_EXIT, buf);
    }
}
