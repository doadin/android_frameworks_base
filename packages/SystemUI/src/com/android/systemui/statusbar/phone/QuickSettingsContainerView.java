/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.systemui.R;

/**
 *
 */
public class QuickSettingsContainerView extends FrameLayout {

    // The number of columns in the QuickSettings grid
    private int mNumColumns;

    // The gap between tiles in the QuickSettings grid
    private float mCellGap;

    private float mPadding4Tiles = -8.0f;
    private float mPadding3Tiles = 0.0f;
    private float mSize4Tiles = 10.0f;
    private float mSize3Tiles = 12.0f;

    private int mTextSize;
    private int mTextPadding;

    private boolean mSingleRow;
    private Context mContext;
    private boolean mSmallIcons;

    public QuickSettingsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.QuickSettingsContainer, 0, 0);
        mSingleRow = a.getBoolean(R.styleable.QuickSettingsContainer_singleRow, false);
        a.recycle();
        mContext = context;
        updateResources();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // TODO: Setup the layout transitions
        LayoutTransition transitions = getLayoutTransition();
    }

    public void updateResources() {
        Resources r = getContext().getResources();
        ContentResolver resolver = mContext.getContentResolver();
        mSmallIcons = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_SETTINGS_SMALL_ICONS, 0, UserHandle.USER_CURRENT) == 1;
        mCellGap = r.getDimension(R.dimen.quick_settings_cell_gap);
        mNumColumns = r.getInteger(R.integer.quick_settings_num_columns);
        mTextSize = (int) mSize3Tiles;
        mTextPadding = (int) mPadding3Tiles;
        if (mSmallIcons) {
            mNumColumns = r.getInteger(R.integer.quick_settings_num_columns_small);
            mTextSize = (int) mSize4Tiles;
            mTextPadding = (int) mPadding4Tiles;
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the cell width dynamically
        int width = MeasureSpec.getSize(widthMeasureSpec);

        int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mNumColumns - 1) * mCellGap);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / mNumColumns);
        int cellHeight = 0;
        float cellGap = mCellGap;

        if (mSingleRow) {
            cellWidth = MeasureSpec.getSize(heightMeasureSpec);
            cellHeight = (int) cellWidth;
            cellGap /= 2;
        } else {
            cellHeight = (int) getResources().getDimension(R.dimen.quick_settings_cell_height);
        }

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        int totalWidth = 0;
        int cursor = 0;
        for (int i = 0; i < N; ++i) {
            // Update the child's width
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int colSpan = v.getColumnSpan();
                lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * cellGap);
                lp.height = cellHeight;

                // Measure the child
                int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                v.measure(newWidthSpec, newHeightSpec);
                cursor += colSpan;
                totalWidth += v.getMeasuredWidth() + cellGap;
            }
        }

        // Set the measured dimensions.  We always fill the tray width, but wrap to the height of
        // all the tiles.
        int numRows = (int) Math.ceil((float) cursor / mNumColumns);
        int newHeight = (int) ((numRows * cellHeight) + ((numRows - 1) * cellGap)) +
                getPaddingTop() + getPaddingBottom();
        if (mSingleRow) {
            int totalHeight = cellHeight + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(totalWidth, totalHeight);
        } else {
            setMeasuredDimension(width, newHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int N = getChildCount();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int cursor = 0;

        float cellGap = mCellGap;

        if (mSingleRow) {
            cellGap /= 2;
        }

        for (int i = 0; i < N; ++i) {
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (v.getVisibility() != GONE) {
                int col = cursor % mNumColumns;
                int colSpan = v.getColumnSpan();
                int row = cursor / mNumColumns;

                // Push the item to the next row if it can't fit on this one
                if ((col + colSpan) > mNumColumns && !mSingleRow) {
                    x = getPaddingLeft();
                    y += lp.height + cellGap;
                    row++;
                }

                // Layout the container
                v.layout(x, y, x + lp.width, y + lp.height);

                // Offset the position by the cell gap or reset the position and cursor when we
                // reach the end of the row
                cursor += v.getColumnSpan();
                if (cursor < (((row + 1) * mNumColumns)) || mSingleRow) {
                    x += lp.width + cellGap;
                } else if (!mSingleRow) {
                    x = getPaddingLeft();
                    y += lp.height + cellGap;
                }
            }
        }
    }

    public int getTileTextSize() {
        ContentResolver resolver = mContext.getContentResolver();
        mSmallIcons = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_SETTINGS_SMALL_ICONS, 0, UserHandle.USER_CURRENT) == 1;
        if (mSmallIcons) {
            return mTextSize = (int) mSize4Tiles;
        } else {
            return mTextSize = (int) mSize3Tiles;
        }
    }

    public int getTileTextPadding() {
        ContentResolver resolver = mContext.getContentResolver();
        mSmallIcons = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_SETTINGS_SMALL_ICONS, 0, UserHandle.USER_CURRENT) == 1;
        if (mSmallIcons) {
            return mTextPadding = (int) mPadding4Tiles;
        } else {
            return mTextPadding = (int) mPadding3Tiles;
        }
    }
}
