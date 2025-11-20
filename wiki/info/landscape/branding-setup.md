# Rebranding and Debranding Configuration Setup

This document explains how to integrate your own branding into **Sailing Analytics** or switch to the **debranded** version.  
It provides step-by-step guidance on configuring, extending, and testing branding functionality.

---

## 1. Changing Between Branding and Debranding Modes

Branding configuration can be controlled via **system properties**.  
Add the following parameter when starting the application:

```bash
-Dcom.sap.sse.branding=YOUR_BRAND_NAME
```

* `YOUR_BRAND_NAME` corresponds to the ID you will define later in your custom branding configuration.
* If no system property is specified, the **debranded version** of the application will be displayed by default.

---

## 2. Enabling Branding Support

To enable custom branding, you need to configure:

* Logos and icons
* Titles and descriptions
* Localized text (via `sailingServerStringMessages`)

As a reference, review the project:
`com.sap.sse.branding.sap`

### Steps to Create and Register a Branding Project

1. **Create a new web application project**

   * Navigate to `Eclipse → File → New → Plug-in Development → Plug-in Project`
   * Place it under the `java` folder in your workspace
   *  A branding bundle must register its implementation of `BrandingConfiguration` in the **OSGi Service Registry**. So, provide an `Activator` that registers your Branding Service.

2. **Register your bundle**

   * Add the new bundle to the following files:

     * `raceanalysis.product`
     * `pom.xml` in the `git/java` folder
     * `feature.xml` (within `com.sap.sse.feature`)

   More information about project and bundle requirements can be found here:
   [Adding an OSGi bundle](https://wiki.sapsailing.com/wiki/info/landscape/typical-development-scenarios.md#adding-an-osgi-bundle) and
   [Bundles structure](https://wiki.sapsailing.com/wiki/info/general/workspace-bundles-projects-structure.md)

---

## 3. Available Branding Configuration Options

Create your own configuration class, for example:

```
YourBrandNameBrandingConfiguration.java
```

This class must **implement** the `BrandingConfiguration` interface (see: `SAPBrandingConfiguration.java`). All current configuration methods' purpose is explained in the Javadoc on the interface itself: see `java/com.sap.sse.branding/src/com/sap/sse/branding/shared/BrandingConfiguration.java` implementation.

You can override methods to define your custom branding behavior.
Some methods rely on `SailingServerStringMessages`, which works similarly to message resources. Provide English and German (de) messages together for any new text added to branding.
Localization details can be found here:
[Translation and i18n](https://wiki.sapsailing.com/wiki/howto/development/i18n.md).

## 4. Adding New Branding Attributes

If you want to extend the branding (e.g., add background color or other brand-specific parameters), follow these implementation steps:

### Step 1: Add New Variables and Getters

Add the corresponding variables and getter methods in:

* `ClientConfiguration.java`
* `ClientConfigurationContextDataJSO.java`
* `YourBrandNameBrandingConfiguration.java`

**Example method name:**

```java
getSailingAnalyticsSapSailing()
```

### Step 2: Update the Branding Configuration Service

* Modify `BrandingConfigurationServiceImpl.java` to include your new property.
* Add it to the property map in `BrandingConfigurationService.java` (e.g., `SAILING_ANALYTICS_SAP_SAILING`).

### Step 3: Update Localization Files

If needed, modify:

```
BrandingStringMessages_lang.properties
```

to include your new configuration string (e.g., `'SAP Sailing'`).

### Step 4: Clean Up Deprecated Strings

Remove old occurrences of the string variable from:

* `StringMessages_lang.properties`
* `StringMessages.java` (client side)

### Step 5: Replace with New Implementation

Use your new getter where the old implementation was previously used:

```java
ClientConfiguration.getInstance().yourGetter()
```

Instead of:

```java
StringMessages.INSTANCE.actualStringMessage()
```

---

## 5. Testing Your Branding Configuration

**Prerequisite:** Ensure **telnet** is installed on your local machine.

### Testing Steps

1. **Connect via telnet**

   ```bash
   telnet localhost 12001
   ```

2. **Search for branding**

   ```text
   osgi> ss brand
   Framework is active.

   id  State       Bundle
   108 ACTIVE      com.sap.sse.branding.sap (1.2.3)
   ```

3. **Toggle branding on/off**

   ```text
   start 108
   stop 108
   ```

   *(ID `108` corresponds to branding for this example.)*

4. **Disconnect**

   ```text
   disconnect
   ```

   Confirm with `y` (yes).

This allows you to verify how your custom branding is applied and displayed dynamically.

---
Here’s your full `.md` documentation version — clear, structured, and with an explanatory section at the end that describes **why** each step is needed and **how** the branding extension mechanism works internally.

---
## 6. Why These Steps Are Necessary

### 1. Separation of Concerns

Each branding-related class has a specific role in the client–server pipeline:

| Component                                | Role                                                                                                                                                                                                      | Location                             |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| **`YourBrandNameBrandingConfiguration`** | Defines static and localized branding resources such as image URLs, localized texts, or external links.                                                                                                   | Server-side (OSGi bundle)            |
| **`BrandingConfigurationServiceImpl`**   | Acts as the bridge between backend branding configurations and the client. It collects active configuration data into a property map and generates the `document.clientConfigurationContext` JSON object. | Server-side                          |
| **`ClientConfigurationContextDataJSO`**  | Reads that JSON object in the browser, providing native GWT access to branding fields.                                                                                                                    | Client-side (GWT JavaScript overlay) |
| **`ClientConfiguration`**                | Consumes values from `ClientConfigurationContextDataJSO` and exposes convenient Java getters for UI code.                                                                                                 | Client-side (GWT Java)               |

When you add a new property (e.g., `backgroundColor`), you extend all these layers so the value flows **end-to-end** — from the backend branding definition to the live UI rendering in the browser.

---

### 2. Why Each Step Exists

* **Step 1 (New Variables/Getters):**
  These getters form the “public interface” for your new property. The client cannot use or bind values unless they are exposed through `ClientConfiguration` and its JavaScript overlay `ClientConfigurationContextDataJSO`.

* **Step 2 (Update Service):**
  The `BrandingConfigurationServiceImpl` class generates the client-facing configuration JSON that feeds GWT. Without updating it, your new field would never reach the browser.
  The property map ensures the new key appears in:

  ```json
  document.clientConfigurationContext={"brandTitle":"SAP","backgroundColor":"#004080",...}
  ```

* **Step 3 (Localization):**
  When branding text differs across languages, translations are handled through `.properties` files loaded by `ResourceBundleStringMessages`. This ensures locale-dependent branding output.

* **Step 4 (Cleanup):**
  Legacy or hardcoded UI strings are replaced by dynamic configuration values. This avoids inconsistencies and allows true runtime branding/debranding.

* **Step 5 (Replacement):**
  The UI must always call `ClientConfiguration.getInstance().yourGetter()` — this guarantees the displayed text, logos, or links match the currently active branding context.

---

### 3. How It Works Technically

1. The backend OSGi service (`BrandingConfigurationServiceImpl`) collects data from the active `BrandingConfiguration` (e.g., `SAPBrandingConfiguration`).
2. It maps these fields to standardized property names (defined in `BrandingConfigurationService.BrandingConfigurationProperty`).
3. It serializes them into a JSON object and injects a `<script>` into the HTML page:

   ```html
   <script>document.clientConfigurationContext={"brandingActive":true,"brandTitle":"SAP",...};</script>
   ```
4. The GWT client loads this script at startup.
5. `ClientConfigurationContextDataJSO` provides Java-accessible wrappers around this JSON.
6. `ClientConfiguration` instantiates itself with these values and becomes globally available through `ClientConfiguration.getInstance()`.

This chain ensures that any new field you define in the backend automatically becomes available to the UI once the above steps are followed.

---

### 4. Summary

By following this pattern:

* Every new branding attribute becomes part of the unified configuration context.
* The GWT client automatically receives and uses the latest configuration from the backend.
* Debranding and white-labeling remain dynamically switchable at runtime.
* All UI components rely on consistent, centrally managed branding information.


---

## 7. Conclusion

By following this setup, you can create and integrate a fully customized branding layer for **Sailing Analytics**, including logos, localized texts, and design assets.
The modular design allows you to extend branding attributes to reflect your organization’s visual identity while maintaining compatibility with the base system.
With consistent testing (via telnet) and clear property configuration, you can reliably switch between **branded** and **debranded** experiences.
