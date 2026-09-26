package raccoonman.reterraforged.test;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.mojang.serialization.Lifecycle;

import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;

// Boots vanilla and RTF's own registries without a mod loader. RegistryUtil.createRegistry is an
// @ExpectPlatform hook with no implementation in common, so it's stubbed while the registry holders initialize.
public final class TestBootstrap {
	private static boolean initialized;

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		try (MockedStatic<RegistryUtil> registryUtil = Mockito.mockStatic(RegistryUtil.class)) {
			registryUtil.when(() -> RegistryUtil.createRegistry(any())).thenAnswer((invocation) -> {
				return newRegistry(invocation.getArgument(0));
			});
			RTFBuiltInRegistries.bootstrap();
		}
		initialized = true;
	}

	@SuppressWarnings("unchecked")
	private static <T> MappedRegistry<T> newRegistry(ResourceKey<?> key) {
		return new MappedRegistry<>((ResourceKey<? extends net.minecraft.core.Registry<T>>) key, Lifecycle.stable());
	}
}
