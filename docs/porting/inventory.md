# ReTerraForged: Minecraft and loader API inventory (baseline MC 1.20.1)

Baseline: commit `036d949` on the 1.20.1 line. The build uses Architectury Loom 1.17.493 with Mojang mappings, Java release 17, Fabric Loader 0.19.5, Fabric API 0.92.12+1.20.1, Forge 47.4.23 (the minimum loader is 47.1, which also covers NeoForge 47.1.x), and TerraBlender 3.0.1.2 as a compile-only dependency. Line numbers are as of that commit.

Paths are relative to the repo root. `C/` means `common/src/main/java/raccoonman/reterraforged/`, `F/` means `fabric/src/main/java/raccoonman/reterraforged/`, and `FG/` means `forge/src/main/java/raccoonman/reterraforged/`. Line numbers are the real file lines.

The mod has no custom ChunkGenerator, BiomeSource, Structure, StructureType, StructurePlacement(Type), network packet or custom ArgumentType. All terrain goes through **vanilla `NoiseBasedChunkGenerator` + `MultiNoiseBiomeSource`**, driven by a datapack that RTF generates. That datapack overrides `noise_settings/overworld`, density functions, `dimension_type/overworld` and similar entries. On top of that, mixins into RandomState, NoiseChunk, NoiseBasedChunkGenerator, ChunkStatus and SurfaceSystem swap in RTF's heightmap engine.

---

## 1. Mixins

The config files are:
- `common/src/main/resources/reterraforged-common.mixins.json`: plugin `raccoonman.reterraforged.mixin.plugin.MixinPlugin`, `compatibilityLevel JAVA_17`, `minVersion 0.8`, `injectors.defaultRequire: 1`.
  - The `mixins` list is MixinNoiseChunk, MixinRandomState, MixinChunkMap, MixinUtil, MixinBiomeGenerationSettings, MixinSurfaceSystem, MixinStructure, MixinMinecraftServer, MixinSpawnFinder, MixinClimateSampler, MixinChunkStatus, MixinServerPacksSource, MixinRegistryDataLoader, MixinNoiseBasedChunkGenerator, MixinContext, MixinSurfaceRules$BiomeConditionSource, terrablender.MixinParameterList, terrablender.MixinClimateSampler, terrablender.MixinTargetPoint, terrablender.MixinNoiseChunk and worldpreview.SampleUtilsMixin.
  - The `client` list is ScreenInvoker and MixinCreateWorldScreen.
- `fabric/src/main/resources/reterraforged-fabric.mixins.json` lists `mixins` MixinMinecraftServer, MixinBiomeModificationImpl and MixinFabricDataGenerator$Pack, plus `client` MixinPresetEditor. It has no plugin.
- `forge/src/main/resources/reterraforged-forge.mixins.json` lists `mixins` MixinTagsProvider, MixinBiomeGenerationSettingsPlainsBuilder, MixinMinecraftServer and MixinServerLifecycleHooks. It is registered in `forge/build.gradle` through `mixinConfig`.

### MixinPlugin (`C/mixin/plugin/MixinPlugin.java`)
- `TB_MIXINS` = terrablender.{MixinClimateSampler, MixinNoiseChunk, MixinParameterList, MixinTargetPoint}. These apply only if `TBCompat.isEnabled()`, which is `ModLoaderUtil.isLoaded("terrablender")` (the `TerraBlender.MOD_ID` constant).
- `WP_MIXINS` = worldpreview.SampleUtilsMixin. It applies only if `WPCompat.isEnabled()`, which is `isLoaded("world_preview")`.
- Every other mixin always applies (`shouldApplyMixin` at lines 34-37).
- `getMixins()` also returns the TB and WP mixins, although they are already listed in the JSON, so each is declared twice. Worth checking whether a newer Mixin rejects that.
- `onLoad` calls `ModLoaderUtil` (an `@ExpectPlatform` method) during mixin bootstrap, so the loader's mod list has to be queryable before any mixin runs.

### Common mixins

| Mixin (file:line) | Target | Injectors (method / descriptor / @At) | Purpose |
|---|---|---|---|
| `C/mixin/MixinBiomeGenerationSettings.java:16` (interface, `@Deprecated`) | `BiomeGenerationSettings` | `@Accessor getFeatures()` gives `List<HolderSet<PlacedFeature>> features` (l.18). `@Accessor setFlowerFeatures(Supplier<List<ConfiguredFeature<?,?>>>)` (l.21) sets the private final `flowerFeatures` field. | Used only by the Fabric biome modifiers (`F/world/worldgen/biome/modifier/fabric/FabricBiomeModifier.java:20-25`) to rebuild the flower list after they mutate the features. |
| `C/mixin/MixinChunkMap.java:28` | `ChunkMap` | `@Inject <init>` TAIL (l.33). The handler signature is `(ServerLevel, LevelStorageSource.LevelStorageAccess, DataFixer, StructureTemplateManager, Executor, BlockableEventLoop<Runnable>, LightChunkGetter, ChunkGenerator, ChunkProgressListener, ChunkStatusUpdateListener, Supplier<DimensionDataStorage>, int, boolean)`. `@Shadow randomState`. | Initializes `RTFRandomState` with `serverLevel.registryAccess()` (this loads the preset and builds the GeneratorContext) and sets `WorldGenFlags.setCullNoiseSections(true)`. **The full constructor signature is pinned.** |
| `C/mixin/MixinChunkStatus.java:31` | `ChunkStatus` | `@Inject` HEAD (l.35) and TAIL (l.55) on `{"method_39464","m_289181_"}` with `remap=false`. This is the STRUCTURE_STARTS generation lambda, signature `static (ChunkStatus, Executor, ServerLevel, ChunkGenerator, StructureTemplateManager, ThreadedLevelLightEngine, Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>>, List<ChunkAccess>, ChunkAccess) -> CompletableFuture<ChunkAccess>`. A second `@Inject` TAIL (l.72) on `{"method_51375","m_279978_"}` with `remap=false` targets the FEATURES simple lambda `static (ChunkStatus, ServerLevel, ChunkGenerator, List<ChunkAccess>, ChunkAccess) -> void`. | STRUCTURE_STARTS HEAD calls `cache.queueAtChunk` and turns fast cell lookups off; TAIL turns them back on. FEATURES TAIL calls `cache.dropAtChunk`. **FRAGILE: these are hard-coded intermediary and SRG lambda names.** ChunkStatus was reworked into ChunkStep/ChunkPyramid in 1.20.5+, so these targets disappear. |
| `C/mixin/MixinClimateSampler.java:11` | `Climate.Sampler` (a record) | `@Implements(RTFClimateSampler, prefix "reterraforged$RTFClimateSampler$")` and adds the field `spawnSearchCenter`. | Stores the spawn search center that `MixinMinecraftServer` sets. |
| `C/mixin/MixinContext.java:27` | `SurfaceRules.Context` | `@Implements(RTFSurfaceContext)`. `@Inject <init>` TAIL (l.36), with no parameters captured. `@Shadow @Final chunk`. | Gathers the biome keys of the surrounding 3x3 chunks from `SurfaceRegion` (a ThreadLocal `WorldGenRegion`), using `LevelChunkSection.getBiomes().getAll(...)`. |
| `C/mixin/MixinCreateWorldScreen.java:28` (client) | `CreateWorldScreen`. The mixin extends `Screen` and implements `TerrainState.Holder` | `@Shadow @Final WorldCreationUiState uiState`, `@Shadow boolean recreated`, `@Shadow onCreate()`.<br>1) `@Inject init` HEAD (l.54).<br>2) `@ModifyArg init`, `@At INVOKE "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"` (l.70).<br>3) `@Inject onCreate` HEAD, cancellable (l.84).<br>4) `@Inject tick` TAIL (l.100). | (1) selects the RTF world type by default and handles the dev creative flag. (2) appends `TerrainTab`. (3) builds and applies the preset datapack before the world is created (it may cancel and wait for the datapack reload). (4) calls `onCreate()` again via `minecraft.tell` once the packs have loaded. Also uses `SystemToast.addOrUpdate(..., SystemToastIds.PACK_LOAD_FAILURE, ...)` (l.95). |
| `C/mixin/MixinMinecraftServer.java:19` | `MinecraftServer` | `@Inject setInitialSpawn` (static, `(ServerLevel, ServerLevelData, boolean, boolean)V`), `@At INVOKE "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"` (l.22). | Sets the sampler's spawn search center from `preset.world().properties.spawnType`. It reads `registryAccess().lookup(RTFRegistries.PRESET)`. |
| `C/mixin/MixinNoiseBasedChunkGenerator.java:40` (priority 9001, "don't break noisium") | `NoiseBasedChunkGenerator` | `@Shadow @Final Holder<NoiseGeneratorSettings> settings`.<br>`@Inject buildSurface` HEAD and TAIL (l.47, l.52) with handler `(WorldGenRegion, StructureManager, RandomState, ChunkAccess)`.<br>`@Redirect {"fillFromNoise","populateNoise"}` (l.57), `@At INVOKE "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"`, handler `(NoiseSettings, Executor, Blender, RandomState, StructureManager, ChunkAccess)`.<br>`@Redirect {"iterateNoiseColumn","sampleHeightmap"}`, `require=2` (l.74), same `NoiseSettings.height()` target, handler `(NoiseSettings, LevelHeightAccessor, RandomState, int, int, MutableObject<NoiseColumn>, Predicate<BlockState>)`.<br>`@Inject addDebugScreenInfo` TAIL (l.91) with `(List<String>, RandomState, BlockPos)`. | Sets and clears the ThreadLocal `SurfaceRegion`. Caps the generation height at the RTF-computed column height (a perf cull). Adds F3 debug lines. `populateNoise` and `sampleHeightmap` are Yarn-style alias names that do not exist under Mojmap; the arrays rely on the Mojmap names matching. |
| `C/mixin/MixinNoiseChunk.java:33` | `NoiseChunk` | `@Shadow`: `initialDensityNoJaggedness`, `firstNoiseX`, `firstNoiseZ`, `cellCountXZ`, `cellCountY` (made mutable by the AW), `cellHeight`.<br>`@Redirect <init>` (l.65), `@At INVOKE "Lnet/minecraft/world/level/levelgen/RandomState;router()Lnet/minecraft/world/level/levelgen/NoiseRouter;"`. The handler captures the ctor args `(int cellCountXZ, RandomState, int minBlockX, int minBlockZ, NoiseSettings, DensityFunctions.BeardifierOrMarker, NoiseGeneratorSettings)`.<br>`@ModifyVariable <init>` HEAD, `name="fluidPicker"`, `index=7`, `ordinal=0`, `argsOnly` (l.100).<br>`@Inject wrapNew` HEAD, cancellable (l.131), with `(DensityFunction)DensityFunction`.<br>`@Redirect computePreliminarySurfaceLevel` (l.141), target `NoiseSettings.height()I`, handler `(NoiseSettings, long)`. | Computes the generation height and shrinks `cellCountY`. Supplies the RTF lava-level fluid picker. Replaces `CellSampler` density functions with the chunk-cached `CacheChunk`. Caps the preliminary surface level. **Pinned to the 1.20.1 NoiseChunk ctor layout and the local index 7.** |
| `C/mixin/MixinRandomState.java:43` | `RandomState` | `@Implements(RTFRandomState)`. `@Shadow @Final Climate.Sampler sampler` and `SurfaceSystem surfaceSystem`. `@Redirect <init>` (l.69), `@At INVOKE "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"`, handler `(NoiseRouter, DensityFunction.Visitor, NoiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters>, long seed)`. | Wraps the vanilla visitor so that the `NoiseSampler.Marker` and `CellSampler.Marker` density functions become seeded samplers. `initialize(RegistryAccess)` (l.~101) reads the `RTFRegistries.PRESET` and `Registries.DENSITY_FUNCTION` lookups, wires TerraBlender's uniqueness function and builds the `GeneratorContext`. It overrides `DensityFunction.Visitor.apply` and `visitNoise(NoiseHolder)`. |
| `C/mixin/MixinRegistryDataLoader.java:11` | `RegistryDataLoader` | `@Redirect loadRegistryContents` (static), `@At INVOKE "Lnet/minecraft/server/packs/resources/Resource;isBuiltin()Z"`, `require=1` (l.19). | Makes RTF preset packs (`file/reterraforged-preset*.zip`) count as built in, which avoids the experimental worldgen warning. Uses `Resource.sourcePackId()`. |
| `C/mixin/MixinServerPacksSource.java:21` | `ServerPacksSource` | `@Inject "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;"` HEAD (l.25).<br>`@Redirect` on the same method, `@At NEW "Lnet/minecraft/server/packs/repository/PackRepository;"`, `require=1` (l.35), handler `(RepositorySource[], Path)`. | On a dedicated server, installs the preset datapack for a new world (`ServerPresets.installIfNewWorld`). Adds `RTFBuiltinPackSource` to the pack repository. |
| `C/mixin/MixinSpawnFinder.java:19` | `Climate` (the comment notes that targeting `Climate$SpawnFinder` did not work) | `@Inject findSpawnPosition` HEAD, cancellable (l.22), `static (List<ParameterPoint>, Sampler) -> BlockPos`. | **Replaces vanilla spawn finding for every world** with `SpawnFinderFix`, which searches around `RTFClimateSampler.getSpawnSearchCenter()` (default 0,0). Uses the AW'd `ParameterPoint.fitness`. |
| `C/mixin/MixinStructure.java:17` | `Structure` | `@Inject isValidBiome` HEAD, cancellable (l.20), `static (Structure.GenerationStub, Structure.GenerationContext) -> boolean`. | Vetoes structure starts with RTF `StructureRule`s from the `RTFRegistries.STRUCTURE_RULE` datapack registry. |
| `C/mixin/MixinSurfaceRules$BiomeConditionSource.java:18` | string target `net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource` (a private record) | `@Shadow @Final Predicate<ResourceKey<Biome>> biomeNameTest`. `@Inject apply` HEAD, cancellable (l.24), `(SurfaceRules.Context) -> SurfaceRules.Condition`. | Short-circuits biome conditions using the chunk's surrounding-biome set from `MixinContext`. |
| `C/mixin/MixinSurfaceSystem.java:29` | `SurfaceSystem` | `@Implements(RTFSurfaceSystem, prefix "reterraforged$RTFSurfaceSystem$")`.<br>`@Inject <init>` TAIL (l.36), handler `(RandomState, BlockState, int, PositionalRandomFactory)`.<br>`@ModifyVariable buildSurface` HEAD, `name="ruleSource"`, `index=7`, `ordinal=0`, `argsOnly` (l.45). | Creates a strata RNG from `randomState.random.fromHashOf(...)`, where `random` is AW'd. Adds a strata cache. The `@ModifyVariable` is currently a no-op pass-through (its TerraBlender code is commented out). **Pinned to the buildSurface parameter index 7.** |
| `C/mixin/MixinUtil.java:15` | `Util` | `@Inject "shutdownExecutors()V"` TAIL (l.18). `@Shadow static shutdownExecutor(ExecutorService)`. | Shuts down the `ThreadPools.WORLD_GEN` and `Cache.SCHEDULER` pools. |
| `C/mixin/ScreenInvoker.java:13` (client, `@Deprecated`) | `Screen` | `@Invoker("addRenderableWidget")` gives `<T extends GuiEventListener & Renderable> T invokeAddRenderableWidget(T)` (l.16). | Used by `C/client/gui/ColumnAlignment.java:31` and `C/client/gui/screen/presetconfig/OptionPage.java:162`. |

### TerraBlender mixins (conditional on the plugin)

| Mixin | Target | Injectors | Purpose |
|---|---|---|---|
| `C/mixin/terrablender/MixinClimateSampler.java:18` | `Climate.Sampler` | `@Implements(TBClimateSampler)`. `@Inject sample` RETURN with **`locals = LocalCapture.CAPTURE_FAILHARD`** (l.24). The handler captures locals `(int i,int j,int k, CIR<TargetPoint>, int l,int m,int n, DensityFunction.SinglePointContext ctx)`. | Stores the TB "uniqueness" value on the returned TargetPoint. **FRAGILE: depends on the exact local-variable layout of `Climate.Sampler.sample(III)`.** |
| `C/mixin/terrablender/MixinNoiseChunk.java:24` | `NoiseChunk` | `@Inject <init>` TAIL (l.28), handler `(int, RandomState, int, int, NoiseSettings, DensityFunctions.BeardifierOrMarker, NoiseGeneratorSettings, Aquifer.FluidPicker, Blender)`. `@Inject cachedClimateSampler` RETURN (l.36), `(NoiseRouter, List<Climate.ParameterPoint>) -> Climate.Sampler`. `@Shadow wrap(DensityFunction)`. | Copies the uniqueness function to the chunk-cached sampler. **Pinned to the full ctor signature.** |
| `C/mixin/terrablender/MixinParameterList.java:17` (priority 1001) | `Climate.ParameterList` | `@Inject initializeForTerraBlender` HEAD (l.24), `(RegistryAccess, RegionType, long)`. This method is added by TerraBlender's own mixin. `@Redirect findValuePositional` (l.41), `@At INVOKE "Lnet/minecraft/world/level/biome/Climate$ParameterList;getUniqueness(III)I"` (also added by TB). `@Shadow getUniqueness(III)`. | Replaces TB's region index with RTF's `CellField.BIOME_REGION` uniqueness. Uses `terrablender.api.Regions.getCount(RegionType)`. **Depends on TB's internal method names.** |
| `C/mixin/terrablender/MixinTargetPoint.java:10` | `Climate.TargetPoint` (a record) | `@Implements(TBTargetPoint)` and adds the field `uniqueness`. | Carries the uniqueness value through. |

### World Preview mixin
- `C/mixin/worldpreview/SampleUtilsMixin.java:29` is `@Pseudo @Mixin(targets="caeruleusTait.world.preview.backend.worker.SampleUtils", remap=false)`.
  - It shadows `randomState` and `registryAccess`.
  - `@Inject` TAIL hits two constructors through `@Desc`: `<init>(MinecraftServer, BiomeSource, ChunkGenerator, WorldOptions, LevelStem, LevelHeightAccessor)` (l.39) and `<init>(BiomeSource, ChunkGenerator, LayeredRegistryAccess, WorldOptions, LevelStem, LevelHeightAccessor, WorldDataConfiguration, Proxy, Path)` (l.53).
  - It calls `RTFRandomState.initialize` with cull disabled.
  - **Pinned to World Preview's internal constructor signatures.**

### Fabric mixins

| Mixin | Target | Injectors | Purpose |
|---|---|---|---|
| `F/fabric/mixin/MixinBiomeModificationImpl.java:28` | `net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl` (**Fabric API internal `impl` class**) | `@Shadow(remap=false) static Comparator<Object> MODIFIER_ORDER_COMPARATOR` and `getSortedModifiers()`. `@Redirect finalizeWorldGen` with `remap=false`, `@At INVOKE target "getSortedModifiers"` (l.36). | Injects RTF biome modifiers from the `RTFRegistries.BIOME_MODIFIER` datapack registry as POST_PROCESSING modifier records. It builds `BiomeModificationImpl$ModifierRecord` **by reflection** with the ctor `(ModificationPhase, ResourceLocation, Predicate, BiConsumer)` (l.74). **Very fragile.** |
| `F/fabric/mixin/MixinFabricDataGenerator$Pack.java:9` | `FabricDataGenerator.Pack` | `@Invoker(value="<init>", remap=false)` gives `invokeNew(FabricDataGenerator, boolean, String, FabricDataOutput)` (l.12). | **Unused**: nothing calls `invokeNew`. |
| `F/fabric/mixin/MixinMinecraftServer.java:17` | `MinecraftServer` | `@Implements(RTFMinecraftServer)`. `@Inject <init>` TAIL (l.21), no args. `@Inject "method_29440"` TAIL (l.33). `@Shadow getResourceManager()`. | Creates `FeatureTemplateManager` and reloads it after `/reload`. **FRAGILE: `method_29440` is an intermediary lambda name**, the `thenAcceptAsync` lambda inside `reloadResources`. |
| `F/fabric/mixin/MixinPresetEditor.java:16` (client, `@Deprecated`) | `PresetEditor` (interface) | `@Redirect <clinit>`, `remap=false`, `@At INVOKE "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"` (l.20). | Adds the RTF world preset's edit button (`PresetConfigScreen`) to `PresetEditor.EDITORS`. **FRAGILE: depends on vanilla building the map with a 2-entry `Map.of`.** |

### Forge mixins

| Mixin | Target | Injectors | Purpose |
|---|---|---|---|
| `FG/forge/mixin/MixinBiomeGenerationSettingsPlainsBuilder.java:12` | `BiomeGenerationSettings.PlainBuilder` | `@Accessor getFeatures()` gives `List<List<Holder<PlacedFeature>>>` (l.14). | Used by the Forge `AddModifier` and `ReplaceModifier`. |
| `FG/forge/mixin/MixinMinecraftServer.java:26` | `MinecraftServer` | `@Implements(RTFMinecraftServer)`. `@Inject <init>` with the explicit descriptor `(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V`, TAIL (l.30). `@Inject "lambda$reloadResources$26"`, `remap=false`, TAIL (l.42). | Same purpose as the Fabric version. **FRAGILE: javac lambda index `$26` (Forge 47.4 / NeoForge 47.1).** |
| `FG/forge/mixin/MixinServerLifecycleHooks.java:16` | `net.minecraftforge.server.ServerLifecycleHooks` | `@ModifyVariable runModifiers`, `remap=false`, `@At(INVOKE "Ljava/util/stream/Stream;toList()Ljava/util/List;", shift AFTER)`, `index=2`, `name="biomeModifiers"` (l.19). | Appends RTF biome modifiers to Forge's biome-modifier list. **Depends on Forge internals.** |
| `FG/forge/mixin/MixinTagsProvider.java:14` | `TagsProvider` | `@Redirect "lambda$getOrCreateRawBuilder$9"`, `remap=false`, `@At INVOKE "Lnet/minecraftforge/common/data/ExistingFileHelper;trackGenerated(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraftforge/common/data/ExistingFileHelper$IResourceType;)V"` (l.17). | Null-guards `ExistingFileHelper`, because RTF runs tag providers at runtime without one. **FRAGILE: Forge-patched lambda name.** |

### Fragile and synthetic targets (summary)
- `MixinChunkStatus` targets `method_39464` / `m_289181_` and `method_51375` / `m_279978_`.
- The Fabric `MixinMinecraftServer` targets `method_29440`.
- The Forge `MixinMinecraftServer` targets `lambda$reloadResources$26`.
- `MixinTagsProvider` targets `lambda$getOrCreateRawBuilder$9`.
- `MixinPresetEditor` redirects `Map.of` in `<clinit>`.
- The TB `MixinClimateSampler` uses `LocalCapture.CAPTURE_FAILHARD`.
- `MixinBiomeModificationImpl` targets a Fabric `impl` class and a reflected record ctor.
- `MixinNoiseChunk` and `MixinSurfaceSystem` hard-code `@ModifyVariable index=7`.
- The `<init>` injections with full ctor signatures are MixinChunkMap, the TB MixinNoiseChunk, the Forge MixinMinecraftServer and the World Preview SampleUtils mixin.
- MixinSurfaceRules$BiomeConditionSource uses a string target for a private record.
- MixinServerLifecycleHooks and MixinBiomeModificationImpl depend on loader internals.

---

## 2. Access widener (`common/src/main/resources/reterraforged.accesswidener`, v2 named)

The widener is wired through `architectury.common.json`, `common/build.gradle:6`, the Fabric `remapJar.injectAccessWidener`, and the Forge `convertAccessWideners`/`extraAccessWideners` (`forge/build.gradle:21-22`). "Used by" lists non-mixin usages. **Unused** means no usage was found in main code.

| Group (AW lines) | Entries | Used by |
|---|---|---|
| SurfaceRules classes (3-8) | `accessible class` Condition, Context, SurfaceRule, LazyCondition, LazyYCondition, LazyXZCondition | Condition and Context are used by all condition and rule classes (`C/world/worldgen/surface/**`, `C/compat/terrablender/TBSurfaceRules.java`); 17 files import `SurfaceRules.Context`. SurfaceRule: `NoiseRule.java:42`, `StrataRule.java:112`, `TBSurfaceRules.java`. LazyXZCondition: `CellCondition.java:14`, `NoiseCondition.java:12`. LazyCondition is used indirectly as the superclass behind `compute()`. **LazyYCondition is unused.** |
| SurfaceRules.Context members (9-22, 74-76) | ctor `<init>(SurfaceSystem, RandomState, ChunkAccess, NoiseChunk, Function, Registry, WorldGenerationContext)`, `updateXZ(II)`, fields noiseChunk, randomState, chunk, context, system, blockX/Y/Z, lastUpdateXZ, pos, biomeGetter (plus `mutable`), biome, stoneDepthBelow, stoneDepthAbove | randomState: `BiomeTagCondition.java:24`, `CellCondition.java:26`, `LayeredSurfaceRule.java:22`, `StrataRule.java:68`. chunk: `CellCondition.java:27`, `HeightModificationDetection.java:49`, `StrataRule.java:71-72`. system: `StrataRule.java:68`. blockX/Z: `CellCondition.java:37-38`, `NoiseCondition.java:25`. blockY: `HeightModificationDetection.java:49`. **Unused: the ctor, updateXZ, noiseChunk, context, lastUpdateXZ, pos, biomeGetter (and its mutable), biome, stoneDepthBelow/Above.** |
| SurfaceSystem (23) | `seaLevel` | **Unused.** |
| NoiseChunk (24-26) | `initialDensityNoJaggedness`, `mutable cellCountY`, `wrap(DensityFunction)` | Only `@Shadow`ed in `MixinNoiseChunk` and `terrablender/MixinNoiseChunk`. `cellCountY` needs `mutable` because the mixin writes it. |
| DensityFunctions (27, 31-33) | `MAX_REASONABLE_NOISE_VALUE`, classes `WeirdScaledSampler`, `BeardifierMarker`, `WeirdScaledSampler$RarityValueMapper` | RarityValueMapper: `C/data/preset/PresetNoiseRouterData.java:97`. **Unused: MAX_REASONABLE_NOISE_VALUE (`Noises` defines its own constant) and BeardifierMarker.** |
| OreVeinifier (28-30) | class `VeinType`, fields `minY` and `maxY` | `PresetNoiseRouterData.java:71-72` |
| NoiseRouterData (49-64) | fields SPAGHETTI_2D, SPAGHETTI_ROUGHNESS_FUNCTION, PILLARS, SLOPED_CHEESE, NOODLE, Y, BASE_3D_NOISE_OVERWORLD, ENTRANCES, SPAGHETTI_2D_THICKNESS_MODULATOR; methods underground, yLimitedInterpolatable, entrances, postProcess, noiseGradientDensity, getFunction, registerAndWrap(**BootstapContext**, ...) | `PresetNoiseRouterData.java:41-113` (about 30 sites; it is a re-implementation of the vanilla overworld router) and `data/preset/settings/Preset.java:80-81` |
| Client GUI (34-40, 65) | `AbstractWidget.height`, `AbstractSelectionList.replaceEntries(Collection)`, `Screen.minecraft`, `Screen.font`, `Screen.rebuildWidgets()`, `CreateWorldScreen.getDataPackSelectionSettings(WorldDataConfiguration)Pair`, `CreateWorldScreen.tryApplyNewDataPacks(PackRepository, boolean, Consumer)`, `CreateWorldScreen.tabNavigationBar` | height writes: `ScrollingPanel.java:45,64`, `TerrainPreview.java:84`, `TerrainTab.java:125,151`, `OptionPage.java:148`, `WidgetList.java:77`. replaceEntries: `OptionPage.java:189`, `PresetListPage.java:195`. Screen.minecraft: `BisectedPage.java:28`, `OptionPage.java:109,136`, `PresetConfigScreen.java:35`, `SavePresetScreen.java:108`. Screen.font: `OptionPage.java:100`, `PresetListPage.java:82`, `SavePresetScreen.java:47,101-102`. rebuildWidgets: `LinkedPageScreen.java:26`. getDataPackSelectionSettings: `PresetApplier.java:38`. tryApplyNewDataPacks: `PresetApplier.java:63`. tabNavigationBar: only `fabric/src/clienttest/.../ClientTest.java:83`. |
| Datagen (41-42) | `DataGenerator.vanillaPackOutput`, `DataGenerator$PackGenerator.<init>(DataGenerator, boolean, String, PackOutput)` | PackGenerator ctor: `C/data/RTFDataGen.java:36` and `common/src/test/.../PresetBlockTagsTest.java:28`. **vanillaPackOutput is unused.** |
| StructurePlacement (43, 67-71) | `salt()`, fields locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone | Fields: `C/data/preset/PresetStructureSets.java:56-58,66,72`. **`salt()` is unused.** |
| Biome (44) | `getTemperature(BlockPos)` | **Unused** (only commented out at `ErodeSnowFeature.java:78`). |
| StructureManager (45) | `level` | **Unused.** |
| WorldGenRegion (46-48) | firstPos, lastPos, cache | **Unused.** |
| Climate (66) | `ParameterPoint.fitness(TargetPoint)` | `C/world/worldgen/biome/spawn/SpawnFinderFix.java:36,55` |
| Commands (72) | `ArgumentTypeInfos.register(Registry, String, Class, ArgumentTypeInfo)` | **Unused.** No custom argument types exist. |
| RandomState (73) | `random` (PositionalRandomFactory) | `C/mixin/MixinSurfaceSystem.java:41` |
| Beardifier (77-78) | `pieceIterator`, `junctionIterator` | **Unused** (only in commented code at `MixinNoiseChunk.java:81`). |

---

## 3. @ExpectPlatform methods

| Common declaration | Fabric impl | Forge impl |
|---|---|---|
| `C/platform/ConfigUtil.java:22` `static Path getConfigPath()` | `F/platform/fabric/ConfigUtilImpl.java` uses `FabricLoader.getInstance().getConfigDir()` | `FG/platform/forge/ConfigUtilImpl.java` uses `FMLPaths.CONFIGDIR.get()` |
| `C/platform/DataGenUtil.java:12` `static DataProvider createRegistryProvider(PackOutput, CompletableFuture<HolderLookup.Provider>)` | `F/platform/fabric/DataGenUtilImpl.java`: a custom `DataProvider` that ports Forge's `RegistriesDatapackGenerator` patch. It iterates `DynamicRegistries.getDynamicRegistries()` (Fabric API `fabric-registry-sync`) as `RegistryDataLoader.RegistryData`, uses `RegistryOps.create(JsonOps, provider)`, `PackOutput.createPathProvider(DATA_PACK, ns-prefixed path)`, `Encoder.encodeStart(...).resultOrPartial` and `DataProvider.saveStable`. Non-minecraft registries go under `<ns>/<path>`. | `FG/platform/forge/DataGenUtilImpl.java`: `new RegistriesDatapackGenerator(output, lookup)`, which Forge patches to include modded datapack registries. |
| `C/platform/ModLoaderUtil.java:7` `static boolean isLoaded(String)` | `FabricLoader.isModLoaded` | `FMLLoader.getLoadingModList().getModFileById(id) != null`. This works before mod construction, which the MixinPlugin needs. |
| `C/platform/ModLoaderUtil.java:12` `static boolean isDedicatedServer()` | `FabricLoader.getEnvironmentType() == EnvType.SERVER` | `FMLLoader.getDist().isDedicatedServer()` |
| `C/platform/RegistryUtil.java:20` `static Registry<BiomeModifier> getBiomeModifierRegistry()` | **No impl** | **No impl** (dead declaration, never called) |
| `C/platform/RegistryUtil.java:25` `static <T> WritableRegistry<T> getWritable(Registry<T>)` | Casts the vanilla registry to `WritableRegistry`. This relies on vanilla built-in registries being unfrozen during mod init, which Fabric allows. | Wraps a `DeferredRegister.create(key, MOD_ID)` in `FG/platform/forge/DeferredRegistry.Writable`, a hand-written `Registry<T>`/`WritableRegistry<T>` delegate over `GameData.getWrapper(key, Lifecycle.stable())`. `register()` creates a stand-alone `Holder.Reference` and calls `DeferredRegister.register(path, supplier)`. It overrides `registerMapping` (1.20.1 only) and about 35 `Registry` methods. |
| `C/platform/RegistryUtil.java:30` `static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>>)` | `FabricRegistryBuilder.createSimple(key).buildAndRegister()` | `DeferredRegister.create(...)` with `makeRegistry(() -> new RegistryBuilder().hasTags())`, returned as a memoized `DeferredRegistry` over `GameData.getWrapper`. |
| `C/platform/RegistryUtil.java:35` `static <T> void createDataRegistry(ResourceKey, Codec<T>)` | `DynamicRegistries.register(key, codec)`. Not synced to the client (no network codec). | Queued, then registered in `DataPackRegistryEvent.NewRegistry` → `event.dataPackRegistry(key, codec)` (`FG/platform/forge/RegistryUtilImpl.java:36-40`). Not synced. |
| `C/server/commands/RTFCommands.java:17` `static void register(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext>)` | `F/server/commands/fabric/RTFCommandsImpl.java`: `CommandRegistrationCallback.EVENT` (fabric-command-api-v2) | `FG/server/commands/forge/RTFCommandsImpl.java`: `@EventBusSubscriber` + `@SubscribeEvent RegisterCommandsEvent` |
| `C/world/worldgen/biome/modifier/BiomeModifiers.java:21` `static void bootstrap()` | `F/world/worldgen/biome/modifier/fabric/BiomeModifiersImpl.java`: registers `add`/`replace` codecs into `RTFBuiltInRegistries.BIOME_MODIFIER_TYPE` | Same, in `FG/.../forge/BiomeModifiersImpl.java` |
| `BiomeModifiers.java:44` `static BiomeModifier add(Order, GenerationStep.Decoration, Optional<Pair<Filter.Behavior, HolderSet<Biome>>>, HolderSet<PlacedFeature>)` | `fabric/AddModifier` implements `FabricBiomeModifier`. It uses `BiomeSelectionContext`/`BiomeModificationContext` (fabric-biome-api-v1), directly mutates `BiomeGenerationSettings.features()` and rebuilds flower features through the accessor mixin. Applied by `MixinBiomeModificationImpl`, not through the public `BiomeModifications` API. | `forge/AddModifier` implements `ForgeBiomeModifier`, which extends `net.minecraftforge.common.world.BiomeModifier`. It implements `modify(Holder<Biome>, Phase, ModifiableBiomeInfo.BiomeInfo.Builder)` in `Phase.AFTER_EVERYTHING` and mutates `PlainBuilder.features` through the accessor. Applied by `MixinServerLifecycleHooks`; Forge's `ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS` is not used. |
| `BiomeModifiers.java:57` `static BiomeModifier replace(GenerationStep.Decoration, Optional<HolderSet<Biome>>, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>>)` | `fabric/ReplaceModifier` (same approach) | `forge/ReplaceModifier` (same approach) |

Loader entrypoints:
- Fabric: `F/fabric/RTFFabric.java` implements `ModInitializer.onInitialize()`, which runs `RTFCommon.bootstrap()`. It also implements `DataGeneratorEntrypoint.onInitializeDataGenerator`, but `fabric.mod.json` declares **only** `main`, not the `fabric-datagen` entrypoint.
- Forge: `FG/forge/RTFForge.java` is `@Mod`. Its ctor runs `RTFCommon.bootstrap()`, `RegistryUtilImpl.register(modBus)`, a `GatherDataEvent` listener (`PackMetadataGenerator.forFeaturePack` plus the lang provider) and, on the client dist, `RegisterPresetEditorsEvent` (`FG/forge/RTFForgeClient.java`) through `FMLJavaModLoadingContext.get().getModEventBus()`. `FMLEnvironment.dist` is used as well.

---

## 4. Registrations

All vanilla registry writes go through `RegistryUtil.register(registry, name, value)`, which calls `getWritable(registry).register(ResourceKey, value, Lifecycle.stable())`. There are **no direct `Registry.register(` calls**.

### Custom static ("built-in") registries: `C/registries/RTFRegistries.java` and `RTFBuiltInRegistries.java`
Each is created with `RegistryUtil.createRegistry`. The element type is `Codec<? extends X>`: every type registry stores **`Codec` values, not `MapCodec`**. Dispatch uses `REG.byNameCodec().dispatch(X::codec, Function.identity())`.

| Key | Element | Registered entries (file) |
|---|---|---|
| `reterraforged:worldgen/noise_type` | `Codec<? extends Noise>` | 42 entries (`C/world/worldgen/noise/module/Noises.java:419`): constant, sin, white, perlin, perlin2, perlin_ridge, simplex, simplex2, simplex_ridge, worley, worley_edge, billow, cubic, line, shift, frequency, add, multiply, power, power_curve, curve, gradient, terrace, advanced_terrace, invert, blend, alpha, boost, steps, abs, map, clamp, threshold, min, max, warp, erosion, linear_spline, cache, cell, legacy_temperature, legacy_moisture |
| `reterraforged:worldgen/domain_type` | `Codec<? extends Domain>` | domain, direction, compound, add, direct (`noise/domain/Domains.java:57`) |
| `reterraforged:worldgen/curve_function_type` | `Codec<? extends CurveFunction>` | interpolation, scurve, terrace (`noise/function/CurveFunctions.java`) |
| `reterraforged:worldgen/chance_modifier_type` | `Codec<? extends ChanceModifier>` | elevation, biome_edge (`feature/chance/RTFChanceModifiers.java`) |
| `reterraforged:worldgen/template_placement_type` | `Codec<? extends TemplatePlacement<?>>` | any, tree (`feature/template/placement/TemplatePlacements.java`; both use `Codec.unit`) |
| `reterraforged:worldgen/template_decorator_type` | `Codec<? extends TemplateDecorator<?>>` | tree (`feature/template/decorator/TemplateDecorators.java`) |
| `reterraforged:worldgen/biome_modifier_type` | `Codec<? extends BiomeModifier>` | add, replace (per-loader `BiomeModifiersImpl`) |
| `reterraforged:worldgen/structure_rule_type` | `Codec<? extends StructureRule>` | cell_test (`structure/rule/StructureRules.java`) |

### Custom datapack (dynamic) registries (`C/RTFCommon.java:63-67`, `RegistryUtil.createDataRegistry`)

| Key | Codec |
|---|---|
| `reterraforged:worldgen/noise` | `Noise.DIRECT_CODEC` (`Codec.either(floatRange, dispatch)`). `Holder<Noise>` fields use `RegistryFileCodec.create(RTFRegistries.NOISE, DIRECT_CODEC)` (`Noise.java:11`). |
| `reterraforged:worldgen/biome_modifier` | `BiomeModifier.CODEC` |
| `reterraforged:worldgen/structure_rule` | `StructureRule.CODEC` |
| `reterraforged:worldgen/surface_layers` | `LayeredSurfaceRule.Layer.CODEC` |
| `reterraforged:worldgen/preset` | `Preset.CODEC` (`PresetFormat.versioned(UNVERSIONED_CODEC)`) |

These are registered with Fabric `DynamicRegistries.register` and Forge `DataPackRegistryEvent.NewRegistry`. Neither loader syncs them to clients; they are server-only. On disk they load from `data/<ns>/reterraforged/worldgen/<name>/…`, because modded datapack registries have their path prefixed by the registry namespace. The test asserts `data/reterraforged/reterraforged/worldgen/preset/preset.json` (`common/src/test/.../PresetDatapackTest.java:24`).

### Vanilla registries RTF registers into

| Registry | Entries | Codec form | File |
|---|---|---|---|
| `BuiltInRegistries.DENSITY_FUNCTION_TYPE` | noise_sampler, cell, clamp_to_nearest_unit, linear_spline | `Codec<? extends DensityFunction>`. Each DF's `codec()` returns `new KeyDispatchDataCodec<>(CODEC)` | `C/world/worldgen/densityfunction/RTFDensityFunctions.java:14-34` |
| `BuiltInRegistries.MATERIAL_CONDITION` | mod, biome_tag, noise, terrain, height, steepness, erosion, sediment, river_bank, height_modification_detection, any | `Codec<? extends SurfaceRules.ConditionSource>` plus `KeyDispatchDataCodec` in `codec()` | `C/world/worldgen/surface/condition/RTFSurfaceConditions.java:21-112` |
| `BuiltInRegistries.MATERIAL_RULE` | layered, strata, noise, plus `terrablender` (only if TB is loaded, `TBSurfaceRules.java:24`) | same as above | `C/world/worldgen/surface/rule/RTFSurfaceRules.java:19-34` |
| `BuiltInRegistries.FEATURE` | template, bush, disk, chance, erode_snow, swamp_surface | `new XFeature(Config.CODEC)` (Codec) | `C/world/worldgen/feature/RTFFeatures.java:12-23` |
| `BuiltInRegistries.PLACEMENT_MODIFIER_TYPE` | dimension_filter, terrain_filter, macro_biome_filter, noise_filter, fast_poission (sic), legacy_count_extra | `PlacementModifierType<P>` built as the lambda `() -> codec` from a `Codec<P>` | `C/world/worldgen/feature/placement/RTFPlacementModifiers.java:19-61` |
| `BuiltInRegistries.FLOAT_PROVIDER_TYPE` | legacy_canyon_y_scale (`Codec.unit`) | `FloatProviderType<T>` lambda from a Codec | `C/world/worldgen/floatproviders/RTFFloatProviderTypes.java` |
| `BuiltInRegistries.HEIGHT_PROVIDER_TYPE` | legacy_carver | `HeightProviderType<T>` lambda from a Codec | `C/world/worldgen/heightproviders/RTFHeightProviderTypes.java` |

- **No** RTF registrations exist for structure types, structure placement types, biome sources, chunk generators, carvers, blocks, items or argument types.
- `VolatileAirBlock` (`C/blocks/VolatileAirBlock.java`) is never registered and is dead code.
- The world preset is not registered in code. It is the static JSON `data/reterraforged/worldgen/world_preset/reterraforged.json` plus the `data/minecraft/tags/worldgen/world_preset/normal.json` tag. `RTFWorldPresets.RETERRAFORGED` is only a `ResourceKey`.

Codec style: 154 `static final Codec<…> CODEC` declarations, **0 `MapCodec` CODECs** (the one `MapCodec` import is the `PresetCodecs.defaulted` helper), 121 `RecordCodecBuilder.create` calls and 0 `RecordCodecBuilder.mapCodec`. There are 20 `KeyDispatchDataCodec` users.

---

## 5. Vanilla subclasses and implementations (with overrides)

**Density functions** (`C/world/worldgen/densityfunction/`):
- `ClampToNearestUnit` (record, implements DensityFunction) overrides compute(FunctionContext), fillArray(double[], ContextProvider), mapAll(Visitor), minValue, maxValue and codec() returning KeyDispatchDataCodec.
- `LinearSplineFunction` (record, implements DensityFunction) overrides compute, fillArray, mapAll and codec.
- `MappedFunction` (interface extends `DensityFunction.SimpleFunction`) provides defaults for codec, compute, minValue and maxValue. Its `Marker` also extends SimpleFunction.
- `NoiseSampler` and `NoiseSampler.Marker` (records): compute, minValue, maxValue, codec, mapAll.
- `CellSampler`, `CellSampler.CacheChunk` and `CellSampler.Marker`: compute, minValue, maxValue, codec.
- `MutableFunctionContext` implements `DensityFunction.FunctionContext`: blockX, blockY, blockZ.
- Anonymous `DensityFunction.Visitor` in `MixinRandomState.java:~75`: apply and visitNoise(NoiseHolder).

**Surface rules and conditions** (`C/world/worldgen/surface/`):
- ConditionSource records `AnyCondition`, `BiomeTagCondition` and `ModCondition`, plus the `Source` records of Erosion, Height, HeightModificationDetection, Noise, RiverBank, Sediment, Steepness and TerrainCondition. Each overrides `apply(Context)` and `codec()` returning KeyDispatchDataCodec.
- `CellCondition` (abstract, extends `SurfaceRules.LazyXZCondition`) overrides `compute()`. `NoiseCondition` extends `LazyXZCondition`. Threshold, Terrain and HeightModificationDetection extend CellCondition.
- RuleSource records `LayeredSurfaceRule`, `NoiseRule`, `StrataRule` and `TBSurfaceRules.TBRule` override `apply(Context)` and `codec()`.
- `NoiseRule.Rule` and `StrataRule.Rule` implement `SurfaceRules.SurfaceRule.tryApply(int,int,int)`.

**Features** (`Feature<C>.place(FeaturePlaceContext<C>)`):
- `BushFeature`, `DiskFeature` (uses vanilla `DiskConfiguration`), `ErodeSnowFeature`, `SwampSurfaceFeature`, `ChanceFeature` and `TemplateFeature`.
- The config records implement `FeatureConfiguration`.
- `TemplateFeature` loads NBT through `FeatureTemplate.load` (`NbtIo.readCompressed(InputStream)`, `NbtUtils.readBlockState(HolderLookup<Block>, CompoundTag)`, `ListTag.getCompound` and `CompoundTag.getList(name, type)`, in `C/world/worldgen/feature/template/template/FeatureTemplate.java:310-341`). It uses `world.getServer()` cast to `RTFMinecraftServer`.

**Placement**:
- `CellFilter` (abstract), `DimensionFilter` and `NoiseFilter` extend `PlacementFilter` and override `shouldPlace(PlacementContext, RandomSource, BlockPos)` and `type()`. `TerrainFilter` and `MacroBiomeFilter` extend CellFilter.
- `LegacyCountExtraModifier` and `FastPoissonModifier` extend `PlacementModifier` and override `getPositions(PlacementContext, RandomSource, BlockPos)` and `type()`.

**Value providers**:
- `LegacyCanyonYScale extends FloatProvider`: sample(RandomSource), getMinValue, getMaxValue, getType.
- `LegacyCarverHeight extends HeightProvider`: sample(RandomSource, WorldGenerationContext), getType.

**Other**:
- `BlockReader implements BlockGetter` overrides getHeight, **getMinBuildHeight**, getBlockEntity, getBlockState and getFluidState.
- `VolatileAirBlock extends AirBlock` overrides updateShape (it is unregistered).
- `RTFBuiltinPackSource extends BuiltInPackSource` overrides createVanillaPack(PackResources), getPackTitle(String) and createBuiltinPack(String, Pack.ResourcesSupplier, Component). It uses `VanillaPackResourcesBuilder` (exposeNamespace, pushAssetPath, applyDevelopmentConfig) and `Pack.readMetaAndCreate(title, desc, false, supplier, PackType.SERVER_DATA, Pack.Position.TOP, PackSource.FEATURE)`.
- Tag providers: `PresetBiomeTagsProvider extends TagsProvider<Biome>`, `PresetBlockTagsProvider extends IntrinsicHolderTagsProvider<Block>` and `PresetSurfaceLayerProvider extends TagsProvider<LayeredSurfaceRule.Layer>` override `addTags(HolderLookup.Provider)`. PresetBlockTagsProvider uses `.addOptional(new ResourceLocation("create", ...))` and `.addOptionalTag`.
- `LanguageProvider implements DataProvider` (run(CachedOutput), getName). The Fabric `DataGenUtilImpl.Provider` also implements DataProvider.
- About 14 enums implement `StringRepresentable`.
- Forge: `DeferredRegistry implements Registry<T>` and `DeferredRegistry.Writable implements WritableRegistry<T>`. This is a full delegate over about 35 Registry methods: byId, size, iterator, key, getKey, getResourceKey, getId, get(ResourceKey), get(ResourceLocation), lifecycle, registryLifecycle, keySet, entrySet, registryKeySet, getRandom, containsKey x2, freeze, createIntrusiveHolder, getHolder(int), getHolder(ResourceKey), wrapAsHolder, holders, getTag, getOrCreateTag, getTags, getTagNames, resetTags, bindTags, holderOwner, asLookup, registerMapping, register, isEmpty and createRegistrationLookup. **The Registry interface changes break this class completely.**
- GUI classes are covered in section 6.

---

## 6. Client and GUI (`C/client/**`, 3,617 lines, plus the `MixinCreateWorldScreen` and `ScreenInvoker` mixins)

**Screens and widgets that subclass vanilla**:
- `SavePresetScreen extends Screen` overrides init, keyPressed(int,int,int), render(GuiGraphics,int,int,float) and onClose, and calls `renderBackground(graphics)` at l.100.
- `LinkedPageScreen extends Screen` (the base of `PresetConfigScreen`) overrides init, render and onClose, calls `super.renderBackground(guiGraphics)` at l.71 and uses `rebuildWidgets`.
- `TerrainTab implements Tab` overrides getTabTitle, visitChildren(Consumer<AbstractWidget>), doLayout(ScreenRectangle) and tick.
- `ScrollingPanel extends AbstractWidget` overrides renderWidget(GuiGraphics,int,int,float), mouseClicked, mouseDragged, mouseReleased, **mouseScrolled(double,double,double)** and updateWidgetNarration(NarrationElementOutput).
- `TerrainPreview extends AbstractWidget` overrides renderWidget, **mouseScrolled(double,double,double)**, onDrag and updateWidgetNarration.
- `Label extends Button` (playDownSound(SoundManager), renderWidget). `ValueButton extends Button` uses the ctor `(int,int,int,int,Component,OnPress,CreateNarration)`.
- `Slider extends AbstractSliderButton` (applyValue, updateMessage).
- `OptionWidgets.OptionSlider extends Slider` and `OptionWidgets.OptionCycleButton extends Button` (renderWidget, mouseClicked).
- `WidgetList extends ContainerObjectSelectionList`. Its ctor `(Minecraft, int, int, int, int, int slotHeight)` is at l.16. It overrides isSelectedItem, getRowWidth and getScrollbarPosition, and its Entry overrides children, render(GuiGraphics, 10 args) and narratables.

**Vanilla GUI API calls** (count and representative sites):
- `Button.builder(...)` x15 (TerrainTab:65-79, LinkedPageScreen:39-49, ...) together with `.tooltip()`, `.bounds` and `.build`.
- `Tooltip.create` x10 and `setTooltip`.
- `CycleButton.builder/booleanBuilder/Builder/OnValueChange` (PresetWidgets:57-80, TerrainTab:~86).
- `new EditBox` x3 (OptionPage:100, PresetWidgets:29, SavePresetScreen:47).
- `new StringWidget(Component, Font).alignLeft()` (TerrainTab:64).
- `addRenderableWidget` x7 and `addWidget` x8 (PresetListPage:128-134).
- `Minecraft.getInstance()` x10, `minecraft.setScreen` x5 and `minecraft.tell` (MixinCreateWorldScreen:106).
- `Minecraft.keyboardHandler.getClipboard/setClipboard` (PresetSharing:23,28).
- `SystemToast.add` / `addOrUpdate` with `SystemToastIds` (Toasts:13, MixinCreateWorldScreen:95) and `minecraft.getToasts()`.
- `Screen.hasShiftDown()` (TerrainTab:65).
- `Util.getPlatform().openUri` (PresetListPage:112,116).
- `WorldCreationUiState`: getSettings, getWorldType, getNormalPresetList, setWorldType, getSeed, setSeed, setGameMode, setAllowCheats (TerrainState:43-141, MixinCreateWorldScreen:64). Also `WorldCreationUiState.WorldTypeEntry.preset().is(key)` (TerrainState:110-123).
- `WorldCreationContext`: selectedDimensions().overworld().getBiomeSource().possibleBiomes() (OptionPage:66), worldgenLoadContext() (PresetApplier:53, OptionPage:67, PresetConfigScreen:59) and dataConfiguration().dataPacks().getEnabled() (PresetApplier).
- `WorldOptions.parseSeed` (TerrainState:136).
- `CreateWorldScreen.getDataPackSelectionSettings` and `tryApplyNewDataPacks` (AW'd; PresetApplier:38,63). `PackRepository.reload/setSelected/getSelectedIds`.
- Preset editor hook: the vanilla `PresetEditor` interface (Fabric mixin) and Forge `RegisterPresetEditorsEvent` via `(screen, ctx) -> new PresetConfigScreen(screen)`.

**Rendering** (these APIs churn heavily):
- `GuiGraphics`: `fill` x6 (ScrollingPanel:91-92, TerrainPreview:175,179,207, OptionWidgets:72), `drawString(Font, Component|String, x, y, color)` x4 (TerrainPreview:184,192,208, Label:28), `drawCenteredString` x2 (SavePresetScreen:101-102), `enableScissor`/`disableScissor` (ScrollingPanel:78,84) and **`blit(ResourceLocation, x, y, u, v, w, h, texW, texH)`** (TerrainPreview:177).
- Texture: `new DynamicTexture(new NativeImage(RES, RES, false))`, `Minecraft.getTextureManager().register(String, DynamicTexture)` returns a ResourceLocation, then `texture.getPixels().setPixelRGBA(x, z, argb)` and `texture.upload()` (`C/client/gui/createworld/TerrainPreview.java:154-166`).
- `Font.width`.
- No `RenderSystem`, `PoseStack`, `BufferBuilder`/`Tesselator` or shaders are used in main code.
- Clienttest uses `Screenshot.takeScreenshot(RenderTarget)` and `NativeImage`.

**Threading in the GUI**: TerrainPreview renders on a `ScheduledExecutorService` (`RTF-Preview`, l.50) and hands back with `Minecraft.getInstance().execute`.

**Client config**: `C/client/ClientConfig.java` stores `config/reterraforged/client.json` (Gson) with the key `useAsDefaultWorldType`.

**Lang datagen**: `C/client/data/LanguageProvider.java` implements DataProvider and `RTFLanguageProvider.EnglishUS`, with output in `assets/reterraforged/lang/en_us.json`.

---

## 7. Datagen and datapacks

- **Runtime preset → datapack** is the core mechanism:
  1. `Preset.buildPatch(HolderLookup.Provider)` (`C/data/preset/settings/Preset.java:45-68`) builds a `new RegistrySetBuilder()`. For each registry it calls `builder.add(key, ctx -> patch.apply(preset, ctx))` with a `BootstapContext<T>`.
     - The RTF registries are PRESET, NOISE, BIOME_MODIFIER, STRUCTURE_RULE and SURFACE_LAYERS.
     - The vanilla registries are CONFIGURED_FEATURE, CONFIGURED_CARVER, STRUCTURE_SET, PLACED_FEATURE, BIOME, DIMENSION_TYPE, NOISE, DENSITY_FUNCTION (including `TBNoiseRouterData`, always) and NOISE_SETTINGS.
     - It finishes with `builder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), registries)`.
  2. `RTFDataGen.makePreset` (`C/data/RTFDataGen.java:34-56`, `@Deprecated`) does `new DataGenerator(path, SharedConstants.getCurrentVersion(), true)`, then `dataGenerator.new PackGenerator(true, "preset", new PackOutput(out))` (AW ctor). It adds the providers `DataGenUtil.createRegistryProvider`, `PresetBlockTagsProvider`, `PresetSurfaceLayerProvider`, `PresetBiomeTagsProvider` and `PackMetadataGenerator.forFeaturePack(output, Component)`. `pack_format` comes implicitly from the current version.
  3. `PresetPacks.export` (`C/data/PresetPacks.java:34`) runs the generator, zips the result with the JDK zipfs and moves it to `reterraforged-preset[-<sha1>].zip` in the world's `datapacks/`.
  4. The pack is detected by the id prefix `file/reterraforged-preset` (`PresetPacks.isPresetPack`), which the RegistryDataLoader mixin relies on.
- The client uses `PresetApplier` (section 6), called from the CreateWorldScreen mixin, with `worldgenLoadContext()` as the base registries.
- The dedicated server uses `ServerPresets.installIfNewWorld` from the ServerPacksSource mixin, with `VanillaRegistries.createLookup()` as the base (`C/server/ServerPresets.java:29`).
- **Bootstrap classes** take `BootstapContext<T>` (misspelled in 1.20.1; 45 occurrences in 21 files). Examples are PresetData, PresetNoiseData, PresetNoiseParameters, PresetNoiseRouterData, PresetNoiseGeneratorSettings, PresetSurfaceRuleData, PresetSurfaceLayerData, PresetBiomeModifierData, PresetConfiguredFeatures, PresetPlacedFeatures, PresetConfiguredCarvers, PresetStructureRuleData, PresetStructureSets, PresetDimensionTypes, PresetBiomeData, PresetTerrainNoise, PresetClimateNoise, PresetSurfaceNoise, PresetStrataNoise, PresetFeatureNoise and TBNoiseRouterData.
  - They call `ctx.register(key, value)` and `ctx.lookup(Registries.X)` (a HolderGetter) and use `getOrThrow` heavily (about 220 `getOrThrow(` sites, mostly HolderGetter).
- **Vanilla worldgen data re-created in code (very version-sensitive)**:
  - `PresetNoiseRouterData` re-implements the overworld NoiseRouter and uses AW'd `NoiseRouterData` internals, `OreVeinifier.VeinType` and `DensityFunctions.weirdScaledSampler`.
  - `PresetNoiseGeneratorSettings` does `new NoiseGeneratorSettings(NoiseSettings.create(...), stone, water, router, surfaceRule, spawnTarget, seaLevel, false, true, largeOreVeins, false)` (10-arg ctor).
  - `PresetDimensionTypes` does `new DimensionType(OptionalLong, ..., BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0f, new MonsterSettings(false, true, UniformInt.of(0,7), 0))`.
  - `PresetSurfaceRuleData` (802 lines) is the vanilla overworld surface rules plus RTF rules.
  - `PresetConfiguredFeatures` and `PresetPlacedFeatures` use `FeatureUtils`, `PlacementUtils`, `TreeFeatures`, `OreFeatures`, `MiscOverworldFeatures`, `VegetationPlacements`, `TreePlacements`, `OrePlacements`, `MiscOverworldPlacements`, `Carvers` and the configuration classes RandomPatch, RandomFeature, SimpleBlock and Disk. They also use the carver configs Cave and Canyon (`CarverDebugSettings`) and the tree decorators Beehive and AlterGround.
  - `PresetStructureSets` builds `new RandomSpreadStructurePlacement(...)` and `new ConcentricRingsStructurePlacement(...)` from AW'd StructurePlacement fields.
- **Mod-jar resource datagen**:
  - `RTFDataGen.generateResourcePacks(ResourcePackFactory)` (lang plus metadata). On Fabric it is only called from `DataGeneratorEntrypoint`, which `fabric.mod.json` does not declare.
  - On Forge it runs through `GatherDataEvent` (`FG/forge/RTFForge.java:34-41`) with the `forge/build.gradle` `dataGen { mod "reterraforged" }`.
- **pack.mcmeta**: `forge/src/main/resources/pack.mcmeta` has `pack_format: 15` (hard-coded, 1.20.1) and a translate description. There is no pack.mcmeta in common or Fabric.
- **Static resources** under `common/src/main/resources`:
  - `data/minecraft/tags/worldgen/world_preset/normal.json` adds `reterraforged:reterraforged` to the normal world types tag.
  - `data/reterraforged/worldgen/world_preset/reterraforged.json` is a copy of vanilla's default preset (noise, multi_noise, preset `overworld`/`nether`, `the_end`).
  - `data/reterraforged/structures/{mushrooms/{brown,red}, trees/{acacia,birch,dark_oak,jungle,meadow,oak,pine,redwood,spruce,willow}/...}/*.nbt` holds 109 `.nbt` templates. They are loaded by RTF's own `FeatureTemplateManager` through `ResourceManager.getResource(reterraforged:structures/…nbt)`; the path is built in `C/data/preset/PresetTemplatePaths.java:48`. They are not loaded through the vanilla StructureTemplateManager.
  - `assets/reterraforged/lang/en_us.json`.
  - `biomes.png` and `biomes.txt` (classpath root, read by `BiomeTypeLoader`/`BiomeTypeColors` with `getResourceAsStream`).
  - `architectury.common.json`, the mixin JSONs and the AW.
- **Generated tag paths** use 1.20.1 directory names, for example `data/reterraforged/tags/blocks/rock.json` (asserted in `PresetBlockTagsTest.java:32`).
- **`RTFBuiltinPackSource`** reads `/data/` from the mod classpath with `PACKS_DIR = reterraforged:datapacks`. `RTFDataGen.DATAPACK_PATH = "data/reterraforged/datapacks"` exists, but no such directory is in resources, so the source currently finds no packs.

---

## 8. Commands, config, networking and server lifecycle

- **Commands**: `C/server/commands/LocateTerrainCommand.java` registers `/rtf locate <terrain>` with permission 2.
  - It uses Brigadier (`CommandDispatcher`, `Commands.literal/argument`, `StringArgumentType.word()`), `SharedSuggestionProvider.suggest`, `DynamicCommandExceptionType` and `CommandSourceStack.sendSuccess(Supplier<Component>, boolean)`.
  - It builds `ClickEvent(SUGGEST_COMMAND)` and `HoverEvent(SHOW_TEXT)` components and uses `ComponentUtils.wrapInSquareBrackets`, `BlockPos.containing(Vec3)` and `commandSourceStack.getLevel().getChunkSource().randomState()`.
  - `TerrainArgument` is a plain word argument with suggestions, not a custom `ArgumentType`, so nothing is registered in ArgumentTypeInfos.
  - Registration happens per loader (section 3) with `CommandBuildContext`.
- **Config**: there is no night-config or loader config API. `night-config:toml` is only `compileOnly` in `common/build.gradle:13` and is unused in code.
  - `ConfigUtil.rtf(path)` resolves to `<configDir>/reterraforged/` and is created in a static init.
  - `PerformanceConfig` (`C/config/PerformanceConfig.java`) is a stub: `read()` returns defaults (`DataResult.success`).
  - `ClientConfig` (JSON), `server-preset.json` (JSON through `Preset.CODEC`) and the `PresetLibrary` user presets and export folder are all Gson plus Codec files.
- **Networking**: none. No packets, `FriendlyByteBuf`, payloads or channels. The datapack registries are not synced.
- **Server lifecycle and level type**:
  - `ServerPresets.isReTerraForgedLevelType` reads `server.properties` directly with `java.util.Properties` and checks for `level-type=reterraforged:reterraforged`.
  - The install hook is the `ServerPacksSource.createPackRepository(Path)` HEAD inject, and it only runs when `level.dat` is missing.
  - Other hooks: the `MinecraftServer` ctor and reload-lambda injects (template manager), `ChunkMap.<init>` (RTF RandomState init), `MinecraftServer.setInitialSpawn`, `Util.shutdownExecutors`, and the Forge `ServerLifecycleHooks.runModifiers`.
  - No loader lifecycle events such as ServerStarting or ServerLifecycleEvents are used.

---

## 9. Compat

- **TerraBlender** (`C/compat/terrablender/*`, `C/mixin/terrablender/*`):
  - Detected with `ModLoaderUtil.isLoaded(TerraBlender.MOD_ID)` (`TBCompat.java:12`).
  - Compiled against `com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.2` (compileOnly in common, even for the Fabric build).
  - API used: `terrablender.core.TerraBlender.MOD_ID`, `terrablender.api.Regions.getCount(RegionType)`, `terrablender.api.RegionType`, `SurfaceRuleManager.getNamespacedRules(RuleCategory, RuleSource)`, `SurfaceRuleManager.getDefaultSurfaceRules(RuleCategory)`, `SurfaceRuleManager.RuleCategory` and `terrablender.worldgen.surface.NamespacedSurfaceRuleSource.sources()` (a TB internal class).
  - Mixins into TB-added methods: `Climate.ParameterList.initializeForTerraBlender` and `getUniqueness(III)` (section 1).
  - Registers the `terrablender` material rule when TB is loaded. `TBNoiseRouterData` registers the density function `terrablender:uniqueness` into every preset pack, whether or not TB is present (`Preset.java:81`).
  - Unused imports of TB classes: `MixinSurfaceSystem.java` imports `NamespacedSurfaceRuleSource`, and `PresetSurfaceLayerData.java` imports `TerraBlender`.
- **World Preview** (`world_preview`, `caeruleusTait.world.preview`): the `@Pseudo` mixin into `SampleUtils`' two constructors (section 1). Detected with `WPCompat.isEnabled()`.
- **Noisium**: no code. `MixinNoiseBasedChunkGenerator` uses priority 9001 to coexist with it.
- **Create**: optional block tag entries `create:{asurine,crimsite,limestone,ochrum,scorchia,scoria,veridium}` (`PresetBlockTagsProvider.java:39-45`).
- **Generic**: the `ModCondition` surface condition (`mod`) calls `ModLoaderUtil.isLoaded(modId)` at rule time.

---

## 10. Other vanilla APIs of note (counts are occurrences / files across all source sets)

- **`new ResourceLocation(`**: 16 occurrences in 5 files.
  - Main code: `C/RTFCommon.java:71-72` (the central `RTFCommon.location()`, 18 call sites in 16 files), `C/compat/terrablender/TBNoiseRouterData.java:11` and `C/data/preset/tags/PresetBlockTagsProvider.java:39-45` (7 occurrences).
  - Tests: `PresetDatapackTest.java:76,79` and `StructureOptionsTest.java:30,83,84,98`.
  - There are no `ResourceLocation.tryParse/of/parse/fromNamespaceAndPath` calls yet.
- **DataResult and DFU**:
  - `DataResult.getOrThrow(false, Consumer)` has 14 sites. Main: `PresetApplier.java:80`, `PresetLibrary.java:134`, `PresetShareCode.java:29`, `ServerPresets.java:57,64`. The rest are tests.
  - `DataResult.error(() -> …)` (Supplier form): 10 sites (`PresetShareCode.java:50-76`, `PresetFormat.java:50-90`).
  - `.result()`: 4 sites (`PresetListPage.java:253` plus tests).
  - `.resultOrPartial(...)`: 5 sites (`TerrainPreview.java:119`, `PresetSharing.java:29`, `PresetLibrary.java:147`, `MixinRandomState.java:121`, the Fabric `DataGenUtilImpl.java:70`).
  - `.error()` returning `PartialResult`: tests only (`PresetDatapack.java:69`, `PresetFormatTest.java:95-96`, `PresetShareCodeTest.java:88`).
  - No `DataResult.get()` calls.
  - `Codec.unit(...)`: 4 sites (AnyPlacement, TreePlacement, LegacyCanyonYScale, DirectWarp).
  - `ExtraCodecs.nonEmptyList`: 2 sites (`LinearSplineFunction.java:21`, `LinearSpline.java:21`).
  - `Codec.either`: `Noises.java:25`. `Codec.unboundedMap`: 4 sites. `Codec.floatRange/intRange`: 11 sites.
  - `RecordCodecBuilder.create`: 121 calls in 100 files. `.fieldOf(`: 372. `optionalFieldOf(`: 22.
  - `JsonOps`: 35 occurrences. `RegistryOps`: 7 (Fabric DataGenUtilImpl and test PresetDatapack).
  - `RegistryFileCodec.create`: `Noise.java:11`.
  - `StringRepresentable`: 36 occurrences.
- **Registry access**:
  - `BuiltInRegistries.*` (vanilla): BLOCK x6, REGISTRY x2, and one each of DENSITY_FUNCTION_TYPE, FEATURE, PLACEMENT_MODIFIER_TYPE, MATERIAL_RULE, MATERIAL_CONDITION, FLOAT_PROVIDER_TYPE and HEIGHT_PROVIDER_TYPE.
  - `Registries.*`: BIOME 9, PLACED_FEATURE 7, STRUCTURE_SET 6, DENSITY_FUNCTION 6, BLOCK 6, NOISE 3, CONFIGURED_FEATURE 3, STRUCTURE 2, CONFIGURED_CARVER 2, and one each of WORLD_PRESET, NOISE_SETTINGS, LEVEL_STEM and DIMENSION_TYPE.
  - `registryAccess.lookupOrThrow` (12 occurrences, 10 files) and `.lookup(key)` (MixinMinecraftServer:~31, Forge MixinServerLifecycleHooks:34).
  - `RegistryLookup.get/listElements/getOrThrow(TagKey)` (BiomeTagCondition:24-28, LayeredSurfaceRule:22-23, MixinStructure).
  - `HolderLookup.RegistryLookup.filterFeatures(FeatureFlagSet)` (FeatureTemplateManager:29).
  - `VanillaRegistries.createLookup()`: 4 main and test sites (`ServerPresets.java:29`, tests).
  - `Holder<`: 127 occurrences in 34 files. `HolderSet`: 88 in 13 files (mostly `PresetBiomeModifierData.java`). `HolderGetter`: 61 in 14. `HolderLookup`: 43 in 19. `Holder.Reference`: 8 in 5 (plus `Holder.Reference.createStandAlone` in the Forge DeferredRegistry:223).
  - `TagKey.create`: 7 sites (`C/tags/RTFBiomeTags.java`, `RTFBlockTags.java`, `RTFSurfaceLayerTags.java`, …). `TagKey`: 74 occurrences in 11 files.
  - `ResourceKey.create`: 12 occurrences in 11 files. `ResourceKey.createRegistryKey` in `RTFRegistries.java:40`.
- **`BootstapContext`** (renamed in later versions): 45 occurrences in 21 files (section 7). `RegistrySetBuilder`: `Preset.java:46-68` only.
- **Worldgen internals**:
  - `ChunkStatus`: 9 occurrences, only in MixinChunkStatus.
  - `Blender`: MixinNoiseBasedChunkGenerator and the TB MixinNoiseChunk.
  - `Beardifier` and `DensityFunctions.BeardifierOrMarker`: MixinNoiseChunk (commented) and the TB MixinNoiseChunk.
  - `NoiseChunk`: 11 occurrences in 4 files.
  - `RandomState`: 138 occurrences in 22 files. The central pattern is `(Object) randomState instanceof RTFRandomState`, with `level.getChunkSource().randomState()`, `randomState.sampler()` and `randomState.router()`.
  - `WorldGenRegion`: `SurfaceRegion.java` ThreadLocal, MixinContext and MixinNoiseBasedChunkGenerator.
  - `Climate.*`: 31 occurrences in 11 files (Sampler, TargetPoint, ParameterPoint, ParameterList, `findSpawnPosition`). SpawnFinderFix is a copy of `Climate.SpawnFinder`.
  - `SurfaceSystem`: 13 occurrences in 4 files.
  - `SurfaceRules.*`: 494 occurrences in 26 files (PresetSurfaceRuleData dominates).
  - `NoiseRouter`: 74 occurrences in 7 files. `DensityFunctions.*`: 91 in 6. `NoiseGeneratorSettings`: 19 in 7. `NoiseSettings`: 16 in 5.
  - `StructureManager`: only as a mixin handler parameter. `ServerLevel`: 14 occurrences in 5 files.
  - `PlacementContext` 15 occurrences in 7 files. `FeaturePlaceContext` 22 in 10. `WorldGenLevel` 8 files.
  - `QuartPos` and `SectionPos` (CellSampler, MixinNoiseChunk, PostProcessing).
  - `LevelHeightAccessor.getMinBuildHeight/getMaxBuildHeight`: `StrataRule.java:71-72`, `BlockUtils.java:50`, the `BlockReader.java:25` override and clienttest:287-305.
  - `NbtIo.readCompressed(InputStream)`: `FeatureTemplate.java:310`.
  - `Block`, `BlockState` and `Blocks.*`: 155 `Blocks.X` or `BlockBehaviour.Properties` references.
- **Threading**: no `Util.backgroundExecutor()`.
  - Own pools: `ThreadPools.WORLD_GEN = Executors.newFixedThreadPool(availableProcessors, daemon "RTF-WorldGen")` (`C/concurrent/ThreadPools.java:9`), `Cache.SCHEDULER` (single-thread scheduled, `C/concurrent/cache/Cache.java:14`) and `TerrainPreview.EXECUTOR` (`RTF-Preview`).
  - The pools are shut down through the `MixinUtil` inject into `Util.shutdownExecutors`.
  - `CompletableFuture`, `ForkJoinPool` and `Executors`: 59 occurrences in 15 files. `ThreadLocal`: 48 occurrences in 16 files (the RTF cell and pool caches, `SurfaceRegion`).
  - Worldgen state is thread-confined through the `WorldGenFlags` static flags (fast lookups and cull toggled from the ChunkStatus mixin).
- **Other**: `Util.getPlatform().openUri` (PresetListPage:112,116). `SharedConstants.getCurrentVersion()` (RTFDataGen:35, test). `Component.translatable/literal`: 91 occurrences in 23 files. `GsonHelper` appears twice. `CubicSpline` and `ToFloatFunction` are used by the noise router. `Mth` is used in 9 files.

---

## 11. Build and test infrastructure

- **Gradle**:
  - Gradle 9.4.0 (wrapper) with daemon toolchain 21 (`gradle/gradle-daemon-jvm.properties`).
  - Plugins: `architectury-plugin 3.5-SNAPSHOT`, `dev.architectury.loom 1.17.493` and `com.gradleup.shadow 9.6.1`.
  - Root `build.gradle`: `minecraft "com.mojang:minecraft:${minecraft_version}"`, `mappings loom.officialMojangMappings()` and `options.release = java_version` (17).
  - Repositories: maven.shedaniel.me, jitpack (TerraBlender) and maven.minecraftforge.net. `settings.gradle` includes common, fabric and forge.
- **gradle.properties**:
  - `minecraft_version=1.20.1`, `java_version=17`, `fabric_loader_version=0.19.5`, `fabric_api_version=0.92.12+1.20.1`, `forge_version=47.4.23`, `forge_min_version=47.1`, `terrablender_version=3.0.1.2`, `mod_version=0.1.0-alpha.1`.
  - `forge/gradle.properties` sets `loom.platform=forge`.
- **common/build.gradle**:
  - `architectury { common("forge","fabric") }` and the AW path.
  - `modImplementation fabric-loader`, `compileOnly TerraBlender-forge:1.20.1-3.0.1.2` and `compileOnly night-config:toml:3.6.7` (unused).
  - JUnit 5.11.4 and Mockito 5.14.2. The tests take the system props `rtf.updateGolden`, `rtf.showOutput` and `rtf.render`, with 2G heap.
- **fabric/build.gradle**:
  - Shadow common with `transformProductionFabric`. `remapJar.injectAccessWidener = true`.
  - The `clienttest` source set has a `clientTest` loom run (`run-clienttest`, `-Dreterraforged.clienttest.out`, `-Dreterraforged.dev.creative`).
  - The processResources expansion into `fabric.mod.json` requires `fabricloader >=0.15.0`, `fabric-api >=${fabric_api_version}`, `minecraft ~${minecraft_version}` and `java >=17`.
  - The `main` entrypoint is RTFFabric, and the mixins are common plus fabric.
- **forge/build.gradle**:
  - `forge "net.minecraftforge:forge:1.20.1-47.4.23"` with `convertAccessWideners` and `extraAccessWideners`.
  - It registers the two mixin configs, `dataGen { mod "reterraforged" }` and runs with `reterraforged.dev.creative`.
  - `mods.toml` sets `loaderVersion="[47,)"` and dependencies `forge [${forge_min_version},)` and `minecraft [${minecraft_version}]`. The comment says NeoForge 1.20.1 reports itself as "forge".
- **CI** (`.github/workflows/build.yml`): ubuntu, temurin **Java 21**, `gradle/actions/setup-gradle@v4` and `./gradlew build`. It uploads `fabric/build/libs/*-fabric.jar` and `forge/build/libs/*-forge.jar`.
- **Headless tests** (`common/src/test/java/raccoonman/reterraforged/test/*`, 17 files, plus `server/ServerPresetsTest.java`):
  - `TestBootstrap.init()` (`TestBootstrap.java:46-77`):
    1. Calls `SharedConstants.tryDetectVersion()` and `Bootstrap.bootStrap()`.
    2. **Unfreezes every `MappedRegistry` in `BuiltInRegistries.REGISTRY` by reflecting the private field `MappedRegistry.frozen`**.
    3. Uses Mockito `mockStatic` on `RegistryUtil` (createRegistry becomes `new MappedRegistry<>(key, Lifecycle.stable())`, getWritable is identity, register calls `WritableRegistry.register(key, value, Lifecycle.stable())`, and createDataRegistry records into `DATA_REGISTRIES`), on `ModLoaderUtil` (`isLoaded → false`), on `RTFCommands` and on `BiomeModifiers`.
    4. Runs `RTFCommon.bootstrap()` and re-freezes the registries.
  - Other tests use `VanillaRegistries.createLookup()`, `Preset.buildPatch`, `DataGenerator`/`PackGenerator` (AW), `RegistryOps` and `JsonOps`.
  - Fixtures are `fixtures/0.0.6_{default,beautiful}.json` and `golden/preset-fingerprints.properties`.
- **Fabric clienttest** (`fabric/src/clienttest/java/raccoonman/reterraforged/clienttest/ClientTest.java`, 379 lines, plus its own `fabric.mod.json` with a `client` entrypoint depending on `fabric-lifecycle-events-v1`):
  - It is driven by `ClientTickEvents.END_CLIENT_TICK`.
  - It uses `CreateWorldScreen.openFresh`, the AW'd `tabNavigationBar.selectTab(3,false)`, `TitleScreen`, `GenericDirtMessageScreen`, `mc.clearLevel(Screen)`, `mc.createWorldOpenFlows().loadLevel(...)`, `IntegratedServer`, `LevelResource`, `Screenshot.takeScreenshot(RenderTarget)` and `NativeImage`.
  - It also reads `level.getMinBuildHeight/getMaxBuildHeight`, implements `CommandSource` (the `LoggingSource` record) and reads `BuiltInRegistries`.
- **Version-sensitive items**:
  - Java release 17 in both the mixin configs (`JAVA_17`) and gradle.
  - The hard-coded `pack_format 15`.
  - `SharedConstants.tryDetectVersion` and `Bootstrap.bootStrap` in tests.
  - The reflection on `MappedRegistry.frozen`.
  - The comment in `DeferredRegistry.registerMapping`.
  - The Forge `mods.toml` layout, since NeoForge 1.20.2+ uses `neoforge.mods.toml`.

---

## 12. Totals

| Scope | Java files | import net.minecraft.* | import com.mojang.* | net.fabricmc.* | net.minecraftforge.* | terrablender.* | dev.architectury.* |
|---|---|---|---|---|---|---|---|
| All | 467 | **229** | **180** | 12 | 11 | 6 | 6 |
| Main (all modules) | 448 | 222 | 172 | 11 | 11 | 6 | 6 |
| common/src/main | 418 | 200 | 158 | 0 | 0 | 6 | 6 |
| common/src/test | 18 | 6 | 7 | 0 | 0 | 0 | 0 |
| fabric/src/main | 14 | 10 | 6 | 11 | 0 | 0 | 0 |
| fabric/src/clienttest | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| forge/src/main | 16 | 12 | 8 | 0 | 11 | 0 | 0 |

There are 273 distinct `net.minecraft` classes imported and 21 distinct `com.mojang` classes. There are no static or wildcard MC imports. A few fully-qualified inline references are not counted, for example `net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator` in `TemplateDecorators`/`TreeDecorator` and `net.minecraft.commands.CommandSource` in ClientTest.

### Loader API imports (complete)

- **Fabric**:
  - `net.fabricmc.api.{ModInitializer, ClientModInitializer (clienttest), EnvType}`
  - `net.fabricmc.loader.api.FabricLoader`
  - `net.fabricmc.fabric.api.biome.v1.{BiomeModificationContext, BiomeSelectionContext, ModificationPhase}`
  - `net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl` (internal)
  - `net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback`
  - `net.fabricmc.fabric.api.datagen.v1.{DataGeneratorEntrypoint, FabricDataGenerator, FabricDataOutput}`
  - `net.fabricmc.fabric.api.event.registry.{DynamicRegistries, FabricRegistryBuilder}`
  - `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents` (clienttest)
- **Forge**:
  - `net.minecraftforge.fml.common.Mod` (+ `Mod.EventBusSubscriber`)
  - `net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext`
  - `net.minecraftforge.fml.loading.{FMLEnvironment, FMLLoader, FMLPaths}`
  - `net.minecraftforge.api.distmarker.Dist`
  - `net.minecraftforge.eventbus.api.{IEventBus, SubscribeEvent}`
  - `net.minecraftforge.registries.{DeferredRegister, GameData, RegistryBuilder, DataPackRegistryEvent}`
  - `net.minecraftforge.data.event.GatherDataEvent`
  - `net.minecraftforge.event.RegisterCommandsEvent`
  - `net.minecraftforge.client.event.RegisterPresetEditorsEvent`
  - `net.minecraftforge.common.world.{BiomeModifier, ModifiableBiomeInfo.BiomeInfo.Builder}`
  - `net.minecraftforge.common.data.ExistingFileHelper` (+ `IResourceType`)
  - `net.minecraftforge.server.ServerLifecycleHooks`
- **Architectury**: `dev.architectury.injectables.annotations.ExpectPlatform` (6 files). No other Architectury API is used.

### Mixin targets (all)
- Vanilla: BiomeGenerationSettings, BiomeGenerationSettings.PlainBuilder (Forge), ChunkMap, ChunkStatus, Climate, Climate.Sampler (x2), Climate.TargetPoint, Climate.ParameterList, SurfaceRules.Context, SurfaceRules$BiomeConditionSource, SurfaceSystem, CreateWorldScreen, Screen, MinecraftServer (common, Fabric and Forge), NoiseBasedChunkGenerator, NoiseChunk (x2), RandomState, RegistryDataLoader, ServerPacksSource, Structure, Util, PresetEditor (Fabric) and TagsProvider (Forge).
- Loader: `net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl`, `FabricDataGenerator.Pack` and `net.minecraftforge.server.ServerLifecycleHooks`.
- Third-party: `caeruleusTait.world.preview.backend.worker.SampleUtils`.

### AW targets (all)
SurfaceRules$Condition, $Context, $SurfaceRule, $LazyCondition, $LazyYCondition, $LazyXZCondition, SurfaceSystem, NoiseChunk, DensityFunctions (+ $WeirdScaledSampler, $BeardifierMarker, $WeirdScaledSampler$RarityValueMapper), OreVeinifier$VeinType, AbstractWidget, AbstractSelectionList, Screen, CreateWorldScreen, DataGenerator, DataGenerator$PackGenerator, StructurePlacement, Biome, StructureManager, WorldGenRegion, NoiseRouterData, Climate$ParameterPoint, ArgumentTypeInfos, RandomState and Beardifier.

### Distinct net.minecraft classes imported (files importing each; complete list, 273)
```
45 core.BlockPos | 42 core.Holder | 40 resources.ResourceKey | 27 network.chat.Component | 26 core.registries.Registries
25 resources.ResourceLocation | 24 world.level.biome.Biome | 22 util.RandomSource | 22 world.level.levelgen.SurfaceRules
21 data.worldgen.BootstapContext | 20 util.KeyDispatchDataCodec | 18 world.level.block.state.BlockState
17 world.level.levelgen.SurfaceRules.Context | 15 world.level.levelgen.placement.PlacedFeature | 14 core.HolderGetter
14 core.HolderLookup | 14 world.level.levelgen.DensityFunction | 14 world.level.levelgen.RandomState | 13 world.level.block.Blocks
12 core.HolderSet | 12 util.StringRepresentable | 12 world.level.LevelAccessor | 11 data.PackOutput | 11 tags.TagKey
10 core.Registry | 10 core.registries.BuiltInRegistries | 10 world.level.block.Block | 10 world.level.levelgen.feature.FeaturePlaceContext
9 ChatFormatting | 9 client.gui.components.AbstractWidget | 9 client.gui.components.Button | 9 client.gui.screens.Screen | 9 util.Mth
9 world.level.biome.Climate | 9 world.level.chunk.ChunkAccess | 9 world.level.levelgen.GenerationStep | 9 world.level.levelgen.feature.Feature
8 core.RegistryAccess | 8 network.chat.CommonComponents | 8 world.level.WorldGenLevel | 7 client.Minecraft | 7 client.gui.GuiGraphics
7 server.MinecraftServer | 7 world.level.block.Mirror | 7 world.level.block.Rotation | 7 world.level.chunk.ChunkGenerator
7 world.level.levelgen.feature.configurations.FeatureConfiguration | 7 world.level.levelgen.placement.PlacementContext
7 world.level.levelgen.placement.PlacementModifierType | 6 client.gui.screens.worldselection.CreateWorldScreen | 6 core.Direction
6 tags.BlockTags | 6 world.level.ChunkPos | 6 world.level.biome.BiomeGenerationSettings | 6 world.level.biome.Biomes
6 world.level.levelgen.NoiseGeneratorSettings | 5 client.gui.components.EditBox | 5 commands.CommandSourceStack
5 core.HolderLookup.RegistryLookup | 5 core.SectionPos | 5 core.WritableRegistry | 5 server.level.ServerLevel | 5 world.level.levelgen.NoiseSettings
4 client.gui.components.Tooltip | 4 client.gui.components.events.GuiEventListener | 4 client.gui.components.toasts.SystemToast.SystemToastIds
4 commands.CommandBuildContext | 4 data.DataProvider | 4 data.registries.VanillaRegistries | 4 world.level.dimension.LevelStem
4 world.level.levelgen.DensityFunctions | 4 world.level.levelgen.NoiseRouter | 4 world.level.levelgen.placement.PlacementModifier
4 world.level.levelgen.structure.Structure | 4 world.level.levelgen.synth.NormalNoise | 3 SharedConstants | 3 client.gui.components.CycleButton
3 client.gui.components.Renderable | 3 core.HolderLookup.Provider | 3 core.Vec3i | 3 data.DataGenerator | 3 data.tags.TagsProvider
3 network.chat.MutableComponent | 3 resources.RegistryDataLoader | 3 server.level.WorldGenRegion | 3 server.packs.repository.PackRepository
3 server.packs.resources.ResourceManager | 3 util.valueproviders.FloatProvider | 3 world.level.biome.Climate.ParameterPoint
3 world.level.levelgen.Heightmap | 3 world.level.levelgen.feature.ConfiguredFeature | 3 world.level.levelgen.feature.configurations.DiskConfiguration
3 world.level.levelgen.heightproviders.HeightProvider | 3 world.level.levelgen.placement.PlacementFilter | 3 world.level.levelgen.structure.StructureSet
2 Util | 2 client.gui.Font | 2 client.gui.components.tabs.Tab | 2 client.gui.components.toasts.SystemToast
2 client.gui.narration.NarrationElementOutput | 2 client.gui.screens.worldselection.WorldCreationContext
2 client.gui.screens.worldselection.WorldCreationUiState | 2 commands.Commands | 2 core.QuartPos | 2 data.CachedOutput
2 data.metadata.PackMetadataGenerator | 2 data.worldgen.features.MiscOverworldFeatures | 2 data.worldgen.placement.PlacementUtils
2 resources.RegistryOps | 2 util.ExtraCodecs | 2 util.GsonHelper | 2 util.valueproviders.FloatProviderType | 2 util.valueproviders.UniformInt
2 world.level.BlockGetter | 2 world.level.LevelHeightAccessor | 2 world.level.biome.Climate.Sampler | 2 world.level.biome.Climate.TargetPoint
2 world.level.dimension.DimensionType | 2 world.level.levelgen.Aquifer | 2 world.level.levelgen.NoiseChunk | 2 world.level.levelgen.Noises
2 world.level.levelgen.SurfaceSystem | 2 world.level.levelgen.VerticalAnchor | 2 world.level.levelgen.WorldOptions
2 world.level.levelgen.blending.Blender | 2 world.level.levelgen.blockpredicates.BlockPredicate | 2 world.level.levelgen.carver.ConfiguredWorldCarver
2 world.level.levelgen.heightproviders.HeightProviderType | 2 world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
2 world.level.levelgen.structure.placement.RandomSpreadStructurePlacement | 2 world.level.levelgen.structure.placement.StructurePlacement
2 world.level.levelgen.structure.templatesystem.StructureTemplateManager | 2 world.level.storage.LevelStorageSource
1 each: client.Screenshot, client.gui.components.AbstractButton, client.gui.components.AbstractSliderButton,
  client.gui.components.ContainerObjectSelectionList, client.gui.components.StringWidget, client.gui.narration.NarratedElementType,
  client.gui.navigation.ScreenRectangle, client.gui.screens.GenericDirtMessageScreen, client.gui.screens.TitleScreen,
  client.gui.screens.worldselection.PresetEditor, client.renderer.texture.DynamicTexture, client.server.IntegratedServer,
  client.sounds.SoundManager, commands.SharedSuggestionProvider, core.Holder.Reference, core.HolderOwner, core.HolderSet.Named,
  core.LayeredRegistryAccess, core.MappedRegistry, core.RegistrySetBuilder, data.DataGenerator.PackGenerator,
  data.registries.RegistriesDatapackGenerator, data.tags.IntrinsicHolderTagsProvider, data.worldgen.Carvers,
  data.worldgen.features.FeatureUtils, data.worldgen.features.OreFeatures, data.worldgen.features.TreeFeatures,
  data.worldgen.placement.MiscOverworldPlacements, data.worldgen.placement.OrePlacements, data.worldgen.placement.TreePlacements,
  data.worldgen.placement.VegetationPlacements, nbt.CompoundTag, nbt.ListTag, nbt.NbtIo, nbt.NbtUtils, network.chat.ClickEvent,
  network.chat.ComponentUtils, network.chat.HoverEvent, resources.RegistryFileCodec, server.Bootstrap, server.Services, server.WorldStem,
  server.level.ChunkHolder, server.level.ChunkMap, server.level.ColumnPos, server.level.ThreadedLevelLightEngine,
  server.level.progress.ChunkProgressListener, server.level.progress.ChunkProgressListenerFactory, server.packs.PackResources,
  server.packs.PackType, server.packs.VanillaPackResources, server.packs.VanillaPackResourcesBuilder,
  server.packs.repository.BuiltInPackSource, server.packs.repository.Pack, server.packs.repository.Pack.ResourcesSupplier,
  server.packs.repository.PackSource, server.packs.repository.RepositorySource, server.packs.repository.ServerPacksSource,
  server.packs.resources.Resource, sounds.Music, tags.FluidTags, tags.StructureTags, util.CubicSpline, util.ToFloatFunction,
  util.thread.BlockableEventLoop, util.valueproviders.ConstantFloat, util.valueproviders.TrapezoidFloat, util.valueproviders.UniformFloat,
  world.effect.MobEffect, world.entity.EntityType, world.item.Item, world.item.ItemStack, world.item.enchantment.Enchantment,
  world.level.EmptyBlockGetter, world.level.Level, world.level.NoiseColumn, world.level.StructureManager, world.level.WorldDataConfiguration,
  world.level.biome.AmbientMoodSettings, world.level.biome.BiomeSource, world.level.biome.BiomeSpecialEffects, world.level.biome.MobSpawnSettings,
  world.level.block.AirBlock, world.level.block.EntityBlock, world.level.block.FallingBlock, world.level.block.GrassBlock,
  world.level.block.LeavesBlock, world.level.block.SnowLayerBlock, world.level.block.SpreadingSnowyDirtBlock,
  world.level.block.entity.BlockEntity, world.level.block.state.properties.Property, world.level.chunk.ChunkStatus,
  world.level.chunk.LevelChunkSection, world.level.chunk.LightChunkGetter, world.level.dimension.BuiltinDimensionTypes,
  world.level.entity.ChunkStatusUpdateListener, world.level.levelgen.Beardifier, world.level.levelgen.DensityFunction.FunctionContext,
  world.level.levelgen.DensityFunction.NoiseHolder, world.level.levelgen.Heightmap.Types, world.level.levelgen.NoiseBasedChunkGenerator,
  world.level.levelgen.NoiseRouterData, world.level.levelgen.OreVeinifier, world.level.levelgen.PositionalRandomFactory,
  world.level.levelgen.SurfaceRules.LazyXZCondition, world.level.levelgen.SurfaceRules.SurfaceRule, world.level.levelgen.WorldGenerationContext,
  world.level.levelgen.carver.CanyonCarverConfiguration, world.level.levelgen.carver.CarverDebugSettings,
  world.level.levelgen.carver.CaveCarverConfiguration, world.level.levelgen.carver.WorldCarver, world.level.levelgen.feature.TreeFeature,
  world.level.levelgen.feature.WeightedPlacedFeature, world.level.levelgen.feature.configurations.RandomFeatureConfiguration,
  world.level.levelgen.feature.configurations.RandomPatchConfiguration, world.level.levelgen.feature.configurations.SimpleBlockConfiguration,
  world.level.levelgen.feature.stateproviders.BlockStateProvider, world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider,
  world.level.levelgen.feature.stateproviders.SimpleStateProvider, world.level.levelgen.feature.treedecorators.AlterGroundDecorator,
  world.level.levelgen.feature.treedecorators.BeehiveDecorator, world.level.levelgen.heightproviders.UniformHeight,
  world.level.levelgen.placement.BiomeFilter, world.level.levelgen.placement.BlockPredicateFilter, world.level.levelgen.placement.CaveSurface,
  world.level.levelgen.placement.CountPlacement, world.level.levelgen.placement.EnvironmentScanPlacement,
  world.level.levelgen.placement.HeightmapPlacement, world.level.levelgen.placement.InSquarePlacement, world.level.levelgen.placement.RarityFilter,
  world.level.levelgen.presets.WorldPreset, world.level.levelgen.structure.Structure.GenerationContext,
  world.level.levelgen.structure.Structure.GenerationStub, world.level.material.FluidState, world.level.material.Fluids,
  world.level.pathfinder.PathComputationType, world.level.storage.DimensionDataStorage, world.level.storage.LevelResource,
  world.level.storage.ServerLevelData, world.phys.Vec3
```
(All entries are prefixed `net.minecraft.`.)

### Distinct com.mojang classes imported (21)
```
153 serialization.Codec | 100 serialization.codecs.RecordCodecBuilder | 14 datafixers.util.Pair | 13 serialization.JsonOps
6 serialization.DataResult | 4 brigadier.CommandDispatcher | 4 serialization.Lifecycle | 2 blaze3d.platform.NativeImage
2 brigadier.exceptions.DynamicCommandExceptionType | 2 datafixers.DataFixer | 2 datafixers.util.Either | 2 serialization.DynamicOps
1 brigadier.Command | 1 brigadier.arguments.StringArgumentType | 1 brigadier.builder.RequiredArgumentBuilder | 1 brigadier.context.CommandContext
1 brigadier.exceptions.CommandSyntaxException | 1 logging.LogUtils | 1 serialization.DataResult.PartialResult | 1 serialization.Encoder | 1 serialization.MapCodec
```

---

## Dead code and oddities spotted (relevant when deciding what to port)
- `RegistryUtil.getBiomeModifierRegistry()` is `@ExpectPlatform` with no impl on either loader, and it is never called.
- The `MixinFabricDataGenerator$Pack` invoker is never called.
- `RTFFabric` implements `DataGeneratorEntrypoint`, but `fabric.mod.json` has no `fabric-datagen` entrypoint.
- `VolatileAirBlock` is never registered. `PresetPackSource` is an empty class.
- About 20 AW entries are unused (section 2).
- `MixinSurfaceSystem`'s `@ModifyVariable` is a no-op.
- `MixinSpawnFinder` affects all worlds.
- TB and WP mixins are declared both in the JSON and in `MixinPlugin.getMixins()`.
- `RTFBuiltinPackSource` points at a `datapacks` dir that doesn't exist in resources.
- `PerformanceConfig.read` is a stub.
- `night-config` is an unused compileOnly dependency.
- `TBNoiseRouterData` always writes `terrablender:uniqueness` into preset packs.
