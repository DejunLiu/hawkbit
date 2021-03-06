/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetTypeUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Create/build implementation.
 *
 */
public class JpaDistributionSetTypeCreate extends AbstractDistributionSetTypeUpdateCreate<DistributionSetTypeCreate>
        implements DistributionSetTypeCreate {

    private final SoftwareManagement softwareManagement;

    JpaDistributionSetTypeCreate(final SoftwareManagement softwareManagement) {
        this.softwareManagement = softwareManagement;
    }

    @Override
    public JpaDistributionSetType build() {
        final JpaDistributionSetType result = new JpaDistributionSetType(key, name, description, colour);

        findSoftwareModuleTypeWithExceptionIfNotFound(mandatory).forEach(result::addMandatoryModuleType);
        findSoftwareModuleTypeWithExceptionIfNotFound(optional).forEach(result::addOptionalModuleType);

        return result;
    }

    private Collection<SoftwareModuleType> findSoftwareModuleTypeWithExceptionIfNotFound(
            final Collection<Long> softwareModuleTypeId) {
        if (CollectionUtils.isEmpty(softwareModuleTypeId)) {
            return Collections.emptyList();
        }

        final Collection<SoftwareModuleType> module = softwareManagement
                .findSoftwareModuleTypesById(softwareModuleTypeId);
        if (module.size() < softwareModuleTypeId.size()) {
            throw new EntityNotFoundException(
                    "SoftwareModules types out of the range {" + softwareModuleTypeId + "} due not exist");
        }

        return module;
    }

}
