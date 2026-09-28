package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
		// no Noisium, AllTheLeaks or ModernFix for 26.2, and C2ME OpenCL is optional
		assertEquals(3, fabric.available());
		assertEquals(PerformanceMods.MODS.size(), fabric.statuses().size());
		assertTrue(!status(fabric, "noisium").isAvailable());
		assertTrue(!status(fabric, "modernFix").isAvailable());
		assertEquals(3, neoforge().available());
		assertTrue(!status(neoforge(), "modernFix").isAvailable());
		assertEquals("C2ME", status(neoforge(), "c2me").recommended().name());
	}

	@Test
	void optionalModsCountOnceInstalled() {
		assertNotNull(status(fabric(), "c2meOpenCl").recommended());
		Report withOpenCl = fabric("c2me", "c2me-opts-accel-opencl", "lithium", "sodium");
		assertEquals(4, withOpenCl.installed());
		assertEquals(4, withOpenCl.available());
		assertEquals(Light.GREEN, withOpenCl.light());
		// the NeoForge build loads under an id with underscores
		assertNotNull(status(neoforge("c2me_opts_accel_opencl"), "c2meOpenCl").installed());
		// and needs Java 25
		assertNull(status(PerformanceMods.report(PerformanceMods.FABRIC, (id) -> false, 21), "c2meOpenCl").recommended());
	}
}
