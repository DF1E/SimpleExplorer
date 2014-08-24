/*
 * Copyright 2014 Yaroslav Mytkalyk
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
	 * Returns true if the fragment is in resumed state and userVisibleHint was
	 * set to true
	 * 
	 * @return true if resumed and visible
	 */
	protected final boolean isResumedAndVisible() {
		return mResumed && getUserVisibleHint();
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
