/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Software module type filter buttons layout.
 */
public class SMTypeFilterLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 1581066345157393665L;

    private final ArtifactUploadState artifactUploadState;

    public SMTypeFilterLayout(final ArtifactUploadState artifactUploadState, final I18N i18n,
            final SpPermissionChecker permChecker, final UIEventBus eventBus, final TagManagement tagManagement,
            final EntityFactory entityFactory, final UINotification uiNotification,
            final SoftwareManagement softwareManagement, final UploadViewClientCriterion uploadViewClientCriterion) {
        super(new SMTypeFilterHeader(i18n, permChecker, eventBus, artifactUploadState, tagManagement, entityFactory,
                uiNotification, softwareManagement),
                new SMTypeFilterButtons(eventBus, artifactUploadState, uploadViewClientCriterion, softwareManagement));

        this.artifactUploadState = artifactUploadState;
        restoreState();

        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.HIDE_FILTER_BY_TYPE) {
            setVisible(false);
        }
        if (event == UploadArtifactUIEvent.SHOW_FILTER_BY_TYPE) {
            setVisible(true);
        }
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return artifactUploadState.isSwTypeFilterClosed();
    }

}
