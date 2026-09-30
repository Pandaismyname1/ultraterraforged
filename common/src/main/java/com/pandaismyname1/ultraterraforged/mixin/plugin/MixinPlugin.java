package com.pandaismyname1.ultraterraforged.mixin.plugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.common.collect.ImmutableList;

import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.compat.worldpreview.WPCompat;
import com.pandaismyname1.ultraterraforged.compat.c2me.C2MECompat;

public class MixinPlugin implements IMixinConfigPlugin {
	private static final String MIXIN_PACKAGE_PREFIX = "com.pandaismyname1.ultraterraforged.mixin.";
	public static final List<String> TB_MIXINS = ImmutableList.of(mixinClassName("terrablender.MixinClimateSampler"), mixinClassName("terrablender.MixinNoiseChunk"), mixinClassName("terrablender.MixinParameterList"), mixinClassName("terrablender.MixinTargetPoint"));
	public static final List<String> WP_MIXINS = ImmutableList.of(mixinClassName("worldpreview.SampleUtilsMixin"));
	public static final List<String> C2ME_OPENCL_MIXINS = ImmutableList.of(mixinClassName("c2me.MixinChunkMapOpenCL"));
	
	@Override
	public void onLoad(String mixinPackage) {
		log(TBCompat.isEnabled(), "TerraBlender");
		log(WPCompat.isEnabled(), "World Preview");
		log(C2MECompat.isOpenCLLoaded(), "C2ME OpenCL");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return TB_MIXINS.contains(mixinClassName) ? TBCompat.isEnabled() : 
			   WP_MIXINS.contains(mixinClassName) ? WPCompat.isEnabled() :
			   C2ME_OPENCL_MIXINS.contains(mixinClassName) ? C2MECompat.isOpenCLLoaded() :
			   true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return Stream.of(TB_MIXINS, WP_MIXINS, C2ME_OPENCL_MIXINS).flatMap(List::stream).map((str) -> {
			return str.replace(MIXIN_PACKAGE_PREFIX, "");
		}).toList();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	private static String mixinClassName(String className) {
		return MIXIN_PACKAGE_PREFIX + className;
	}
	
	private static void log(boolean isModLoaded, String modName) {
		if(isModLoaded) {
			UTFCommon.LOGGER.info("Enabling {} compat", modName);
		} else {
			UTFCommon.LOGGER.info("Disabling {} compat", modName);
		}
	}
}
