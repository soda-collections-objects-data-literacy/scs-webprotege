package edu.stanford.bmir.protege.web.server.api.resources;

import edu.stanford.bmir.protege.web.server.sharing.ProjectCollaboratorManager;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.sharing.SharingPermission;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * REST wrapper around {@link ProjectCollaboratorManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CollaboratorsResource_TestCase {

    private CollaboratorsResource resource;

    @Mock
    private ProjectCollaboratorManager collaboratorManager;

    private ProjectId projectId;

    private UserId actor;

    @Before
    public void setUp() {
        projectId = ProjectId.get(UUID.randomUUID().toString());
        actor = UserId.getUserId("scs-manager");
        resource = new CollaboratorsResource(projectId, collaboratorManager);
    }

    @Test
    public void shouldPutCollaborator() {
        Response response = resource.putCollaborator(actor, "alice", new CollaboratorPermission(SharingPermission.EDIT));
        assertThat(response.getStatus(), is(204));
        verify(collaboratorManager).setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
    }

    @Test
    public void shouldRejectMissingPermissionBody() {
        Response response = resource.putCollaborator(actor, "alice", null);
        assertThat(response.getStatus(), is(400));
        verifyZeroInteractions(collaboratorManager);
    }

    @Test
    public void shouldDeleteCollaborator() {
        Response response = resource.deleteCollaborator(actor, "alice");
        assertThat(response.getStatus(), is(204));
        verify(collaboratorManager).removeCollaborator(projectId, actor, "alice");
    }

    @Test
    public void shouldMapIllegalArgumentToBadRequest() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Cannot change sharing settings for the project owner"))
                .when(collaboratorManager)
                .setCollaborator(projectId, actor, "alice", SharingPermission.EDIT);
        Response response = resource.putCollaborator(actor, "alice", new CollaboratorPermission(SharingPermission.EDIT));
        assertThat(response.getStatus(), is(400));
    }
}
