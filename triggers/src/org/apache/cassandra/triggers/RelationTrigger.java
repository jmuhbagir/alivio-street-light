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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;


public class RelationTrigger implements ITrigger
{
    private final Logger log = LoggerFactory.getLogger(RelationTrigger.class);
    private final ObjectMapper mapperObj = new ObjectMapper();

    private List<Mutation> list = new ArrayList<Mutation>();
 
    public Collection<Mutation> augment(Partition update)
    {
        list.clear();
        UnfilteredRowIterator it = update.unfilteredIterator();
        Row row = null;
        CFMetaData metadata = Schema.instance.getCFMetaData("thingsboard", "street_light_pooling");
        PartitionUpdate.SimpleBuilder pooling = null;
        List<Object> keyValues = null;
        Map map = null;
        
        while (it.hasNext()) {
           Unfiltered un = it.next();
           if(un.isRow()){
              row = update.getRow((Clustering)un.clustering());
              if(row.deletion().time().markedForDeleteAt() > Long.MIN_VALUE) {
		log.info("Deleted Row");
                pooling = PartitionUpdate.simpleBuilder(metadata,row.clustering().get(2));
                pooling.row()
                        .delete("asset_id")
                        .delete("asset_name")
                        .delete("device_status");

                list.add(pooling.buildAsMutation());
              }else {
              log.info("Updated Row");
              map = new HashMap<String,String>();

              pooling = PartitionUpdate.simpleBuilder(metadata,row.clustering().get(2));

              Iterator<ColumnDefinition> columns = row.columns().iterator();
              while(columns.hasNext()) {
                 ColumnDefinition columnDef = columns.next();
                 Cell cell = row.getCell(columnDef);
                 log.info("Adding cell {}",columnDef.name.toString());
                 if(!cell.isTombstone()){
                     try {
                     String value = ByteBufferUtil.string(cell.value());
                     if(value.startsWith("{")) {
                         Map map2 = mapperObj.readValue(value,new TypeReference<HashMap<String,String>>(){});
                         map.put(columnDef.name.toString(),map2.get("name"));
                     } else {
                         map.put(columnDef.name.toString(),value);
                     }
                     }catch(Exception e) { e.printStackTrace();}
                 }
               }

              if(pooling != null && map.size() > 0) {
                 keyValues = Utils.parsePrimaryKey(update.metadata(),update.partitionKey().getKey());
                 map.put("asset_id",keyValues.get(0).toString());
                 log.info("Adding key {}",keyValues.get(0).toString());
                 pooling.row()
                        .add("asset_id", map.get("asset_id"))
                        .add("asset_name",map.get("additional_info"))
                        .add("device_status", "joined");
                 list.add(pooling.buildAsMutation());
              }
            }
           }
        }

        return list;
    }
}
