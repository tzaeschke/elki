package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Weighted Canberra distance function, a variation of Manhattan distance.
 * 
 * TODO: add parameterizer. As of now, this can only be used from Java code.
 * 
 * @author Erich Schubert
 */
public class WeightedCanberraDistanceFunction extends AbstractSpatialDoubleDistanceFunction {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   */
  public WeightedCanberraDistanceFunction(double[] weights) {
    super();
    this.weights = weights;
  }

  @Override
  public double doubleDistance(NumberVector<?> o1, NumberVector<?> o2) {
    final int dim = weights.length;
    double sum = 0.0;
    for (int d = 0; d < dim; d++) {
      double v1 = o1.doubleValue(d);
      double v2 = o2.doubleValue(d);
      final double div = Math.abs(v1) + Math.abs(v2);
      if (div > 0) {
        sum += weights[d] * Math.abs(v1 - v2) / div;
      }
    }
    return sum;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = weights.length;
    double sum = 0.0;
    for (int d = 0; d < dim; d++) {
      final double m1, m2;
      if (mbr1.getMax(d) < mbr2.getMin(d)) {
        m1 = mbr2.getMin(d);
        m2 = mbr1.getMax(d);
      } else if (mbr1.getMin(d) > mbr2.getMax(d)) {
        m1 = mbr1.getMin(d);
        m2 = mbr2.getMax(d);
      } else { // The mbrs intersect!
        continue;
      }
      final double manhattanI = m1 - m2;
      final double a1 = Math.max(-mbr1.getMin(d), mbr1.getMax(d));
      final double a2 = Math.max(-mbr2.getMin(d), mbr2.getMax(d));
      final double div = a1 + a2;
      if (div > 0) {
        sum += weights[d] * manhattanI / div;
      }
    }
    return sum;
  }

  @Override
  public boolean isMetric() {
    // As this is also reffered to as "canberra metric", it is probably a metric
    // But *maybe* only for positive numbers only?
    return true;
  }
}