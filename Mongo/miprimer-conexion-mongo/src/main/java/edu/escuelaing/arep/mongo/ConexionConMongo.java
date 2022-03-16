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

/**
 * Hello world!
 *
 */
public class App 
{
    private static SimpleDateFormat fechaDelDato = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static MongoCliente clienteMongo;
    private static MongoDatabase database;
    private static MongoCollection<Document> coleccionDatos;
    private static ArrayList<String> respuesta = new ArrayList<>();

    public static void main(String[] args) {
        port(getPort());
        post("/",(req, res)->{
            res.type("application/json");
            return insert(req.queryParams("value"));
        });
    }

    private static String conexionMongo(String palabra){
        clienteMongo = new MongoClient("database");
        database = clienteMongo.getDatabase("Lista");
        coleccionDatos = database.getCollection("datos");
        if(coleccionDatos.countDocuments()==10){ // Mirar si hay al menos diez datos, si hay más de diez eliminar dato.
            coleccionDatos.deleteOne(Filters.eq("id",0));
            Document updated = new Document().append("$inc", new Document().append("id", -1));
            coleccionDatos.updateMany(Filters.gt("id",0),updated);
        }
        insertarDatoMongo();
        mostrarDatosMongo();
        clienteMongo.close();
        return Arrays.toString(respuesta.toArray(new String[respuesta.size()]));
    }

    private static void insertarDatoMongo(){
        coleccionDatos.insertOne(new Document().append("fecha",fechaDelDato.format(new Date())).append("value", palabra).append("id",(int)coleccionDatos.countDocuments()));
    }

    private static void mostrarDatosMongo(){
        coleccionDatos.find().forEach((Consumer<Document>) (Document d) -> { d.remove("_id");d.remove("id");res.add(d.toJson());});
    }

    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 4567;
    }
}
