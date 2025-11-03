package com.sap.sse.gwt.client.controls.slider;

/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RequiresResize;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.shared.DebugConstants;

/**
 * A widget that allows the user to select a value within a range of possible values using a sliding bar that responds
 * to mouse events.
 * 
 * <h3>Keyboard Events</h3>
 * <p>
 * SliderBar listens for the following key events. Holding down a key will repeat the action until the key is released.
 * <ul class='css'>
 * <li>left arrow - shift left one step</li>
 * <li>right arrow - shift right one step</li>
 * <li>ctrl+left arrow - jump left 10% of the distance</li>
 * <li>ctrl+right arrow - jump right 10% of the distance</li>
 * <li>home - jump to min value</li>
 * <li>end - jump to max value</li>
 * <li>space - jump to middle value</li>
 * </ul>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SliderBar-shell { primary style }</li>
 * <li>.gwt-SliderBar-shell-focused { primary style when focused }</li>
 * <li>.gwt-SliderBar-shell gwt-SliderBar-line { the line that the knob moves along }</li>
 * <li>.gwt-SliderBar-shell gwt-SliderBar-line-sliding { the line that the knob moves along when sliding }</li>
 * <li>.gwt-SliderBar-shell .gwt-SliderBar-knob { the sliding knob }</li>
 * <li>.gwt-SliderBar-shell .gwt-SliderBar-knob-sliding { the sliding knob when sliding }</li>
 * <li>
 * .gwt-SliderBar-shell .gwt-SliderBar-tick { the ticks along the line }</li>
 * <li>.gwt-SliderBar-shell .gwt-SliderBar-ticklabel { the text labels along the line }</li>
 * </ul>
 */
public class SliderBar extends FocusPanel implements RequiresResize, HasValue<Double>, HasValueChangeHandlers<Double> {
    /**
     * A formatter used to format the labels displayed in the widget.
     */
    public static interface LabelFormatter {
        /**
         * Generate the text to display in each label based on the label's value.
         * 
         * Override this method to change the text displayed within the SliderBar.
         * 
         * @param slider
         *            the Slider bar
         * @param value
         *            the value the label displays
         * @return the text to display for the label
         */
        String formatLabel(SliderBar slider, Double value, Double previousValue);
    }

    /**
     * A {@link ClientBundle} that provides images for {@link SliderBar}.
     */
    public static interface SliderBarImages extends ClientBundle {
        public static final SliderBarImages INSTANCE = GWT.create(SliderBarImages.class);

        /**
         * An image used for the sliding knob.
         * 
         * @return a prototype of this image
         */
        @Source("slider_blue.png")
        ImageResource slider();

        /**
         * An image used for the disabled sliding knob.
         * 
         * @return a prototype of this image
         */
        @Source("slider_blue.png")
        ImageResource sliderDisabled();

        /**
         * An image used for the sliding knob while sliding.
         * 
         * @return a prototype of this image
         */
        @Source("slider_blue.png")
        ImageResource sliderSliding();

        @NotStrict
        @Source("SliderBar.css")
        CssResource sliderBarCss();
    }

    /**
     * The timer used to continue to shift the knob as the user holds down one of the left/right arrow keys. Only IE
     * auto-repeats, so we just keep catching the events.
     */
    private class KeyTimer extends Timer {
        /**
         * A bit indicating that this is the first run.
         */
        private boolean firstRun = true;

        /**
         * The delay between shifts, which shortens as the user holds down the button.
         */
        private int repeatDelay = 30;

        /**
         * A bit indicating whether we are shifting to a higher or lower value.
         */
        private boolean shiftRight = false;

        /**
         * The number of steps to shift with each press.
         */
        private int multiplier = 1;

        /**
         * This method will be called when a timer fires. Override it to implement the timer's logic.
         */
        @Override
        public void run() {
            // Highlight the knob on first run
            if (firstRun) {
                firstRun = false;
                startSliding(true, false);
            }

            // Slide the slider bar
            if (shiftRight) {
                setCurrentValue(curValue + multiplier * stepSize);
            } else {
                setCurrentValue(curValue - multiplier * stepSize);
            }

            // Repeat this timer until cancelled by keyup event
            schedule(repeatDelay);
        }

        /**
         * Schedules a timer to elapse in the future.
         * 
         * @param delayMillis
         *            how long to wait before the timer elapses, in milliseconds
         * @param shiftRight
         *            whether to shift up or not
         * @param multiplier
         *            the number of steps to shift
         */
        public void schedule(int delayMillis, boolean shiftRight, int multiplier) {
            firstRun = true;
            this.shiftRight = shiftRight;
            this.multiplier = multiplier;
            super.schedule(delayMillis);
        }
    }

    /**
     * The current value.
     */
    protected Double curValue;

    /**
     * The knob that slides across the line.
     */
    protected Image knobImage = new Image();

    /**
     * The timer used to continue to shift the knob if the user holds down a key.
     */
    private KeyTimer keyTimer = new KeyTimer();

    /**
     * The elements used to display labels above the ticks.
     */
    protected List<Element> tickLabelElements = new ArrayList<Element>();

    /**
     * The elements used to display the marker labels.
     */
    protected List<Element> markerLabelElements = new ArrayList<Element>();

    /**
     * The formatter used to generate label text.
     */
    protected LabelFormatter tickLabelFormatter;

    /**
     * The line that the knob moves over.
     */
    protected Element lineElement;

    /**
     * The offset between the edge of the shell and the line.
     */
    protected int lineLeftOffset = 0;

    /**
     * The maximum slider value.
     */
    protected Double maxValue;

    /**
     * The minimum slider value.
     */
    protected Double minValue;

    /**
     * The number of labels to show.
     */
    private int numTickLabels = 0;

    /**
     * The number of tick marks to show.
     */
    protected int numTicks = 0;

    /**
     * A bit indicating whether or not we are currently sliding the slider bar due to keyboard events.
     */
    private boolean slidingKeyboard = false;

    /**
     * A bit indicating whether or not we are currently sliding the slider bar due to mouse events.
     */
    private boolean slidingMouse = false;

    /**
     * A bit indicating whether or not the slider is enabled
     */
    protected boolean enabled = true;

    /**
     * The images used with the sliding bar.
     */
    private SliderBarImages images;

    /**
     * The size of the increments between knob positions.
     */
    protected double stepSize;

    /**
     * The elements used to display tick marks, which are the vertical lines along the slider bar.
     */
    protected List<Element> tickElements = new ArrayList<Element>();

    /**
     * The elements used to display additional markers on the slider bar.
     */
    protected List<Element> markerElements = new ArrayList<Element>();

    private List<Marker> markers = new ArrayList<Marker>();

    private class Marker {
        String name;
        Double position;

        public Marker(String name, Double position) {
            super();
            this.name = name;
            this.position = position;
        }
    }

    /**
     * Create a slider bar.
     */
    public SliderBar() {
        this(null, null, null);
    }

    /**
     * Create a slider bar.
     * 
     * @param minValue
     *            the minimum value in the range
     * @param maxValue
     *            the maximum value in the range
     */
    public SliderBar(double minValue, double maxValue) {
        this(minValue, maxValue, null);
    }

    /**
     * Create a slider bar.
     * 
     * @param minValue
     *            the minimum value in the range
     * @param maxValue
     *            the maximum value in the range
     * @param tickLabelFormatter
     *            the label formatter
     */
    public SliderBar(Double minValue, Double maxValue, LabelFormatter tickLabelFormatter) {
        this(minValue, maxValue, tickLabelFormatter, SliderBarImages.INSTANCE);
    }

    /**
     * Create a slider bar.
     * 
     * @param minValue
     *            the minimum value in the range
     * @param maxValue
     *            the maximum value in the range
     * @param tickLabelFormatter
     *            the label formatter
     * @param images
     *            the images to use for the slider
     */
    public SliderBar(Double minValue, Double maxValue, LabelFormatter tickLabelFormatter, SliderBarImages images) {
        super();
        images.sliderBarCss().ensureInjected();
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.images = images;
        setLabelFormatter(tickLabelFormatter);
        // Create the outer shell
        getElement().getStyle().setPosition(Position.RELATIVE);
        setStyleName("gwt-SliderBar-shell");
        // Create the line
        lineElement = DOM.createDiv();
        DOM.appendChild(getElement(), lineElement);
        lineElement.getStyle().setPosition(Position.ABSOLUTE);
        lineElement.setPropertyString("className", "gwt-SliderBar-line");
        // Create the knob
        knobImage.setResource(images.slider());
        Element knobElement = knobImage.getElement();
        DOM.appendChild(getElement(), knobElement);
        knobElement.getStyle().setPosition(Position.ABSOLUTE);
        knobElement.setPropertyString("className", "gwt-SliderBar-knob");
        sinkEvents(Event.MOUSEEVENTS | Event.KEYEVENTS | Event.FOCUSEVENTS | Event.TOUCHEVENTS);
        // workaround to render properly when parent Widget does not
        // implement ProvidesResize since DOM doesn't provide element
        // height and width until onModuleLoad() finishes.
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                onResize();
            }
        });
    }

    public boolean isMinMaxInitialized() {
        return minValue != null && maxValue != null;
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Double> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     * Return the current value.
     * 
     * @return the current value
     */
    public Double getCurrentValue() {
        return curValue;
    }

    /**
     * Return the label formatter.
     * 
     * @return the label formatter
     */
    public LabelFormatter getLabelFormatter() {
        return tickLabelFormatter;
    }

    /**
     * Return the max value.
     * 
     * @return the max value
     */
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * Return the minimum value.
     * 
     * @return the minimum value
     */
    public Double getMinValue() {
        return minValue;
    }

    /**
     * Return the number of labels.
     * 
     * @return the number of labels
     */
    public int getNumLabels() {
        return numTickLabels;
    }

    /**
     * Return the number of ticks.
     * 
     * @return the number of ticks
     */
    public int getNumTicks() {
        return numTicks;
    }

    /**
     * Return the step size.
     * 
     * @return the step size
     */
    public double getStepSize() {
        return stepSize;
    }

    /**
     * Return the total range between the minimum and maximum values.
     * 
     * @return the total range
     */
    public double getTotalRange() {
        if (minValue == null || maxValue == null || minValue > maxValue) {
            return 0;
        } else {
            return maxValue - minValue;
        }
    }

    public Double getValue() {
        return curValue;
    }

    /**
     * @return Gets whether this widget is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Listen for events that will move the knob.
     * 
     * @param event
     *            the event that occurred
     */
    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (enabled) {
            switch (DOM.eventGetType(event)) {
            // Unhighlight and cancel keyboard events
            case Event.ONBLUR:
                keyTimer.cancel();
                if (slidingMouse) {
                    DOM.releaseCapture(getElement());
                    slidingMouse = false;
                    // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
                    slideKnob(event.getClientX());
                    stopSliding(true, true);
                } else if (slidingKeyboard) {
                    slidingKeyboard = false;
                    stopSliding(true, true);
                }
                unhighlight();
                break;
            // Highlight on focus
            case Event.ONFOCUS:
                highlight();
                break;
            // Mousewheel events
            case Event.ONMOUSEWHEEL:
                final double velocityY = event.getDeltaY();
                event.preventDefault();
                if (velocityY > 0) {
                    shiftRight(1);
                } else {
                    shiftLeft(1);
                }
                break;
            // Shift left or right on key press
            case Event.ONKEYDOWN:
                if (!slidingKeyboard) {
                    int multiplier = 1;
                    if (event.getCtrlKey()) {
                        multiplier = (int) (getTotalRange() / stepSize / 10);
                    }
                    switch (event.getKeyCode()) {
                    case KeyCodes.KEY_HOME:
                        event.preventDefault();
                        setCurrentValue(minValue);
                        break;
                    case KeyCodes.KEY_END:
                        event.preventDefault();
                        setCurrentValue(maxValue);
                        break;
                    case KeyCodes.KEY_LEFT:
                        event.preventDefault();
                        slidingKeyboard = true;
                        startSliding(false, true);
                        shiftLeft(multiplier);
                        keyTimer.schedule(400, false, multiplier);
                        break;
                    case KeyCodes.KEY_RIGHT:
                        event.preventDefault();
                        slidingKeyboard = true;
                        startSliding(false, true);
                        shiftRight(multiplier);
                        keyTimer.schedule(400, true, multiplier);
                        break;
                    case 32:
                        event.preventDefault();
                        setCurrentValue(minValue + getTotalRange() / 2);
                        break;
                    }
                }
                break;
            // Stop shifting on key up
            case Event.ONKEYUP:
                keyTimer.cancel();
                if (slidingKeyboard) {
                    slidingKeyboard = false;
                    stopSliding(true, true);
                }
                break;
            // Mouse Events
            case Event.ONMOUSEDOWN:
                event.preventDefault();
                startSlidingAtX(event.getClientX());
                break;
            case Event.ONMOUSEUP:
                if (slidingMouse) {
                    DOM.releaseCapture(getElement());
                    slidingMouse = false;
                    // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
                    slideKnob(event.getClientX());
                    stopSliding(true, true);
                }
                break;
            case Event.ONMOUSEMOVE:
                if (slidingMouse) {
                    // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
                    slideKnob(event.getClientX());
                }
                break;
            case Event.ONTOUCHSTART:
                event.preventDefault();
                startSlidingAtX(event.getTouches().get(0).getClientX());
                break;
            case Event.ONTOUCHMOVE:
                if (slidingMouse) {
                    // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
                    slideKnob(event.getTouches().get(event.getTouches().length()-1).getClientX());
                }
                break;
            case Event.ONTOUCHEND:
                if (slidingMouse) {
                    DOM.releaseCapture(getElement());
                    slidingMouse = false;
                    // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
                    slideKnob(event.getTouches().get(event.getTouches().length()-1).getClientX());
                    stopSliding(true, true);
                }
                break;
            }
        }
    }

    private void startSlidingAtX(int clientX) {
        setFocus(true);
        slidingMouse = true;
        DOM.setCapture(getElement());
        startSliding(true, true);
        // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
        slideKnob(clientX);
    }

    /**
     * This method is called when the dimensions of the parent element change. Subclasses should override this method as
     * needed.
     * 
     * @param width
     *            the new client width of the element
     * @param height
     *            the new client height of the element
     */
    public void onResize(int width, int height) {
        // Center the line in the shell
        int lineWidth = lineElement.getOffsetWidth();
        lineLeftOffset = (width / 2) - (lineWidth / 2);
        lineElement.getStyle().setLeft(lineLeftOffset, Unit.PX);

        // Draw the other components
        drawTickLabels();
        drawTicks();
        drawMarkers();
        drawMarkerLabels();
        drawKnob();
    }

    /**
     * Redraw the progress bar when something changes the layout.
     */
    public void redraw() {
        if (isAttached()) {
            int width = getElement().getClientWidth();
            int height = getElement().getClientHeight();
            onResize(width, height);
        }
    }

    /**
     * Set the current value and fire the onValueChange event.
     * 
     * @param curValue
     *            the current value
     */
    public synchronized void setCurrentValue(Double curValue) {
        setCurrentValue(curValue, true);
    }

    /**
     * Set the current value and optionally fire the onValueChange event.
     * 
     * @param curValue
     *            the current value
     * @param fireEvent
     *            fire the onValue change event if true
     */
    public synchronized void setCurrentValue(Double curValue, boolean fireEvent) {
        // Confine the value to the range
        if (!isMinMaxInitialized() || curValue == null) {
            return;
        }
        Double newValue = Math.max(minValue, Math.min(maxValue, curValue));
        final double remainder = (newValue - minValue) % stepSize;
        newValue -= remainder;
        // Go to next step if more than halfway there
        if ((remainder > (stepSize / 2)) && ((newValue + stepSize) <= maxValue)) {
            newValue += stepSize;
        }
        final boolean isValueChanged = !newValue.equals(this.curValue) && this.curValue != null;
        this.curValue = newValue;
        // Redraw the knob
        drawKnob();
        // Fire the ValueChangeEvent if the value actually changed
        if (fireEvent && isValueChanged) {
            ValueChangeEvent.fire(this, this.curValue);
        }
    }

    /**
     * Sets whether this widget is enabled.
     * 
     * @param enabled
     *            true to enable the widget, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            knobImage.setResource(images.slider());
            lineElement.setPropertyString("className", "gwt-SliderBar-line");
        } else {
            knobImage.setResource(images.sliderDisabled());
            lineElement.setPropertyString("className", "gwt-SliderBar-line gwt-SliderBar-line-disabled");
        }
        redraw();
    }

    /**
     * Set the label formatter.
     * 
     * @param labelFormatter
     *            the label formatter
     */
    public void setLabelFormatter(LabelFormatter labelFormatter) {
        this.tickLabelFormatter = labelFormatter;
    }

    /**
     * Set the max value.
     * 
     * @param maxValue
     *            the current value
     */
    public void setMaxValue(Double maxValue, boolean fireEvent) {
        if (!Util.equalsWithNull(maxValue, this.maxValue)) {
            this.maxValue = maxValue;
            onMinMaxValueChanged(fireEvent);
        }
    }

    /**
     * Set the minimum value.
     * 
     * @param minValue
     *            the current value
     */
    public void setMinValue(Double minValue, boolean fireEvent) {
        if (!Util.equalsWithNull(minValue, this.minValue)) {
            this.minValue = minValue;
            onMinMaxValueChanged(fireEvent);
        }
    }

    /**
     * Set the minimum and maximum value
     * 
     * @param minValue
     *            the current value for min
     * @param maxValue
     *            the current value for max
     * @return {@link Pair} of min and max if they have changed
     */
    public Optional<Pair<Double, Double>> setMinAndMaxValue(Double minValue, Double maxValue, boolean fireEvent) {
        boolean changed = false;
        if (!Util.equalsWithNull(minValue, this.minValue)) {
            this.minValue = minValue;
            changed = true;
        }
        if (!Util.equalsWithNull(maxValue, this.maxValue)) {
            this.maxValue = maxValue;
            changed = true;
        }
        if (changed) {
            onMinMaxValueChanged(fireEvent);
            return Optional.of(new Pair<>(this.minValue, this.maxValue));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Handle value changes of min and/or max value
     */
    protected void onMinMaxValueChanged(boolean fireEvent) {
        redraw();
        resetCurrentValue(fireEvent);
    }

    /**
     * Set the number of tick labels to show on the line. Tick labels indicate the value of the slider at that point.
     * Use this method to enable tick labels.
     * 
     * If you set the number of tick labels equal to the total range divided by the step size, you will get a properly
     * aligned "jumping" effect where the knob jumps between tick labels.
     * 
     * Note that the number of tick labels displayed will be one more than the number you specify, so specify 1 labels
     * to show labels on either end of the line. In other words, numTickLabels is really the number of slots between the
     * labels.
     * 
     * setNumTickLabels(0) will disable the labels.
     * 
     * @param numTickLabels
     *            the number of tick labels to show
     */
    public void setNumTickLabels(int numTickLabels) {
        this.numTickLabels = numTickLabels;
        drawTickLabels();
    }

    /**
     * Set the number of ticks to show on the line. A tick is a vertical line that represents a division of the overall
     * line. Use this method to enable ticks.
     * 
     * If you set the number of ticks equal to the total range divided by the step size, you will get a properly aligned
     * "jumping" effect where the knob jumps between ticks.
     * 
     * Note that the number of ticks displayed will be one more than the number you specify, so specify 1 tick to show
     * ticks on either end of the line. In other words, numTicks is really the number of slots between the ticks.
     * 
     * setNumTicks(0) will disable ticks.
     * 
     * @param numTicks
     *            the number of ticks to show
     */
    public void setNumTicks(int numTicks) {
        this.numTicks = numTicks;
        drawTicks();
    }

    /**
     * Set the step size.
     * 
     * @param stepSize
     *            the current value
     */
    public void setStepSize(double stepSize, boolean fireEvent) {
        this.stepSize = stepSize;
        resetCurrentValue(fireEvent);
    }

    public void setValue(Double value) {
        setCurrentValue(value, false);
    }

    public void setValue(Double value, boolean fireEvent) {
        setCurrentValue(value, fireEvent);
    }

    /**
     * Shift to the left (smaller value).
     * 
     * @param numSteps
     *            the number of steps to shift
     */
    public void shiftLeft(int numSteps) {
        setCurrentValue(getCurrentValue() - numSteps * stepSize);
    }

    /**
     * Shift to the right (greater value).
     * 
     * @param numSteps
     *            the number of steps to shift
     */
    public void shiftRight(int numSteps) {
        setCurrentValue(getCurrentValue() + numSteps * stepSize);
    }

    /**
     * Format the label to display above the ticks
     * 
     * Override this method in a subclass to customize the format. By default, this method returns the integer portion
     * of the value.
     * 
     * @param value
     *            the value at the label
     * @return the text to put in the label
     */
    protected String formatTickLabel(Double value, Double previousValue) {
        final String result;
        if (tickLabelFormatter != null) {
            result = tickLabelFormatter.formatLabel(this, value, previousValue);
        } else {
            result = (int) (10 * value) / 10.0 + "";
        }
        return result;
    }

    /**
     * Get the percentage of the knob's position relative to the size of the line. The return value will be between 0.0
     * and 1.0.
     * 
     * @return the current percent complete
     */
    protected double getKnobPercent() {
        // If we have no range
        if (maxValue <= minValue) {
            return 0;
        }

        // Calculate the relative progress
        double percent = (curValue - minValue) / (maxValue - minValue);
        return Math.max(0.0, Math.min(1.0, percent));
    }

    /**
     * This method is called immediately after a widget becomes attached to the browser's document.
     */
    @Override
    protected void onLoad() {
        // Reset the position attribute of the parent element
        getElement().getStyle().setPosition(Position.RELATIVE);
    }

    /**
     * Draw the knob where it is supposed to be relative to the line.
     */
    protected void drawKnob() {
        if (isAttached() && isMinMaxInitialized()) {
            // Move the knob to the correct position
            Element knobElement = knobImage.getElement();
            int lineWidth = lineElement.getOffsetWidth();
            int knobWidth = knobElement.getOffsetWidth();
            int knobLeftOffset = (int) (lineLeftOffset + (getKnobPercent() * lineWidth) - (knobWidth / 2));
            knobLeftOffset = Math.min(knobLeftOffset, lineLeftOffset + lineWidth - (knobWidth / 2) - 1);
            knobElement.getStyle().setLeft(knobLeftOffset, Unit.PX);
        }
    }

    /**
     * Draw the labels along the line.
     */
    protected void drawTickLabels() {
        if (isAttached() && isMinMaxInitialized()) {
            // Draw the tick labels
            int lineWidth = lineElement.getOffsetWidth();
            if (numTickLabels > 0) {
                // Create the labels or make them visible
                Double previousValue = null;
                for (int i = 0; i <= numTickLabels; i++) {
                    Element label = null;
                    if (i < tickLabelElements.size()) {
                        label = tickLabelElements.get(i);
                    } else { // Create the new label
                        label = DOM.createDiv();
                        label.getStyle().setPosition(Position.ABSOLUTE);
                        label.getStyle().setDisplay(Display.NONE);
                        if (enabled) {
                            label.setPropertyString("className", "gwt-SliderBar-ticklabel");
                        } else {
                            label.setPropertyString("className", "gwt-SliderBar-ticklabel-disabled");
                        }
                        DOM.appendChild(getElement(), label);
                        tickLabelElements.add(label);
                    }
                    // Set the label text
                    double value = minValue + (getTotalRange() * i / numTickLabels);
                    label.getStyle().setVisibility(Visibility.HIDDEN);
                    label.getStyle().setProperty("display", "");
                    label.setPropertyString("innerHTML", formatTickLabel(value, previousValue));
                    // Move to the left so the label width is not clipped by the shell
                    label.getStyle().setLeft(0, Unit.PX);
                    // Position the label and make it visible
                    int labelWidth = label.getOffsetWidth();
                    int labelLeftOffset = lineLeftOffset + (lineWidth * i / numTickLabels) - (labelWidth / 2);
                    labelLeftOffset = Math.min(labelLeftOffset, lineLeftOffset + lineWidth - labelWidth);
                    labelLeftOffset = Math.max(labelLeftOffset, lineLeftOffset);
                    label.getStyle().setLeft(labelLeftOffset, Unit.PX);
                    label.getStyle().setVisibility(Visibility.VISIBLE);
                    previousValue = value;
                }
                // Hide unused labels
                for (int i = (numTickLabels + 1); i < tickLabelElements.size(); i++) {
                    tickLabelElements.get(i).getStyle().setDisplay(Display.NONE);
                }
            } else { // Hide all labels
                for (Element elem : tickLabelElements) {
                    elem.getStyle().setDisplay(Display.NONE);
                }
            }
        }
    }

    /**
     * Draw the tick along the line.
     */
    protected void drawTicks() {
        if (isAttached() && isMinMaxInitialized()) {
            // Draw the ticks
            int lineWidth = lineElement.getOffsetWidth();
            if (numTicks > 0) {
                // Create the ticks or make them visible
                for (int i = 0; i <= numTicks; i++) {
                    Element tick = null;
                    if (i < tickElements.size()) {
                        tick = tickElements.get(i);
                    } else { // Create the new tick
                        tick = DOM.createDiv();
                        tick.getStyle().setPosition(Position.ABSOLUTE);
                        tick.getStyle().setDisplay(Display.NONE);
                        DOM.appendChild(getElement(), tick);
                        tickElements.add(tick);
                    }
                    if (enabled) {
                        tick.setPropertyString("className", "gwt-SliderBar-tick");
                    } else {
                        tick.setPropertyString("className", "gwt-SliderBar-tick gwt-SliderBar-tick-disabled");
                    }
                    // Position the tick and make it visible
                    tick.getStyle().setVisibility(Visibility.HIDDEN);
                    tick.getStyle().setProperty("display", "");
                    int tickWidth = tick.getOffsetWidth();
                    int tickLeftOffset = lineLeftOffset + (lineWidth * i / numTicks) - (tickWidth / 2);
                    tickLeftOffset = Math.min(tickLeftOffset, lineLeftOffset + lineWidth - tickWidth);
                    tick.getStyle().setLeft(tickLeftOffset, Unit.PX);
                    tick.getStyle().setVisibility(Visibility.VISIBLE);
                }
                // Hide unused ticks
                for (int i = (numTicks + 1); i < tickElements.size(); i++) {
                    tickElements.get(i).getStyle().setDisplay(Display.NONE);
                }
            } else { // Hide all ticks
                for (Element elem : tickElements) {
                    elem.getStyle().setDisplay(Display.NONE);
                }
            }
        }
    }

    /**
     * Draw the markers.
     */
    private void drawMarkers() {
        if (isAttached() && isMinMaxInitialized()) {
            int numMarkers = markers.size();
            // Draw the markers
            int lineWidth = lineElement.getOffsetWidth();
            if (numMarkers > 0) {
                // Create the markers or make them visible
                Element lastMarkerElement = null;
                int lastMarkerOffsetLeft = 0;
                for (int i = 0; i < numMarkers; i++) {
                    Marker marker = markers.get(i);
                    Element markerElem = null;
                    if (i < markerElements.size()) {
                        markerElem = markerElements.get(i);
                    } else { // Create the new markes
                        markerElem = DOM.createDiv();
                        markerElem.getStyle().setPosition(Position.ABSOLUTE);
                        markerElem.getStyle().setDisplay(Display.NONE);
                        markerElem.setAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, "sliderBarMarker-" + marker.name);
                        DOM.appendChild(getElement(), markerElem);
                        markerElements.add(markerElem);
                    }
                    markerElem.setTitle(formatTickLabel(marker.position, null));
                    if (enabled) {
                        markerElem.setPropertyString("className", "gwt-SliderBar-mark");
                    } else {
                        markerElem.setPropertyString("className", "gwt-SliderBar-mark gwt-SliderBar-mark-disabled");
                    }
                    // Position the marker and make it visible
                    markerElem.getStyle().setVisibility(Visibility.HIDDEN);
                    markerElem.getStyle().setProperty("display", "");
                    double markerLinePosition = (marker.position - minValue) * lineWidth / getTotalRange();
                    int markerWidth = markerElem.getOffsetWidth();
                    int markerLeftOffset = lineLeftOffset + (int) markerLinePosition - (markerWidth / 2);
                    markerLeftOffset = Math.min(markerLeftOffset, lineLeftOffset + lineWidth - markerWidth);
                    markerElem.getStyle().setLeft(markerLeftOffset, Unit.PX);
                    markerElem.getStyle().setVisibility(Visibility.VISIBLE);
                    // change class of last marker if it is too close to the one just created
                    if (lastMarkerOffsetLeft > 0 && (markerLeftOffset-lastMarkerOffsetLeft) <= markerWidth) {
                        if (lastMarkerElement != null) {
                            lastMarkerElement.setPropertyString("className", "gwt-SliderBar-mark-tooClose");
                            lastMarkerElement.getStyle().setLeft(markerLeftOffset-10, Unit.PX);
                        }
                    }
                    lastMarkerElement = markerElem;
                    lastMarkerOffsetLeft = markerLeftOffset;
                }
                // Hide unused markers
                for (int i = numMarkers; i < markerElements.size(); i++) {
                    markerElements.get(i).getStyle().setDisplay(Display.NONE);
                }
            } else { // Hide all markers
                for (Element elem : markerElements) {
                    elem.getStyle().setDisplay(Display.NONE);
                }
            }
        }
    }

    /**
     * Draw the marker labels.
     */
    private void drawMarkerLabels() {
        if (isAttached() && isMinMaxInitialized()) {
            int numMarkers = markers.size();
            // Draw the marker labels
            int lineWidth = lineElement.getOffsetWidth();
            if (numMarkers > 0) {
                Element lastMarkerLabel = null;
                Marker lastMarker = null;
                int lastMarkerLabelOffsetLeft = 0;
                // Create the labels or make them visible
                for (int i = 0; i < numMarkers; i++) {
                    Marker marker = markers.get(i);
                    Element label = null;
                    if (i < markerLabelElements.size()) {
                        label = markerLabelElements.get(i);
                    } else { // Create the new label
                        label = DOM.createDiv();
                        label.getStyle().setPosition(Position.ABSOLUTE);
                        label.getStyle().setDisplay(Display.NONE);
                        DOM.appendChild(getElement(), label);
                        markerLabelElements.add(label);
                    }
                    label.setTitle(formatTickLabel(marker.position, null));
                    if (enabled) {
                        label.setPropertyString("className", "gwt-SliderBar-markerlabel");
                    } else {
                        label.setPropertyString("className", "gwt-SliderBar-markerlabel-disabled");
                    }
                    // Set the marker label text
                    label.getStyle().setVisibility(Visibility.HIDDEN);
                    label.getStyle().setProperty("display", "");
                    label.setPropertyString("innerHTML", marker.name);
                    // Move to the left so the label width is not clipped by the shell
                    label.getStyle().setLeft(0, Unit.PX);
                    // Position the label and make it visible
                    double markerLinePosition = (marker.position - minValue) * lineWidth / getTotalRange();
                    int labelWidth = label.getOffsetWidth();
                    int labelLeftOffset = lineLeftOffset + (int) markerLinePosition - (labelWidth / 2);
                    labelLeftOffset = Math.min(labelLeftOffset, lineLeftOffset + lineWidth - labelWidth);
                    label.getStyle().setLeft(labelLeftOffset, Unit.PX);
                    label.getStyle().setVisibility(Visibility.VISIBLE);
                    // hide last marker label if it is too close to the one just created
                    if (lastMarkerLabelOffsetLeft > 0 && (labelLeftOffset-lastMarkerLabelOffsetLeft) <= 20) {
                        if (lastMarkerLabel != null && lastMarker != null) {
                            lastMarkerLabel.setPropertyString("className", "gwt-SliderBar-markerlabel-tooClose");
                            lastMarkerLabel.setPropertyString("innerHTML", lastMarker.name); // no space for more
                            int currentMarkerOffsetLeft = labelLeftOffset;
                            try {
                                currentMarkerOffsetLeft = Integer.parseInt(markerElements.get(i).getStyle().getLeft().replace("px", ""));
                            } catch (NumberFormatException ex) {
                                // ignore, can't do anything
                            }
                            lastMarkerLabel.getStyle().setLeft(currentMarkerOffsetLeft-8, Unit.PX);
                        }
                    }
                    lastMarker = marker;
                    lastMarkerLabel = label;
                    lastMarkerLabelOffsetLeft = labelLeftOffset;
                }
                // Hide unused labels
                for (int i = (numMarkers + 1); i < markerLabelElements.size(); i++) {
                    markerLabelElements.get(i).getStyle().setDisplay(Display.NONE);
                }
            } else { // Hide all labels
                for (Element elem : markerLabelElements) {
                    elem.getStyle().setDisplay(Display.NONE);
                }
            }
        }
    }

    /**
     * Highlight this widget.
     */
    private void highlight() {
        String styleName = getStylePrimaryName();
        getElement().setPropertyString("className", styleName + " " + styleName + "-focused");
    }

    /**
     * Reset the progress to constrain the progress to the current range and redraw the knob as needed.
     */
    private synchronized void resetCurrentValue(boolean fireEvent) {
        setCurrentValue(getCurrentValue(), fireEvent);
    }

    /**
     * Slide the knob to a new location.
     * 
     * @param x
     *            the x coordinate, without scroll consideration, as extracted from a click or touch event
     */
    private void slideKnob(int clientX) {
        // Adding scrollLeft to adjust the position, if the user had scrolled with the lower scroll bar
        int x = clientX + Window.getScrollLeft();
        if (x > 0) {
            int lineWidth = lineElement.getOffsetWidth();
            int lineLeft = lineElement.getAbsoluteLeft();
            double percent = (double) (x - lineLeft) / lineWidth * 1.0;
            setCurrentValue(getTotalRange() * percent + minValue, true);
        }
    }

    /**
     * Start sliding the knob.
     * 
     * @param highlight
     *            true to change the style
     * @param fireEvent
     *            true to fire the event
     */
    private void startSliding(boolean highlight, boolean fireEvent) {
        if (highlight) {
            lineElement.setPropertyString("className", "gwt-SliderBar-line gwt-SliderBar-line-sliding");
            knobImage.getElement().setPropertyString("className", "gwt-SliderBar-knob gwt-SliderBar-knob-sliding");
            knobImage.setResource(images.sliderSliding());
        }
    }

    /**
     * Stop sliding the knob.
     * 
     * @param unhighlight
     *            true to change the style
     * @param fireEvent
     *            true to fire the event
     */
    private void stopSliding(boolean unhighlight, boolean fireEvent) {
        if (unhighlight) {
            lineElement.setPropertyString("className", "gwt-SliderBar-line");
            knobImage.getElement().setPropertyString("className", "gwt-SliderBar-knob");
            knobImage.setResource(images.slider());
        }
    }

    /**
     * Unhighlight this widget.
     */
    private void unhighlight() {
        getElement().setPropertyString("className", getStylePrimaryName());
    }

    @Override
    public void onResize() {
        redraw();
    }

    public void clearMarkers() {
        markers.clear();
    }
    
    public boolean addMarker(String markerName, Double markerPosition) {
        return markers.add(new Marker(markerName, markerPosition));
    }

    public boolean setMarker(String markerName, Double markerPosition) {
        Marker marker = findMarkerByName(markerName);
        if (marker != null) {
            marker.position = markerPosition;
            return true;
        }
        return false;
    }

    public boolean removeMarker(String markerName) {
        Marker marker = findMarkerByName(markerName);
        if (marker != null) {
            return markers.remove(marker);
        }
        return false;
    }

    public Iterator<Marker> getMarkers() {
        return markers.iterator();
    }

    private Marker findMarkerByName(String markerName) {
        for (Marker marker : markers) {
            if (marker.name.equals(markerName)) {
                return marker;
            }
        }
        return null;
    }
}
