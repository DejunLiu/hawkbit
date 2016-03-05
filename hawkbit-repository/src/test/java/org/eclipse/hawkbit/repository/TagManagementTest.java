/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Test class for {@link TagManagement}.
 *
 *
 *
 */


// TODO: fully document tests -> @Description for long text and reasonable
// method name as short text
public class TagManagementTest extends AbstractIntegrationTest {
    public TagManagementTest() {
        LOG = LoggerFactory.getLogger(TagManagementTest.class);
    }

    @Test
    
    public void createAndAssignAndDeleteDistributionSetTags() {
        final List<DistributionSet> dsAs = TestDataUtil.generateDistributionSets("DS-A", 20, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsBs = TestDataUtil.generateDistributionSets("DS-B", 10, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsCs = TestDataUtil.generateDistributionSets("DS-C", 25, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsABs = TestDataUtil.generateDistributionSets("DS-AB", 5, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsACs = TestDataUtil.generateDistributionSets("DS-AC", 11, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsBCs = TestDataUtil.generateDistributionSets("DS-BC", 13, softwareManagement,
                distributionSetManagement);
        final List<DistributionSet> dsABCs = TestDataUtil.generateDistributionSets("DS-ABC", 9, softwareManagement,
                distributionSetManagement);

        final DistributionSetTag tagA = tagManagement.createDistributionSetTag(new DistributionSetTag("A"));
        final DistributionSetTag tagB = tagManagement.createDistributionSetTag(new DistributionSetTag("B"));
        final DistributionSetTag tagC = tagManagement.createDistributionSetTag(new DistributionSetTag("C"));
        final DistributionSetTag tagX = tagManagement.createDistributionSetTag(new DistributionSetTag("X"));
        final DistributionSetTag tagY = tagManagement.createDistributionSetTag(new DistributionSetTag("Y"));

        distributionSetManagement.toggleTagAssignment(dsAs, tagA);
        distributionSetManagement.toggleTagAssignment(dsBs, tagB);
        distributionSetManagement.toggleTagAssignment(dsCs, tagC);

        distributionSetManagement.toggleTagAssignment(dsABs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsABs, tagManagement.findDistributionSetTag(tagB.getName()));

        distributionSetManagement.toggleTagAssignment(dsACs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsACs, tagManagement.findDistributionSetTag(tagC.getName()));

        distributionSetManagement.toggleTagAssignment(dsBCs, tagManagement.findDistributionSetTag(tagB.getName()));
        distributionSetManagement.toggleTagAssignment(dsBCs, tagManagement.findDistributionSetTag(tagC.getName()));

        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagB.getName()));
        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagC.getName()));

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setTagNames(
                Lists.newArrayList(tagA.getName()));
        assertEquals(dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setTagNames(
                Lists.newArrayList(tagB.getName()));
        assertEquals(dsBs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setTagNames(
                Lists.newArrayList(tagC.getName()));
        assertEquals(dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setTagNames(
                Lists.newArrayList(tagX.getName()));
        assertEquals(0,
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        assertEquals(5, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        tagManagement.deleteDistributionSetTag(tagY.getName());
        assertEquals(4, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());
        tagManagement.deleteDistributionSetTag(tagX.getName());
        assertEquals(3, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        tagManagement.deleteDistributionSetTag(tagB.getName());
        assertEquals(2, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setTagNames(
                Lists.newArrayList(tagA.getName()));
        assertEquals(dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setTagNames(
                Lists.newArrayList(tagB.getName()));
        assertEquals(0,
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setTagNames(
                Lists.newArrayList(tagC.getName()));
        assertEquals(dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());
    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#findTargetTag(java.lang.String)}
     * .
     */
    @Test
    public void testFindTargetTag() {
        assertThat(targetTagRepository.findAll()).isEmpty();

        final List<TargetTag> tags = createTargetsWithTags();

        assertThat(targetTagRepository.findAll()).isEqualTo(tagManagement.findAllTargetTags()).isEqualTo(tags)
                .hasSize(20);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#createTargetTag(org.eclipse.hawkbit.repository.model.TargetTag)}
     * .
     */
    @Test
    public void testCreateTargetTag() {
        assertThat(targetTagRepository.findAll()).isEmpty();

        final Tag tag = tagManagement.createTargetTag(new TargetTag("kai1", "kai2", "colour"));

        assertThat(targetTagRepository.findByNameEquals("kai1").getDescription()).isEqualTo("kai2");
        assertThat(tagManagement.findTargetTag("kai1").getColour()).isEqualTo("colour");
        assertThat(tagManagement.findTargetTagById(tag.getId()).getColour()).isEqualTo("colour");
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#deleteTargetTag(java.lang.String[])}
     * .
     */
    @Test
    public void testDeleteTargetTagsStringArray() {
        assertThat(targetTagRepository.findAll()).isEmpty();

        // create test data
        final Iterable<TargetTag> tags = createTargetsWithTags();
        final TargetTag toDelete = tags.iterator().next();

        for (final Target target : targetRepository.findAll()) {
            assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getTags())
                    .contains(toDelete);
        }

        // delete
        tagManagement.deleteTargetTag(toDelete.getName());

        // check
        for (final Target target : targetRepository.findAll()) {
            assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getTags()).doesNotContain(
                    toDelete);
        }
        assertThat(targetTagRepository.findOne(toDelete.getId())).isNull();
        assertThat(tagManagement.findAllTargetTags()).hasSize(19);
    }

    @Test
    
    public void updateTargetTag() {
        assertThat(targetTagRepository.findAll()).isEmpty();

        // create test data
        final List<TargetTag> tags = createTargetsWithTags();

        // change data
        final TargetTag savedAssigned = tags.iterator().next();
        savedAssigned.setName("test123");

        // persist
        tagManagement.updateTargetTag(savedAssigned);

        // check data
        assertThat(tagManagement.findAllTargetTags()).hasSize(tags.size());
        assertThat(targetTagRepository.findOne(savedAssigned.getId()).getName()).isEqualTo("test123");
        assertThat(targetTagRepository.findOne(savedAssigned.getId()).getOptLockRevision()).isEqualTo(2);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#createDistributionSetTag(org.eclipse.hawkbit.repository.model.DistributionSetTag)}
     * .
     */
    @Test
    public void testCreateDistributionSetTag() {
        assertThat(distributionSetTagRepository.findAll()).isEmpty();

        final Tag tag = tagManagement.createDistributionSetTag(new DistributionSetTag("kai1", "kai2", "colour"));

        assertThat(distributionSetTagRepository.findByNameEquals("kai1").getDescription()).isEqualTo("kai2");
        assertThat(tagManagement.findDistributionSetTag("kai1").getColour()).isEqualTo("colour");
        assertThat(tagManagement.findDistributionSetTagById(tag.getId()).getColour()).isEqualTo("colour");
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#createDistributionSetTags(java.lang.Iterable)}
     * .
     */
    @Test
    public void testCreateDistributionSetTags() {
        assertThat(distributionSetTagRepository.findAll()).isEmpty();

        final List<DistributionSetTag> tags = createDsSetsWithTags();

        assertThat(distributionSetTagRepository.findAll()).hasSize(tags.size());
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#deleteDistributionSetTag(java.lang.String[])}
     * .
     */
    @Test
    public void testDeleteDistributionSetTag() {
        assertThat(distributionSetTagRepository.findAll()).isEmpty();

        // create test data
        final Iterable<DistributionSetTag> tags = createDsSetsWithTags();
        final DistributionSetTag toDelete = tags.iterator().next();

        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetManagement.findDistributionSetByIdWithDetails(set.getId()).getTags()).contains(
                    toDelete);
        }

        // delete
        tagManagement.deleteDistributionSetTag(tags.iterator().next().getName());

        // check
        assertThat(distributionSetTagRepository.findOne(toDelete.getId())).isNull();
        assertThat(tagManagement.findAllDistributionSetTags()).hasSize(19);
        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetManagement.findDistributionSetByIdWithDetails(set.getId()).getTags())
                    .doesNotContain(toDelete);
        }
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void testFailedDuplicateTargetTagNameException() {
        tagManagement.createTargetTag(new TargetTag("A"));
        tagManagement.createTargetTag(new TargetTag("A"));
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void testFailedDuplicateDsTagNameException() {
        tagManagement.createDistributionSetTag(new DistributionSetTag("A"));
        tagManagement.createDistributionSetTag(new DistributionSetTag("A"));
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#updateDistributionSetTag(org.eclipse.hawkbit.repository.model.DistributionSetTag)}
     * .
     */
    @Test
    public void testUpdateDistributionSetTag() {
        assertThat(distributionSetTagRepository.findAll()).isEmpty();

        // create test data
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // change data
        final DistributionSetTag savedAssigned = tags.iterator().next();
        savedAssigned.setName("test123");

        // persist
        tagManagement.updateDistributionSetTag(savedAssigned);

        // check data
        assertThat(tagManagement.findAllDistributionSetTags()).hasSize(tags.size());
        assertThat(distributionSetTagRepository.findOne(savedAssigned.getId()).getName()).isEqualTo("test123");
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.TagManagement#findAllDistributionSetTags()}
     * .
     */
    @Test
    public void testFindDistributionSetTagsAll() {
        assertThat(distributionSetTagRepository.findAll()).isEmpty();

        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // test
        assertThat(tagManagement.findAllDistributionSetTags()).hasSize(tags.size());
        assertThat(distributionSetTagRepository.findAll()).hasSize(20);
    }

    private List<TargetTag> createTargetsWithTags() {
        targetManagement.createTargets(TestDataUtil.generateTargets(20));
        final Iterable<TargetTag> tags = tagManagement.createTargetTags(TestDataUtil.generateTargetTags(20));

        tags.forEach(tag -> targetManagement.toggleTagAssignment(targetRepository.findAll(), tag));

        return tagManagement.findAllTargetTags();
    }

    private List<DistributionSetTag> createDsSetsWithTags() {

        final List<DistributionSet> sets = TestDataUtil.generateDistributionSets(20, softwareManagement,
                distributionSetManagement);
        final Iterable<DistributionSetTag> tags = tagManagement.createDistributionSetTags(TestDataUtil
                .generateDistributionSetTags(20));

        tags.forEach(tag -> distributionSetManagement.toggleTagAssignment(sets, tag));

        return tagManagement.findAllDistributionSetTags();
    }
}
