package org.apache.cassandra.triggers;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CompositeType;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Utils {

   public static List<Object> parsePrimaryKey(final CFMetaData cfMetaData, final ByteBuffer partitionKey) {
        final List<Object> keyValue = new ArrayList<>(cfMetaData.partitionKeyColumns().size());
        final AbstractType<?> keyDataType = cfMetaData.getKeyValidator();

        if (keyDataType instanceof CompositeType) {
            final ByteBuffer[] components = ((CompositeType) cfMetaData.getKeyValidator()).split(partitionKey);
            cfMetaData.partitionKeyColumns();
            for (int i = 0; i < cfMetaData.partitionKeyColumns().size(); i++) {
                final ColumnDefinition colDef = cfMetaData.partitionKeyColumns().get(i);
                //logDebug("Partition Key ::: " + colDef.debugString)
                keyValue.add(colDef.cellValueType().compose(components[i]));
            }
        } else {
            final ColumnDefinition colDef = cfMetaData.partitionKeyColumns().get(0);
            keyValue.add(colDef.cellValueType().compose(partitionKey));
        }

        return keyValue;
    }
}
