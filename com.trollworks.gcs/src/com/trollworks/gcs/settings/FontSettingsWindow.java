/*
 * Copyright ©1998-2021 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, version 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, version 2.0.
 */

package com.trollworks.gcs.settings;

import com.trollworks.gcs.ui.Fonts;
import com.trollworks.gcs.ui.ThemeFont;
import com.trollworks.gcs.ui.UIUtilities;
import com.trollworks.gcs.ui.layout.PrecisionLayout;
import com.trollworks.gcs.ui.layout.PrecisionLayoutData;
import com.trollworks.gcs.ui.widget.BaseWindow;
import com.trollworks.gcs.ui.widget.FontPanel;
import com.trollworks.gcs.ui.widget.Label;
import com.trollworks.gcs.ui.widget.LayoutConstants;
import com.trollworks.gcs.ui.widget.Panel;
import com.trollworks.gcs.utility.Dirs;
import com.trollworks.gcs.utility.FileType;
import com.trollworks.gcs.utility.I18n;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A window for editing font settings. */
public final class FontSettingsWindow extends SettingsWindow<Fonts> {
    private static FontSettingsWindow INSTANCE;

    private List<FontTracker> mFontPanels;
    private boolean           mIgnore;

    /** Displays the font settings window. */
    public static void display() {
        if (!UIUtilities.inModalState()) {
            FontSettingsWindow wnd;
            synchronized (FontSettingsWindow.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FontSettingsWindow();
                }
                wnd = INSTANCE;
            }
            wnd.setVisible(true);
        }
    }

    public FontSettingsWindow() {
        super(I18n.text("Font Settings"));
        fill();
    }

    @Override
    protected void preDispose() {
        synchronized (FontSettingsWindow.class) {
            INSTANCE = null;
        }
    }

    @Override
    protected Panel createContent() {
        Panel panel = new Panel(new PrecisionLayout().setColumns(2).
                setMargins(LayoutConstants.WINDOW_BORDER_INSET), false);
        mFontPanels = new ArrayList<>();
        for (ThemeFont font : Fonts.ALL) {
            if (font.isEditable()) {
                FontTracker tracker = new FontTracker(font);
                panel.add(new Label(font.toString()), new PrecisionLayoutData().setFillHorizontalAlignment());
                panel.add(tracker);
                mFontPanels.add(tracker);
            }
        }
        return panel;
    }

    @Override
    public void establishSizing() {
        pack();
        int width = getSize().width;
        setMinimumSize(new Dimension(width, 200));
        setMaximumSize(new Dimension(width, getPreferredSize().height));
    }

    @Override
    protected boolean shouldResetBeEnabled() {
        for (ThemeFont font : Fonts.ALL) {
            if (font.isEditable() && !font.getFont().equals(Fonts.defaultThemeFonts().getFont(font.getIndex()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Fonts getResetData() {
        return Fonts.defaultThemeFonts();
    }

    @Override
    protected void doResetTo(Fonts fonts) {
        mIgnore = true;
        for (FontTracker tracker : mFontPanels) {
            tracker.resetTo(fonts);
        }
        mIgnore = false;
        BaseWindow.forceRevalidateAndRepaint();
    }

    @Override
    protected Dirs getDir() {
        return Dirs.SETTINGS;
    }

    @Override
    protected FileType getFileType() {
        return FileType.FONT_SETTINGS;
    }

    @Override
    protected Fonts createSettingsFrom(Path path) throws IOException {
        String validityCheck = Fonts.verifyFontsAreValid(path);
        if (validityCheck == null) {
            return new Fonts(path);
        }
        throw new IOException(validityCheck);
    }

    @Override
    protected void exportSettingsTo(Path path) throws IOException {
        Fonts.currentThemeFonts().save(path);
    }

    private class FontTracker extends FontPanel {
        private int mIndex;

        FontTracker(ThemeFont font) {
            super(font.getFont());
            mIndex = font.getIndex();
            addActionListener((evt) -> {
                if (!mIgnore) {
                    Fonts.currentThemeFonts().setFont(mIndex, getCurrentFont());
                    adjustResetButton();
                    BaseWindow.forceRevalidateAndRepaint();
                }
            });
        }

        void resetTo(Fonts fonts) {
            Font font = fonts.getFont(mIndex);
            setCurrentFont(font);
            Fonts.currentThemeFonts().setFont(mIndex, font);
        }
    }
}
