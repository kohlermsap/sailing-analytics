package com.sap.sailing.gwt.home.mobile.partials.header;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.app.ResettableNavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.partials.header.HeaderConstants;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.client.controls.dropdown.DropdownHandler;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.authentication.AuthenticationSignOutRequestEvent;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;

/**
 * Mobile page header with SAP logo title and a dropdown menu (burger button) on the right.
 */
public class Header extends Composite implements HeaderConstants {
    @UiField HeaderResources local_res;
    @UiField DivElement dropdownTriggerUi;
    @UiField Element dropdownContainerUi;
    @UiField FlowPanel dropdownListUi;
    @UiField FlowPanel dropdownListExtUi;
    @UiField Element searchUi;
    @UiField AnchorElement logoAnchor;
    @UiField ImageElement logoImage;

    @UiField
    DivElement locationTitleUi;

    private final ResettableNavigationPathDisplay navigationPathDisplay;

    interface HeaderUiBinder extends UiBinder<Widget, Header> {
    }
    
    private static HeaderUiBinder uiBinder = GWT.create(HeaderUiBinder.class);
    private final DropdownHandler dropdownHandler;
    private final HeaderNavigationItem signInNavigationItem;
    private final HeaderNavigationItem userDetailsNavigationItem;
    private final HeaderNavigationItem signOutNavigationItem;
    
    public Header(final MobilePlacesNavigator placeNavigator, final EventBus eventBus) {
        initWidget(uiBinder.createAndBindUi(this));
        local_res.css().ensureInjected();
        dropdownListExtUi.getElement().getStyle().setDisplay(Display.NONE);
        navigationPathDisplay = new DropdownNavigationPathDisplay();
        addNavigation(placeNavigator.getHomeNavigation(), StringMessages.INSTANCE.home());
        addNavigation(placeNavigator.getEventsNavigation(), StringMessages.INSTANCE.events());
        addNavigation(placeNavigator.getSolutionsNavigation(SolutionsNavigationTabs.SailingAnalytics),
                StringMessages.INSTANCE.solutions());
        addNavigation(placeNavigator.getSubscriptionsNavigation(),
                StringMessages.INSTANCE.subscriptions());
        HeaderNavigationItem manageEventsNavItem = addNavigation(ADMIN_CONSOLE_PATH,
                StringMessages.INSTANCE.manageEvents(), ()->{});
        manageEventsNavItem.getElement().getStyle().setDisplay(Display.NONE);
        manageEventsNavItem.addClickHandler(event -> {
            Window.open(manageEventsNavItem.getHref(), manageEventsNavItem.getTarget(), null);
        });
        signInNavigationItem = addNavigation(com.sap.sse.security.ui.client.i18n.StringMessages.INSTANCE.signIn(), new Runnable() {
            @Override
            public void run() {
                        placeNavigator.getSignInNavigation().goToPlace();
            }
        });
        userDetailsNavigationItem = addNavigation(placeNavigator.getUserProfileNavigation(), com.sap.sse.security.ui.client.i18n.StringMessages.INSTANCE.userDetails());
        signOutNavigationItem = addNavigation(com.sap.sse.security.ui.client.i18n.StringMessages.INSTANCE.signOut(), new Runnable() {
            @Override
            public void run() {
                        eventBus.fireEvent(new AuthenticationSignOutRequestEvent());
            }
        });
        dropdownHandler = new DropdownHandler(dropdownTriggerUi, dropdownContainerUi);
        Event.sinkEvents(searchUi, Event.ONCLICK);
        Event.setEventListener(searchUi, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (LinkUtil.handleLinkClick(event)) {
                    event.preventDefault();
                    placeNavigator.getSearchResultNavigation("").goToPlace();
                }
                
            }
        });
        eventBus.addHandler(AuthenticationContextEvent.TYPE, new AuthenticationContextEvent.Handler() {
            @Override
            public void onUserChangeEvent(AuthenticationContextEvent event) {
                String loggedInStyle = HeaderResources.INSTANCE.css().header_navigation_iconsignedin();
                AuthenticationContext authContext = event.getCtx();
                UIObject.setStyleName(dropdownTriggerUi, loggedInStyle, authContext.isLoggedIn());
                signInNavigationItem.setVisible(!authContext.isLoggedIn());
                userDetailsNavigationItem.setVisible(authContext.isLoggedIn());
                signOutNavigationItem.setVisible(authContext.isLoggedIn());
                if (authContext.hasServerPermission(ServerActions.CREATE_OBJECT)) {
                    manageEventsNavItem.setHref(UriUtils.fromString(ADMIN_CONSOLE_PATH));
                    manageEventsNavItem.setTarget(ADMIN_CONSOLE_WINDOW);
                    manageEventsNavItem.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                } else if (authContext.getCurrentUser() != null
                        && !authContext.getCurrentUser().getName().equals("Anonymous")) {
                    String base = authContext.getServerInfo().getManageEventsBaseUrl();
                    manageEventsNavItem.setHref(UriUtils.fromString(base + ADMIN_CONSOLE_PATH));
                    manageEventsNavItem.setTarget(ADMIN_CONSOLE_WINDOW);
                    manageEventsNavItem.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                } else {
                    manageEventsNavItem.getElement().getStyle().setDisplay(Display.NONE);
                }
            }
        });
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            logoImage.getStyle().setDisplay(Display.NONE);
            logoAnchor.setHref("");
            logoAnchor.setTitle(StringMessages.INSTANCE.sailingAnalytics());
            
        } else {
            logoAnchor.setHref(UriUtils.fromString(StringMessages.INSTANCE.sapAnalyticsURL()).asString());
        }
    }

    public ResettableNavigationPathDisplay getNavigationPathDisplay() {
        return navigationPathDisplay;
    }

    private HeaderNavigationItem addNavigation(final PlaceNavigation<?> placeNavigation, String name) {
        return addNavigation(placeNavigation.getTargetUrl(), name, new Runnable() {
            @Override
            public void run() {
                placeNavigation.goToPlace();
            }
            
        });
    }
    
    private HeaderNavigationItem addNavigation(String name, final Runnable action) {
        return addNavigation(null, name, action);
    }

    private HeaderNavigationItem addNavigation(String url, String name, final Runnable action) {
        HeaderNavigationItem navigationItem = new HeaderNavigationItem(name, url);
        navigationItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if(LinkUtil.handleLinkClick(event.getNativeEvent().<Event>cast())) {
                    event.preventDefault();
                    action.run();
                    dropdownHandler.setVisible(false);
                }
            }
        });
        dropdownListUi.add(navigationItem);
        return navigationItem;
    }
    
    public void setLocationTitle(String locationTitle) {
        locationTitleUi.setInnerText(locationTitle);
    }

    private class DropdownNavigationPathDisplay implements ResettableNavigationPathDisplay {
        @Override
        public void showNavigationPath(NavigationItem... navigationPath) {
            dropdownListExtUi.clear();
            for (final NavigationItem navigationItem : navigationPath) {
                HeaderNavigationItem headerNavItem = new HeaderNavigationItem(navigationItem.getDisplayName(),
                        navigationItem.getTargetUrl());
                headerNavItem.addStyleName(local_res.css().header_navigation_nav_sublist_item());
                headerNavItem.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        if (LinkUtil.handleLinkClick(event.getNativeEvent().<Event> cast())) {
                            event.preventDefault();
                            navigationItem.run();
                            dropdownHandler.setVisible(false);
                        }
                    }
                });
                dropdownListExtUi.add(headerNavItem);
            }
            dropdownListExtUi.getElement().getStyle().clearDisplay();
        }

        @Override
        public void reset() {
            dropdownListExtUi.clear();
            dropdownListExtUi.getElement().getStyle().setDisplay(Display.NONE);
        }
    }
}
