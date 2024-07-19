package mekanism.common.network.to_client.container.property.list;

import java.util.List;
import javax.annotation.Nonnull;
import mekanism.common.content.filter.BaseFilter;
import mekanism.common.content.filter.IFilter;
import net.minecraft.network.FriendlyByteBuf;

public class FilterListPropertyData<FILTER extends IFilter<?>> extends ListPropertyData<FILTER> {

    public FilterListPropertyData(short property, @Nonnull List<FILTER> values) {
        super(property, ListType.FILTER, values);
    }

    @SuppressWarnings("unchecked")
    static <FILTER extends IFilter<?>> FilterListPropertyData<FILTER> read(short property, ListPropertyReader<FILTER> reader) {
        return new FilterListPropertyData<>(property, reader.apply(buf -> (FILTER) BaseFilter.readFromPacket(buf)));
    }

    @Override
    protected void writeListElement(FriendlyByteBuf buffer, FILTER value) {
        value.write(buffer);
    }
}