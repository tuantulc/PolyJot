package net.polyjot.platform;

import java.nio.file.Path;

public interface DiscoveredMod {
    String getModId();
    String getDisplayName();
    Path findResource(String... pathParts);
}
