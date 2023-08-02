package gov.sandia.webcomms.http.rsc;

import org.bson.types.ObjectId;

import gov.sandia.mongo.AbstractMongoPersistenceManager;
import gov.sandia.mongo.MongoGridFsUtil;

public class HttpResourceMongoPersistenceManager extends AbstractMongoPersistenceManager {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpResourceMongoPersistenceManager() {
        super(HttpResource.class);
    }

    public void save(HttpResource resource) {
        datastore.save(prefix("resource"), resource);
    }

    public HttpResource fetch(ObjectId id) {
        return datastore.get(prefix("resource"), HttpResource.class, id);
    }

    public void delete(HttpResource resource) {
        datastore.delete(prefix("resource"), HttpResource.class, resource.getId());
        deleteContent(resource);
    }
    public void deleteContent(HttpResource resource) {
        MongoGridFsUtil.removeGridFsFile(getDb(), prefix("resource"), resource.getId());
    }
}
