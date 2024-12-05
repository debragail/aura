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
package configuration;

import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import org.auraframework.adapter.RegistryAdapter;
import org.auraframework.integration.test.service.DefinitionServiceImplTest.AuraTestRegistryProviderWithNulls;
import org.auraframework.integration.test.util.SeleniumServerLauncher;
import org.auraframework.test.util.PooledRemoteWebDriverFactory;
import org.auraframework.test.util.RemoteWebDriverFactory;
import org.auraframework.test.util.SauceUtil;
import org.auraframework.test.util.WebDriverProvider;
import org.auraframework.util.ServiceLoaderImpl.AuraConfiguration;
import org.auraframework.util.ServiceLoaderImpl.Impl;
import org.auraframework.util.test.util.TestInventory;
import org.openqa.selenium.net.PortProber;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

@AuraConfiguration
public class AuraIntegrationTestConfig {
    private static WebDriverProvider webDriverProvider = null;

    @Impl(name = "auraIntegrationTestInventory")
    public static TestInventory auraIntegrationTestInventory() throws Exception {
        return new TestInventory(AuraIntegrationTestConfig.class);
    }

    @Impl
    public static RegistryAdapter auraImplTestRegistryAdapterWithNulls() {
        return new AuraTestRegistryProviderWithNulls();
    }

    @Impl
    public synchronized static WebDriverProvider auraWebDriverProvider() throws Exception {
        if (webDriverProvider == null) {
            URL serverUrl;
            boolean runningOnSauceLabs = SauceUtil.areTestsRunningOnSauce();
            try {
                String hubUrlString = System.getProperty(WebDriverProvider.WEBDRIVER_SERVER_PROPERTY);
                if ((hubUrlString != null) && !hubUrlString.equals("")) {
                    if (runningOnSauceLabs) {
                        serverUrl = SauceUtil.getSauceServerUrl();
                    } else {
                        serverUrl = Urls.create(hubUrlString, Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS);
                    }
                } else {

                    int serverPort = PortProber.findFreePort();

                    // quiet the verbose grid logging
                    Logger selLog = Logger.getLogger("org.openqa");
                    selLog.setLevel(Level.SEVERE);

                    SeleniumServerLauncher.start("localhost", serverPort);
                    serverUrl = Urls.create(String.format("http://localhost:%s/wd/hub", serverPort), Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS);
                    System.setProperty(WebDriverProvider.WEBDRIVER_SERVER_PROPERTY, serverUrl.toString());
                }
                Logger.getLogger(AuraIntegrationTestConfig.class.getName()).info("Selenium server url: " + serverUrl);
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error(e);
            }
            if (!runningOnSauceLabs
                    && Boolean.parseBoolean(System.getProperty(WebDriverProvider.REUSE_BROWSER_PROPERTY))) {
                webDriverProvider = new PooledRemoteWebDriverFactory(serverUrl);
            } else {
                webDriverProvider = new RemoteWebDriverFactory(serverUrl);
            }
        }
        return webDriverProvider;
    }
}
