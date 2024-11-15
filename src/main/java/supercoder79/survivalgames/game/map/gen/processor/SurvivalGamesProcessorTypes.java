package supercoder79.survivalgames.game.map.gen.processor;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;

public class SurvivalGamesProcessorTypes {
	public static StructureProcessorType<LootChestProcessor> LOOT;
	public static StructureProcessorType<ChanceAtProcessor> CHANCE_AT;
	public static void init() {
		LOOT = Registry.register(Registries.STRUCTURE_PROCESSOR, Identifier.of("survivalgames", "loot"), () -> LootChestProcessor.CODEC);
		CHANCE_AT = Registry.register(Registries.STRUCTURE_PROCESSOR, Identifier.of("survivalgames", "chance_at"), () -> ChanceAtProcessor.CODEC);
	}
}
