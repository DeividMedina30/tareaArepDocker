FROM openjdk:8

WORKDIR /tareaArepDocker/back/bin

COPY /target/classes /tareaArepDocker/back/bin/classes

COPY /target/dependency /tareaArepDocker/back/bin/dependency

CMD ["java","-cp","./classes:./dependency/*","edu.escuelaing.arep.mongo.ConexionConMongo"]