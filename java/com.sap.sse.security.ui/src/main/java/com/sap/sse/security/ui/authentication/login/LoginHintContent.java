package com.sap.sse.security.ui.authentication.login;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.shared.ClientConfiguration;

/**
 * Widget that shows a message to inform the user that it has benefits to log in. There are two links the user can
 * press:
 * <ul>
 * <li>"Dismiss" to hide the message. This will typically make the message disappear for some time.</li>
 * <li>"Show details" to show a page providing some information about the benefits of creating an account.</li>
 * </ul>
 * the concrete actions to be triggered when clicking the links need to be defined by the using context through the
 * {@link Runnable Runnables} given to the constructor.
 */
public class LoginHintContent extends Composite {

    private static LoginPopupContentUiBinder uiBinder = GWT.create(LoginPopupContentUiBinder.class);

    interface LoginPopupContentUiBinder extends UiBinder<Widget, LoginHintContent> {
    }

    @UiField
    Anchor moreInfo;
    @UiField
    Anchor dismiss;
    @UiField
    Element main;

    public LoginHintContent(final Runnable onDismiss, final Runnable onMoreInfo, final Runnable toLogin) {
        initWidget(uiBinder.createAndBindUi(this));
        dismiss.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                onDismiss.run();
            }
        });
        moreInfo.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                onMoreInfo.run();
            }
        });
        DOM.sinkEvents(main, Event.ONCLICK);
        DOM.setEventListener(main, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                com.google.gwt.dom.client.EventTarget eventTarget = event.getEventTarget();
                if (!Element.is(eventTarget)) {
                    return;
                }
                final Element target = eventTarget.cast();
                if (target != moreInfo.getElement() && target != dismiss.getElement()){
                    toLogin.run();
                }
            }
        });
    }
}
