/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 *
 *
 */
public class DistributionTagHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = -1439667766337270066L;

    private final ManagementUIState managementUIState;
    private final CreateUpdateDistributionTagLayoutWindow createORUpdateDistributionTagLayout;

    DistributionTagHeader(final I18N i18n, final ManagementUIState managementUIState,
            final SpPermissionChecker permChecker, final UIEventBus eventBus, final TagManagement tagManagement,
            final EntityFactory entityFactory, final UINotification uiNotification) {
        super(permChecker, eventBus, i18n);
        this.managementUIState = managementUIState;
        this.createORUpdateDistributionTagLayout = new CreateUpdateDistributionTagLayoutWindow(i18n, tagManagement,
                entityFactory, eventBus, permChecker, uiNotification);
        if (hasCreateUpdatePermission()) {
            createORUpdateDistributionTagLayout.init();
        }
    }

    @Override
    protected String getHideButtonId() {
        return "hide.distribution.tags";
    }

    @Override
    protected boolean hasCreateUpdatePermission() {
        return permChecker.hasCreateDistributionPermission() || permChecker.hasUpdateDistributionPermission();
    }

    @Override
    protected String getTitle() {
        return i18n.get("header.filter.tag", new Object[] {});
    }

    @Override
    protected void settingsIconClicked(final ClickEvent event) {
        final Window addUpdateWindow = createORUpdateDistributionTagLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setModal(true);
        addUpdateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected boolean dropHitsRequired() {
        return true;
    }

    @Override
    protected void hideFilterButtonLayout() {
        managementUIState.setDistTagFilterClosed(true);
        eventBus.publish(this, ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return UIComponentIdProvider.ADD_DISTRIBUTION_TAG;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

}
