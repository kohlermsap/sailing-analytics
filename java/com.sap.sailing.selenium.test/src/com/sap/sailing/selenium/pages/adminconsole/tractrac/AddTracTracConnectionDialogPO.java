package com.sap.sailing.selenium.pages.adminconsole.tractrac;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class AddTracTracConnectionDialogPO extends DataEntryDialogPO {
    
    // TODO [D049941]: Prefix the Ids with the component (e.g. "Button")
    @FindBy(how = BySeleniumId.class, using = "LiveURITextBox")
    private WebElement liveURITextBox;
    
    @FindBy(how = BySeleniumId.class, using = "StoredURITextBox")
    private WebElement storedURITextBox;
    
    @FindBy(how = BySeleniumId.class, using = "JsonURLTextBox")
    private WebElement jsonURLTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "TracTracApiTokenTextBox")
    private WebElement tracTracApiTokenTextBox;

    protected AddTracTracConnectionDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void setJsonUrl(String jsonUrl) {
        jsonURLTextBox.clear();
        jsonURLTextBox.sendKeys(jsonUrl);
    }
    
    public void setTracTracApiToken(String tracTracApiToken) {
        tracTracApiTokenTextBox.clear();
        tracTracApiTokenTextBox.sendKeys(tracTracApiToken);
    }
}
