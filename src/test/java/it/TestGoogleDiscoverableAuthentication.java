package it;

import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.pages.ViewProfilePage;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.pageobjects.DelayedBinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.base.Preconditions;
import it.pageobjects.AddProviderPage;
import it.pageobjects.ErrorPage;
import it.pageobjects.OpenIdLoginPage;
import it.pageobjects.google.GoogleAccountChooserPage;
import it.pageobjects.google.GoogleApprovePage;
import it.pageobjects.google.GoogleLoginPage;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static org.hamcrest.CoreMatchers.containsString;

public class TestGoogleDiscoverableAuthentication extends BaseJiraWebTest {

    final static Map<String, Object> passwords = ItEnvironment.getConfiguration();

    @BeforeClass
    public static void setUp() throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        jira.backdoor().restoreBlankInstance();
        jira.backdoor().plugins().setPluginLicense(ItEnvironment.PLUGIN_KEY, ItEnvironment.LICENSE_3HR);
        jira.backdoor().project().addProject("Test", "TST", "admin");

        AddProviderPage addProvider = jira.gotoLoginPage().loginAsSysAdmin(AddProviderPage.class);
        addProvider.setProviderType("OpenID Connect/OAuth 2.0")
                .setName("Google")
                .setEndpointUrl("https://accounts.google.com")
                .setCallbackId((String) getProperty(passwords, "google.callbackId"))
                .setClientId((String) getProperty(passwords, "google.clientId"))
                .setClientSecret((String) getProperty(passwords, "google.clientSecret"))
                .setAllowedDomains("test.pl, teamstatus.tv, abc.pl").save();

        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    @After
    public void tearDown() {
        jira.getTester().getDriver().manage().deleteAllCookies();
        jira.getTester().getDriver().navigate().to("https://accounts.google.com/Logout?hl=en&continue=https://www.google.pl/");
    }

    @Test
    @LoginAs(anonymous = true)
    public void testLogInWithinAllowedDomainsWork() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        OpenIdLoginPage loginPage = jira.visit(OpenIdLoginPage.class);
        Poller.waitUntilTrue(loginPage.isOpenIdButtonVisible());
        loginPage.getOpenIdProviders().openAndClick(By.id("openid-1"));

        googleDance((String) getProperty(passwords, "teamstatus.user"), (String) getProperty(passwords, "teamstatus.password"));

        jira.getPageBinder().bind(DashboardPage.class);
    }

    protected void googleDance(String email, String password)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(password);

        GoogleLoginPage googleLoginPage;

        DelayedBinder<GoogleLoginPage> delayedLoginPage = jira.getPageBinder().delayedBind(GoogleLoginPage.class);
        if (delayedLoginPage.canBind()) {
            googleLoginPage = delayedLoginPage.bind();
            if (!googleLoginPage.isEmailVisible()) {
                googleLoginPage = googleLoginPage.selectDifferentAccount().addAccount();
            }
        } else {
            googleLoginPage = jira.getPageBinder().bind(GoogleAccountChooserPage.class).addAccount();
        }

        googleLoginPage.setEmail(email);
        googleLoginPage.setPassword(password);

        Poller.waitUntilTrue(googleLoginPage.isSignInEnabled());
        DelayedBinder<GoogleApprovePage> approvePage = googleLoginPage.signIn();
        if (approvePage.canBind()) {
            approvePage.bind().approve();
        }
    }

    @Test
    @LoginAs(anonymous = true)
    public void testLogInOutsideAllowedDomainsIsProhibited() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        OpenIdLoginPage loginPage = jira.visit(OpenIdLoginPage.class);
        Poller.waitUntilTrue(loginPage.isOpenIdButtonVisible());
        loginPage.getOpenIdProviders().openAndClick(By.id("openid-1"));

        googleDance((String) getProperty(passwords, "gmail.user"), (String) getProperty(passwords, "gmail.password"));

        ErrorPage errorPage = jira.getPageBinder().bind(ErrorPage.class);
        Poller.waitUntil(errorPage.getErrorMessage(), containsString("allowed domains"));
    }

    @Test
    @LoginAs(anonymous = true)
    public void testLogInRedirectsToReturnUrl() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        jira.getTester().getDriver().navigate().to(jira.getProductInstance().getBaseUrl()
                + "/login.jsp?os_destination=%2Fsecure%2FViewProfile.jspa");
        OpenIdLoginPage loginPage = jira.getPageBinder().bind(OpenIdLoginPage.class);
        Poller.waitUntilTrue(loginPage.isOpenIdButtonVisible());
        loginPage.getOpenIdProviders().openAndClick(By.id("openid-1"));

        googleDance((String) getProperty(passwords, "teamstatus.user"), (String) getProperty(passwords, "teamstatus.password"));

        jira.getPageBinder().bind(ViewProfilePage.class);
    }
}
