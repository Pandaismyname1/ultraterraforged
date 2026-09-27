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
		assertEquals(Light.GREEN, fabric("c2me", "c2me-opts-accel-opencl", "noisiumed", "lithium", "modernfix", "sodium").light());
		// Noisium itself counts as much as its fork
		assertEquals(Light.GREEN, fabric("c2me", "c2me-opts-accel-opencl", "noisium", "lithium", "modernfix", "sodium").light());

		assertEquals(Light.RED, neoforge().light());
		assertEquals(Light.ORANGE, neoforge("sodium", "lithium", "modernfix", "alltheleaks").light());
		assertEquals(Light.YELLOW, neoforge("c2me", "noisiumed", "lithium", "modernfix", "alltheleaks", "sodium").light());
		// the NeoForge build of C2ME OpenCL loads under an id with underscores
		assertEquals(Light.GREEN, neoforge("c2me", "c2me_opts_accel_opencl", "noisiumed", "lithium", "modernfix", "alltheleaks", "sodium").light());
	}

	@Test
	void onlyModsWithABuildCount() {
		Report fabric = fabric();
		// AllTheLeaks is NeoForge only
		assertEquals(6, fabric.available());
		assertEquals(PerformanceMods.MODS.size(), fabric.statuses().size());
		assertTrue(!status(fabric, "allTheLeaks").isAvailable());
		// every mod has a NeoForge build on 1.21.1, C2ME included, so nothing needs Sinytra Connector
		assertEquals(7, neoforge().available());
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
	void theModsThemselvesAreRecommended() {
		assertEquals("Lithium", status(neoforge(), "lithium").recommended().name());
		assertEquals("Sodium", status(neoforge(), "sodium").recommended().name());
		assertEquals("Noisiumed", status(fabric(), "noisium").recommended().name());
		PerformanceMods.Status noisium = status(neoforge("noisium"), "noisium");
		assertNotNull(noisium.installed());
		assertEquals("Noisium", noisium.installed().name());
		assertNull(status(fabric(), "allTheLeaks").recommended());
	}
}
