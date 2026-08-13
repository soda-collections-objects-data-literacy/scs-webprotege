package edu.stanford.bmir.protege.web.server.sharing;

import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.access.Resource;
import edu.stanford.bmir.protege.web.server.access.Subject;
import edu.stanford.bmir.protege.web.server.app.UserInSessionFactory;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsManager;
import edu.stanford.bmir.protege.web.shared.access.BuiltInAction;
import edu.stanford.bmir.protege.web.shared.access.BuiltInRole;
import edu.stanford.bmir.protege.web.shared.app.UserInSession;
import edu.stanford.bmir.protege.web.shared.permissions.PermissionDeniedException;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.project.UnknownProjectException;
import edu.stanford.bmir.protege.web.shared.sharing.SharingPermission;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additive collaborator REST support used by SCS Manager membership sync.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectCollaboratorManager_TestCase {

    private ProjectCollaboratorManager manager;

    @Mock
    private AccessManager accessManager;

    @Mock
    private ProjectDetailsManager projectDetailsManager;

    @Mock
    private UserInSessionFactory userInSessionFactory;

    @Mock
    private UserInSession userInSession;

    private ProjectId projectId;

    private UserId actor;

    private UserId collaborator;

    @Before
    public void setUp() {
        projectId = ProjectId.get(UUID.randomUUID().toString());
        actor = UserId.getUserId("scs-manager");
        collaborator = UserId.getUserId("alice");
        manager = new ProjectCollaboratorManager(accessManager, projectDetailsManager, userInSessionFactory);
        when(projectDetailsManager.isExistingProject(projectId)).thenReturn(true);
        when(accessManager.hasPermission(eq(Subject.forUser(actor)),
                                         any(Resource.class),
                                         eq(BuiltInAction.EDIT_SHARING_SETTINGS))).thenReturn(true);
        when(projectDetailsManager.isProjectOwner(collaborator, projectId)).thenReturn(false);
        when(userInSessionFactory.getUserInSession(any())).thenReturn(userInSession);
    }

    @Test
    public void shouldGrantEditToCollaborator() {
        manager.setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
        verify(accessManager).setAssignedRoles(eq(Subject.forUser(collaborator)),
                                               any(Resource.class),
                                               eq(Roles.fromSharingPermission(SharingPermission.EDIT)));
    }

    @Test
    public void shouldRemoveCollaboratorRoles() {
        manager.removeCollaborator(projectId, actor, "alice");
        verify(accessManager).setAssignedRoles(eq(Subject.forUser(collaborator)),
                                               any(Resource.class),
                                               eq(Collections.emptySet()));
    }

    @Test(expected = PermissionDeniedException.class)
    public void shouldDenyWhenActorCannotEditSharing() {
        when(accessManager.hasPermission(eq(Subject.forUser(actor)),
                                         any(Resource.class),
                                         eq(BuiltInAction.EDIT_SHARING_SETTINGS))).thenReturn(false);
        manager.setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
    }

    @Test(expected = UnknownProjectException.class)
    public void shouldRejectUnknownProject() {
        when(projectDetailsManager.isExistingProject(projectId)).thenReturn(false);
        manager.setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyUserName() {
        manager.setCollaborator(projectId, actor, "  ", SharingPermission.EDIT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOwnerAsCollaborator() {
        when(projectDetailsManager.isProjectOwner(collaborator, projectId)).thenReturn(true);
        manager.setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
    }

    @Test
    public void shouldAssignCanEditRoleId() {
        Collection<?> roles = Roles.fromSharingPermission(SharingPermission.EDIT);
        org.junit.Assert.assertTrue(roles.contains(BuiltInRole.CAN_EDIT.getRoleId()));
    }
}
