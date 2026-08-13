package edu.stanford.bmir.protege.web.server.sharing;

import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.access.ProjectResource;
import edu.stanford.bmir.protege.web.server.app.UserInSessionFactory;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsManager;
import edu.stanford.bmir.protege.web.shared.permissions.PermissionDeniedException;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.project.UnknownProjectException;
import edu.stanford.bmir.protege.web.shared.sharing.SharingPermission;
import edu.stanford.bmir.protege.web.shared.user.UserId;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.server.access.Subject.forUser;
import static edu.stanford.bmir.protege.web.shared.access.BuiltInAction.EDIT_SHARING_SETTINGS;

/**
 * Additive collaborator updates that do not replace the full sharing-settings set.
 * Roles are stored against a {@link UserId} string, so the collaborator does not
 * need a local account yet.
 */
public class ProjectCollaboratorManager {

    @Nonnull
    private final AccessManager accessManager;

    @Nonnull
    private final ProjectDetailsManager projectDetailsManager;

    @Nonnull
    private final UserInSessionFactory userInSessionFactory;

    @Inject
    public ProjectCollaboratorManager(@Nonnull AccessManager accessManager,
                                      @Nonnull ProjectDetailsManager projectDetailsManager,
                                      @Nonnull UserInSessionFactory userInSessionFactory) {
        this.accessManager = checkNotNull(accessManager);
        this.projectDetailsManager = checkNotNull(projectDetailsManager);
        this.userInSessionFactory = checkNotNull(userInSessionFactory);
    }

    public void setCollaborator(@Nonnull ProjectId projectId,
                                @Nonnull UserId actor,
                                @Nonnull String userName,
                                @Nonnull SharingPermission permission) {
        ensureCanEditSharing(projectId, actor);
        UserId collaborator = requireCollaboratorUserId(userName);
        ensureNotOwner(projectId, collaborator);
        accessManager.setAssignedRoles(forUser(collaborator),
                                       new ProjectResource(projectId),
                                       Roles.fromSharingPermission(permission));
    }

    public void removeCollaborator(@Nonnull ProjectId projectId,
                                   @Nonnull UserId actor,
                                   @Nonnull String userName) {
        ensureCanEditSharing(projectId, actor);
        UserId collaborator = requireCollaboratorUserId(userName);
        ensureNotOwner(projectId, collaborator);
        accessManager.setAssignedRoles(forUser(collaborator),
                                       new ProjectResource(projectId),
                                       Collections.emptySet());
    }

    private void ensureCanEditSharing(@Nonnull ProjectId projectId, @Nonnull UserId actor) {
        if (!projectDetailsManager.isExistingProject(projectId)) {
            throw new UnknownProjectException(projectId);
        }
        if (!accessManager.hasPermission(forUser(actor), new ProjectResource(projectId), EDIT_SHARING_SETTINGS)) {
            throw new PermissionDeniedException("You do not have permission to edit sharing settings",
                                                userInSessionFactory.getUserInSession(actor));
        }
    }

    @Nonnull
    private static UserId requireCollaboratorUserId(@Nonnull String userName) {
        String trimmed = checkNotNull(userName).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Collaborator user name must not be empty");
        }
        UserId userId = UserId.getUserId(trimmed);
        if (userId.isGuest()) {
            throw new IllegalArgumentException("The guest user cannot be a collaborator");
        }
        return userId;
    }

    private void ensureNotOwner(@Nonnull ProjectId projectId, @Nonnull UserId collaborator) {
        if (projectDetailsManager.isProjectOwner(collaborator, projectId)) {
            throw new IllegalArgumentException("Cannot change sharing settings for the project owner");
        }
    }
}
