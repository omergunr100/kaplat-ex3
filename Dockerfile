FROM openjdk:17

RUN mkdir /app

COPY /target/kaplat-ex5.jar /app

WORKDIR /app

CMD java -jar kaplat-ex5.jar

