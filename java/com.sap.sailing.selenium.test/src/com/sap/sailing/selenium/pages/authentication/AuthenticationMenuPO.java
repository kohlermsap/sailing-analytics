package com.sap.sailing.selenium.pages.authentication;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.common.AttributeHelper;

public class AuthenticationMenuPO extends PageArea {
    
    public AuthenticationMenuPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public boolean isOpen() {
        return AttributeHelper.isEnabled(getWebElement(), "data-open");
    }
    
    public boolean isLoggedIn() {
        return AttributeHelper.isEnabled(getWebElement(), "data-auth");
    }
    
    public void doLogin(String username, String password) {
        AuthenticationViewPO authenticationView = showAuthenticationView();
        authenticationView.getSignInView().doLogin(username, password);
        waitUntil(this::isLoggedIn);
    }
    
    private AuthenticationViewPO showAuthenticationView() {
        getWebElement().click();
        waitUntil(this::isOpen);
        return getPO(AuthenticationViewPO::new, "authenticationView");
    }
    
}
