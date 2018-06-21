/*
 * Copyright © 2018 Erik Jaaniso
 *
 * This file is part of PubFetcher.
 *
 * PubFetcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PubFetcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PubFetcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.edamontology.pubfetcher.core.db.publication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PublicationPartList<T> extends PublicationPart {

	private static final long serialVersionUID = 1637409412666625844L;

	private List<T> list;

	PublicationPartList(PublicationPartName name) {
		super(name);
		list = new ArrayList<>();
	}

	public List<T> getList() {
		return list;
	}

	void set(List<T> list, PublicationPartType type, String url) {
		if (list == null ) {
			this.list = new ArrayList<>();
			set(null, null);
		} else {
			this.list = list;
			set(type, url);
		}
	}

	@Override
	public int getSize() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public String toStringPlain() {
		return "[" + list.stream().map(e -> e.toString()).collect(Collectors.joining("; ")) + "]";
	}

	@Override
	public String toStringPlainHtml() {
		return list.stream().map(e -> {
			if (e instanceof MeshTerm) return ((MeshTerm) e).toStringHtml();
			else if (e instanceof MinedTerm) return ((MinedTerm) e).toStringHtml();
			else return e.toString();
		}).collect(Collectors.joining("; "));
	}
}
