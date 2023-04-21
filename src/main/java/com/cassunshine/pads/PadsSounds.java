package com.cassunshine.pads;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class PadsSounds {

	public static final Identifier TELEPORT_CHARGE_SOUND_ID = new Identifier("pads:teleport_charge");
	public static SoundEvent TELEPORT_CHARGE_SOUND_EVENT = SoundEvent.createVariableRangeEvent(TELEPORT_CHARGE_SOUND_ID);

	public static final Identifier TELEPORT_SOUND_ID = new Identifier("pads:teleport");
	public static SoundEvent TELEPORT_SOUND_EVENT = SoundEvent.createVariableRangeEvent(TELEPORT_SOUND_ID);


	public static void initialize() {
		Registry.register(Registries.SOUND_EVENT, TELEPORT_SOUND_ID, TELEPORT_SOUND_EVENT);
		Registry.register(Registries.SOUND_EVENT, TELEPORT_CHARGE_SOUND_ID, TELEPORT_CHARGE_SOUND_EVENT);
	}

}
