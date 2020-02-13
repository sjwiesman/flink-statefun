FROM statefun

RUN mkdir -p /opt/statefun/modules/statefun-workshop-protocol
RUN mkdir -p /opt/statefun/modules/statefun-workshop-io
RUN mkdir -p /opt/statefun/modules/statefun-workshop-functions

COPY statefun-workshop-protocol/target/statefun-workshop-protocol*jar /opt/statefun/modules/statefun-workshop-protocol/
COPY statefun-workshop-io/target/statefun-workshop-io*jar /opt/statefun/modules/statefun-workshop-io/
COPY statefun-workshop-functions/target/statefun-workshop-functions*jar /opt/statefun/modules/statefun-workshop-functions/
