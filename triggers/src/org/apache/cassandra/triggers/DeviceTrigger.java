/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.triggers;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.nio.ByteBuffer;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.ByteBufferUtil;

public class DeviceTrigger implements ITrigger
{
    private final Logger log = LoggerFactory.getLogger(DeviceTrigger.class);
    
 
    public Collection<Mutation> augment(Partition update)
    {
        UnfilteredRowIterator it = update.unfilteredIterator();
        CFMetaData metadata = Schema.instance.getCFMetaData("thingsboard", "street_light_pooling");
        PartitionUpdate.SimpleBuilder pooling = null;

        while (it.hasNext()) {
           Unfiltered un = it.next();
           if(un.isRow()){
              Row row = update.getRow((Clustering)un.clustering());
              Map map = new HashMap<String,String>();
              pooling = PartitionUpdate.simpleBuilder(metadata,update.partitionKey().getKey());

              Iterator<ColumnDefinition> columns = row.columns().iterator();
              while(columns.hasNext()) {
                 ColumnDefinition columnDef = columns.next();
                 Cell cell = row.getCell(columnDef);
                 log.info("Adding column {}",columnDef.name.toString());
                 if(!cell.isTombstone()){
                    try {
                    map.put(columnDef.name.toString(),ByteBufferUtil.string(cell.value()));
                    }catch(Exception e) { e.printStackTrace();}
                 }
              }

              if(pooling != null && map.size() > 0) {
                 pooling.row().add("device_name", map.get("name"));
                 pooling.row().add("device_status", "inactive");
                 return Collections.singletonList(pooling.buildAsMutation());
             }
           }
        }

        return Collections.emptyList();
    }
}
