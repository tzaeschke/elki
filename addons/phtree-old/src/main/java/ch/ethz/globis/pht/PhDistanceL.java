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

/**
 * Calculate the distance for integer values.
 * 
 * @see PhDistance
 * 
 * @author ztilmann
 */
public class PhDistanceL implements PhDistance {

  public static final PhDistanceL THIS = new PhDistanceL();
  
  /**
   * Calculate the distance for integer values.
   * 
   * @see PhDistance#dist(long[], long[])
   */
  @Override
  public double dist(long[] v1, long[] v2) {
    double d = 0;
    for (int i = 0; i < v1.length; i++) {
      double dl = (double)v1[i] - (double)v2[i];
      d += dl*dl;
    }
    return Math.sqrt(d);
  }
  
  /**
   * Calculate the estimated distance for integer values.
   * 
   * @see PhDistance#dist(long[], long[])
   */
  @Override
  public double distEst(long[] v1, long[] v2) {
    double d = 0;
    for (int i = 0; i < v1.length; i++) {
      double dl = (double)v1[i] - (double)v2[i];
      d += dl*dl;
    }
    return d;
  }
}