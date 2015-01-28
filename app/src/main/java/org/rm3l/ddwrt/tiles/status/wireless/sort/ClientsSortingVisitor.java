package org.rm3l.ddwrt.tiles.status.wireless.sort;

import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.resources.Device;

import java.util.Set;

public interface ClientsSortingVisitor {

    @NotNull
    Set<Device> visit(@NotNull final Set<Device> devices);
}
