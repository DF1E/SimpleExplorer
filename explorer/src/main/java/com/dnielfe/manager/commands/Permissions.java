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

package com.dnielfe.manager.commands;

import java.io.Serializable;

public final class Permissions implements Serializable {

	private static final long serialVersionUID = 2682238088276963741L;

	public final boolean ur;
	public final boolean uw;
	public final boolean ux;

	public final boolean gr;
	public final boolean gw;
	public final boolean gx;

	public final boolean or;
	public final boolean ow;
	public final boolean ox;

	private final String output;

	public Permissions(boolean r, boolean w, boolean x) {
		this.ur = r;
		this.uw = w;
		this.ux = x;

		this.gr = false;
		this.gw = false;
		this.gx = false;

		this.or = false;
		this.ow = false;
		this.ox = false;

		final StringBuilder sb = new StringBuilder();
		sb.append(ur ? 'r' : '-');
		sb.append(uw ? 'w' : '-');
		sb.append(ux ? 'x' : '-');
		this.output = sb.toString();
	}

	public Permissions(boolean ur, boolean uw, boolean ux, boolean gr,
			boolean gw, boolean gx, boolean or, boolean ow, boolean ox) {

		this.ur = ur;
		this.uw = uw;
		this.ux = ux;

		this.gr = gr;
		this.gw = gw;
		this.gx = gx;

		this.or = or;
		this.ow = ow;
		this.ox = ox;

		StringBuilder sb = new StringBuilder();
		sb.append(ur ? 'r' : '-');
		sb.append(uw ? 'w' : '-');
		sb.append(ux ? 'x' : '-');
		sb.append(gr ? 'r' : '-');
		sb.append(gw ? 'w' : '-');
		sb.append(gx ? 'x' : '-');
		sb.append(or ? 'r' : '-');
		sb.append(ow ? 'w' : '-');
		sb.append(ox ? 'x' : '-');
		this.output = sb.toString();
	}

	public Permissions(String line) {
		if (line.length() != 10) {
			throw new IllegalArgumentException("Bad permission line");
		}

		this.ur = line.charAt(1) == 'r';
		this.uw = line.charAt(2) == 'w';
		this.ux = line.charAt(3) == 'x';

		this.gr = line.charAt(4) == 'r';
		this.gw = line.charAt(5) == 'w';
		this.gx = line.charAt(6) == 'x';

		this.or = line.charAt(7) == 'r';
		this.ow = line.charAt(8) == 'w';
		this.ox = line.charAt(9) == 'x';

		final StringBuilder sb = new StringBuilder();
		sb.append(ur ? 'r' : '-');
		sb.append(uw ? 'w' : '-');
		sb.append(ux ? 'x' : '-');
		sb.append(gr ? 'r' : '-');
		sb.append(gw ? 'w' : '-');
		sb.append(gx ? 'x' : '-');
		sb.append(or ? 'r' : '-');
		sb.append(ow ? 'w' : '-');
		sb.append(ox ? 'x' : '-');
		this.output = sb.toString();
	}

	@Override
	public String toString() {
		return this.output;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Permissions)) {
			return false;
		}
		final Permissions p = (Permissions) o;
		return this.ur == p.ur && this.uw == p.uw && this.ux == p.ux
				&& this.gr == p.gr && this.gw == p.gw && this.gx == p.gx
				&& this.or == p.or && this.ow == p.ow && this.ox == p.ox;
	}

	@Override
	public int hashCode() {
		int result = (ur ? 1 : 0);
		result = 31 * result + (uw ? 1 : 0);
		result = 31 * result + (ux ? 1 : 0);
		result = 31 * result + (gr ? 1 : 0);
		result = 31 * result + (gw ? 1 : 0);
		result = 31 * result + (gx ? 1 : 0);
		result = 31 * result + (or ? 1 : 0);
		result = 31 * result + (ow ? 1 : 0);
		result = 31 * result + (ox ? 1 : 0);
		return result;
	}
}
