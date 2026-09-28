package com.pandaismyname1.ultraterraforged.data.preset.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Per structure set overrides. Only changed sets are stored, and within a set only the changed values; everything
 * else comes from the structure set as registered by vanilla, the datapacks and other mods.
 */
public class StructureSettings {
	public static final Codec<StructureSettings> CODEC = Codec.unboundedMap(Identifier.CODEC, StructureSetEntry.CODEC).xmap(StructureSettings::new, (settings) -> new TreeMap<>(settings.entries));

	public final Map<Identifier, StructureSetEntry> entries;

	public StructureSettings(Map<Identifier, StructureSetEntry> entries) {
		this.entries = new HashMap<>();
		entries.forEach((key, entry) -> this.entries.put(key, entry.copy()));
	}

	public StructureSettings() {
		this(Map.of());
	}

	public Optional<StructureSetEntry> get(Identifier set) {
		return Optional.ofNullable(this.entries.get(set));
	}

	public StructureSetEntry getOrCreate(Identifier set) {
		return this.entries.computeIfAbsent(set, (key) -> new StructureSetEntry());
	}

	// drops the entry once it no longer overrides anything, so reset sets don't linger in the file
	public void clean(Identifier set) {
		StructureSetEntry entry = this.entries.get(set);
		if (entry != null && entry.isEmpty()) {
			this.entries.remove(set);
		}
	}

	public StructureSettings copy() {
		return new StructureSettings(this.entries);
	}

	public static class StructureSetEntry {
		public static final Codec<StructureSetEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled").forGetter((o) -> Optional.ofNullable(o.enabled)),
			Codec.intRange(1, 4096).optionalFieldOf("spacing").forGetter((o) -> Optional.ofNullable(o.spacing)),
			Codec.intRange(0, 4095).optionalFieldOf("separation").forGetter((o) -> Optional.ofNullable(o.separation)),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency").forGetter((o) -> Optional.ofNullable(o.frequency)),
			Codec.INT.optionalFieldOf("salt").forGetter((o) -> Optional.ofNullable(o.salt)),
			Codec.intRange(0, 1023).optionalFieldOf("distance").forGetter((o) -> Optional.ofNullable(o.distance)),
			Codec.intRange(0, 1023).optionalFieldOf("spread").forGetter((o) -> Optional.ofNullable(o.spread)),
			Codec.intRange(1, 4095).optionalFieldOf("count").forGetter((o) -> Optional.ofNullable(o.count))
		).apply(instance, (enabled, spacing, separation, frequency, salt, distance, spread, count) -> {
			return new StructureSetEntry(enabled.orElse(null), spacing.orElse(null), separation.orElse(null), frequency.orElse(null), salt.orElse(null), distance.orElse(null), spread.orElse(null), count.orElse(null));
		}));

		// null means "as registered"
		@Nullable public Boolean enabled;
		// random spread placement
		@Nullable public Integer spacing;
		@Nullable public Integer separation;
		@Nullable public Float frequency;
		@Nullable public Integer salt;
		// concentric rings placement (strongholds)
		@Nullable public Integer distance;
		@Nullable public Integer spread;
		@Nullable public Integer count;

		public StructureSetEntry(@Nullable Boolean enabled, @Nullable Integer spacing, @Nullable Integer separation, @Nullable Float frequency, @Nullable Integer salt, @Nullable Integer distance, @Nullable Integer spread, @Nullable Integer count) {
			this.enabled = enabled;
			this.spacing = spacing;
			this.separation = separation;
			this.frequency = frequency;
			this.salt = salt;
			this.distance = distance;
			this.spread = spread;
			this.count = count;
		}

		public StructureSetEntry() {
			this(null, null, null, null, null, null, null, null);
		}

		public boolean isEmpty() {
			return this.enabled == null && this.spacing == null && this.separation == null && this.frequency == null && this.salt == null && this.distance == null && this.spread == null && this.count == null;
		}

		public StructureSetEntry copy() {
			return new StructureSetEntry(this.enabled, this.spacing, this.separation, this.frequency, this.salt, this.distance, this.spread, this.count);
		}
	}
}
