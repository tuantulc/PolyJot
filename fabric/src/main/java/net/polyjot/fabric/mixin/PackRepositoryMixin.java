package net.polyjot.fabric.mixin;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.polyjot.fabric.pack.FabricLivePackManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Shadow @Final @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void polyjot$injectDynamicPack(RepositorySource[] repositorySources, CallbackInfo ci) {
        Set<RepositorySource> mutableSources = new HashSet<>(this.sources);
        mutableSources.add(FabricLivePackManager.createRepositorySource());
        this.sources = mutableSources;
    }
}
