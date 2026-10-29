package edu.stanford.bmir.protege.web.server.api.resources;

import com.mongodb.client.MongoDatabase;
import edu.stanford.bmir.protege.web.server.api.ApiRootResource;
import edu.stanford.bmir.protege.web.server.user.UserRecordRepository;
import edu.stanford.bmir.protege.web.shared.api.ApiKey;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.bson.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Deletes a local WebProtégé account created for SCS single sign-on.
 *
 * Project roles of that user go with the account. Ontology projects themselves
 * stay; SCS Manager trashes those the user owns before calling this.
 */
@Path("users")
public class UsersResource implements ApiRootResource {

    @Nonnull
    private final MongoDatabase database;

    @Nonnull
    private final UserRecordRepository userRecordRepository;

    @Inject
    public UsersResource(@Nonnull MongoDatabase database,
                         @Nonnull UserRecordRepository userRecordRepository) {
        this.database = checkNotNull(database);
        this.userRecordRepository = checkNotNull(userRecordRepository);
    }

    /**
     * Removes the user record, login activity and role assignments.
     *
     * Missing accounts are a success, so a repeated account deletion is safe.
     */
    @DELETE
    @Path("{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@Nullable @Context ApiKey apiKey,
                               @PathParam("userName") String userName) {
        if (apiKey == null) {
            return Response.status(UNAUTHORIZED).build();
        }
        String name = userName == null ? "" : userName.trim();
        if (name.isEmpty() || "guest".equalsIgnoreCase(name)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        UserId userId = UserId.getUserId(name);
        userRecordRepository.delete(userId);
        database.getCollection("UserActivity").deleteOne(new Document("_id", name));
        database.getCollection("RoleAssignments").deleteMany(new Document("userName", name));
        return Response.noContent().build();
    }
}
