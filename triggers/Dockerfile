FROM thingsboard/cassandra:1.4.0
RUN sed -ri 's/(enable_user_defined_functions:).*/\1 true/' "$CASSANDRA_CONFIG/cassandra.yaml"
COPY build/thingsboard-triggers.jar /etc/cassandra/triggers/thingsboard-triggers.jar
