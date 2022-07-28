package server;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class JSONdb {
    private final static String ERROR = "{ \"response\": \"ERROR\", \"reason\": \"No such key\" }";
    private final static JsonElement OK = JsonParser.parseString("OK");

    private final HashMap<String, String> cell = new LinkedHashMap<>();
    private final int MAX_CELLS = 100;
    private String result = "";
    private boolean isExit = false;
    private boolean running = false;
    Gson gson = new Gson();
    Map<String, JsonElement> res = new HashMap<>();
    final String filedb = "./src/server/data/db.json";

    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();
    Lock writeLock = lock.writeLock();


    public void doOperation(String type, String[] key, JsonElement value) throws IOException {

        switch (type) {
            case "get":
                getKey(key);
                break;
            case "set":
                setKey(key, value);
                break;
            case "delete":
                deleteByIndex(key);
                break;
            case "exit":
                res.put("response", OK);
                result = gson.toJson(res);
                isExit = true;
                break;
            default:
                result = (ERROR);
        }
    }

    private void deleteByIndex(String[] index) throws FileNotFoundException {

        Type type = new TypeToken<Map<String, JsonElement>>(){}.getType();
        readLock.lock();
        Map<String, JsonElement> existingJson = gson.fromJson(new JsonReader(new FileReader(new File(filedb))), type);
        readLock.unlock();

        if (!existingJson.containsKey(index[0])) {
            result = (ERROR); return;
        } else if (existingJson.get(index[0]).equals(" ")) {
            result = (ERROR); return;
        } else {
            JsonElement jsonElement = existingJson.get(index[0]);
            if (index.length == 1) {
                existingJson.remove(index[0]);
            } else if (index.length > 1) {
                for (int i = 1; i < index.length - 1; i++) {
                    JsonObject objectjs = jsonElement.getAsJsonObject();
                    if (objectjs.has(index[i])) {
                        jsonElement = objectjs.get(index[i]);
                    } else {result = (ERROR); return;}
                }
            }

            JsonObject deleteObject = jsonElement.getAsJsonObject();
            deleteObject.remove(index[index.length-1]);
            writeLock.lock();
            try (FileWriter writer = new FileWriter(new File(filedb))) {
                writer.write(gson.toJson(existingJson));
                writeLock.unlock();
            } catch (IOException e) {
                e.printStackTrace();
            }
            res.put("response", OK);
            result = (gson.toJson(res));
            res.clear();
        }
    }

    public void setKey(String[] index, JsonElement jsonInput) throws IOException {
//        JsonElement jsonElem = JsonParser.parseString(inputArray);
//        JsonObject jsonval = jsonElem.getAsJsonObject();

//        if file already contains json:
//            1. only one key
//                   -- contains the key
//                   -- doesn't contain the key
//            2. more than 1 key
//                   --check if json contains the key
//                   --must contain all keys maybe except 1
//        file doesn't contain json
//            ERROR: if more than 1 key

        readLock.lock();
        readLock.unlock();
        Type type = new TypeToken<Map<String, JsonElement>>(){}.getType();
        Map<String, JsonElement> originalJson = new HashMap<>();
        try { originalJson = gson.fromJson(new JsonReader(new FileReader(new File(filedb))), type);
        } catch (JsonIOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        JsonElement jsonInput = JsonParser.parseString(inputArray);
        if (originalJson == null){
            Map<String, JsonElement> meow = new HashMap<>();
            meow.put(index[0], jsonInput);
            try (FileWriter writer = new FileWriter(new File(filedb))) {
                writer.write(gson.toJson(meow));
                res.put("response", OK);
                result = gson.toJson(res);
            }
        }else {
            if (originalJson.containsKey(index[0])){
                JsonElement mapElement = originalJson.get(index[0]);
                if (index.length == 1){
                    if (mapElement.isJsonPrimitive()) {originalJson.replace(index[0], jsonInput);}
                    else {
                        JsonObject jsonObject = jsonInput.getAsJsonObject();
                        String[] key = jsonObject.keySet().toArray(new String[0]);
                        JsonObject mapobj = mapElement.getAsJsonObject();
                        mapobj.add(key[0], jsonObject.get(key[0]));
                    }
                    try (FileWriter writer = new FileWriter(new File(filedb))) {
                        writer.write(gson.toJson(originalJson));
                        res.put("response", OK);
                        result = gson.toJson(res);
                    }

                }else if (index.length > 1) {
                    if (mapElement.isJsonPrimitive()) {result = (ERROR); return;}
                    Map<String, JsonElement> midJson = new HashMap<>();
                    midJson.put(index[0], originalJson.get(index[0]));
                    JsonObject mapObject = mapElement.getAsJsonObject();
                    JsonObject mapObject2 = mapElement.getAsJsonObject();
                    boolean hasnext = true;
                    int i =1;

                    while (midJson.get(index[i-1]).getAsJsonObject().has(index[i])) {
                        midJson.put(index[i], mapObject2.get(index[i]));
                        mapElement = midJson.get(index[i]);
                        if (i == index.length - 1) {
                            hasnext = false; break;}
                        else {mapObject2 = mapElement.getAsJsonObject(); i++;}


                    }
                    //add the left over keys to the midJson
                    if (!hasnext) {
                        Map<String, JsonElement> midJson2 = new HashMap<>();
                        for (int j = i; j < index.length; j++) {
                            midJson2.put(index[j-1], midJson.get(index[j-1]));
                            JsonElement midElement = gson.toJsonTree( midJson.get(index[j-1]));
                            JsonObject obj = midElement.getAsJsonObject();
                            JsonObject mapobj = obj.getAsJsonObject();
                            mapobj.add(index[j], jsonInput);
                            System.out.println(mapobj);
                            midJson.put(index[j-1], mapobj);
                            mapObject.remove(index[j-1]);
                            mapObject.add(index[j-1], midJson.get(index[j-1]));
                            System.out.println(mapobj);
                        }
                    }else {
                        JsonElement checkelem = midJson.get(index[i]);
                        if (checkelem.isJsonPrimitive()) {
                            JsonElement jsonElement = midJson.get(index[i-1]);
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
                            System.out.println(index[i-1]);
                            jsonObject.remove(index[i]);
                            jsonObject.addProperty(index[i], jsonObject.toString());
                            mapObject.remove(index[i-1]);
                            mapObject.add(index[i-1], jsonObject);
                        } else {
                            JsonArray jsonArray = new JsonArray();
                            jsonArray.add(midJson.get(index[i]));
                            jsonArray.add(jsonInput);
                            mapObject.remove(index[i]);
                            mapObject.add(index[i], jsonArray);
                        }
                    }
                    try (FileWriter writer = new FileWriter(new File(filedb))) {
                        writer.write(gson.toJson(originalJson));
                        res.put("response", OK);
                        result = gson.toJson(res);
                    }


                }else {result = (ERROR); return;}
            } else {originalJson.put(index[0], jsonInput);
                try (FileWriter writer = new FileWriter(new File(filedb))) {
                    writer.write(gson.toJson(originalJson));
                    res.put("response", OK);
                    result = gson.toJson(res);
                }
            }
        }
    }

    private void getKey(String[] index) throws FileNotFoundException {
        try {
            readLock.lock();
            File input = new File(filedb);
            readLock.unlock();
            JsonElement jsonElement = JsonParser.parseReader(new FileReader(input));

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (!jsonObject.has(index[0])) {
                result = (ERROR);
                return;
            }
            JsonObject finalobj = new JsonObject();
            finalobj.add(index[0], jsonObject.get(index[0]));

            for (String j : index) {
                if (finalobj.has(j)) {
                    if (finalobj.get(j).isJsonPrimitive()) {
                        jsonElement = finalobj.get(j);
                    } else {
                        finalobj = finalobj.getAsJsonObject(j);
                        jsonElement = finalobj;
                    }

                } else {
                    result = (ERROR);
                    return;
                }
            }

            res.put("response", OK);
            res.put("value", jsonElement);
            result = gson.toJson(res);
            res.clear();

        } catch (JsonIOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getResponse() {
        System.out.println(result);
        return result;
    }

    public boolean isExit() {
        return isExit;
    }

}


