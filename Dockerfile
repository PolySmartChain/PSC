FROM openjdk:11.0-jre

WORKDIR /app
ADD polysmartchain/build/libs/sunflower*.jar /app/app.jar
ADD polysmartchain/src/main/resources/code.json /app/code.json
ADD polysmartchain/src/main/resources/abi.json /app/abi.json
RUN mkdir -p /var/lib/psc
ENV SUNFLOWER_DATABASE_DIRECTORY /var/lib/psc
ENV SUNFLOWER_CONSENSUS_ABI /app/abi.json
ENV SUNFLOWER_CONSENSUS_CODE /app/code.json
CMD ["java", "-jar", "app.jar"]
