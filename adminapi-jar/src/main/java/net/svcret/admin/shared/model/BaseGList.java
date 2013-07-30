package net.svcret.admin.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class BaseGList<T extends BaseGObject<T>> implements Iterable<T>, Serializable {

	private static final long serialVersionUID = 1L;

	private Comparator<? super T> myComparator;
	private Date myLastMerged;
	private List<T> myList;

	public BaseGList() {
		myList = new ArrayList<T>();
	}

	public void add(T theObject) {
		myList.add(theObject);
		sort();
	}

	public List<T> toList() {
		return Collections.unmodifiableList(myList);
	}

	public void addAll(Collection<T> theList) {
		myList.addAll(theList);
		sort();
	}

	public void clear() {
		myList.clear();
	}

	public T get(int theIndex) {
		return myList.get(theIndex);
	}

	/**
	 * @return the lastMerged
	 */
	public Date getLastMerged() {
		return myLastMerged;
	}

	public Iterator<T> iterator() {
		return myList.iterator();
	}

	public void mergeResults(BaseGList<T> theResult) {
		for (int i = 0; i < theResult.size(); i++) {
			T nextSrc = theResult.get(i);

			if (size() <= i) {
				myList.add(nextSrc);
			} else {
				T nextDest = get(i);
				if (nextDest.getPid() == nextSrc.getPid()) {
					nextDest.merge(nextSrc);
				} else {
					myList.add(i, nextSrc);
				}
			}
		}
		while (myList.size() > theResult.size()) {
			myList.remove(myList.size() - 1);
		}
		sort();
		myLastMerged = new Date();
	}

	public void remove(T theObject) {
		myList.remove(theObject);
	}

	protected void setComparator(Comparator<? super T> theComparator) {
		myComparator = theComparator;
		sort();
	}

	public int size() {
		return myList.size();
	}

	private void sort() {
		if (myComparator != null) {
			Collections.sort(myList, myComparator);
		}
	}

	public Collection<T> toCollection() {
		return Collections.unmodifiableCollection(myList);
	}

	// public AbstractDataProvider<T> asDataProvider() {
	// return myDataProvider;
	// }
	//
	// public class MyDataProvider extends ListDataProvider<T> {
	//
	//
	// }

}
