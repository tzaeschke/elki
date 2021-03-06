package ch.ethz.globis.pht;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import ch.ethz.globis.pht.util.PhIteratorBase;
import ch.ethz.globis.pht.util.PhTreeQStats;
import ch.ethz.globis.pht.v8.PhTree8;

/**
 * k-dimensional index (quad-/oct-/n-tree).
 * Supports key/value pairs.
 *
 * See also : T. Zaeschke, C. Zimmerli, M.C. Norrie; 
 * "The PH-Tree -- A Space-Efficient Storage Structure and Multi-Dimensional Index", 
 * (SIGMOD 2014)
 *
 * @author ztilmann (Tilmann Zaeschke)
 * 
 * @param <T> The value type of the tree 
 *
 */
public abstract class PhTree<T> {


  public abstract int size();

  public abstract int getNodeCount();

  public abstract PhTreeQStats getQuality();

  public abstract PhTreeHelper.Stats getStats();

  public abstract PhTreeHelper.Stats getStatsIdealNoNode();


  /**
   * Insert an entry associated with a k dimensional key.
   * @param key
   * @param value
   * @return the previously associated value or {@code null} if the key was found
   */
  public abstract T put(long[] key, T value);

  public abstract boolean contains(long ... key);

  public abstract T get(long ... key);


  /**
   * Remove the entry associated with a k dimensional key.
   * @param key
   * @return the associated value or {@code null} if the key was found
   */
  public abstract T remove(long... key);

  public abstract String toStringPlain();

  public abstract String toStringTree();

  public abstract PhIterator<T> queryExtent();


  /**
   * Performs a range query. The parameters are the min and max keys.
   * @param min
   * @param max
   * @return Result iterator.
   */
  public abstract PhQuery<T> query(long[] min, long[] max);

  public abstract int getDIM();

  public abstract int getDEPTH();

  /**
   * Locate nearest neighbours for a given point in space.
   * @param nMin number of entries to be returned. More entries may be returned with several have
   * 				the same distance.
   * @param key
   * @return The query iterator.
   */
  public abstract PhQueryKNN<T> nearestNeighbour(int nMin, long... key);

  /**
   * Locate nearest neighbours for a given point in space.
   * @param nMin number of entries to be returned. More entries may be returned with several have
   * 				the same distance.
   * @param dist the distance function, can be {@code null}. The default is {@link PhDistanceL}.
   * @param dims the dimension filter, can be {@code null}
   * @param key
   * @return The query iterator.
   */
  public abstract PhQueryKNN<T> nearestNeighbour(int nMin, PhDistance dist, PhDimFilter dims, 
      long... key);

  /**
   * Find all entries within a given distance from a center point.
   * @param dist Maximum distance
   * @param center Center point
   * @return All entries with at most distance `dist` from `center`.
   */
  public PhRangeQuery<T> rangeQuery(double dist, long... center) {
    return rangeQuery(dist, null, center);
  }

  /**
   * Find all entries within a given distance from a center point.
   * @param dist Maximum distance
   * @param optionalDist Distance function, optional, can be `null`.
   * @param center Center point
   * @return All entries with at most distance `dist` from `center`.
   */
  public abstract PhRangeQuery<T> rangeQuery(double dist, PhDistance optionalDist, long... center);

  /**
   * Update the key of an entry. Update may fail if the old key does not exist, or if the new
   * key already exists.
   * @param oldKey
   * @param newKey
   * @return the value (can be {@code null}) associated with the updated key if the key could be 
   * updated, otherwise {@code null}.
   */
  public abstract T update(long[] oldKey, long[] newKey);

  /**
   * Create a new tree with the specified number of dimensions.
   * 
   * @param dim number of dimensions
   * @return PhTree
   */
  public static <T> PhTree<T> create(int dim) {
    return new PhTree8<T>(dim);
  }

  public static interface PhIterator<T> extends PhIteratorBase<long[], T, PhEntry<T>> {

    /**
     * Special 'next' method that avoids creating new objects internally by reusing Entry objects.
     * Advantage: Should completely avoid any GC effort.
     * Disadvantage: Returned PhEntries are not stable and are only valid until the
     * next call to next(). After that they may change state. Modifying returned entries may
     * invalidate the backing tree.
     * @return The next entry
     */
    PhEntry<T> nextEntryReuse();

  }

  public static interface PhQuery<T> extends PhIterator<T> {

    /**
     * Reset the query with the new 'min' and 'max' boundaries.
     * @param min
     * @param max
     */
    void reset(long[] min, long[] max);
  }

  public static interface PhQueryKNN<T> extends PhIterator<T> {

    /**
     * Reset the query with the new parameters.
     * @param nMin Minimum result count
     * @param dist Distance function
     * @param dims dimension filter to specify ignored dimensions
     * @param center The point to find the nearest neighbours for
     * @return the query itself
     */
    PhQueryKNN<T> reset(int nMin, PhDistance dist, PhDimFilter dims, long... center);
  }

  /**
   * Clear the tree.
   */
  public abstract void clear();
}

