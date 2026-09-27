package raccoonman.reterraforged.compat.performance;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.platform.ModLoaderUtil;

/**
 * Performance mods worth running alongside ReTerraForged, with the builds that exist for this Minecraft version on
 * each loader. When the mod itself has no build for a loader, a port or fork that does the same job stands in for
 * it. The list is data only; docs/porting/performance-mods.md tracks what exists for later Minecraft versions, and
 * this file is updated from it when porting.
 */
public final class PerformanceMods {
	public static final String MINECRAFT_VERSION = "1.20.1";
	public static final String FABRIC = "fabric";
	public static final String FORGE = "forge";
	// Sinytra Connector, which runs Fabric mods on Forge
	private static final String CONNECTOR = "connector";

	public enum Category {
		// speeds up generating new chunks: what ReTerraForged is heaviest on
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
	 * @param viaConnector whether it's a Fabric build that only runs on Forge through Sinytra Connector
	 */
	public record Build(String name, List<String> modIds, String url, boolean alternative, boolean viaConnector) {

		static Build of(String name, String modId, String url) {
			return new Build(name, List.of(modId), url, false, false);
		}

		static Build alternative(String name, String modId, String url) {
			return new Build(name, List.of(modId), url, true, false);
		}
	}

	/**
	 * @param key names the mod's description and translations
	 * @param name the mod itself
	 * @param url its page
	 * @param fabric the builds for Fabric, best first
	 * @param forge the builds for Forge and NeoForge, best first
	 * @param availableFrom for a mod with no build for this Minecraft version, the first version it has one for
	 */
	public record Mod(String key, String name, String url, Category category, List<Build> fabric, List<Build> forge, @Nullable String availableFrom) {

		public List<Build> builds(String loader) {
			return loader.equals(FABRIC) ? this.fabric : this.forge;
		}
	}

	private static final String MODRINTH = "https://modrinth.com/mod/";
	public static final String CONNECTOR_URL = MODRINTH + "connector";
	private static final String CURSEFORGE = "https://www.curseforge.com/minecraft/mc-mods/";

	public static final List<Mod> MODS = List.of(
		new Mod("c2me", "C2ME", MODRINTH + "c2me-fabric", Category.WORLD_GENERATION,
			List.of(Build.of("C2ME", "c2me", MODRINTH + "c2me-fabric")),
			// no Forge build, but the Fabric one runs through Sinytra Connector (tested with ReTerraForged)
			List.of(new Build("C2ME", List.of("c2me"), MODRINTH + "c2me-fabric", false, true)),
			null),
		new Mod("c2meOpenCl", "C2ME OpenCL", MODRINTH + "c2me-ocl", Category.WORLD_GENERATION,
			List.of(),
			List.of(),
			"1.21.1"),
		new Mod("noisium", "Noisium", MODRINTH + "noisiumed", Category.WORLD_GENERATION,
			// Noisium itself is archived; Noisiumed is the maintained fork, and either does the job
			List.of(Build.of("Noisiumed", "noisiumed", MODRINTH + "noisiumed"), Build.alternative("Noisium", "noisium", MODRINTH + "noisium")),
			List.of(Build.of("Noisiumed", "noisiumed", MODRINTH + "noisiumed"), Build.alternative("Noisium", "noisium", MODRINTH + "noisium")),
			null),
		new Mod("lithium", "Lithium", MODRINTH + "lithium", Category.GENERAL,
			List.of(Build.of("Lithium", "lithium", MODRINTH + "lithium")),
			List.of(Build.alternative("Radium", "radium", MODRINTH + "radium"), Build.alternative("Canary", "canary", MODRINTH + "canary")),
			null),
		new Mod("modernFix", "ModernFix", MODRINTH + "modernfix", Category.GENERAL,
			List.of(Build.of("ModernFix", "modernfix", MODRINTH + "modernfix")),
			List.of(Build.of("ModernFix", "modernfix", MODRINTH + "modernfix")),
			null),
		new Mod("allTheLeaks", "AllTheLeaks", CURSEFORGE + "alltheleaks", Category.GENERAL,
			List.of(),
			List.of(Build.of("AllTheLeaks", "alltheleaks", CURSEFORGE + "alltheleaks")),
			null),
		new Mod("sodium", "Sodium", MODRINTH + "sodium", Category.CLIENT,
			List.of(Build.of("Sodium", "sodium", MODRINTH + "sodium")),
			List.of(Build.alternative("Embeddium", "embeddium", MODRINTH + "embeddium")),
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
		return report(ModLoaderUtil.loaderName(), ModLoaderUtil::isLoaded);
	}

	public static Report report(String loader, Predicate<String> loaded) {
		boolean connector = loaded.test(CONNECTOR);
		List<Status> statuses = MODS.stream().map((mod) -> {
			Build installed = null;
			Build recommended = null;
			for (Build build : mod.builds(loader)) {
				if (installed == null && build.modIds().stream().anyMatch(loaded)) {
					installed = build;
				}
				if (recommended == null && (!build.viaConnector() || connector)) {
					recommended = build;
				}
			}
			return new Status(mod, installed, recommended);
		}).toList();

		int installed = (int) statuses.stream().filter((status) -> status.installed() != null).count();
		int available = (int) statuses.stream().filter(Status::isAvailable).count();
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
