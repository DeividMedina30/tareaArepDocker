# AREP- ARQUITECTURAS EMPRESARIAL.

## AREP- TAREA DE DE MODULARIZACIÓN CON VIRTUALIZACIÓN DOCKER Y A AWS

### INTRODUCCIÓN.

1. El servicio MongoDB es una instancia de MongoDB corriendo en un container de docker en una máquina virtual de EC2
2. LogService es un servicio REST que recibe una cadena, la almacena en la base de datos y responde en un objeto JSON con las 10 ultimas cadenas almacenadas en la base de datos y la fecha en que fueron almacenadas.
3. La aplicación web APP-LB-RoundRobin está compuesta por un cliente web y al menos un servicio REST. El cliente web tiene un campo y un botón y cada vez que el usuario envía un mensaje, este se lo envía al servicio REST y actualiza la pantalla con la información que este le regresa en formato JSON. El servicio REST recibe la cadena e implementa un algoritmo de balanceo de cargas de Round Robin, delegando el procesamiento del mensaje y el retorno de la respuesta a cada una de las tres instancias del servicio LogService.

![imagen1.png](https://i.postimg.cc/zGz0RCTB/imagen1.png)

### Creando Proyecto.

**Cree un proyecto java usando maven**

Para crear el proyecto hacemos uso del siguiente comando.
Abrimos primero una consola cmd y ejecutamos el siguiente codigo. Dentro de la carpeta
tareaArepDocker, la cual tendra dos proyectos, uno sera la conexión con mongo y otro la api.

```Crear proyecto
mvn archetype:generate -DgroupId=edu.escuelaing.arep.miprimer-api_web_docker -DartifactId = miprimer-api_web_docker -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

```Crear proyecto2
mvn archetype:generate -DgroupId=edu.escuelaing.arep.miprimer-conexion-mongo -DartifactId = miprimer-conexion-mongo -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```


**Cree Conexión con MongoDB**

Cree la clase CenexionConMongo.java en el directorio Mongo.miprimer-conexion-mongo.src.main.java.edu.escuelaing.arep.mongo

```CrearClaseMongo
package edu.escuelaing.arep.mongo;
import static spark.Spark.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.*;

import javax.swing.*;


public class ConexionConMongo
{
    private static SimpleDateFormat fechaDelDato = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static MongoClient clienteMongo;
    private static MongoDatabase database;
    private static MongoCollection<Document> coleccionDatos;
    private static ArrayList<String> respuesta = new ArrayList<>();

    public static void main(String[] args) {
        port(getPort());
        post("/",(req, res)->{
            res.type("application/json");
            return conexionMongo(req.queryParams("value"));
        });
    }

    private static String conexionMongo(String palabra){
        try {
            clienteMongo = new MongoClient("db");
            database = clienteMongo.getDatabase("Lista");
            coleccionDatos = database.getCollection("datos");
            if(coleccionDatos.countDocuments()==10){ // Mirar si hay al menos diez datos, si hay más de diez eliminar dato.
                coleccionDatos.deleteOne(Filters.eq("id",0));
                Document updated = new Document().append("$inc", new Document().append("id", -1));
                coleccionDatos.updateMany(Filters.gt("id",0),updated);
            }
            insertarDatoMongo(palabra);
            mostrarDatosMongo();
            clienteMongo.close();
        }catch (MongoException e){
            JOptionPane.showMessageDialog(null,"Error con la conexión de la base de datos MongoDB, error: " + e.toString());
        }
        return Arrays.toString(respuesta.toArray(new String[respuesta.size()]));
    }

    private static void insertarDatoMongo(String palabra){
        //insertOne(), se utiliza para insertar un solo documento o registro en la base de datos.
        coleccionDatos.insertOne(new Document().append("fecha",fechaDelDato.format(new Date())).append("value", palabra).append("id",(int)coleccionDatos.countDocuments()));
    }

    private static void mostrarDatosMongo(){
        // forEach en MongoDB, nos permite recorrer los documentos de una consulta de una forma sencilla y sin tener que realizar un bucle.
        coleccionDatos.find().forEach((Consumer<Document>) (Document d) -> { d.remove("_id");d.remove("id");respuesta.add(d.toJson());});
    }

    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 4567;
    }
}

```

**Cree Clase AppBalanceadorWebDocker.java**

Cree la clase AppBalanceadorWebDocker.java en el directorio ApiWeb.miprimer-api_web_docker.src.main.java.edu.escuelaing.arep.mongo

```CrearClaseBalanceador
package edu.escuelaing.arep.api;

import java.io.*;
import java.net.*;
import static spark.Spark.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AppBalanceadorWebDocker
{
    private static ConcurrentLinkedQueue<String> cola = new ConcurrentLinkedQueue<String>() {
        {
            add("1");
            add("2");
            add("3");
        }
    };

    public static void main( String[] args )
    {
        staticFiles.location("/public");
        port(getPort());
        post("/balancer", (req, res) -> {
            res.header("Access-Control-Allow-Origin","*");
            res.type("application/json");
            return balanceardor(req.queryParams("value"));
        });
    }

    private static String balanceardor(String value) {
        String temp = cola.poll();
        cola.add(temp);
        return doPost(value, temp);
    }

    private static String doPost(String value, String temp) {
        String linea = "";
        try {
            String data = "value="+value;
            System.out.println("Docker en ejecucusión: " + temp);
            URL url = new URL("http://backend"+temp+":3500"+temp);
            System.out.println("URL, actual backend: " + url.toString());
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("POST");
            conexion.setDoOutput(true);
            conexion.getOutputStream().write(data.getBytes("UTF-8"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
            linea = reader.readLine();
            reader.close();
        } catch (MalformedURLException me) {
            System.err.println("MalformedURLException: " + me.toString());
        } catch (IOException ioe) {
            System.err.println("IOException:  " + ioe.toString());
        }
        return linea;
    }

    private static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 4567;
    }
}

```

**Cree Dockerfile de la Api**

```CrearDockerFileApi
FROM openjdk:8

WORKDIR /DeividMedina30/balancer/bin

COPY /target/classes /DeividMedina30/balancer/bin/classes

COPY /target/dependency /DeividMedina30/balancer/bin/dependency

CMD ["java","-cp","./classes:./dependency/*","edu.escuelaing.arep.api.AppBalanceadorWebDocker"]
```

**Cree Dockerfile de MongoDB**

```Crear proyecto
FROM openjdk:8

WORKDIR /tareaArepDocker/back/bin

COPY /target/classes /tareaArepDocker/back/bin/classes

COPY /target/dependency /tareaArepDocker/back/bin/dependency

CMD ["java","-cp","./classes:./dependency/*","edu.escuelaing.arep.mongo.ConexionConMongo"]
```

**Cree docker-compese.yml**

```DcoekrCompese.yml
version: '2'

services:
  backend1:
    environment:
      - PORT=35001
    build:
      context: ./Mongo/miprimer-conexion-mongo
      dockerfile: Dockerfile
    container_name: back1
    links:
      - db
  backend2:
    environment:
      - PORT=35002
    build:
      context: ./Mongo/miprimer-conexion-mongo
      dockerfile: Dockerfile
    container_name: back2
    links:
      - db
  backend3:
    environment:
      - PORT=35003
    build:
      context: ./Mongo/miprimer-conexion-mongo
      dockerfile: Dockerfile
    container_name: back3
    links:
      - db
  balance:
    environment:
      - PORT=35000
    build:
      context: ./ApiWeb/miprimer-api_web_docker
      dockerfile: Dockerfile
    container_name: balance
    ports:
      - "35000:35000"
    links:
      - backend1
      - backend2
      - backend3
  db:
    image: mongo:3.6.1
    container_name: db
    volumes:
      - mongodb:/data/db
      - mongodb_config:/data/configdb
    command: mongod
volumes:
  mongodb:
  mongodb_config:
```

**Agregar dependencias tanto en el pom de Api como de MongoDB**

```CrearDependencias
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.sparkjava</groupId>
      <artifactId>spark-core</artifactId>
      <version>2.9.3</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>1.7.35</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>1.7.35</version>
    </dependency>
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.8.9</version>
    </dependency>
    <dependency>
      <groupId>org.mongodb</groupId>
      <artifactId>mongo-java-driver</artifactId>
      <version>3.12.10</version>
    </dependency>
    <dependency>
      <groupId>org.webjars.bower</groupId>
      <artifactId>jquery</artifactId>
      <version>3.6.0</version>
    </dependency>
  </dependencies>
```

**Asegúrese que el proyecto esté compilando hacia la versión 8 de Java, en el archivo pom**

```CrearPropiedades
  <properties>
    <prohect.build.sourceEncoding>UTF-8</prohect.build.sourceEncoding>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
  </properties>
```

**Asegúrese que el proyecto este copiando las dependencias en el directorio target. En archivo pom.**

```CrearBuild
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.0</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.0.1</version>
        <executions>
          <execution>
            <id>copy-dependencies</id>
            <phase>package</phase>
            <goals><goal>copy-dependencies</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

**Compilar Proyecto.**

Para compilar el proyecto debemos dirigirnos a la ruta de la carpeta del proyecto y abrir un cmd.

```Compilar
    mvn clean install
```

**Verificar que se crearon las dependencias en la carpeta target.**

![Imagen20.png](https://i.postimg.cc/KYNzQSb6/Imagen20.png)

### Pasos AWS.

1. Acceda a la maquina virtual
2. Instalar java.
3. Instalar Docker.
4. Instalar docker-compose
5. Iniciando el servicio Docker.
6. Configure su usuario en el grupo de docker para no tener que ingresar “sudo” cada vez que invoca un comando
7. Desconectes de la máquina virtual e ingrese nuevamente para que la configuración de grupos de usuarios tenga efecto.
8. Ejecute el docker-compose.yml
9. Subir proyecto
10. Verifique que se creo correctamente.
11. Cree la nueva regla de seguridad.
12. Realizar Prueba

**Acceda a la máquina virtual.**

![cuartaparte1.png](https://i.postimg.cc/sXqdp4z7/cuartaparte1.png)

![cuartaparte2.png](https://i.postimg.cc/ZnNkjvYg/cuartaparte2.png)

![cuartaparte3.png](https://i.postimg.cc/sgV8GNWb/cuartaparte3.png)

![cuartaparte4.png](https://i.postimg.cc/htpYL1bv/cuartaparte4.png)

![Imagen2.png](https://i.postimg.cc/N0Vn3LQm/Imagen2.png)

![Imagen3.png](https://i.postimg.cc/qML5k1kk/Imagen3.png)

![Imagen4.png](https://i.postimg.cc/L8QQPhYw/Imagen4.png)

![Imagen5.png](https://i.postimg.cc/1zgCrPDt/Imagen5.png)

![Imagen6.png](https://i.postimg.cc/Wb25f00x/Imagen6.png)

![Imagen7.png](https://i.postimg.cc/pLrqPxGZ/Imagen7.png)

**Instalar java.**

```instalanadoJava
   sudo yum install java-1.8.0
```

```instalanadoJava2
   sudo yum install java-1.8.0-openjdk-devel
```

**Instalar Docker.**

```instalanadoDocker
   sudo yum update -y
```

```instalanadoDocker2
   sudo yum install docker
```

**Instalar Docker Compose.**

```instalanadoDockerCompose
   sudo curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose
```

Corregir permisos después de la descarga:

```ConfigurandoPermisos
   sudo chmod +x /usr/local/bin/docker-compose
```

Vereficar descarga:

```verificandoDescarga
   docker-compose version
```

![Imagen8.png](https://i.postimg.cc/bJhxSjT4/Imagen8.png)

**Iniciando el servicio Docker.**

```iniciandoDocker
   sudo service docker start
```

**Configure su usuario en el grupo de docker para no tener que ingresar “sudo”.**

```configurandoUsuarioDocker
   sudo usermod -a -G docker ec2-user
```

**Desconectes de la máquina virtual e ingrese nuevamente para que la configuración de grupos.**

```salir
   exit
```

**Subir Proyecto**

![Imagen9.png](https://i.postimg.cc/44w6yqhF/Imagen9.png)

![Imagen10.png](https://i.postimg.cc/9f3Tsjvz/Imagen10.png)

![Imagen11.png](https://i.postimg.cc/1tsF5rRQ/Imagen11.png)

![Imagen12.png](https://i.postimg.cc/VvH0gyqf/Imagen12.png)

![Imagen13.png](https://i.postimg.cc/y8R3mbkK/Imagen13.png)

![Imagen14.png](https://i.postimg.cc/7LyGmn22/Imagen14.png)

**Ejecute el docker-compose.yml**

```CrearDockerCompose
    docker-compose up -d --build
```

**Verifique que se creo correctamente**

```VerificarImagesyPS
    docker images
```

![Imagen15.png](https://i.postimg.cc/rpCdbR1Y/Imagen15.png)

```VerificarImagesyPS
    docker ps
```

![Imagen16.png](https://i.postimg.cc/qqgzzm99/Imagen16.png)

**Cree la nueva regla de seguridad**

![Imagen17.png](https://i.postimg.cc/90SzTd2z/Imagen17.png)

**Realizar Prueba**

![Imagen18.png](https://i.postimg.cc/Gh84R5PD/Imagen18.png)

![Imagen19.png](https://i.postimg.cc/8CDchk2d/Imagen19.png)

### PASOS PARA CLONAR.

-  Nos dirigimos a la parte superior de nuestra ubicación, donde daremos clic y escribimos la palabra cmd, luego damos enter, con el fin de desplegar
   el Command Prompt, el cual es necesario.

![img1.png](https://i.postimg.cc/GmSNVZZL/img1.png)

![Imagen2.png](https://i.postimg.cc/vB5N1DDT/Imagen2.png)

![Imagen3.png](https://i.postimg.cc/T3hNVthZ/Imagen3.png)

- Una vez desplegado el Command Prompt, pasamos a verificar que tengamos instalado git, ya que sin él no podremos realizar la descarga.
  Para esto ejecutamos el siguiente comando.

`git --version`

![Imagen4.png](https://i.postimg.cc/nh5R0qDM/Imagen4.png)

- Si contamos con git instalado, tendra que salir algo similar. La version puede variar de cuando se este realizando este tutorial.
  Si no cuenta con git, puede ver este tutorial.

[Instalación de Git][id/name]

[id/name]: https://www.youtube.com/watch?v=cYLapo1FFmA

![Imagen5.png](https://i.postimg.cc/fR6CxZG9/Imagen5.png)

-  Una vez comprobado de que contamos con git. pasamos a escribir el siguiente comando. git clone,
   que significa que clonamos el repositorio, y damos la url del repositorio.

`$ git clone https://github.com/DeividMedina30/tareaArepDocker.git`

![Imagen6.png](https://i.postimg.cc/gjkHY0Zf/Imagen6.png)

- Luego podemos acceder al proyecto escribiendo.

`$ cd tareaArepDocker`

### TECNOLOGÍAS USADAS PARA EL DESARROLLO DEL LABORATORIO.

* [Maven](https://maven.apache.org/) - Administrador de dependencias.

* [GitHub](https://github.com/) - Forja para alojar proyectos utilizando el sistema de control de versiones Git.

* [Spark](http://sparkjava.com/) - Spark Framework es un DSL de marco web Java/Kotlin.

* [Docker](https://www.docker.com/) - Automatiza el despliegue de aplicaciones dentro de contenedores de software.

* [AWS](https://aws.amazon.com/es/free/?trk=eb709b95-5dcd-4cf8-8929-6f13b8f2781f&sc_channel=ps&sc_campaign=acquisition&sc_medium=ACQ-P|PS-GO|Brand|Desktop|SU|Core-Main|Core|LATAMO|ES|Text&ef_id=EAIaIQobChMIoueptLLJ9gIVw52GCh2YxwNgEAAYASAAEgIqMPD_BwE:G:s&s_kwcid=AL!4422!3!561348326837!e!!g!!aws&ef_id=EAIaIQobChMIoueptLLJ9gIVw52GCh2YxwNgEAAYASAAEgIqMPD_BwE:G:s&s_kwcid=AL!4422!3!561348326837!e!!g!!aws&all-free-tier.sort-by=item.additionalFields.SortRank&all-free-tier.sort-order=asc&awsf.Free%20Tier%20Types=*all&awsf.Free%20Tier%20Categories=*all) - MongoDB es un sistema de base de datos NoSQL

* [MongoDB](https://www.mongodb.com/cloud/atlas/lp/try-atlas?utm_content=controlhterms&utm_source=google&utm_campaign=gs_americas_colombia_search_core_brand_atlas_desktop&utm_term=mongodb&utm_medium=cpc_paid_search&utm_ad=e&utm_ad_campaign_id=12212624317&adgroup=115749712463&gclid=EAIaIQobChMI65T-mYjL9gIVycfICh1JnwCtEAAYASAAEgJsFfD_BwE) - MongoDB es un sistema de base de datos NoSQL

### LIMITACIONES.

Por parte de las limitaciones tenemos que docker-compose, tocaba instalarlo y fue lo que no me dejaba
ejecutarlo correctamente. Además de no haber especificado bien la ruta del dockerfile en el 
docker-compose, lo cual me llevo tiempo en encontrarlo.

### EXTENDER.

Se podriar crear más funcionalidades las cuales tenga correlación con respecto a MongoDB,
además de distintas api las cuales puedan almacenar data para futuras investigaciones.

### Documentación

Para generar la documentación se debe ejecutar:

`$ mvn javadoc:javadoc`

Esta quedará en la carpeta target/site/apidocs

### AUTOR.

> Deivid Sebastián Medina Rativa.

### Licencia.

Este laboratorio esta bajo la licencia de GNU GENERAL PUBLIC LICENSE.



