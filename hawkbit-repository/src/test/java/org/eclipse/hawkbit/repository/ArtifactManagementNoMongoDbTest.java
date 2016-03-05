/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Addition tests next to {@link ArtifactManagementTest} with no running MongoDB
 *
 *
 *
 *
 */


public class ArtifactManagementNoMongoDbTest extends AbstractIntegrationTest {

    @BeforeClass
    public static void initialize() {
        // set property to mongoPort which does not start any mongoDB of
        // parallel test execution
        System.setProperty("spring.data.mongodb.port", "1020");
    }

    @Test(expected = ArtifactUploadFailedException.class)
    
    public void createLocalArtifactWithMongoDbDown() throws IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
    }

}
