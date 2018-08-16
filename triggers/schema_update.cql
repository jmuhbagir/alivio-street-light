CREATE TABLE thingsboard.street_light_pooling (
    device_id uuid PRIMARY KEY,
    asset_id text,
    asset_name text,
    device_name text,
    device_status text,
    telemetry map<timestamp, text>
);

CREATE MATERIALIZED VIEW thingsboard.street_light_pooling_by_cluster AS
    SELECT asset_name,device_id,asset_id,device_status,telemetry
    FROM thingsboard.street_light_pooling
    WHERE asset_name IS NOT NULL
    PRIMARY KEY (asset_name,device_id);

CREATE TRIGGER device_trigger ON thingsboard.device USING 'org.apache.cassandra.triggers.DeviceTrigger';
CREATE TRIGGER relation_trigger ON thingsboard.relation USING 'org.apache.cassandra.triggers.RelationTrigger';
CREATE TRIGGER telemetry_trigger ON thingsboard.ts_kv_latest_cf USING 'org.apache.cassandra.triggers.TelemetryTrigger';

CREATE FUNCTION IF NOT EXISTS thingsboard.fnEnergy (frames map<timestamp,text>, kwh Boolean)
   CALLED ON NULL INPUT
   RETURNS list<double>
   LANGUAGE java AS '
     List<Double> list = new ArrayList<Double>();
     double value = 0;
     for(Map.Entry<Date,String> e : frames.entrySet()){
        value = Integer.parseInt(e.getValue().substring(8,12),16);
        if(kwh){
           value = value/1000;
        }
        list.add(value);
     }

     return list;
   ';


CREATE FUNCTION IF NOT EXISTS thingsboard.fnSumEnergy (type text, frames map<timestamp,text>) 
   CALLED ON NULL INPUT 
   RETURNS double 
   LANGUAGE java AS '
     double sum = 0;
     double value = 0;
     double range = 0;
     java.time.LocalDateTime now = java.time.LocalDateTime.now();
     java.time.LocalDateTime my = null;
     java.time.LocalDateTime hour = null;

     if(type.equals("day"))
        range = 24;
     else if(type.equals("week"))
        range = 168;
     else
        range = 1;
     
     for(int i=1; i<=range; i++) {
       int count = 0;
       double avg = 0;
       hour = java.time.LocalDateTime.now().minusHours(i);
       for(Map.Entry<Date,String> e : frames.entrySet()){
          my = e.getKey().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
          if(my.compareTo(hour) == 1 && my.compareTo(now) == -1){
             value = Integer.parseInt(e.getValue().substring(8,12),16);

             avg = avg + value;
             count++;
          }
       }

       if(avg>0) sum = sum + (avg/count);
     }

     double result = Math.round(sum*100)/100;
     return sum;
     ';

     
CREATE FUNCTION IF NOT EXISTS thingsboard.fnSumCost (type text, frames map<timestamp,text>, cost double)
   CALLED ON NULL INPUT
   RETURNS double
   LANGUAGE java AS '
     double sum = 0;
     double value = 0;
     double range = 0;
     java.time.LocalDateTime now = java.time.LocalDateTime.now();
     java.time.LocalDateTime my = null;
     java.time.LocalDateTime hour = null;
     
     if(type.equals("day"))
        range = 24;
     else if(type.equals("week"))
        range = 168;
     else
        range = 1;

     for(int i=1; i<=range; i++) {
       int count = 0;
       double avg = 0;
       hour = java.time.LocalDateTime.now().minusHours(i);
       for(Map.Entry<Date,String> e : frames.entrySet()){
          my = e.getKey().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
          if(my.compareTo(hour) == 1 && my.compareTo(now) == -1){
             value = Integer.parseInt(e.getValue().substring(8,12),16);

             avg = avg + value;
             count++;
          }
       }

       if(avg > 0) sum = sum + (avg/count);
     }

     double result = Math.round(sum*cost*100)/100;
     return sum*cost;
     '; 
