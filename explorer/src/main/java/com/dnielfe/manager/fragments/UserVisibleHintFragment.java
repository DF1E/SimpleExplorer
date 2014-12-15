/*
 * Copyright (C) 2014 Simple Explorer
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.dnielfe.manager.fragments;

import android.app.Fragment;

/**
 * Fragment that manages resumed and visible states combining them
 */
public abstract class UserVisibleHintFragment extends Fragment {

    private boolean mResumed;

    @Override
    public final void setUserVisibleHint(boolean isVisibleToUser) {
        final boolean needUpdate = mResumed
                && isVisibleToUser != this.getUserVisibleHint();
        super.setUserVisibleHint(isVisibleToUser);
        if (needUpdate) {
            if (isVisibleToUser) {
                this.onVisible();
            } else {
                this.onInvisible();
            }
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        mResumed = true;
        if (this.getUserVisibleHint()) {
            this.onVisible();
        }
    }

    @Override
    public final void onPause() {
        super.onPause();
        mResumed = false;
        this.onInvisible();
    }

    /**
     * Called when onResume was called and userVisibleHint is set to true or
     * vice-versa
     */
    protected abstract void onVisible();

    /**
     * Called when onStop was called or userVisibleHint is set to false
     */
    protected abstract void onInvisible();
}
