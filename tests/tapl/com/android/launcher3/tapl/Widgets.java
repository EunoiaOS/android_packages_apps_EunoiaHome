/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.tapl;

import static com.android.launcher3.tapl.LauncherInstrumentation.WAIT_TIME_MS;

import android.graphics.Point;
import android.graphics.Rect;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.android.launcher3.testing.TestProtocol;

import java.util.Collection;

/**
 * All widgets container.
 */
public final class Widgets extends LauncherInstrumentation.VisibleContainer {
    private static final int FLING_STEPS = 10;

    Widgets(LauncherInstrumentation launcher) {
        super(launcher);
        verifyActiveContainer();
    }

    /**
     * Flings forward (down) and waits the fling's end.
     */
    public void flingForward() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "want to fling forward in widgets")) {
            LauncherInstrumentation.log("Widgets.flingForward enter");
            final UiObject2 widgetsContainer = verifyActiveContainer();
            mLauncher.scroll(
                    widgetsContainer,
                    Direction.DOWN,
                    new Rect(0, 0, 0,
                            mLauncher.getBottomGestureMarginInContainer(widgetsContainer) + 1),
                    FLING_STEPS, false);
            try (LauncherInstrumentation.Closable c1 = mLauncher.addContextLayer("flung forward")) {
                verifyActiveContainer();
            }
            LauncherInstrumentation.log("Widgets.flingForward exit");
        }
    }

    /**
     * Flings backward (up) and waits the fling's end.
     */
    public void flingBackward() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "want to fling backwards in widgets")) {
            LauncherInstrumentation.log("Widgets.flingBackward enter");
            final UiObject2 widgetsContainer = verifyActiveContainer();
            mLauncher.scroll(
                    widgetsContainer,
                    Direction.UP,
                    new Rect(0, 0, mLauncher.getVisibleBounds(widgetsContainer).width(), 0),
                    FLING_STEPS, false);
            try (LauncherInstrumentation.Closable c1 = mLauncher.addContextLayer("flung back")) {
                verifyActiveContainer();
            }
            LauncherInstrumentation.log("Widgets.flingBackward exit");
        }
    }

    @Override
    protected LauncherInstrumentation.ContainerType getContainerType() {
        return LauncherInstrumentation.ContainerType.WIDGETS;
    }

    private int getWidgetsScroll() {
        return mLauncher.getTestInfo(
                TestProtocol.REQUEST_WIDGETS_SCROLL_Y)
                .getInt(TestProtocol.TEST_INFO_RESPONSE_FIELD);
    }

    public Widget getWidget(String labelText) {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "getting widget " + labelText + " in widgets list")) {
            final UiObject2 fullWidgetsPicker = verifyActiveContainer();
            mLauncher.assertTrue("Widgets container didn't become scrollable",
                    fullWidgetsPicker.wait(Until.scrollable(true), WAIT_TIME_MS));
            final Point displaySize = mLauncher.getRealDisplaySize();

            Rect headerRect = new Rect();
            final UiObject2 widgetsContainer = findTestAppWidgetsTableContainer(headerRect);
            mLauncher.assertTrue("Can't locate widgets list for the test app: "
                            + mLauncher.getLauncherPackageName(),
                    widgetsContainer != null);
            final BySelector labelSelector = By.clazz("android.widget.TextView").text(labelText);
            int i = 0;
            for (; ; ) {
                final Collection<UiObject2> tableRows = widgetsContainer.getChildren();
                for (UiObject2 row : tableRows) {
                    final Collection<UiObject2> widgetCells = row.getChildren();
                    for (UiObject2 widget : widgetCells) {
                        final UiObject2 label = widget.findObject(labelSelector);
                        if (label == null) {
                            continue;
                        }
                        mLauncher.assertEquals(
                                "View is not WidgetCell",
                                "com.android.launcher3.widget.WidgetCell",
                                widget.getClassName());

                        return new Widget(mLauncher, widget);
                    }
                }

                mLauncher.assertTrue("Too many attempts", ++i <= 40);
                final int scroll = getWidgetsScroll();
                mLauncher.scroll(
                        fullWidgetsPicker,
                        Direction.DOWN,
                        headerRect,
                        10,
                        true);
                final int newScroll = getWidgetsScroll();
                mLauncher.assertTrue(
                        "Scrolled in a wrong direction in Widgets: from " + scroll + " to "
                                + newScroll, newScroll >= scroll);
                mLauncher.assertTrue("Unable to scroll to the widget", newScroll != scroll);
            }
        }
    }

    /** Finds the widgets list of this test app from the collapsed full widgets picker. */
    private UiObject2 findTestAppWidgetsTableContainer(Rect outHeaderRect) {
        final BySelector headerSelector = By.res(mLauncher.getLauncherPackageName(),
                "widgets_list_header");
        final BySelector targetAppSelector = By.clazz("android.widget.TextView").text(
                mLauncher.getContext().getPackageName());
        final BySelector widgetsContainerSelector = By.res(mLauncher.getLauncherPackageName(),
                "widgets_table");

        boolean hasHeaderExpanded = false;
        for (int i = 0; i < 40; i++) {
            UiObject2 fullWidgetsPicker = verifyActiveContainer();

            UiObject2 header = fullWidgetsPicker.findObject(headerSelector);
            outHeaderRect.set(0, 0, 0, header.getVisibleBounds().height());
            mLauncher.assertTrue("Can't find a widget header", header != null);

            // Look for a header that has the test app name.
            UiObject2 headerTitle = fullWidgetsPicker.findObject(targetAppSelector);
            if (headerTitle != null) {
                // If we find the header and it has not been expanded, let's click it to see the
                // widgets list.
                if (!hasHeaderExpanded) {
                    hasHeaderExpanded = true;
                    mLauncher.clickLauncherObject(headerTitle);
                    // After clicking the header, the recyclerview has been updated. Let's refresh
                    // the container UIObject2.
                    fullWidgetsPicker = verifyActiveContainer();
                    // Refresh headerTitle because the first instance is stale after
                    // verifyActiveContainer call.
                    headerTitle = fullWidgetsPicker.findObject(targetAppSelector);
                }

                // Look for a widgets list.
                UiObject2 widgetsContainer = fullWidgetsPicker.findObject(widgetsContainerSelector);
                if (widgetsContainer != null) {
                    return widgetsContainer;
                }

            }
            mLauncher.scroll(
                    fullWidgetsPicker,
                    Direction.DOWN,
                    outHeaderRect,
                    /* steps= */ 10,
                    /* slowDown= */ true);
        }

        return null;
    }
}
