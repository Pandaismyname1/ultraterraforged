package raccoonman.reterraforged.client.gui.createworld;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.presetconfig.RenderMode;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.data.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.biome.spawn.SpawnType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.tile.Tile;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * A map of the terrain the current settings will generate. Rendering happens on a background thread shortly after
 * the last change, so dragging a slider never blocks the screen. Scroll to zoom, drag to move.
 */
public class TerrainPreview extends AbstractWidget {
	// blocks per pixel is the zoom; the tile is always this many pixels across
	private static final int TILE_SIZE = 4;
	private static final int RESOLUTION = (1 << 4) << TILE_SIZE;
	private static final long DEBOUNCE_MS = 150;
	private static final float MIN_ZOOM = 1.0F;
	private static final float MAX_ZOOM = 400.0F;

	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(ThreadPools.daemonFactory("RTF-Preview"));
	private static final AtomicInteger GENERATION = new AtomicInteger();
	@Nullable
	private static DynamicTexture texture;
	@Nullable
	private static ResourceLocation textureId;

	private final PreviewSource state;
	private RenderMode mode = RenderMode.BIOME_TYPE;
	private float zoom = 40.0F;
	private float panX;
	private float panZ;

	private int renderedRevision = -1;
	private long renderedSeed;
	@Nullable
	private ScheduledFuture<?> scheduled;
	@Nullable
	private Rendered rendered;
	private boolean loading;

	// what's currently in the texture, for the hover readout
	private record Rendered(Tile tile, float centerX, float centerZ, float zoom) {
	}

	public TerrainPreview(PreviewSource state) {
		super(0, 0, 0, 0, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_PREVIEW));
		this.state = state;
	}

	public void setBounds(int x, int y, int size) {
		this.setX(x);
		this.setY(y);
		this.width = size;
		this.height = size;
	}

	public RenderMode mode() {
		return this.mode;
	}

	public void setMode(RenderMode mode) {
		this.mode = mode;
		this.refresh();
	}

	// schedules a new render with the current settings, replacing any that hasn't started
	public void refresh() {
		this.renderedRevision = this.state.revision();
		this.renderedSeed = this.state.seed();
		if (this.scheduled != null) {
			this.scheduled.cancel(false);
		}
		int generation = GENERATION.incrementAndGet();
		Preset preset = this.state.preset().copy();
		long seed = this.renderedSeed;
		float zoom = this.zoom;
		float panX = this.panX;
		float panZ = this.panZ;
		RenderMode mode = this.mode;
		this.loading = true;
		this.scheduled = EXECUTOR.schedule(() -> this.generate(generation, preset, seed, zoom, panX, panZ, mode), DEBOUNCE_MS, TimeUnit.MILLISECONDS);
	}

	private void generate(int generation, Preset preset, long seed, float zoom, float panX, float panZ, RenderMode mode) {
		if (generation != GENERATION.get()) {
			return;
		}
		try {
			int batchCount = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH).resultOrPartial(RTFCommon.LOGGER::error).orElseGet(PerformanceConfig::makeDefault).batchCount();
			GeneratorContext context = GeneratorContext.makeUncached(preset, (int) seed, TILE_SIZE, 0, batchCount);

			// center on the continent the player will spawn on, like the world does
			float centerX = panX;
			float centerZ = panZ;
			WorldSettings.Properties properties = preset.world().properties;
			if (properties.spawnType == SpawnType.CONTINENT_CENTER) {
				long center = context.localHeightmap.get().continent().getNearestCenter(0.0F, 0.0F);
				centerX += PosUtil.unpackLeft(center);
				centerZ += PosUtil.unpackRight(center);
			}
			Tile tile = context.generator.generateZoomed(centerX, centerZ, zoom, false).join();

			Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
			int[] pixels = new int[RESOLUTION * RESOLUTION];
			tile.iterate((cell, x, z) -> {
				if (x < RESOLUTION && z < RESOLUTION) {
					pixels[z * RESOLUTION + x] = mode.getColor(cell, levels);
				}
			});
			Rendered rendered = new Rendered(tile, centerX, centerZ, zoom);
			Minecraft.getInstance().execute(() -> {
				if (generation == GENERATION.get()) {
					upload(pixels);
					this.rendered = rendered;
					this.loading = false;
				}
			});
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("Couldn't render the terrain preview", t);
			Minecraft.getInstance().execute(() -> this.loading = false);
		}
	}

	private static void upload(int[] pixels) {
		if (texture == null) {
			texture = new DynamicTexture(new NativeImage(RESOLUTION, RESOLUTION, false));
			textureId = Minecraft.getInstance().getTextureManager().register(RTFCommon.MOD_ID + "-terrain-preview", texture);
		}
		NativeImage image = texture.getPixels();
		for (int z = 0; z < RESOLUTION; z++) {
			for (int x = 0; x < RESOLUTION; x++) {
				image.setPixelRGBA(x, z, pixels[z * RESOLUTION + x]);
			}
		}
		texture.upload();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.state.revision() != this.renderedRevision || this.state.seed() != this.renderedSeed) {
			this.refresh();
		}
		int x = this.getX();
		int y = this.getY();
		graphics.fill(x - 1, y - 1, x + this.width + 1, y + this.height + 1, 0xFF000000);
		if (this.rendered != null && textureId != null) {
			graphics.blit(textureId, x, y, 0, 0, this.width, this.height, this.width, this.height);
		} else {
			graphics.fill(x, y, x + this.width, y + this.height, 0xFF202020);
		}

		Font font = Minecraft.getInstance().font;
		if (this.loading) {
			graphics.drawString(font, Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_PREVIEW_LOADING), x + 4, y + 4, 0xFFFFFF);
		}
		if (this.rendered != null && this.isMouseOver(mouseX, mouseY)) {
			this.renderReadout(graphics, font, mouseX, mouseY);
		} else {
			// only where it fits, the editor's preview can be small
			Component hint = Component.translatable(RTFTranslationKeys.GUI_TERRAIN_TAB_PREVIEW_HINT).withStyle(ChatFormatting.GRAY);
			if (font.width(hint) <= this.width - 8) {
				graphics.drawString(font, hint, x + 4, y + this.height - 12, 0xFFFFFF);
			}
		}
	}

	private void renderReadout(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		Rendered rendered = this.rendered;
		int pixelX = Mth.clamp((int) ((mouseX - this.getX()) * (float) RESOLUTION / this.width), 0, RESOLUTION - 1);
		int pixelZ = Mth.clamp((int) ((mouseY - this.getY()) * (float) RESOLUTION / this.height), 0, RESOLUTION - 1);
		Cell cell = rendered.tile().lookup(pixelX, pixelZ);
		int blockX = Mth.floor(rendered.centerX() + (pixelX - RESOLUTION / 2.0F) * rendered.zoom());
		int blockZ = Mth.floor(rendered.centerZ() + (pixelZ - RESOLUTION / 2.0F) * rendered.zoom());
		String terrain = cell.terrain.isRiver() ? "river" : cell.terrain.getName().toLowerCase();
		String text = blockX + ", " + blockZ + "  " + terrain + "  " + cell.biomeType.name().toLowerCase();
		int y = this.getY() + this.height - 12;
		graphics.fill(this.getX(), y - 2, this.getX() + this.width, this.getY() + this.height, 0xA0000000);
		graphics.drawString(font, text, this.getX() + 4, y, 0xFFFFFF);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!this.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		this.zoom = Mth.clamp(this.zoom * (delta > 0 ? 0.8F : 1.25F), MIN_ZOOM, MAX_ZOOM);
		this.refresh();
		return true;
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		float blocksPerScreenPixel = this.zoom * RESOLUTION / (float) this.width;
		this.panX -= dragX * blocksPerScreenPixel;
		this.panZ -= dragY * blocksPerScreenPixel;
		this.refresh();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, this.getMessage());
	}
}
