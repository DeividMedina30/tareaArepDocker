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
