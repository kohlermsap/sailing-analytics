package com.sap.sailing.selenium.pages.adminconsole.advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class LocalServerPO extends PageArea {

    public LocalServerPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    @FindBy(how = BySeleniumId.class, using = "isSelfServiceServerCheckbox-input")
    private WebElement isSelfServiceServerCheckbox;
    
    @FindBy(how = BySeleniumId.class, using = "isPublicServerCheckbox-input")
    private WebElement isPublicServerCheckbox;
    
    @FindBy(how = BySeleniumId.class, using = "isStandaloneServerCheckbox-input")
    private WebElement isStandaloneServerCheckbox;

    public void setSelfServiceServer(boolean selfService) {
        if (selfService != isSelfServiceServerCheckbox.isSelected()) {
            isSelfServiceServerCheckbox.click();
            awaitServerConfigurationUpdated();
        }
    }
    
    public void setPublicServer(boolean publicServer) {
        if (publicServer != isPublicServerCheckbox.isSelected()) {
            isPublicServerCheckbox.click();
            awaitServerConfigurationUpdated();
        }
    }
    
    public void setStandaloneServer(boolean standalone) {
        if (standalone != isStandaloneServerCheckbox.isSelected()) {
            isStandaloneServerCheckbox.click();
            awaitServerConfigurationUpdated();
        }
    }

    private void awaitServerConfigurationUpdated() {
        while (isServerConfigurationUpdating()) {
        }
    }

    public boolean isServerConfigurationUpdating() {
        final String updating = isSelfServiceServerCheckbox.getAttribute("updating");
        return "true".equals(updating);
    }

}
