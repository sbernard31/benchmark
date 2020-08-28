package org.eclipse.californium.benchmark;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.serialization.IdentitySerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.Registration.Builder;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import redis.clients.jedis.JedisPool;

public class LinkFormatParsing {

    @State(Scope.Benchmark)
    public static class MyState {

        public JedisPool j;
        public RegistrationStore store;
        public Registration reg;
        public Registration reg_long;
        public Pattern p;
        public String regser;
        public JsonObject regjson;

        public MyState() {
            try {
                j = new JedisPool(new URI("redis://localhost:6379"));
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            store = new RedisRegistrationStore(j);
            Builder builder = new Registration.Builder("regid", "endpoint",Identity.unsecure(new InetSocketAddress("localhost", 4000)));
            Link[] links = Link.parse("</1/0>,</1/1>,</3/0>,</44>;ver=“2.2”,</44/0>".getBytes());
            builder.objectLinks(links);
            reg = builder.build();
            store.addRegistration(reg);
            p = Pattern.compile("^\\Q" + "/" + "\\E(\\d+)(?:/\\d+)*$");
            
            builder = new Registration.Builder("regid_long", "endpoint2",Identity.unsecure(new InetSocketAddress("localhost", 4000)));
            links = new Link[1000];
            for (int i = 0; i < links.length; i++) {
                HashMap<String, String> attr = new HashMap<>();
                attr.put("ver", "2.1");
                links[i] = new Link("/"+i+"/0", attr);
            }
            builder.objectLinks(links);
            reg_long = builder.build();
            store.addRegistration(reg_long);
            
            regser = RegistrationSerDes.sSerialize(reg_long);
            regjson = (JsonObject) Json.parse(regser);
        }

        @Setup(Level.Invocation)
        public void prepare() {
        }
    }
    
    @Benchmark
    public void short_registration(MyState state) {
        state.store.getRegistration("regid");
    }
    
    @Benchmark
    public void short_registration_and_parse_supported_object(MyState state) {
        Registration registration = state.store.getRegistration("regid");
        Pattern p = Pattern.compile("^\\Q" + registration.getRootPath() + "\\E(\\d+)(?:/\\d+)*$");
        getSupportedObject(registration.getRootPath(),registration.getObjectLinks(), p);
    }
    
    @Benchmark
    public void short_registration_and_parse_supported_object_enhanced(MyState state) {
        Registration registration = state.store.getRegistration("regid");
        getSupportedObject_enhanced4(registration.getRootPath(),registration.getObjectLinks());
    }
    
    @Benchmark
    public void long_registration_1_total(MyState state) {
        state.store.getRegistration("regid_long");
    }
    
    @Benchmark
    public void long_registration_2_json_total(MyState state) {
        deserialize(state.regser);
    }
    
    @Benchmark
    public void long_registration_3_json_minimal(MyState state) {
        Json.parse(state.regser);
    }
    
    @Benchmark
    public void long_registration_4_json_leshan(MyState state) {
        deserialize(state.regjson);
    }
    
    @Benchmark
    public void long_registration_5_get_supported(MyState state) {
        state.reg_long.getSortedObjectLinks();
    }
    
    @Benchmark
    public void long_registration_5_get_enhanced(MyState state) {
        getSupportedObject_enhanced(state.reg_long.getRootPath(), state.reg_long.getObjectLinks());
    }
    
    @Benchmark
    public void long_registration_5_get_enhanced2(MyState state) {
        getSupportedObject_enhanced2(state.reg_long.getRootPath(), state.reg_long.getObjectLinks());
    }
    
    @Benchmark
    public void long_registration_5_get_enhanced3(MyState state) {
        getSupportedObject_enhanced3(state.reg_long.getRootPath(), state.reg_long.getObjectLinks());
    }
    
    @Benchmark
    public void long_registration_5_get_enhanced4(MyState state) {
        getSupportedObject_enhanced4(state.reg_long.getRootPath(), state.reg_long.getObjectLinks());
    }

    @Benchmark
    public void long_registration_1_total_with_supportedobj(MyState state) {
        Registration registration = state.store.getRegistration("regid_long");
        Pattern p = Pattern.compile("^\\Q" + registration.getRootPath() + "\\E(\\d+)(?:/\\d+)*$");
        getSupportedObject(registration.getRootPath(),registration.getObjectLinks(), p);
    }
    
    @Benchmark
    public void long_registration_1_total_with_supportedobj_enhanced(MyState state) {
        Registration registration = state.store.getRegistration("regid_long");
        getSupportedObject_enhanced3(registration.getRootPath(), registration.getObjectLinks());
    }
    
    public static Map<Integer, String> getSupportedObject(String rootPath, Link[] objectLinks, Pattern p  ){
        return getSupportedObject(rootPath, objectLinks, p, true);
    }
    /**
     * Build a Map (object Id => object Version) from root path and registration object links.
     */
    public static Map<Integer, String> getSupportedObject(String rootPath, Link[] objectLinks, Pattern p, boolean add ) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Matcher m = p.matcher(link.getUrl());
                if (m.matches()) {
                    try {
                        if (add) {
                        // extract object id and version
                        int objectId = Integer.parseInt(m.group(1));
                        Object version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                            if (currentVersion == null) {
                                // we never find version for this object add it
                                if (version instanceof String) {
                                    objects.put(objectId, (String) version);
                                } else {
                                    objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                                }
                            } else {
                                // if version is already set, we override it only if new version is not DEFAULT_VERSION
                                if (version instanceof String && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                    objects.put(objectId, (String) version);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }
    
    
    /**
     * Build a Map (object Id => object Version) from root path and registration object links.
     * better version
     */
    public static Map<Integer, String> getSupportedObject_enhanced(String rootPath, Link[] objectLinks ) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Integer objectId = getLwM2mPath(link.getUrl(), rootPath);
                if(objectId != null) {
                    
                    try {
                        // extract object id and version
                        Object version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                            if (currentVersion == null) {
                                // we never find version for this object add it
                                if (version instanceof String) {
                                    objects.put(objectId, (String) version);
                                } else {
                                    objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                                }
                            } else {
                                // if version is already set, we override it only if new version is not DEFAULT_VERSION
                                if (version instanceof String && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                    objects.put(objectId, (String) version);
                                }
                            }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }
    
    public static Map<Integer, String> getSupportedObject_enhanced2(String rootPath, Link[] objectLinks ) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Integer objectId = getLwM2mPath2(link.getUrl(), rootPath);
                if(objectId != null) {
                    
                    try {
                        // extract object id and version
                        Object version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                            if (currentVersion == null) {
                                // we never find version for this object add it
                                if (version instanceof String) {
                                    objects.put(objectId, (String) version);
                                } else {
                                    objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                                }
                            } else {
                                // if version is already set, we override it only if new version is not DEFAULT_VERSION
                                if (version instanceof String && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                    objects.put(objectId, (String) version);
                                }
                            }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }
    
    public static Map<Integer, String> getSupportedObject_enhanced3(String rootPath, Link[] objectLinks ) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Integer objectId = getLwM2mPath3(link.getUrl(), rootPath);
                if(objectId != null) {
                    
                    try {
                        // extract object id and version
                        Object version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                            if (currentVersion == null) {
                                // we never find version for this object add it
                                if (version instanceof String) {
                                    objects.put(objectId, (String) version);
                                } else {
                                    objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                                }
                            } else {
                                // if version is already set, we override it only if new version is not DEFAULT_VERSION
                                if (version instanceof String && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                    objects.put(objectId, (String) version);
                                }
                            }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }
    
    public static Map<Integer, String> getSupportedObject_enhanced4(String rootPath, Link[] objectLinks ) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Integer objectId = getLwM2mPath4(link.getUrl(), rootPath);
                if(objectId != null) {
                    
                    try {
                        // extract object id and version
                        Object version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                            if (currentVersion == null) {
                                // we never find version for this object add it
                                if (version instanceof String) {
                                    objects.put(objectId, (String) version);
                                } else {
                                    objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                                }
                            } else {
                                // if version is already set, we override it only if new version is not DEFAULT_VERSION
                                if (version instanceof String && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                    objects.put(objectId, (String) version);
                                }
                            }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }
    
    public static void main(String[] args) {
        System.out.println(getLwM2mPath4("/0/1/22", "/"));
    }
    
    public static Integer getLwM2mPath(String URI, String rootpath) {
        if (!URI.startsWith(rootpath))
            return null;
        String path = URI.substring(rootpath.length());

        Validate.notNull(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        Scanner scanner = new Scanner(path);
        scanner.useDelimiter("/");
        int objectid = -1;
        int  i = 0;
        while (scanner.hasNext()) {
                int nextInt = scanner.nextInt();
                i++;
                if (objectid == -1)
                    objectid = nextInt;
                if (i>3)
                    return null;
            
        }
        scanner.close();
        return objectid;
    }
    
    public static Integer getLwM2mPath2(String URI, String rootpath) {
        if (!URI.startsWith(rootpath))
            return null;
        String path = URI.substring(rootpath.length());

       LwM2mPath lwM2mPath = new LwM2mPath(path);
       if (lwM2mPath != null)
           return lwM2mPath.getObjectId();
                  return null;
    }
    
    public static Integer getLwM2mPath3(String URI, String rootpath) {
        if (!URI.startsWith(rootpath))
            return null;
        String path = URI.substring(rootpath.length());

       StringTokenizer stringTokenizer = new StringTokenizer(path, "/");
       int objectid = -1;
       int i = 0;
       while(stringTokenizer.hasMoreTokens()) {
           
           int nextInt = Integer.parseInt(stringTokenizer.nextToken());
          // System.out.println(nextInt);
           if (objectid == -1)
               objectid = nextInt;
           if (i>3)
               return null;
       }
       return objectid;
    }
    
    public static Integer getLwM2mPath4(String URI, String rootpath) {
        if (!URI.startsWith(rootpath))
            return null;
       
       int[] id = new int[3];
       int j = 0;
       boolean delimiter = true;
       int currentIndex = rootpath.length();
       for (int i = rootpath.length(); i < URI.length(); i++) {
       {
         char currentChar = URI.charAt(i);
         if (currentChar == '/') {
             if (delimiter == true) {
                 return null;
             }
             else {
                 id[j] = Integer.parseInt(URI.substring(currentIndex, i));
                 currentIndex=i+1;
                 j++;
             }
         } else if (i == URI.length() -1){
             id[j] = Integer.parseInt(URI.substring(currentIndex));
         }else {
             delimiter = false;
         }
       }
    }
    return id[0];
    }
    
    
    public static Registration deserialize(JsonObject jObj) {
        Registration.Builder b = new Registration.Builder(jObj.getString("regId", null), jObj.getString("ep", null),
                IdentitySerDes.deserialize(jObj.get("identity").asObject()));
        b.bindingMode(BindingMode.valueOf(jObj.getString("bnd", null)));
        b.lastUpdate(new Date(jObj.getLong("lastUp", 0)));
        b.lifeTimeInSec(jObj.getLong("lt", 0));
        b.lwM2mVersion(jObj.getString("ver", "1.0"));
        b.registrationDate(new Date(jObj.getLong("regDate", 0)));
        if (jObj.get("sms") != null) {
            b.smsNumber(jObj.getString("sms", ""));
        }

        JsonArray links = (JsonArray) jObj.get("objLink");
        Link[] linkObjs = new Link[links.size()];
        for (int i = 0; i < links.size(); i++) {
            JsonObject ol = (JsonObject) links.get(i);

            JsonObject att = (JsonObject) ol.get("at");
            Map<String, String> attMap = Collections.emptyMap();
            if (!att.isEmpty()) {
                attMap = new HashMap<>();
                for (String k : att.names()) {
                    JsonValue jsonValue = att.get(k);
                    if (jsonValue.isNull()) {
                        attMap.put(k, null);
//                    } else if (jsonValue.isNumber()) {
//                        attMap.put(k, jsonValue.asInt());
                    } else {
                        attMap.put(k, jsonValue.asString());
                    }
                }
            }
            Link o = new Link(ol.getString("url", null), attMap);
            linkObjs[i] = o;
        }
        b.objectLinks(linkObjs);
        Map<String, String> addAttr = new HashMap<>();
        JsonObject o = (JsonObject) jObj.get("addAttr");
        for (String k : o.names()) {
            addAttr.put(k, o.getString(k, ""));
        }
        b.additionalRegistrationAttributes(addAttr);

        return b.build();
    }

    public static Registration deserialize(byte[] data) {
        return deserialize((JsonObject) Json.parse(new String(data)));
    }
    
    public static Registration deserialize(String data) {
        return deserialize((JsonObject) Json.parse(data));
    }
}
