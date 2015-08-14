package de.lmu.ifi.dbs.elki.visualization.projector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Produce scatterplot projections.
 *
 * @author Erich Schubert
 *
 * @apiviz.has ScatterPlotProjector
 */
public class ScatterPlotFactory implements ProjectorFactory {
  /**
   * Maximum number of dimensions to visualize.
   *
   * FIXME: add scrolling function for higher dimensionality!
   */
  public static final int MAX_DIMENSIONS_DEFAULT = 10;

  /**
   * Stores the maximum number of dimensions to show.
   */
  private int maxdim = MAX_DIMENSIONS_DEFAULT;

  /**
   * Constructor.
   *
   * @param maxdim Maximum number of dimensions to show.
   */
  public ScatterPlotFactory(int maxdim) {
    super();
    this.maxdim = maxdim;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<Relation<?>> it = VisualizationTree.filterResults(context, start, Relation.class);
    candidate: for(; it.valid(); it.advance()) {
      Relation<?> rel = it.get();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      // Do not enable nested relations by default:
      Hierarchy.Iter<Relation<?>> it2 = new VisualizationTree.FilteredIter<>(context.getHierarchy().iterAncestors(rel), Relation.class);
      parent: for(; it2.valid(); it2.advance()) {
        // Parent relation
        final Relation<?> rel2 = (Relation<?>) it2.get();
        if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel2.getDataTypeInformation())) {
          continue parent;
        }
        // Only first child relation
        Hierarchy.Iter<Relation<?>> it3 = new VisualizationTree.FilteredIter<>(context.getHierarchy().iterDescendants(rel2), Relation.class);
        if(it3.valid() && it3.get() == rel) {
          continue parent; // First child vector relation, this is ok.
        }
        // TODO: add Actions instead.
        continue candidate;
      }
      @SuppressWarnings("unchecked")
      Relation<NumberVector> vrel = (Relation<NumberVector>) rel;
      final int dim = RelationUtil.dimensionality(vrel);
      ScatterPlotProjector<NumberVector> proj = new ScatterPlotProjector<>(vrel, Math.min(maxdim, dim));
      context.addVis(vrel, proj);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the maximum number of dimensions.
     *
     * <p>
     * Code: -vis.maxdim
     * </p>
     */
    public static final OptionID MAXDIM_ID = new OptionID("vis.maxdim", "Maximum number of dimensions to display.");

    /**
     * Stores the maximum number of dimensions to show.
     */
    private int maxdim = MAX_DIMENSIONS_DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter maxdimP = new IntParameter(MAXDIM_ID, MAX_DIMENSIONS_DEFAULT);
      maxdimP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxdimP)) {
        maxdim = maxdimP.intValue();
      }
    }

    @Override
    protected ScatterPlotFactory makeInstance() {
      return new ScatterPlotFactory(maxdim);
    }
  }
}