package mekanism.common.recipe.condition;

import com.google.gson.JsonObject;
import mekanism.common.Mekanism;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class ModVersionLoadedCondition implements ICondition {

    private static final ResourceLocation NAME = Mekanism.rl("mod_version_loaded");
    private final String minVersion;
    private final String modid;

    public ModVersionLoadedCondition(String modid, String version) {
        this.modid = modid;
        this.minVersion = version;
    }

    @Override
    public ResourceLocation getID() {
        return NAME;
    }

    @Override
    @SuppressWarnings("removal")
    public boolean test() {
        return test(IContext.EMPTY);
    }

    @Override
    public boolean test(IContext context) {
        //They match or we are ahead of the min version
        return ModList.get().getModContainerById(modid).filter(modContainer -> new ComparableVersion(minVersion).compareTo(
              new ComparableVersion(modContainer.getModInfo().getVersion().toString())) <= 0).isPresent();
    }

    @Override
    public String toString() {
        return "mod_version_loaded(\"" + modid + "\", \"" + minVersion + "\")";
    }

    public static class Serializer implements IConditionSerializer<ModVersionLoadedCondition> {

        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ModVersionLoadedCondition value) {
            json.addProperty("modid", value.modid);
            json.addProperty("minVersion", value.minVersion);
        }

        @Override
        public ModVersionLoadedCondition read(JsonObject json) {
            return new ModVersionLoadedCondition(GsonHelper.getAsString(json, "modid"), GsonHelper.getAsString(json, "minVersion"));
        }

        @Override
        public ResourceLocation getID() {
            return ModVersionLoadedCondition.NAME;
        }
    }
}