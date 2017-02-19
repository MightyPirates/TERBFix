package li.cil.terbfix;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.10.2")
public class FMLLoadingPluginTERBFix implements IFMLLoadingPlugin {
    private static final String[] asmTransformerClass = {
            "li.cil.terbfix.ClassTransformerTERBFix"
    };

    @Override
    public String[] getASMTransformerClass() {
        return asmTransformerClass;
    }

    @Override
    public String getModContainerClass() {
        return "li.cil.terbfix.ModContainerTERBFix";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
