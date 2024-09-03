/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.integration.test.validation;

import com.google.common.base.Charsets;
import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.auraframework.integration.test.util.AuraHttpTestCase;
import org.auraframework.util.AuraFiles;
import org.auraframework.util.json.JsonEncoder;
import org.auraframework.util.test.annotation.UnAdaptableTest;
import org.auraframework.util.validation.ValidationError;
import org.auraframework.util.validation.ValidationTestUtil;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Testing validation tool functionality. UnAdaptableTest because requires source to check present in file system.
 */
@UnAdaptableTest
public final class AuraValidationServletHttpTest extends AuraHttpTestCase {

    private HttpRequestBase method;

    @Override
    public void tearDown() throws Exception {
        try {
            if (method != null) {
                method.releaseConnection();
                method = null;
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testServlet() throws Exception {
        String path = AuraFiles.Core.getPath() + "/aura-components/src/test/components/validationTest/basic";
        assertTrue(path, new File(path).exists());
        method = obtainGetMethod("/qa/auraValidation?path=" + path);
        HttpResponse response = perform(method);
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertDefaultAntiClickjacking(response, true, false);
        String contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
        assertTrue(contentType, contentType.contains(JsonEncoder.MIME_TYPE));
        assertTrue(contentType, contentType.contains(Charsets.UTF_8.toString()));

        String content = getResponseBody(response);
        List<String> errors = ValidationError.parseErrors(content);

        ValidationTestUtil.verifyValidationTestBasicErrors(errors);
    }

    /**
     * Same as testServlet(), but only uses JDK classes
     */
    @Test
    public void testServletStandalone() throws Exception {
        String path = AuraFiles.Core.getPath() + "/aura-components/src/test/components/validationTest/basic";
        assertTrue(path, new File(path).exists());
        String url = getTestServletConfig().getBaseUrl().toURI().resolve("/qa/auraValidation?path=" + path).toString();

        InputStream stream = Urls.create(url, Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS).openStream();
        List<String> errors = ValidationError.parseErrors(new InputStreamReader(stream));

        ValidationTestUtil.verifyValidationTestBasicErrors(errors);
    }
}
