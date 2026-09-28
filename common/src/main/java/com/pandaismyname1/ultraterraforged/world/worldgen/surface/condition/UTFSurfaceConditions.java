package com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition;

import com.pandaismyname1.ultraterraforged.data.UTFCodecs;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import com.pandaismyname1.ultraterraforged.platform.RegistryUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;

public class UTFSurfaceConditions {

	public static void bootstrap() {
		register("mod", ModCondition.CODEC);
		register("biome_tag", BiomeTagCondition.CODEC);
		register("noise", NoiseCondition.Source.CODEC);
		register("terrain", TerrainCondition.Source.CODEC);
		register("height", HeightCondition.Source.CODEC);
		register("steepness", SteepnessCondition.Source.CODEC);
		register("erosion", ErosionCondition.Source.CODEC);
		register("sediment", SedimentCondition.Source.CODEC);
		register("river_bank", RiverBankCondition.Source.CODEC);
		register("river_side", RiverSideCondition.Source.CODEC);
		register("near_ground", NearGroundCondition.Source.CODEC);
		register("height_modification_detection", HeightModificationDetection.Source.CODEC);
		register("any", AnyCondition.CODEC);
	}

	public static AnyCondition any(MaterialCondition... conditions) {
		return new AnyCondition(List.of(conditions));
	}
	
	public static ModCondition modLoaded(String modId) {
		return new ModCondition(modId);
	}
	
	public static BiomeTagCondition biomeTag(TagKey<Biome> tag) {
		return new BiomeTagCondition(tag);
	}
	
	public static NoiseCondition.Source noise(Holder<Noise> noise, float threshold) {
		return new NoiseCondition.Source(noise, threshold);
	}
	
	public static TerrainCondition.Source terrain(Terrain... terrain) {
		return new TerrainCondition.Source(Arrays.stream(terrain).collect(Collectors.toSet()));
	}
	
	public static HeightCondition.Source height(float threshold) {
		return height(constant(threshold));
	}

	public static HeightCondition.Source height(float threshold, Holder<Noise> variance) {
		return height(constant(threshold), variance);
	}

	public static HeightCondition.Source height(Holder<Noise> threshold) {
		return height(threshold, constant(0.0F));
	}

	public static HeightCondition.Source height(Holder<Noise> threshold, Holder<Noise> variance) {
		return new HeightCondition.Source(threshold, variance);
	}
	
	public static SteepnessCondition.Source steepness(float threshold) {
		return steepness(constant(threshold), constant(0.0F));
	}

	public static SteepnessCondition.Source steepness(float threshold, Holder<Noise> variance) {
		return steepness(constant(threshold), variance);
	}
	
	public static SteepnessCondition.Source steepness(Holder<Noise> threshold, Holder<Noise> variance) {
		return new SteepnessCondition.Source(threshold, variance);
	}
	
	public static ErosionCondition.Source erosion(float threshold) {
		return erosion(constant(threshold), constant(0.0F));
	}
	
	public static ErosionCondition.Source erosion(Holder<Noise> threshold, Holder<Noise> variance) {
		return new ErosionCondition.Source(threshold, variance);
	}
	
	public static SedimentCondition.Source sediment(float threshold) {
		return sediment(constant(threshold), constant(0.0F));
	}
	
	public static SedimentCondition.Source sediment(Holder<Noise> threshold, Holder<Noise> variance) {
		return new SedimentCondition.Source(threshold, variance);
	}

	public static RiverBankCondition.Source riverBank(float threshold) {
		return riverBank(constant(threshold), constant(0.0F));
	}
	
	public static RiverBankCondition.Source riverBank(Holder<Noise> threshold, Holder<Noise> variance) {
		return new RiverBankCondition.Source(threshold, variance);
	}
	
	/**
	 * On the bed or banks of a river or lake: within this share of the width of its water and banks from its middle,
	 * 1 being their outer edge.
	 */
	public static RiverSideCondition.Source riverSide(float within) {
		return new RiverSideCondition.Source(within);
	}
	
	/**
	 * Within this many blocks under the ground the terrain was generated with: the surface of the land or the sea
	 * floor, not the floor of a cave.
	 */
	public static NearGroundCondition.Source nearGround(int depth) {
		return new NearGroundCondition.Source(depth);
	}
	
	public static HeightModificationDetection.Source heightModificationDetection(HeightModificationDetection.Target target) {
		return new HeightModificationDetection.Source(target);
	}
	
	public static void register(String name, Codec<? extends MaterialCondition> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_CONDITION_TYPE, name, UTFCodecs.entry(value));
	}
	
	private static Holder<Noise> constant(float value) {
		return Holder.direct(Noises.constant(value));
	}
}
