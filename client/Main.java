package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;


class Entity {
    String type;
    String key;
    String value;
    public Entity(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
    public Entity(String type, String key) {
        this.type = type;
        this.key = key;
    }
    public Entity(String type) {
        this.type = type;
    }
}

public class Main {

    @Parameter(names = { "-t"}, description = "type of the request")
    private static String type = "";
    //
    @Parameter(names = "-k", description = "index of the cell")
    private static String key = "";
    //
    @Parameter(names = "-v", description = "index of the cell")
    private static String value = "";

    @Parameter(names = "-in", description = "")
    private static String common = "";

    private static final int PORT = 34522;
    private static final String SERVER_ADDRESS = "127.0.0.1";

    private static final String DATA_PATH = System.getProperty("user.dir") + "/src/client/data";

    public static void main(String ... argv) {

        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);
        Gson gson = new Gson();
        Entity entity;

        System.out.println("Client started!");
        try (
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            String msg = "";
            if (common == "") {
                if (type != "" && key != "" && value != "") {
                    entity = new Entity(type, key, value);
                    msg = gson.toJson(entity);
                } else if (type != "" && key != "") {
                    entity = new Entity(type, key);
                    msg = gson.toJson(entity);
                } else {
                    entity = new Entity(type);
                    msg = gson.toJson(entity);
                }
            }
            else{
                File file = new File(DATA_PATH + "/" + common);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                msg = reader.readLine();
            }

            output.writeUTF(msg); // sending message to the server
            System.out.println("Sent: " + msg);
            System.out.println("Received: " + input.readUTF());
            if (msg.equalsIgnoreCase("exit")) System.exit(0);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
