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
		assertEquals(Light.ORANGE, fabric("sodium", "lithium", "modernfix").light());
		assertEquals(Light.YELLOW, fabric("c2me").light());
		assertEquals(Light.GREEN, fabric("c2me", "noisiumed", "lithium", "modernfix", "sodium").light());
		// Noisium itself counts as much as its fork
		assertEquals(Light.GREEN, fabric("c2me", "noisium", "lithium", "modernfix", "sodium").light());

		assertEquals(Light.RED, neoforge().light());
		assertEquals(Light.ORANGE, neoforge("sodium", "lithium", "modernfix", "alltheleaks").light());
		assertEquals(Light.YELLOW, neoforge("c2me", "noisiumed", "lithium", "modernfix", "alltheleaks").light());
		assertEquals(Light.GREEN, neoforge("c2me", "noisiumed", "lithium", "modernfix", "alltheleaks", "sodium").light());
		// Embeddium stands in for Sodium on NeoForge
		assertEquals(Light.GREEN, neoforge("c2me", "noisiumed", "lithium", "modernfix", "alltheleaks", "embeddium").light());
	}

	@Test
	void onlyModsWithABuildCount() {
		Report fabric = fabric();
		// AllTheLeaks is NeoForge only, and C2ME OpenCL is optional
		assertEquals(5, fabric.available());
		assertEquals(PerformanceMods.MODS.size(), fabric.statuses().size());
		assertTrue(!status(fabric, "allTheLeaks").isAvailable());
		// every mod has a NeoForge build on 1.21.1, C2ME included, so nothing needs Sinytra Connector
		assertEquals(6, neoforge().available());
		assertEquals("C2ME", status(neoforge(), "c2me").recommended().name());
		assertNotNull(status(neoforge("c2me"), "c2me").installed());
	}

	@Test
	void buildsForANewerJavaDontCount() {
		// C2ME OpenCL needs Java 25; the launcher runs 1.21.1 on Java 21
		Report fabric = PerformanceMods.report(PerformanceMods.FABRIC, Set.of("c2me", "noisiumed", "lithium", "modernfix", "sodium")::contains, 21);
		assertEquals(5, fabric.available());
		assertEquals(Light.GREEN, fabric.light());
		assertTrue(!status(fabric, "c2meOpenCl").isAvailable());
		assertEquals(6, PerformanceMods.report(PerformanceMods.NEOFORGE, (id) -> false, 21).available());
	}

	@Test
	void optionalModsCountOnceInstalled() {
		// C2ME OpenCL isn't required for green (it can't run next to Noisium), but counts when it's there
		assertNotNull(status(fabric(), "c2meOpenCl").recommended());
		Report withOpenCl = fabric("c2me", "c2me-opts-accel-opencl", "lithium", "modernfix", "sodium");
		assertEquals(5, withOpenCl.installed());
		assertEquals(6, withOpenCl.available());
		assertEquals(Light.YELLOW, withOpenCl.light());
		// the NeoForge build loads under an id with underscores
		assertNotNull(status(neoforge("c2me_opts_accel_opencl"), "c2meOpenCl").installed());
	}

	@Test
	void theModsThemselvesAreRecommended() {
		assertEquals("Lithium", status(neoforge(), "lithium").recommended().name());
		assertEquals("Sodium", status(neoforge(), "sodium").recommended().name());
		assertEquals("Embeddium", status(neoforge("embeddium"), "sodium").installed().name());
		assertEquals("Noisiumed", status(fabric(), "noisium").recommended().name());
		PerformanceMods.Status noisium = status(neoforge("noisium"), "noisium");
		assertNotNull(noisium.installed());
		assertEquals("Noisium", noisium.installed().name());
		assertNull(status(fabric(), "allTheLeaks").recommended());
	}
}
