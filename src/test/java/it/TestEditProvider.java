package it;

import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.pageobjects.elements.query.Poller;
import com.pawelniewiadomski.jira.openid.authentication.activeobjects.OpenIdProvider;
import it.pageobjects.AddProviderPage;
import it.pageobjects.ConfigurationPage;
import it.pageobjects.EditProviderPage;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static it.pageobjects.AddProviderPage.hasErrorMessage;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class TestEditProvider extends BaseJiraWebTest {

    @Before
    public void setUp() {
        jira.backdoor().restoreBlankInstance();
        jira.backdoor().project().addProject("Test", "TST", "admin");
    }

    @Test
    public void editOpenIdProvider()
    {
        final String name = "Testing";
        final String endpointUrl = "http://asdkasjdkald.pl";

        AddProviderPage addPage = jira.visit(AddProviderPage.class);

        addPage.setProviderType("OpenID 1.0");
        addPage.setName(name);
        addPage.setEndpointUrl(endpointUrl);
        addPage.setExtensionNamespace("ext1");

        ConfigurationPage configurationPage = addPage.save();

        EditProviderPage editPage = configurationPage.editProvider("Testing");
        waitUntil(editPage.getName(), (Matcher<String>) equalTo(name));
        waitUntil(editPage.getEndpointUrl(), (Matcher<String>) equalTo(endpointUrl));

        editPage.setName("");
        editPage.setEndpointUrl("");
        editPage.setExtensionNamespace("");

        assertThat(editPage.getFormError("name"), hasErrorMessage("Please provide the name."));
        assertThat(editPage.getFormError("endpointUrl"), hasErrorMessage("Please provide the provider URL."));
        assertThat(editPage.getFormError("extensionNamespace"), hasErrorMessage("Please provide the alias."));
    }

    @Test
    public void testEditOpenIdProvider() {
        final String name = "Testing";
        final String endpointUrl = "http://asdkasjdkald.pl";

        AddProviderPage addPage = jira.visit(AddProviderPage.class);

        addPage.setProviderType("OpenID 1.0");
        addPage.setName(name);
        addPage.setEndpointUrl(endpointUrl);
        addPage.setExtensionNamespace("ext1");

        ConfigurationPage configurationPage = addPage.save();

        EditProviderPage editPage = configurationPage.editProvider("Testing");
        waitUntil(editPage.getName(), (Matcher<String>) equalTo(name));
        waitUntil(editPage.getEndpointUrl(), (Matcher<String>) equalTo(endpointUrl));

        configurationPage = editPage.setName("New Name").setEndpointUrl("http://wp.pl").setExtensionNamespace("testing").save();
        editPage = configurationPage.editProvider("New Name");
        waitUntil(editPage.getName(), (Matcher<String>) equalTo("New Name"));
        waitUntil(editPage.getEndpointUrl(), (Matcher<String>) equalTo("http://wp.pl"));
        waitUntil(editPage.getExtensionNamespace(), (Matcher<String>) equalTo("testing"));
    }

    @Test
    public void testEditOpenIdConnectProvider() {
        AddProviderPage addPage = jira.visit(AddProviderPage.class);

        addPage.setProviderType("OpenID Connect/OAuth 2.0")
                .setName("OAuth")
                .setEndpointUrl("https://accounts.google.com")
                .setClientId("AAA")
                .setClientSecret("BBB");

        ConfigurationPage configurationPage = addPage.save();

        EditProviderPage editPage = configurationPage.editProvider("OAuth");
        waitUntil(editPage.getName(), (Matcher<String>) equalTo("OAuth"));
        waitUntil(editPage.getEndpointUrl(), (Matcher<String>) equalTo("https://accounts.google.com"));
        waitUntil(editPage.getClientId(), (Matcher<String>) equalTo("AAA"));
        waitUntil(editPage.getClientSecret(), (Matcher<String>) equalTo("BBB"));

        configurationPage = editPage.setName("New Name")
                .setClientId("NNN")
                .setClientSecret("GGG")
                .save();
        editPage = configurationPage.editProvider("New Name");
        waitUntil(editPage.getName(), (Matcher<String>) equalTo("New Name"));
        waitUntil(editPage.getEndpointUrl(), (Matcher<String>) equalTo("https://accounts.google.com"));
        waitUntil(editPage.getClientId(), (Matcher<String>) equalTo("NNN"));
        waitUntil(editPage.getClientSecret(), (Matcher<String>) equalTo("GGG"));
    }

    @Test
    public void testEditGoogleProvider() {
        AddProviderPage addPage = jira.visit(AddProviderPage.class);

        addPage.setProviderType("Google Apps")
                .setClientId("AAA")
                .setClientSecret("BBB");

        ConfigurationPage configurationPage = addPage.save();

        EditProviderPage editPage = configurationPage.editProvider("Google");
        waitUntilFalse(editPage.isEndpointUrlVisible());
        waitUntilTrue(editPage.isAllowedDomainsVisible());
        waitUntil(editPage.getClientId(), (Matcher<String>) equalTo("AAA"));
        waitUntil(editPage.getClientSecret(), (Matcher<String>) equalTo("BBB"));

        configurationPage = editPage
                .setClientId("NNN")
                .setClientSecret("GGG")
                .setAllowedDomains("test.pl")
                .save();

        editPage = configurationPage.editProvider("Google");
        waitUntil(editPage.getClientId(), (Matcher<String>) equalTo("NNN"));
        waitUntil(editPage.getClientSecret(), (Matcher<String>) equalTo("GGG"));
        waitUntil(editPage.getAllowedDomains(), (Matcher<String>) equalTo("test.pl"));
    }
}
