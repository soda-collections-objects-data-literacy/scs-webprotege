package edu.stanford.bmir.protege.web.server.api.resources;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import edu.stanford.bmir.protege.web.server.api.ResponseUtil;
import edu.stanford.bmir.protege.web.server.sharing.ProjectCollaboratorManager;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.sharing.SharingPermission;
import edu.stanford.bmir.protege.web.shared.user.UserId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Additive collaborator management for a single project.
 */
public class CollaboratorsResource {

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final ProjectCollaboratorManager collaboratorManager;

    @AutoFactory
    public CollaboratorsResource(@Nonnull ProjectId projectId,
                                 @Provided @Nonnull ProjectCollaboratorManager collaboratorManager) {
        this.projectId = checkNotNull(projectId);
        this.collaboratorManager = checkNotNull(collaboratorManager);
    }

    @PUT
    @Path("{userName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putCollaborator(@Nonnull @Context UserId actor,
                                    @PathParam("userName") String userName,
                                    @Nullable CollaboratorPermission body) {
        if (body == null || body.getPermission() == null) {
            return ResponseUtil.badRequest("Missing permission in body of PUT");
        }
        SharingPermission permission = body.getPermission();
        try {
            collaboratorManager.setCollaborator(projectId, actor, userName, permission);
            return Response.noContent().build();
        }
        catch (IllegalArgumentException e) {
            return ResponseUtil.badRequest(e.getMessage());
        }
    }

    @DELETE
    @Path("{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCollaborator(@Nonnull @Context UserId actor,
                                       @PathParam("userName") String userName) {
        try {
            collaboratorManager.removeCollaborator(projectId, actor, userName);
            return Response.noContent().build();
        }
        catch (IllegalArgumentException e) {
            return ResponseUtil.badRequest(e.getMessage());
        }
    }
}
