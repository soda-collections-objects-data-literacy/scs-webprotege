package edu.stanford.bmir.protege.web.server.api.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.bmir.protege.web.shared.sharing.SharingPermission;

import javax.annotation.Nullable;

/**
 * Request body for PUT /data/projects/{id}/collaborators/{userName}.
 */
public class CollaboratorPermission {

    public static final String PERMISSION = "permission";

    @Nullable
    private final SharingPermission permission;

    @JsonCreator
    public CollaboratorPermission(@JsonProperty(PERMISSION) @Nullable SharingPermission permission) {
        this.permission = permission;
    }

    @Nullable
    @JsonProperty(PERMISSION)
    public SharingPermission getPermission() {
        return permission;
    }
}
