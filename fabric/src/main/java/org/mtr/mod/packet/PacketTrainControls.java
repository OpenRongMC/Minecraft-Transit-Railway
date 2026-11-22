package org.mtr.mod.packet;

import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.entity.TransportEntity;

/** Sends key presses. The server supplies the train and driver permissions. */
public final class PacketTrainControls extends PacketHandler {
	public static final int ACCELERATE = 1;
	public static final int BRAKE = 2;
	public static final int TOGGLE_DOORS = 4;

	private final int pressed;

	public PacketTrainControls(int pressed) {
		this.pressed = pressed;
	}

	public PacketTrainControls(PacketBufferReceiver buffer) {
		this(buffer.readInt());
	}

	@Override
	public void write(PacketBufferSender buffer) {
		buffer.writeInt(pressed);
	}

	@Override
	public void runServer(MinecraftServer server, ServerPlayerEntity player) {
		if (player.data.getVehicle() instanceof TransportEntity train) {
			train.applyDriverInput(player.data, pressed);
		}
	}
}
