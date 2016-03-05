/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

/**
 * Test for {@link SoftwareModuleTypeResource}.
 *
 *
 *
 *
 */


public class SoftwareModuleTypeResourceTest extends AbstractIntegrationTest {

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    
    public void getSoftwareModuleTypes() throws Exception {
        SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));
        testType.setDescription("Desc1234");
        testType = softwareManagement.updateSoftwareModuleType(testType);

        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content.[?(@.key==" + osType.getKey() + ")][0].name", equalTo(osType.getName())))
                .andExpect(jsonPath("$content.[?(@.key==" + osType.getKey() + ")][0].description",
                        equalTo(osType.getDescription())))
                .andExpect(jsonPath("$content.[?(@.key==" + osType.getKey() + ")][0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("$content.[?(@.key==" + osType.getKey() + ")][0].key", equalTo("os")))
                .andExpect(jsonPath("$content.[?(@.key==" + runtimeType.getKey() + ")][0].name",
                        equalTo(runtimeType.getName())))
                .andExpect(jsonPath("$content.[?(@.key==" + runtimeType.getKey() + ")][0].description",
                        equalTo(runtimeType.getDescription())))
                .andExpect(jsonPath("$content.[?(@.key==" + runtimeType.getKey() + ")][0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("$content.[?(@.key==" + runtimeType.getKey() + ")][0].key", equalTo("runtime")))
                .andExpect(
                        jsonPath("$content.[?(@.key==" + appType.getKey() + ")][0].name", equalTo(appType.getName())))
                .andExpect(jsonPath("$content.[?(@.key==" + appType.getKey() + ")][0].description",
                        equalTo(appType.getDescription())))
                .andExpect(jsonPath("$content.[?(@.key==" + appType.getKey() + ")][0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("$content.[?(@.key==" + appType.getKey() + ")][0].key", equalTo("application")))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].name", equalTo("TestName123")))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].lastModifiedAt",
                        equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$content.[?(@.key==test123)][0].key", equalTo("test123")))
                .andExpect(jsonPath("$total", equalTo(4)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    
    public void createSoftwareModuleTypes() throws JSONException, Exception {

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(new SoftwareModuleType("test1", "TestName1", "Desc1", 1));
        types.add(new SoftwareModuleType("test2", "TestName2", "Desc2", 2));
        types.add(new SoftwareModuleType("test3", "TestName3", "Desc3", 3));

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo("TestName1"))).andExpect(jsonPath("[0].key", equalTo("test1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("[1].name", equalTo("TestName2"))).andExpect(jsonPath("[1].key", equalTo("test2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].maxAssignments", equalTo(2)))
                .andExpect(jsonPath("[2].name", equalTo("TestName3"))).andExpect(jsonPath("[2].key", equalTo("test3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].maxAssignments", equalTo(3))).andReturn();

        final SoftwareModuleType created1 = softwareManagement.findSoftwareModuleTypeByKey("test1");
        final SoftwareModuleType created2 = softwareManagement.findSoftwareModuleTypeByKey("test2");
        final SoftwareModuleType created3 = softwareManagement.findSoftwareModuleTypeByKey("test3");

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created1.getId());
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created2.getId());
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created3.getId());

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(6);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    
    public void getSoftwareModuleType() throws Exception {
        SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));
        testType.setDescription("Desc1234");
        testType = softwareManagement.updateSoftwareModuleType(testType);

        mvc.perform(get("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$name", equalTo("TestName123")))
                .andExpect(jsonPath("$description", equalTo("Desc1234")))
                .andExpect(jsonPath("$maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$lastModifiedAt", equalTo(testType.getLastModifiedAt())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    
    public void deleteSoftwareModuleTypeUnused() throws Exception {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(3);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    
    public void deleteSoftwareModuleTypeUsed() throws Exception {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));
        softwareManagement
                .createSoftwareModule(new SoftwareModule(testType, "name", "version", "description", "vendor"));

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(4);
        assertThat(softwareModuleTypeRepository.count()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleTypeRepository.count()).isEqualTo(4);
        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(3);
    }

    @Test
    
    public void updateSoftwareModuleTypeOnlyDescriptionAndNameUntouched() throws Exception {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/softwaremoduletypes/{smId}", testType.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$description", equalTo("foobardesc")))
                .andExpect(jsonPath("$name", equalTo("TestName123"))).andReturn();

    }

    @Test
    
    public void getSoftwareModuleTypesWithoutAddtionalRequestParameters() throws Exception {
        final int types = 3;
        mvc.perform(get(RestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    
    public void getSoftwareModuleTypesWithPagingLimitRequestParameter() throws Exception {
        final int types = 3;
        final int limitSize = 1;
        mvc.perform(get(RestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
                .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    
    public void getSoftwareModuleTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int types = 3;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(RestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
                .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(TargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    
    public void invalidRequestsOnSoftwaremoduleTypesResource() throws Exception {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(testType);

        // SM does not exist
        mvc.perform(get("/rest/v1/softwaremoduletypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremoduletypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/softwaremoduletypes").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/softwaremoduletypes").content("sdfjsdlkjfskdjf".getBytes())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/softwaremoduletypes").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremoduletypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremoduletypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    
    public void searchSoftwareModuleTypeRsql() throws Exception {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test123", "TestName123", "Desc123", 5));
        final SoftwareModuleType testType2 = softwareManagement
                .createSoftwareModuleType(new SoftwareModuleType("test1234", "TestName1234", "Desc123", 5));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get("/rest/v1/softwaremoduletypes?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));

    }

    private void createSoftwareModulesAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            final SoftwareModule softwareModule = new SoftwareModule(osType, str, str, str, str);

            softwareManagement.createSoftwareModule(softwareModule);
            character++;
        }
    }

}
