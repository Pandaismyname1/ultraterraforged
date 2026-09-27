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

	private static Report fabric(String... ids) {
		return PerformanceMods.report(PerformanceMods.FABRIC, Set.of(ids)::contains);
	}

	private static Report forge(String... ids) {
		return PerformanceMods.report(PerformanceMods.FORGE, Set.of(ids)::contains);
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

		assertEquals(Light.RED, forge().light());
		assertEquals(Light.ORANGE, forge("embeddium", "radium", "modernfix", "alltheleaks").light());
		// green on Forge doesn't need C2ME, which has no Forge build, unless Connector is there to run it
		assertEquals(Light.GREEN, forge("noisiumed", "radium", "modernfix", "alltheleaks", "embeddium").light());
		assertEquals(Light.YELLOW, forge("connector", "noisiumed", "radium", "modernfix", "alltheleaks", "embeddium").light());
		assertEquals(Light.GREEN, forge("connector", "c2me", "noisiumed", "radium", "modernfix", "alltheleaks", "embeddium").light());
		assertEquals(Light.GREEN, forge("noisiumed", "canary", "modernfix", "alltheleaks", "embeddium").light());
	}

	@Test
	void onlyModsWithABuildCount() {
		Report fabric = fabric();
		// C2ME OpenCL has nothing for 1.20.1, and AllTheLeaks is Forge only
		assertEquals(5, fabric.available());
		assertEquals(PerformanceMods.MODS.size(), fabric.statuses().size());
		assertTrue(!status(fabric, "c2meOpenCl").isAvailable());
		assertTrue(!status(fabric, "allTheLeaks").isAvailable());
		assertEquals(5, forge().available());
		// C2ME runs on Forge only through Sinytra Connector
		assertTrue(!status(forge(), "c2me").isAvailable());
		assertEquals(6, forge("connector").available());
		assertEquals("C2ME", status(forge("connector"), "c2me").recommended().name());
		assertNotNull(status(forge("connector", "c2me"), "c2me").installed());
	}

	@Test
	void standInsAreRecommendedWhereTheModHasNoBuild() {
		assertEquals("Radium", status(forge(), "lithium").recommended().name());
		assertEquals("Embeddium", status(forge(), "sodium").recommended().name());
		assertEquals("Noisiumed", status(fabric(), "noisium").recommended().name());
		PerformanceMods.Status canary = status(forge("canary"), "lithium");
		assertNotNull(canary.installed());
		assertEquals("Canary", canary.installed().name());
		assertNull(status(fabric(), "c2meOpenCl").recommended());
	}
}
