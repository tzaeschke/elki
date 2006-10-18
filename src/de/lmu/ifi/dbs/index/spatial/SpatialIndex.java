package de.lmu.ifi.dbs.index.spatial;

import java.util.List;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.EqualStringConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Abstract super class for all spatial index classes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class SpatialIndex<O extends NumberVector, N extends SpatialNode<N,E>, E extends SpatialEntry> extends Index<O, N, E> {
  /**
   * Option string for parameter bulk.
   */
  public static final String BULK_LOAD_F = "bulk";

  /**
   * Description for parameter bulk.
   */
  public static final String BULK_LOAD_D = "flag to specify bulk load (default is no bulk load)";

  /**
   * Option string for parameter bulkstrategy.
   */
  public static final String BULK_LOAD_STRATEGY_P = "bulkstrategy";

  /**
   * Description for parameter bulkstrategy.
   */
  public static final String BULK_LOAD_STRATEGY_D = "the strategy for bulk load, available strategies are: [" +
                                                    BulkSplit.Strategy.MAX_EXTENSION + "| " + BulkSplit.Strategy.ZCURVE + "]"
                                                    + "(default is " + BulkSplit.Strategy.ZCURVE + ")";

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  public SpatialIndex() {
    super();
    optionHandler.put(BULK_LOAD_F, new Flag(BULK_LOAD_F,BULK_LOAD_D));
    
   
    StringParameter bulk = new StringParameter(BULK_LOAD_STRATEGY_P,BULK_LOAD_STRATEGY_D,new EqualStringConstraint(new String[]{BulkSplit.Strategy.MAX_EXTENSION.toString(),BulkSplit.Strategy.ZCURVE.toString()}));
    bulk.setDefaultValue(BulkSplit.Strategy.ZCURVE.getClass().getName());
    optionHandler.put(BULK_LOAD_STRATEGY_P, bulk);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    bulk = optionHandler.isSet(BULK_LOAD_F);

    if (bulk) {
      if (optionHandler.isSet(BULK_LOAD_STRATEGY_P)) {
        String strategy = optionHandler.getOptionValue(BULK_LOAD_STRATEGY_P);
        if (strategy.equals(BulkSplit.Strategy.MAX_EXTENSION.toString())) {
          bulkLoadStrategy = BulkSplit.Strategy.MAX_EXTENSION;
        }
        else if (strategy.equals(BulkSplit.Strategy.ZCURVE.toString())) {
          bulkLoadStrategy = BulkSplit.Strategy.ZCURVE;
        }
        else throw new WrongParameterValueException(BULK_LOAD_STRATEGY_P, strategy, BULK_LOAD_STRATEGY_D);
      }
      else bulkLoadStrategy = BulkSplit.Strategy.ZCURVE;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(BULK_LOAD_F, Boolean.toString(bulk));
    if (bulk) {
      mySettings.addSetting(BULK_LOAD_STRATEGY_P, bulkLoadStrategy.toString());
    }

    return attributeSettings;

  }

  /**
   * Sets the databse in the distance function of this index (if existing).
   * Subclasses may need to overwrite this method.
   *
   * @param database the database
   */
  public void setDatabase(Database<O> database) {
    //do nothing
  }

  /**
   * Performs a range query for the given object with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> rangeQuery(final O obj, final String epsilon,
                                                                          final DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> kNNQuery(final O obj, final int k,
                                                                        final DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object           the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(final O object, final int k,
                                                                               final DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a bulk k-nearest neighbor query for the given object IDs. The
   * query result is in ascending order to the distance to the query objects.
   *
   * @param ids              the query objects
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>>List<List<QueryResult<D>>> bulkKNNQueryForIDs(List<Integer> ids, int k, SpatialDistanceFunction<O, D> distanceFunction);

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   *
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}
