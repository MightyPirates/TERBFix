package li.cil.terbfix;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;

public class ModContainerTERBFix extends DummyModContainer {
    public ModContainerTERBFix() {
        super(buildModMetadata());
    }

    private static ModMetadata buildModMetadata() {
        final ModMetadata metadata = new ModMetadata();
        metadata.authorList.add("Sangar");
        metadata.modId = "terbfix";
        metadata.name = "TERBFix";
        metadata.version = "1.0.0";
        metadata.description = "Fixes a bug in how TileEntity render bounds are computed.";
        return metadata;
    }
}
