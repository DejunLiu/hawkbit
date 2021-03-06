/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Update action resource.
 */
public class DdiDeploymentBase extends ResourceSupport {

    @JsonProperty("id")
    @NotNull
    private final String deplyomentId;

    @NotNull
    private final DdiDeployment deployment;

    /**
     * Constructor.
     *
     * @param id
     *            of the update action
     * @param deployment
     *            details.
     */
    @JsonCreator
    public DdiDeploymentBase(@JsonProperty("id") final String id,
            @JsonProperty("deplyomentId") final DdiDeployment deployment) {
        this.deplyomentId = id;
        this.deployment = deployment;
    }

    public DdiDeployment getDeployment() {
        return deployment;
    }

    @Override
    public String toString() {
        return "DeploymentBase [id=" + deplyomentId + ", deployment=" + deployment + "]";
    }

}
