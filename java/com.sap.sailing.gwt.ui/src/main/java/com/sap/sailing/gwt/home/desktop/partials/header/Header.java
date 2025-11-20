package com.sap.sailing.gwt.home.desktop.partials.header;

import static com.google.gwt.dom.client.Style.Display.BLOCK;
import static com.google.gwt.dom.client.Style.Display.NONE;
import static com.google.gwt.dom.client.Style.Visibility.HIDDEN;
import static com.google.gwt.dom.client.Style.Visibility.VISIBLE;
import static com.sap.sse.gwt.shared.DebugConstants.DEBUG_ID_ATTRIBUTE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.home.client.place.event.legacy.EventPlace;
import com.sap.sailing.gwt.home.client.place.event.legacy.RegattaPlace;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.header.HeaderConstants;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sailing.gwt.home.shared.places.searchresult.SearchResultPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionPlace;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.controls.dropdown.DropdownHandler;
import com.sap.sse.gwt.client.mvp.PlaceChangedEvent;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.gwt.shared.DebugConstants;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.NamedSecuredObjectDTO;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.UserActions;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuView;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuViewImpl;

public class Header extends Composite implements HeaderConstants {

    @UiField Anchor startPageLinkMenu;
    @UiField Anchor eventsPageLinkMenu;
    @UiField Anchor solutionsPageLinkMenu;
    @UiField Anchor subscriptionsPageLinkMenu;
    @UiField Anchor adminConsolePageLinkMenu;
    @UiField Anchor dataMiningPageLinkMenu;
    @UiField Anchor strategySimulatorPageLinkMenu;
    @UiField Anchor startPageLink;
    @UiField Anchor eventsPageLink;
    @UiField Anchor solutionsPageLink;
    @UiField Anchor subscriptionsPageLink;
    @UiField AnchorElement logoAnchor;
    @UiField Anchor adminConsolePageLink;
    @UiField Anchor dataMiningPageLink;
    @UiField Anchor strategySimulatorPageLink;
    @UiField TextBox searchText;
    @UiField Button searchButton;
    @UiField Anchor hamburgerMenuIcon;
    @UiField Element headerNavigationDropDownMenuContainer;
    @UiField Element centerMenuPanel;
    @UiField Anchor usermenu;
    @UiField ImageElement logoImage;

    private static final HyperlinkImpl HYPERLINK_IMPL = GWT.create(HyperlinkImpl.class);
    private final List<Anchor> links;
    private final DesktopPlacesNavigator navigator;
    private final PlaceNavigation<StartPlace> homeNavigation;
    private final PlaceNavigation<EventsPlace> eventsNavigation;
    private final PlaceNavigation<SolutionsPlace> solutionsNavigation;
    private final PlaceNavigation<SubscriptionPlace> subscriptionsNavigation;
    private final AuthenticationMenuView authenticationMenuView;

    interface HeaderUiBinder extends UiBinder<Widget, Header> {
    }

    /**
     * Contains the logic for the dynamic hamburger menu on top of the page.
     * @author Georg Herdt
     *
     */
    private static final class MenuItemVisibilityHandler implements ResizeHandler {
        /** maps relationship between menu item in menu bar and menu item in hamburger drop down */
        private final Map<Anchor, Anchor> menuToDropDownItemMap;
        /** keeps width of the menu items in the menu bar */
        private final Map<Anchor, Integer> menuItemWidthMap = new HashMap<>();
        /** list of menu item that shall not be rendered at all, e.g data mining or manage events entry */
        private final Set<Anchor> ignoreMenuItems = new HashSet<>();
        private final Anchor hamburgerNavigationIcon;
        private final DropdownHandler dropdownHandler;
        /** view port of the entries in the menu bar */
        private final Element centerMenuPanel;

        public MenuItemVisibilityHandler(Map<Anchor, Anchor> menuToDropDownItemMap, DropdownHandler dropdownHandler,
                Anchor hamburgerNavigationIcon, Element centerMenuPanel) {
            this.dropdownHandler = dropdownHandler;
            this.hamburgerNavigationIcon = hamburgerNavigationIcon;
            this.menuToDropDownItemMap = menuToDropDownItemMap;
            this.centerMenuPanel = centerMenuPanel;
        }

        @Override
        public void onResize(ResizeEvent event) {
            refreshVisibilityDeferred();
        }

        /**
         * Determine if menu item fits fully into the panel and should be visible then.
         * 
         * @param anchor
         *            menu item that shall be checked
         * @param leftOffset
         *            left offset, where the given menu item can be placed
         * @return true when the menu item can be fully displayed in menu bar.
         */
        private boolean isVisibilityInMenuBar(Anchor anchor, int leftOffset) {
            int anchorWidth = this.menuItemWidthMap.getOrDefault(anchor, 200);
            // calculate if the element is outside of the center menu panel, which is the view port here
            boolean fitsFullyIntoPanel = leftOffset + anchorWidth < this.centerMenuPanel.getAbsoluteRight();
            return fitsFullyIntoPanel;
        }

        /**
         * Calculates the visibility of the menu items. Items which cannot be shown in the menu bar due to space
         * limitation will be shown in a hamburger menu.
         */
        private void refreshVisibility() {
            int noOfVisibleItems = 0;
            int leftOffset = centerMenuPanel.getAbsoluteLeft(); // outer left where the next menu item can be placed
            for (Map.Entry<Anchor, Anchor> item : menuToDropDownItemMap.entrySet()) {
                Anchor menuAnchor = item.getKey();
                int currentWidth = getStaticWidth(menuAnchor, 200);
                Element listItem = item.getValue().getElement().getParentElement();
                boolean visibilityInMenuBar = !ignoreMenuItems.contains(menuAnchor)
                        && isVisibilityInMenuBar(menuAnchor, leftOffset);
                menuAnchor.setVisible(visibilityInMenuBar);
                Display listItemDisplay = visibilityInMenuBar || ignoreMenuItems.contains(menuAnchor) ? NONE : BLOCK;
                listItem.getStyle().setDisplay(listItemDisplay);
                noOfVisibleItems += listItemDisplay == BLOCK ? 1 : 0;
                leftOffset += ignoreMenuItems.contains(menuAnchor) ? 0 : currentWidth;
            }
            this.hamburgerNavigationIcon.getElement().getStyle()
                    .setVisibility(noOfVisibleItems == 0 ? HIDDEN : VISIBLE);
            if (noOfVisibleItems == 0) { // hide if nothing to display. Otherwise do not touch visibility
                this.dropdownHandler.setVisible(false);
            }
        }

        /**
         * Calculate item width within the menu bar on top of the page. The width is stored within a map, because when
         * the anchor is invisible the returned with of the element is zero.
         * 
         * @param key
         *            the menu item for which the width shall be determined
         * @param defaultWidth
         *            default value as fall back, when there is no width available. Such cases  are, when the width from
         *            browser is 0 and there is no previous value available, e.g. during initialization of page.
         * @return the width of the menu item
         */
        private int getStaticWidth(Anchor key, int defaultWidth) {
            Integer width = menuItemWidthMap.compute(key, (menuItem, oldWidth) -> {
                int newWidth = menuItem.getElement().getClientWidth();
                // might be called in a situation where the components do not have a width
                // so we need a mechanism to update detected width later
                if (newWidth > 0) {
                    return newWidth;
                } else {
                    return oldWidth;
                }
            });
            return width == null ? defaultWidth : width;
        }

        public void addIgnore(Anchor menuItem) {
            this.ignoreMenuItems.add(menuItem);
        }

        public boolean removeIgnore(Anchor menuItem) {
            return this.ignoreMenuItems.remove(menuItem);
        }

        public void refreshVisibility(int delayMillis) {
            new Timer() {
                @Override
                public void run() {
                    refreshVisibility();
                }
            }.schedule(delayMillis);
        }

        public void refreshVisibilityDeferred() {
            Scheduler.get().scheduleDeferred(this::refreshVisibility);
        }
    }

    private static HeaderUiBinder uiBinder = GWT.create(HeaderUiBinder.class);

    private MenuItemVisibilityHandler menuItemVisibilityHandler;

    public Header(final DesktopPlacesNavigator navigator, EventBus eventBus) {
        this.navigator = navigator;
        HeaderResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        Map<Anchor,Anchor> menuToDropDownItemMap = new HashMap<>();
        menuToDropDownItemMap.put(startPageLink, startPageLinkMenu);
        menuToDropDownItemMap.put(eventsPageLink, eventsPageLinkMenu);
        menuToDropDownItemMap.put(solutionsPageLink, solutionsPageLinkMenu);
        menuToDropDownItemMap.put(subscriptionsPageLink, subscriptionsPageLinkMenu);
        menuToDropDownItemMap.put(adminConsolePageLink, adminConsolePageLinkMenu);
        menuToDropDownItemMap.put(dataMiningPageLink, dataMiningPageLinkMenu);
        menuToDropDownItemMap.put(strategySimulatorPageLink, strategySimulatorPageLinkMenu);
        headerNavigationDropDownMenuContainer.getStyle().setDisplay(Display.NONE);
        final DropdownHandler dropdownHandler = new DropdownHandler(hamburgerMenuIcon, headerNavigationDropDownMenuContainer);
        menuItemVisibilityHandler = new MenuItemVisibilityHandler(menuToDropDownItemMap, dropdownHandler, hamburgerMenuIcon, centerMenuPanel);
        // close menu if menu item was clicked
        for (Anchor anchor: menuToDropDownItemMap.values()) {
            anchor.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    dropdownHandler.setVisible(false);
                }
            });
        }
        Window.addResizeHandler(menuItemVisibilityHandler);
        links = Arrays.asList(new Anchor[] { startPageLink, eventsPageLink, solutionsPageLink, subscriptionsPageLink, adminConsolePageLink, dataMiningPageLink });
        homeNavigation = navigator.getHomeNavigation();
        eventsNavigation = navigator.getEventsNavigation();
        solutionsNavigation = navigator.getSolutionsNavigation(SolutionsNavigationTabs.SapInSailing);
        subscriptionsNavigation = navigator.getSubscriptionsNavigation();
        startPageLink.setHref(homeNavigation.getTargetUrl());
        eventsPageLink.setHref(eventsNavigation.getTargetUrl());
        solutionsPageLink.setHref(solutionsNavigation.getTargetUrl());
        subscriptionsPageLink.setHref(subscriptionsNavigation.getTargetUrl());
        authenticationMenuView = new AuthenticationMenuViewImpl(usermenu, HeaderResources.INSTANCE.css().loggedin(),
                HeaderResources.INSTANCE.css().open(), HeaderResources.INSTANCE.css().user_menu_premium());
        // make the Admin link visible only for signed-in users
        adminConsolePageLink.getElement().getStyle().setDisplay(Display.NONE);
        // initially hide admin console, data mining and strategy simulator in hamburger menu
        menuItemVisibilityHandler.addIgnore(adminConsolePageLink);
        menuItemVisibilityHandler.addIgnore(dataMiningPageLink);
        menuItemVisibilityHandler.addIgnore(strategySimulatorPageLink);
        eventBus.addHandler(AuthenticationContextEvent.TYPE, event->{
            AuthenticationContext authContext = event.getCtx();
            // make it point to the current server if the user has CREATE_OBJECT permission there
            if (authContext.isLoggedIn() && authContext.hasServerPermission(ServerActions.CREATE_OBJECT)) {
                adminConsolePageLinkMenu.setHref(ADMIN_CONSOLE_PATH);
                adminConsolePageLinkMenu.setTarget(ADMIN_CONSOLE_WINDOW);
                adminConsolePageLink.setHref(ADMIN_CONSOLE_PATH);
                adminConsolePageLink.setTarget(ADMIN_CONSOLE_WINDOW);
                adminConsolePageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                menuItemVisibilityHandler.removeIgnore(adminConsolePageLink);
            } else if (authContext.getCurrentUser() != null && !authContext.getCurrentUser().getName().equals("Anonymous")) {
                // make it point to the default "manage events" self-service server configured in ServerInfo otherwise
                String base = authContext.getServerInfo().getManageEventsBaseUrl();
                adminConsolePageLinkMenu.setHref(UriUtils.fromString(base + ADMIN_CONSOLE_PATH));
                adminConsolePageLinkMenu.setTarget(ADMIN_CONSOLE_WINDOW);
                adminConsolePageLink.setHref(UriUtils.fromString(base + ADMIN_CONSOLE_PATH));
                adminConsolePageLink.setTarget(ADMIN_CONSOLE_WINDOW);
                adminConsolePageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                menuItemVisibilityHandler.removeIgnore(adminConsolePageLink);
            } else {
                adminConsolePageLink.getElement().getStyle().setDisplay(Display.NONE);
                menuItemVisibilityHandler.addIgnore(adminConsolePageLink);
            }
            menuItemVisibilityHandler.removeIgnore(dataMiningPageLink);
            if (authContext.getPaywallResolver().hasPermission(ServerActions.DATA_MINING, authContext.getServerInfo())) {
                dataMiningPageLinkMenu.setHref(DATA_MINING_PATH);
                dataMiningPageLinkMenu.setTarget(DATA_MINING_WINDOW);
                dataMiningPageLinkMenu.removeStyleName(HeaderResources.INSTANCE.css().premium_hint());
                dataMiningPageLink.setHref(DATA_MINING_PATH);
                dataMiningPageLink.setTarget(DATA_MINING_WINDOW);
                dataMiningPageLink.removeStyleName(HeaderResources.INSTANCE.css().premium_hint());
                dataMiningPageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
            } else {
                dataMiningPageLinkMenu.setHref(subscriptionsNavigation.getTargetUrl());
                dataMiningPageLinkMenu.setTarget(SELF);
                dataMiningPageLinkMenu.addStyleName(HeaderResources.INSTANCE.css().premium_hint());
                dataMiningPageLink.setHref(subscriptionsNavigation.getTargetUrl());
                dataMiningPageLink.setTarget(SELF);
                dataMiningPageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                dataMiningPageLink.addStyleName(HeaderResources.INSTANCE.css().premium_hint());
                // add possible subscription plans to URL
                authContext.getPaywallResolver().getUnlockingSubscriptionPlans(ServerActions.DATA_MINING, authContext.getServerInfo(),
                        (unlockingPlans) -> {
                            dataMiningPageLinkMenu.setHref(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans));
                            dataMiningPageLink.setHref(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans));
                        });
            }
            menuItemVisibilityHandler.removeIgnore(strategySimulatorPageLink);
            if (authContext.getPaywallResolver().hasPermission(DefaultActions.READ, NamedSecuredObjectDTO.create(authContext.getServerInfo().getName(),
                    SecuredDomainType.SIMULATOR, new TypeRelativeObjectIdentifier(authContext.getServerInfo().getName())))) {
                strategySimulatorPageLinkMenu.setHref(STRATEGY_SIMULATOR_PATH);
                strategySimulatorPageLinkMenu.setTarget(STRATEGY_SIMULATOR_WINDOW);
                strategySimulatorPageLinkMenu.removeStyleName(HeaderResources.INSTANCE.css().premium_hint());
                strategySimulatorPageLink.setHref(STRATEGY_SIMULATOR_PATH);
                strategySimulatorPageLink.setTarget(STRATEGY_SIMULATOR_WINDOW);
                strategySimulatorPageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                strategySimulatorPageLink.removeStyleName(HeaderResources.INSTANCE.css().premium_hint());
            } else {
                strategySimulatorPageLinkMenu.setHref(subscriptionsNavigation.getTargetUrl());
                strategySimulatorPageLinkMenu.setTarget(SELF);
                strategySimulatorPageLinkMenu.addStyleName(HeaderResources.INSTANCE.css().premium_hint());
                strategySimulatorPageLink.setHref(subscriptionsNavigation.getTargetUrl());
                strategySimulatorPageLink.setTarget(SELF);
                strategySimulatorPageLink.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
                strategySimulatorPageLink.addStyleName(HeaderResources.INSTANCE.css().premium_hint());
                // add possible subscription plans to URL
                authContext.getPaywallResolver().getUnlockingSubscriptionPlans(DefaultActions.READ, NamedSecuredObjectDTO.create(authContext.getServerInfo().getName(),
                        SecuredDomainType.SIMULATOR, new TypeRelativeObjectIdentifier(authContext.getServerInfo().getName())),
                        (unlockingPlans) -> {
                            strategySimulatorPageLinkMenu.setHref(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans));
                            strategySimulatorPageLink.setHref(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans));
                        });
                
            }
            menuItemVisibilityHandler.refreshVisibilityDeferred();
            authenticationMenuView.showPremium(authContext.hasPermission(authContext.getCurrentUser(), UserActions.BE_PREMIUM));
        });
        searchText.getElement().setAttribute("placeholder", StringMessages.INSTANCE.headerSearchPlaceholder());
        searchText.addFocusHandler((focusEvent) -> menuItemVisibilityHandler.refreshVisibility(370));
        searchText.addBlurHandler(blurEvent -> {
            menuItemVisibilityHandler.refreshVisibility(370);
        });
        searchText.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    searchButton.click();
                }
            }
        });
        eventBus.addHandler(PlaceChangedEvent.TYPE, new PlaceChangedEvent.Handler() {
            @Override
            public void onPlaceChanged(PlaceChangedEvent event) {
                updateActiveLink(event.getNewPlace());
            }
        });
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            logoImage.getStyle().setDisplay(Display.NONE);
            logoImage.setTitle(StringMessages.INSTANCE.sailingAnalytics());
            solutionsPageLink.getElement().getStyle().setDisplay(Display.NONE);
            logoAnchor.setHref("");
            logoAnchor.setTitle(StringMessages.INSTANCE.sailingAnalytics());
            //menuItemVisibilityHandler.addIgnore(solutionsPageLink);
        } else {
            logoAnchor.setHref(UriUtils.fromString(StringMessages.INSTANCE.sapAnalyticsURL()).asString());
        }
        logoImage.setAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, "logoImage");
        solutionsPageLink.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "solutionsPageLink");
        subscriptionsPageLink.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "subscriptionsPageLink");
        logoAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "logoAnchor");
        eventsPageLink.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "eventsPage");
        menuItemVisibilityHandler.refreshVisibilityDeferred();
    }

    @UiHandler({ "startPageLink", "startPageLinkMenu" })
    public void goToHome(ClickEvent e) {
        handleClickEvent(e, homeNavigation, startPageLink);
    }

    @UiHandler({ "eventsPageLink", "eventsPageLinkMenu" })
    public void goToEvents(ClickEvent e) {
        handleClickEvent(e, eventsNavigation, eventsPageLink);
    }

    @UiHandler({ "solutionsPageLink", "solutionsPageLinkMenu" })
    public void goToSolutions(ClickEvent e) {
        handleClickEvent(e, solutionsNavigation, solutionsPageLink);
    }

    @UiHandler({ "subscriptionsPageLink", "subscriptionsPageLinkMenu" })
    public void goToSubscriptions(ClickEvent e) {
        handleClickEvent(e, subscriptionsNavigation, subscriptionsPageLink);
    }

    @UiHandler("searchButton")
    void searchButtonClick(ClickEvent event) {
        PlaceNavigation<SearchResultPlace> searchResultNavigation = navigator.getSearchResultNavigation(searchText
                .getText());
        navigator.goToPlace(searchResultNavigation);
    }
    
    private void updateActiveLink(Place place) {
        if(place instanceof EventsPlace
                || place instanceof AbstractEventPlace
                || place instanceof EventPlace
                || place instanceof RegattaPlace) {
            setActiveLink(eventsPageLink);
        } else if(place instanceof StartPlace) {
            setActiveLink(startPageLink);
        } else if(place instanceof SolutionsPlace) {
            setActiveLink(solutionsPageLink);
        } else if(place instanceof SubscriptionPlace) {
            setActiveLink(subscriptionsPageLink);
        } else {
            setActiveLink(null);
        }
    }
    
    private void setActiveLink(Anchor link) {
        final String activeStyle = HeaderResources.INSTANCE.css().sitenavigation_linkactive();
        for (Anchor l : links) {
            if (l == link) {
                l.addStyleName(activeStyle);
            } else {
                l.removeStyleName(activeStyle);
            }
        }
    }

    private void handleClickEvent(ClickEvent e, PlaceNavigation<?> placeNavigation, Anchor activeLink) {
        if (HYPERLINK_IMPL.handleAsClick((Event) e.getNativeEvent())) {
            navigator.goToPlace(placeNavigation);
            e.preventDefault();
            setActiveLink(activeLink);
         }
    }

    public AuthenticationMenuView getAuthenticationMenuView() {
        return authenticationMenuView;
    }
}
