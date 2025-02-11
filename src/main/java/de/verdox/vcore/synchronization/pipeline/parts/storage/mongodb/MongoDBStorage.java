/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.pipeline.parts.storage.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.verdox.vcore.plugin.VCorePlugin;
import de.verdox.vcore.plugin.subsystem.VCoreSubsystem;
import de.verdox.vcore.synchronization.pipeline.datatypes.NetworkData;
import de.verdox.vcore.synchronization.pipeline.datatypes.VCoreData;
import de.verdox.vcore.synchronization.pipeline.parts.storage.GlobalStorage;
import de.verdox.vcore.synchronization.pipeline.parts.storage.RemoteStorage;
import de.verdox.vcore.util.global.AnnotationResolver;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 24.06.2021 16:00
 */
public class MongoDBStorage implements GlobalStorage, RemoteStorage {


    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private final VCorePlugin<?, ?> vCorePlugin;
    private final String host;
    private final String database;
    private final int port;
    private final String user;
    private final String password;
    //private final CodecRegistry codecRegistry;

    public MongoDBStorage(VCorePlugin<?, ?> vCorePlugin, String host, String database, int port, String user, String password) {
        this.vCorePlugin = vCorePlugin;
        this.host = host;
        this.database = database;
        this.port = port;
        this.user = user;
        this.password = password;
        //this.codecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),CodecRegistries.fromProviders(new UuidCodecProvider(UuidRepresentation.JAVA_LEGACY)));

        connect();
        vCorePlugin.consoleMessage("&eMongoDB Global Storage started", true);
    }

    public MongoDBStorage(VCorePlugin<?, ?> vCorePlugin, String host, String database, int port) {
        this(vCorePlugin, host, database, port, "", "");
    }


    @Override
    public Map<String, Object> loadData(@Nonnull @NotNull Class<? extends VCoreData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        Document filter = new Document("objectUUID", objectUUID.toString());

        Document mongoDBData = getMongoStorage(dataClass, getSuffix(dataClass)).find(filter).first();

        if (mongoDBData == null)
            mongoDBData = filter;
        return convertDocumentToHashMap(mongoDBData);
    }

    private Map<String, Object> convertDocumentToHashMap(Object object) {
        if (!(object instanceof Document))
            return null;
        Document document = (Document) object;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(document);
        map.remove("_id");
        document.forEach((s, o) -> {
            Map<String, Object> possibleFoundDocument = convertDocumentToHashMap(o);
            if (possibleFoundDocument == null)
                return;
            map.put(s, possibleFoundDocument);
        });
        return map;
    }

    @Override
    public boolean dataExist(@Nonnull @NotNull Class<? extends VCoreData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        Document document = getMongoStorage(dataClass, getSuffix(dataClass)).find(new Document("objectUUID", objectUUID.toString())).first();
        return document != null;
    }

    @Override
    public void save(@Nonnull @NotNull Class<? extends VCoreData> dataClass, @Nonnull @NotNull UUID objectUUID, @Nonnull @NotNull Map<String, Object> dataToSave) {
        Document filter = new Document("objectUUID", objectUUID.toString());

        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));

        if (collection.find(filter).first() == null) {
            Document newData = new Document("objectUUID", objectUUID.toString());
            newData.putAll(dataToSave);
            collection.insertOne(newData);
        } else {
            Document newData = new Document(dataToSave);
            Document updateFunc = new Document("$set", newData);
            collection.updateOne(filter, updateFunc);
        }
    }

    @Override
    public boolean remove(@Nonnull @NotNull Class<? extends VCoreData> dataClass, @Nonnull @NotNull UUID objectUUID) {
        Document filter = new Document("objectUUID", objectUUID.toString());

        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));
        return collection.deleteOne(filter).getDeletedCount() >= 1;
    }

    @Override
    public Set<UUID> getSavedUUIDs(@Nonnull @NotNull Class<? extends VCoreData> dataClass) {
        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));
        Set<UUID> uuids = new HashSet<>();
        for (Document document : collection.find()) {
            if (!document.containsKey("objectUUID"))
                continue;
            uuids.add(UUID.fromString((String) document.get("objectUUID")));
        }
        return uuids;
    }

    private MongoCollection<Document> getMongoStorage(Class<? extends VCoreData> dataClass, String suffix) {
        if (NetworkData.class.isAssignableFrom(dataClass)) {
            return getCollection("VCore_NetworkData_" + dataClass.getCanonicalName() + "_" + suffix);
        } else {
            Class<? extends VCoreSubsystem<?>> subsystemClass = AnnotationResolver.findDependSubsystemClass(dataClass);
            if (subsystemClass == null)
                throw new NullPointerException("Dependent Subsystem Annotation not set. [" + dataClass.getCanonicalName() + "]");
            String mongoIdentifier = AnnotationResolver.getDataStorageIdentifier(subsystemClass);
            if (mongoIdentifier == null)
                throw new NullPointerException("MongoDBIdentifier Annotation not set. [" + subsystemClass.getCanonicalName() + "]");
            return getCollection(AnnotationResolver.getDataStorageIdentifier(subsystemClass) + "_" + suffix);
        }
    }

    private final com.mongodb.client.MongoCollection<Document> getCollection(String name) {
        try {
            return mongoDatabase.getCollection(name);
        }
        // Collection does not exist
        catch (IllegalArgumentException e) {
            mongoDatabase.createCollection(name);
            return mongoDatabase.getCollection(name);
        }
    }

    @Override
    public void connect() {
        vCorePlugin.consoleMessage("&6Trying to connect to MongoDB&7: &b" + host + "&7:&b" + port, false);
        if (user.isEmpty() && password.isEmpty())
            this.mongoClient = new MongoClient(host, port);
        else
            this.mongoClient = new MongoClient(new ServerAddress(host, port), List.of(MongoCredential.createCredential(user, database, password.toCharArray())));
        this.mongoDatabase = mongoClient.getDatabase(database);
    }

    @Override
    public void disconnect() {
        this.mongoClient.close();
    }

    private String getSuffix(Class<? extends VCoreData> dataClass) {
        return AnnotationResolver.getDataStorageIdentifier(dataClass);
    }
}
