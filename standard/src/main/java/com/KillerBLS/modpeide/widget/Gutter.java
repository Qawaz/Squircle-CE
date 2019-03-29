/*
 * Licensed to the Light Team Software (Light Team) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Light Team licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.KillerBLS.modpeide.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.KillerBLS.modpeide.utils.commons.EditorController;

/**
 * @author Henry Thompson
 */
public class Gutter extends View {

    private EditorController mController;
    private TextProcessor mEditor;

    // region CONSTRUCTOR

    public Gutter(Context context) {
        super(context);
    }

    public Gutter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Gutter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Gutter(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    // endregion CONSTRUCTOR

    public void link(TextProcessor editor, EditorController controller) {
        if (editor != null) {
            mEditor = editor;
            mController = controller;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mEditor == null) {
            super.onDraw(canvas);
            return;
        }
        if (mController != null) {
            mEditor.updateGutter();
        }
    }
}