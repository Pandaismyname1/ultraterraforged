package com.pandaismyname1.ultraterraforged.compat.terrablender;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.UTFSurfaceRules;
import terrablender.api.MaterialRuleManager;
import terrablender.api.MaterialRuleManager.RuleCategory;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

public class TBSurfaceRules {

	public static void bootstrap() {
		UTFSurfaceRules.register("terrablender", TBRule.CODEC);
	}

	public static TBRule rule(String category, String modId) {
		return new TBRule(category, modId);
	}

	// the material rules a mod gave TerraBlender for its biomes
	private record TBRule(String category, String modId, Supplier<MaterialRule> rules) implements MaterialRule {
		private static final Supplier<Map<String, RuleCategory>> BY_KEY = Suppliers.memoize(() ->
			Arrays.stream(RuleCategory.values()).collect(Collectors.toMap(RuleCategory::name, Function.identity()))
		);
		public static final Codec<TBRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("category").forGetter(TBRule::category),
			Codec.STRING.fieldOf("mod_id").forGetter(TBRule::modId)
		).apply(instance, TBRule::new));

		public TBRule(String categoryKey, String modId) {
			this(categoryKey, modId, Suppliers.memoize(() -> {
				RuleCategory category = BY_KEY.get().get(categoryKey);
				return MaterialRuleManager.getNamespacedRules(category, MaterialRuleManager.getDefaultRules(category));
			}));
		}

		@Override
		public RuleEvaluator compile(MaterialRuleContext ctx) {
			if(this.rules.get() instanceof NamespacedSurfaceRuleSource ruleSource) {
				return ruleSource.sources().get(this.modId).compile(ctx);
			} else {
				throw new IllegalArgumentException(this.modId);
			}
		}

		@Override
		public MapCodec<TBRule> codec() {
			return UTFCodecs.asMap(CODEC);
		}
	}
}
