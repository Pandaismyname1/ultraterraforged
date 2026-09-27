import json, sys
vs = ['1.20.1', '1.21.1', '26.1', '26.1.2', '26.2', '26.3']
D = {v: json.load(open(f'{sys.argv[1]}/maps/{v}.json')) for v in vs}
n = 'net/minecraft/'
T = [
 ('MixinBiomeGenerationSettings', 'world/level/biome/BiomeGenerationSettings', 'f', 'features'),
 ('MixinBiomeGenerationSettings', 'world/level/biome/BiomeGenerationSettings', 'f', 'flowerFeatures'),
 ('MixinChunkMap', 'server/level/ChunkMap', 'm', '<init>'),
 ('MixinChunkMap', 'server/level/ChunkMap', 'f', 'randomState'),
 ('MixinChunkStatus 1.21+ equiv', 'world/level/chunk/status/ChunkStatusTasks', 'm', 'generateStructureStarts'),
 ('MixinChunkStatus 1.21+ equiv', 'world/level/chunk/status/ChunkStatusTasks', 'm', 'generateFeatures'),
 ('MixinChunkStatus 1.21+ equiv', 'world/level/chunk/status/ChunkStatusTasks', 'm', 'generateNoise'),
 ('MixinChunkStatus 1.21+ equiv', 'world/level/chunk/status/ChunkStatusTasks', 'm', 'generateSurface'),
 ('MixinChunkStatus 1.21+ equiv', 'world/level/chunk/status/ChunkStatusTasks', 'm', 'generateTerrain'),
 ('MixinClimateSampler', 'world/level/biome/Climate$Sampler', 'm', 'sample'),
 ('MixinClimateSampler', 'world/level/biome/Climate$Sampler', 'm', 'findSpawnPosition'),
 ('MixinSpawnFinder', 'world/level/biome/Climate', 'm', 'findSpawnPosition'),
 ('MixinContext', 'world/level/levelgen/SurfaceRules$Context', 'm', '<init>'),
 ('MixinContext', 'world/level/levelgen/SurfaceRules$Context', 'f', 'chunk'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'm', 'init'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'm', 'onCreate'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'm', 'tick'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'f', 'recreated'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'f', 'uiState'),
 ('MixinCreateWorldScreen', 'client/gui/screens/worldselection/CreateWorldScreen', 'f', 'tabNavigationBar'),
 ('MixinCreateWorldScreen', 'client/gui/components/tabs/TabNavigationBar$Builder', 'm', 'addTabs'),
 ('MixinMinecraftServer', 'server/MinecraftServer', 'm', 'setInitialSpawn'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'm', 'buildSurface'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'm', 'fillFromNoise'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'm', 'doFill'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'm', 'iterateNoiseColumn'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'm', 'addDebugScreenInfo'),
 ('MixinNoiseBasedChunkGenerator', 'world/level/levelgen/NoiseBasedChunkGenerator', 'f', 'settings'),
 ('(INVOKE target)', 'world/level/levelgen/NoiseSettings', 'm', 'height'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'm', '<init>'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'm', 'wrapNew'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'm', 'wrap'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'm', 'computePreliminarySurfaceLevel'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'm', 'cachedClimateSampler'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'initialDensityNoJaggedness'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'firstNoiseX'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'firstNoiseZ'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'cellCountXZ'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'cellCountY'),
 ('MixinNoiseChunk', 'world/level/levelgen/NoiseChunk', 'f', 'cellHeight'),
 ('(INVOKE target)', 'world/level/levelgen/RandomState', 'm', 'router'),
 ('MixinRandomState', 'world/level/levelgen/RandomState', 'm', '<init>'),
 ('MixinRandomState', 'world/level/levelgen/RandomState', 'f', 'sampler'),
 ('MixinRandomState', 'world/level/levelgen/RandomState', 'f', 'surfaceSystem'),
 ('MixinRandomState', 'world/level/levelgen/RandomState', 'f', 'random'),
 ('(INVOKE target)', 'world/level/levelgen/NoiseRouter', 'm', 'mapAll'),
 ('MixinRegistryDataLoader', 'resources/RegistryDataLoader', 'm', 'loadRegistryContents'),
 ('MixinRegistryDataLoader', 'resources/RegistryDataLoader', 'm', 'loadElementFromResource'),
 ('(INVOKE target)', 'server/packs/resources/Resource', 'm', 'isBuiltin'),
 ('(INVOKE target)', 'server/packs/resources/Resource', 'm', 'knownPackInfo'),
 ('MixinServerPacksSource', 'server/packs/repository/ServerPacksSource', 'm', 'createPackRepository'),
 ('MixinStructure', 'world/level/levelgen/structure/Structure', 'm', 'isValidBiome'),
 ('MixinSurfaceRules$BiomeConditionSource', 'world/level/levelgen/SurfaceRules$BiomeConditionSource', 'm', 'apply'),
 ('MixinSurfaceRules$BiomeConditionSource', 'world/level/levelgen/SurfaceRules$BiomeConditionSource', 'f', 'biomeNameTest'),
 ('MixinSurfaceSystem', 'world/level/levelgen/SurfaceSystem', 'm', '<init>'),
 ('MixinSurfaceSystem', 'world/level/levelgen/SurfaceSystem', 'm', 'buildSurface'),
 ('MixinUtil', 'Util', 'm', 'shutdownExecutors'),
 ('MixinUtil', 'Util', 'm', 'shutdownExecutor'),
 ('MixinUtil 26.x', 'util/Util', 'm', 'shutdownExecutors'),
 ('MixinUtil 26.x', 'util/Util', 'm', 'shutdownExecutor'),
 ('ScreenInvoker', 'client/gui/screens/Screen', 'm', 'addRenderableWidget'),
 ('terrablender.MixinParameterList', 'world/level/biome/Climate$ParameterList', 'm', 'findValuePositional'),
 ('terrablender.MixinTargetPoint', 'world/level/biome/Climate$TargetPoint', 'm', '<init>'),
 ('fabric MixinPresetEditor', 'client/gui/screens/worldselection/PresetEditor', 'm', '<clinit>'),
 ('forge PlainBuilder accessor', 'world/level/biome/BiomeGenerationSettings$PlainBuilder', 'f', 'features'),
 ('MinecraftServer reloadResources', 'server/MinecraftServer', 'm', 'reloadResources'),
]
extra = sys.argv[2:]  # optional: owner.name.kind triples
def show(v, own, kind, name):
    c = D[v].get(n + own)
    if c is None:
        return 'CLASS GONE'
    if kind == 'f':
        f = c['fields'].get(name)
        return f[0] if f else 'MISSING'
    ms = [k[len(name):] for k in c['methods'] if k.startswith(name + '(')]
    return ' | '.join(sorted(ms)) if ms else 'MISSING'
for who, own, kind, name in T:
    print(f'## {who}: {own.replace("/", ".")}.{name} [{kind}]')
    last = None
    for v in vs:
        s = show(v, own, kind, name)
        for p in ('Lnet/minecraft/', 'Ljava/util/', 'Ljava/lang/'):
            s = s.replace(p, '')
        if s != last:
            print(f'   {v:7} {s[:700]}')
        last = s
