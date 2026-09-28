package com.pandaismyname1.ultraterraforged.compat.performance;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/**
 * Performance mods worth running alongside UltraTerraForged, with the builds that exist for this Minecraft version on
 * each loader. When the mod itself has no build for a loader, a port or fork that does the same job stands in for
 * it. The list is data only; docs/porting/performance-mods.md tracks what exists for later Minecraft versions, and
 * this file is updated from it when porting.
 */
public final class PerformanceMods {
	public static final String MINECRAFT_VERSION = "26.2";
	public static final String FABRIC = "fabric";
	public static final String NEOFORGE = "neoforge";
	// Sinytra Connector, which runs Fabric mods on NeoForge; none of the mods needs it on this version
	private static final String CONNECTOR = "connector";

	public enum Category {
		// speeds up generating new chunks: what UltraTerraForged is heaviest on
		WORLD_GENERATION,
		// the server and memory in general
		GENERAL,
		// the client's rendering
		CLIENT
	}

	/**
	 * A build that fills a slot on one loader.
	 *
	 * @param name what players know it as
	 * @param modIds the ids it loads under, any of which counts
	 * @param url where to get it
	 * @param alternative whether it's a port or fork standing in for the mod itself
	 * @param viaConnector whether it's a Fabric build that only runs on NeoForge through Sinytra Connector
	 * @param minJava the oldest Java it runs on, or 0 for any the game runs on
	 */
	public record Build(String name, List<String> modIds, String url, boolean alternative, boolean viaConnector, int minJava) {

		static Build of(String name, String modId, String url) {
			return new Build(name, List.of(modId), url, false, false, 0);
		}

		static Build alternative(String name, String modId, String url) {
			return new Build(name, List.of(modId), url, true, false, 0);
		}

		Build needsJava(int version) {
			return new Build(this.name, this.modIds, this.url, this.alternative, this.viaConnector, version);
		}
	}

	/**
	 * @param key names the mod's description and translations
	 * @param name the mod itself
	 * @param url its page
	 * @param fabric the builds for Fabric, best first
	 * @param neoforge the builds for NeoForge, best first
	 * @param availableFrom for a mod with no build for this Minecraft version, the first version it has one for
	 * @param optional recommended, but only counted towards the light once installed: for mods that don't suit every
	 *        game, or can't run together with another one on the list
	 */
	public record Mod(String key, String name, String url, Category category, List<Build> fabric, List<Build> neoforge, @Nullable String availableFrom, boolean optional) {

		public Mod(String key, String name, String url, Category category, List<Build> fabric, List<Build> neoforge, @Nullable String availableFrom) {
			this(key, name, url, category, fabric, neoforge, availableFrom, false);
		}

		public List<Build> builds(String loader) {
			return loader.equals(FABRIC) ? this.fabric : this.neoforge;
		}
	}

	private static final String MODRINTH = "https://modrinth.com/mod/";
	public static final String CONNECTOR_URL = MODRINTH + "connector";
	private static final String CURSEFORGE = "https://www.curseforge.com/minecraft/mc-mods/";

	public static final List<Mod> MODS = List.of(
		new Mod("c2me", "C2ME", MODRINTH + "c2me-fabric", Category.WORLD_GENERATION,
			List.of(Build.of("C2ME", "c2me", MODRINTH + "c2me-fabric")),
			List.of(Build.of("C2ME", "c2me", MODRINTH + "c2me-neoforge")),
			null),
		new Mod("c2meOpenCl", "C2ME OpenCL", MODRINTH + "c2me-ocl", Category.WORLD_GENERATION,
			List.of(Build.of("C2ME OpenCL", "c2me-opts-accel-opencl", MODRINTH + "c2me-ocl").needsJava(25)),
			// the NeoForge build nests a mod with the id spelled with underscores
			List.of(Build.of("C2ME OpenCL", "c2me_opts_accel_opencl", MODRINTH + "c2me-ocl").needsJava(25)),
			// needs an OpenCL device, so it counts towards the light only once installed
			null, true),
		// no Noisium, Noisiumed or AllTheLeaks for 26.x; they stay listed, greyed out
		new Mod("noisium", "Noisium", MODRINTH + "noisiumed", Category.WORLD_GENERATION,
			List.of(),
			List.of(),
			null),
		new Mod("lithium", "Lithium", MODRINTH + "lithium", Category.GENERAL,
			List.of(Build.of("Lithium", "lithium", MODRINTH + "lithium")),
			List.of(Build.of("Lithium", "lithium", MODRINTH + "lithium")),
			null),
		// no ModernFix for 26.2 yet on either loader
		new Mod("modernFix", "ModernFix", MODRINTH + "modernfix", Category.GENERAL,
			List.of(),
			List.of(),
			null),
		new Mod("allTheLeaks", "AllTheLeaks", CURSEFORGE + "alltheleaks", Category.GENERAL,
			List.of(),
			List.of(),
			null),
		new Mod("sodium", "Sodium", MODRINTH + "sodium", Category.CLIENT,
			List.of(Build.of("Sodium", "sodium", MODRINTH + "sodium")),
			List.of(Build.of("Sodium", "sodium", MODRINTH + "sodium")),
			null)
	);

	public enum Light {
		// none of the mods
		RED,
		// some, but nothing that speeds up world generation
		ORANGE,
		// some of the ones there are for this game
		YELLOW,
		// all of the ones there are for this game
		GREEN
	}

	/**
	 * How one mod stands in this game.
	 *
	 * @param installed the build that's running, or null
	 * @param recommended the build to get when none is, or null if there's none for this loader and version
	 */
	public record Status(Mod mod, @Nullable Build installed, @Nullable Build recommended) {

		public boolean isAvailable() {
			return this.installed != null || this.recommended != null;
		}
	}

	public record Report(List<Status> statuses, Light light, int installed, int available) {
	}

	public static Report report() {
		return report(ModLoaderUtil.loaderName(), ModLoaderUtil::isLoaded, Runtime.version().feature());
	}

	// java: the version the game runs on; builds that need a newer one aren't recommended, nor counted as available
	public static Report report(String loader, Predicate<String> loaded, int java) {
		boolean connector = loaded.test(CONNECTOR);
		List<Status> statuses = MODS.stream().map((mod) -> {
			Build installed = null;
			Build recommended = null;
			for (Build build : mod.builds(loader)) {
				if (installed == null && build.modIds().stream().anyMatch(loaded)) {
					installed = build;
				}
				if (recommended == null && (!build.viaConnector() || connector) && java >= build.minJava()) {
					recommended = build;
				}
			}
			return new Status(mod, installed, recommended);
		}).toList();

		int installed = (int) statuses.stream().filter((status) -> status.installed() != null).count();
		int available = (int) statuses.stream().filter((status) -> status.isAvailable() && (!status.mod().optional() || status.installed() != null)).count();
		boolean worldGeneration = statuses.stream().anyMatch((status) -> status.installed() != null && status.mod().category() == Category.WORLD_GENERATION);
		Light light;
		if (installed == 0) {
			light = Light.RED;
		} else if (!worldGeneration) {
			light = Light.ORANGE;
		} else if (installed < available) {
			light = Light.YELLOW;
		} else {
			light = Light.GREEN;
		}
		return new Report(statuses, light, installed, available);
	}

	private PerformanceMods() {
	}
}
