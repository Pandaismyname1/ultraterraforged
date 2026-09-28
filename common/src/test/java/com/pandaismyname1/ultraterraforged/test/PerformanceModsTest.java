package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.compat.performance.PerformanceMods;
import com.pandaismyname1.ultraterraforged.compat.performance.PerformanceMods.Light;
import com.pandaismyname1.ultraterraforged.compat.performance.PerformanceMods.Report;

public class PerformanceModsTest {

	// on Java 25, which every build runs on
	private static Report fabric(String... ids) {
		return PerformanceMods.report(PerformanceMods.FABRIC, Set.of(ids)::contains, 25);
	}

	private static Report neoforge(String... ids) {
		return PerformanceMods.report(PerformanceMods.NEOFORGE, Set.of(ids)::contains, 25);
	}

	private static PerformanceMods.Status status(Report report, String key) {
		return report.statuses().stream().filter((status) -> status.mod().key().equals(key)).findFirst().orElseThrow();
	}

	@Test
	void lightFollowsWhatsInstalled() {
		assertEquals(Light.RED, fabric().light());
		assertEquals(Light.ORANGE, fabric("sodium", "lithium").light());
		assertEquals(Light.YELLOW, fabric("c2me").light());
		assertEquals(Light.GREEN, fabric("c2me", "lithium", "sodium").light());

		assertEquals(Light.RED, neoforge().light());
		assertEquals(Light.ORANGE, neoforge("sodium", "lithium").light());
		assertEquals(Light.YELLOW, neoforge("c2me").light());
		assertEquals(Light.GREEN, neoforge("c2me", "lithium", "sodium").light());
	}

	@Test
	void onlyModsWithABuildCount() {
		Report fabric = fabric();
		// no Noisium, AllTheLeaks, ModernFix or C2ME OpenCL for 26.3
		assertEquals(3, fabric.available());
		assertEquals(PerformanceMods.MODS.size(), fabric.statuses().size());
		assertTrue(!status(fabric, "noisium").isAvailable());
		assertTrue(!status(fabric, "modernFix").isAvailable());
		assertEquals(3, neoforge().available());
		assertTrue(!status(neoforge(), "modernFix").isAvailable());
		assertEquals("C2ME", status(neoforge(), "c2me").recommended().name());
	}

	@Test
	void optionalModsWithoutABuildDontCount() {
		// no C2ME OpenCL for 26.3: listed, greyed out, and not asked for
		assertNull(status(fabric(), "c2meOpenCl").recommended());
		assertNull(status(neoforge(), "c2meOpenCl").recommended());
		assertEquals(Light.GREEN, fabric("c2me", "lithium", "sodium").light());
	}
}
