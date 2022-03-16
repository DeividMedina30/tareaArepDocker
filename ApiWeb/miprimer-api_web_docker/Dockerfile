FROM openjdk:8

WORKDIR /DeividMedina30/balancer/bin

COPY /target/classes /DeividMedina30/balancer/bin/classes

COPY /target/dependency /DeividMedina30/balancer/bin/dependency

CMD ["java","-cp","./classes:./dependency/*","edu.escuelaing.arep.api.AppBalanceadorWebDocker"]