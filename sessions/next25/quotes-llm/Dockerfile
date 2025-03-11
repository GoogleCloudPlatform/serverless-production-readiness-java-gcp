FROM bellsoft/liberica-openjre-debian:21-cds
COPY target/quotes-1.0.0.jar quotes-1.0.0.jar

RUN java -Djarmode=tools -jar /quotes-1.0.0.jar list-layers
RUN java -Djarmode=tools -jar /quotes-1.0.0.jar extract

RUN rm /quotes-1.0.0.jar
RUN java -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh -XX:ArchiveClassesAtExit=/application.jsa -jar /quotes-1.0.0/quotes-1.0.0.jar
ENTRYPOINT ["java","-Dspring.aot.enabled=true","-XX:SharedArchiveFile=/application.jsa","-jar","/quotes-1.0.0/quotes-1.0.0.jar"]