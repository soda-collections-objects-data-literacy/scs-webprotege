package edu.stanford.bmir.protege.web.server.api.resources;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.access.ApplicationResource;
import edu.stanford.bmir.protege.web.server.api.ActionExecutor;
import edu.stanford.bmir.protege.web.server.api.ResponseUtil;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsManager;
import edu.stanford.bmir.protege.web.shared.project.GetProjectDetailsAction;
import edu.stanford.bmir.protege.web.shared.project.MoveProjectsToTrashAction;
import edu.stanford.bmir.protege.web.shared.project.ProjectDetails;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.project.UnknownProjectException;
import edu.stanford.bmir.protege.web.shared.user.UserId;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.server.access.Subject.forUser;
import static edu.stanford.bmir.protege.web.shared.access.BuiltInAction.EDIT_SHARING_SETTINGS;
import static edu.stanford.bmir.protege.web.shared.access.BuiltInAction.MOVE_ANY_PROJECT_TO_TRASH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 15 Apr 2018
 */
public class ProjectResource {

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final ActionExecutor executor;

    @Nonnull
    private final AxiomsResourceFactory axiomsResourceFactory;

    @Nonnull
    private final DeleteAxiomsResourceFactory deleteAxiomsResourceFactory;

    @Nonnull
    private final RevisionsResourceFactory changesResourceFactory;

    @Nonnull
    private final FormsResourceFactory formsResourceFactory;

    @Nonnull
    private final ProjectSettingsResourceFactory projectSettingsResourceFactory;

    @Nonnull
    private final CollaboratorsResourceFactory collaboratorsResourceFactory;

    @Nonnull
    private final AccessManager accessManager;

    @Nonnull
    private final ProjectDetailsManager projectDetailsManager;

    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    @AutoFactory
    @Inject
    public ProjectResource(@Nonnull ProjectId projectId,
                           @Provided @Nonnull ActionExecutor executor,
                           @Provided @Nonnull AxiomsResourceFactory axiomsResourceFactory,
                           @Provided @Nonnull DeleteAxiomsResourceFactory deleteAxiomsResourceFactory,
                           @Provided @Nonnull RevisionsResourceFactory changesResourceFactory,
                           @Provided @Nonnull FormsResourceFactory formsResourceFactory,
                           @Provided @Nonnull ProjectSettingsResourceFactory projectSettingsResourceFactory,
                           @Provided @Nonnull CollaboratorsResourceFactory collaboratorsResourceFactory,
                           @Provided @Nonnull AccessManager accessManager,
                           @Provided @Nonnull ProjectDetailsManager projectDetailsManager) {
        this.projectId = checkNotNull(projectId);
        this.executor = checkNotNull(executor);
        this.axiomsResourceFactory = checkNotNull(axiomsResourceFactory);
        this.deleteAxiomsResourceFactory = checkNotNull(deleteAxiomsResourceFactory);
        this.changesResourceFactory = checkNotNull(changesResourceFactory);
        this.formsResourceFactory = checkNotNull(formsResourceFactory);
        this.projectSettingsResourceFactory = checkNotNull(projectSettingsResourceFactory);
        this.collaboratorsResourceFactory = checkNotNull(collaboratorsResourceFactory);
        this.accessManager = checkNotNull(accessManager);
        this.projectDetailsManager = checkNotNull(projectDetailsManager);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/")
    public Response getProjectDetails(@Context UserId userId, @Context UriInfo uriInfo) {
        ProjectDetails projectDetails = executor.execute(new GetProjectDetailsAction(projectId), userId)
                                                .getProjectDetails();
        return Response.ok(projectDetails).build();
    }

    @Path("revisions")
    public RevisionsResource getRevisions() {
        return changesResourceFactory.create(projectId);
    }

    @Path("axioms")
    public AxiomsResource getProjectAxiomsResource() {
        return axiomsResourceFactory.create(projectId);
    }

    @Path("delete-axioms")
    public DeleteAxiomsResource getProjectDeleteAxiomsResource() {
        return deleteAxiomsResourceFactory.create(projectId);
    }

    @Path("forms")
    public FormsResource getForms() {
        return formsResourceFactory.create(projectId);
    }

    @Path("settings")
    public ProjectSettingsResource getSettings() {
        return projectSettingsResourceFactory.create(projectId);
    }

    @Path("collaborators")
    public CollaboratorsResource getCollaborators() {
        return collaboratorsResourceFactory.create(projectId);
    }

    /**
     * Moves the project to trash. Used by SCS Manager when an SCS project is deleted.
     */
    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/")
    public Response trashProject(@Context UserId userId) {
        if (!projectDetailsManager.isExistingProject(projectId)) {
            throw new UnknownProjectException(projectId);
        }
        edu.stanford.bmir.protege.web.server.access.ProjectResource resource =
                new edu.stanford.bmir.protege.web.server.access.ProjectResource(projectId);
        boolean allowed = projectDetailsManager.isProjectOwner(userId, projectId)
                || accessManager.hasPermission(forUser(userId), resource, EDIT_SHARING_SETTINGS)
                || accessManager.hasPermission(forUser(userId), ApplicationResource.get(), MOVE_ANY_PROJECT_TO_TRASH);
        if (!allowed) {
            return ResponseUtil.forbidden("You do not have permission to move this project to trash");
        }
        executor.execute(new MoveProjectsToTrashAction(projectId), userId);
        return Response.noContent().build();
    }
}
