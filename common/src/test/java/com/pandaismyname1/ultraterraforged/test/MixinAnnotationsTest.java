package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.pandaismyname1.ultraterraforged.RTFCommon;

/**
 * The common mixins must keep a single @At in the injectors where older Mixin expects one. Compiled against Fabric's
 * Mixin 0.17.4 or later they get an array instead, and on Forge, MixinExtras up to 0.5.0 (bundled by ModernFix,
 * AllTheLeaks, Noisium and many others) crashes the game at startup with a ClassCastException.
 */
public class MixinAnnotationsTest {
	private static final Set<String> SINGLE_AT = Set.of(
		"Lorg/spongepowered/asm/mixin/injection/Redirect;",
		"Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
		"Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
		"Lorg/spongepowered/asm/mixin/injection/ModifyVariable;"
	);

	@Test
	@SuppressWarnings("unchecked")
	void injectorsHaveASingleAt() throws IOException, URISyntaxException {
		Path root = Path.of(RTFCommon.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve("com/pandaismyname1/ultraterraforged/mixin");
		List<String> wrong = new ArrayList<>();
		int checked = 0;
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : files.filter((f) -> f.toString().endsWith(".class")).toList()) {
				ClassNode node = new ClassNode();
				try (InputStream in = Files.newInputStream(file)) {
					new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
				}
				for (MethodNode method : node.methods) {
					for (List<AnnotationNode> annotations : new List[] { method.visibleAnnotations, method.invisibleAnnotations }) {
						if (annotations == null) {
							continue;
						}
						for (AnnotationNode annotation : annotations) {
							if (!SINGLE_AT.contains(annotation.desc)) {
								continue;
							}
							checked++;
							for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
								if (annotation.values.get(i).equals("at") && !(annotation.values.get(i + 1) instanceof AnnotationNode)) {
									wrong.add(node.name + "." + method.name);
								}
							}
						}
					}
				}
			}
		}
		System.out.println(checked + " injectors checked");
		assertTrue(checked > 10, "only found " + checked + " injectors to check");
		assertTrue(wrong.isEmpty(), "injectors with an array of @At: " + wrong);
	}
}
