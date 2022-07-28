package server;

import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Main {

    private static final int PORT = 34522;
    private static final String SERVER_ADDRESS = "127.0.0.1";
    public static ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        System.out.println("Server started!");
        JSONdb db = new JSONdb();
        
        try (ServerSocket server = new ServerSocket(PORT);) {
            while (true) {
                Socket socket = server.accept();// accepting a new client
                executor.submit(() ->
                    {
                        try (
                                DataInputStream input = new DataInputStream(socket.getInputStream());
                                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                        ) {
                            String msg = input.readUTF(); // reading a message
                            JsonElement jsonElement = JsonParser.parseString(msg);
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
//type
                            String type = jsonObject.get("type").getAsString();
//value
                            JsonElement value = new JsonElement() {
                                @Override
                                public JsonElement deepCopy() {
                                    return null;
                                }
                            };

                            try {
                                value = jsonObject.get("value");
                                System.out.println(value);} catch (Exception e) {
                            }
                            try { value = jsonObject.getAsJsonObject("value");
                            } catch (Exception e) {

                            }
//key
                            try {
                                JsonArray keyArray = jsonObject.getAsJsonArray("key");
                                String key[] = new String[keyArray.size()];
                                int i = -1;
                                for (JsonElement keyarr : keyArray) {
                                    i++;
                                    key[i] = keyarr.toString();
                                    key[i] = key[i].replace("\"", "");
                                }
                                db.doOperation(type, key, value);

                            } catch (Exception e) {
                                String key[] = new String[1];
                                if (!type.equalsIgnoreCase("exit")){
                                    key[0] = jsonObject.get("key").getAsString();
                                    key[0] = key[0].replace("\"","");
                                }
                                db.doOperation(type, key, value);
                            }


                            if (db.isExit() == true) {
                                server.close();
                                System.exit(0);
                            }
                            output.writeUTF(db.getResponse());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
