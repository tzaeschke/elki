package de.lmu.ifi.dbs.elki.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.lmu.ifi.dbs.elki.application.GeneratorXMLSpec;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorMain;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorStatic;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.Distribution;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.xml.XMLNodeIterator;

public class GeneratorXMLDatabaseConnection implements DatabaseConnection {
  /**
   * Logger
   */
  private static Logging logger = Logging.getLogger(GeneratorXMLDatabaseConnection.class);

  /**
   * A pattern defining whitespace.
   */
  public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  /**
   * Parameter to give the configuration file
   */
  public static final OptionID CONFIGFILE_ID = OptionID.getOrCreateOptionID("bymodel.spec", "The generator specification file.");

  /**
   * Parameter to give the configuration file
   */
  public static final OptionID RANDOMSEED_ID = OptionID.getOrCreateOptionID("bymodel.randomseed", "The random generator seed.");

  /**
   * Parameter to give the configuration file
   */
  public static final OptionID SIZE_SCALE_ID = OptionID.getOrCreateOptionID("bymodel.sizescale", "Factor for scaling the specified cluster sizes.");

  /**
   * File name of the generators XML Schema file.
   */
  private static final String GENERATOR_SCHEMA_FILE = GeneratorXMLSpec.class.getPackage().getName().replace('.', '/') + '/' + "GeneratorByModel.xsd";

  /**
   * The configuration file.
   */
  File specfile;

  /**
   * Parameter for scaling the cluster sizes.
   */
  double sizescale = 1.0;

  /**
   * The actual generator class
   */
  public GeneratorMain gen = new GeneratorMain();

  /**
   * Random generator used for initializing cluster generators.
   */
  private Random clusterRandom = null;

  /**
   * Set testAgainstModel flag
   */
  private boolean testAgainstModel = true;

  /**
   * Constructor.
   * 
   * @param specfile Specification file
   * @param sizescale Size scaling
   * @param clusterRandom Random number generator
   */
  public GeneratorXMLDatabaseConnection(File specfile, double sizescale, Random clusterRandom) {
    super();
    this.specfile = specfile;
    this.sizescale = sizescale;
    this.clusterRandom = clusterRandom;
    if(this.clusterRandom == null) {
      this.clusterRandom = new Random();
    }
  }

  @Override
  public MultipleObjectsBundle loadData() {
    if(logger.isVerbose()) {
      logger.verbose("Loading specification ...");
    }
    try {
      loadXMLSpecification();
    }
    catch(UnableToComplyException e) {
      throw new AbortException("Cannot load XML specification", e);
    }
    if(logger.isVerbose()) {
      logger.verbose("Generating clusters ...");
    }
    gen.setTestAgainstModel(testAgainstModel);
    try {
      gen.generate();
      return gen.getBundle();
    }
    catch(UnableToComplyException e) {
      throw new AbortException("Data generation failed. ", e);
    }
  }

  /**
   * Load the XML configuration file.
   * 
   * @throws UnableToComplyException
   */
  private void loadXMLSpecification() throws UnableToComplyException {
    try {
      InputStream in = new FileInputStream(specfile);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      URL url = ClassLoader.getSystemResource(GENERATOR_SCHEMA_FILE);
      if(url != null) {
        try {
          Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
          dbf.setSchema(schema);
          dbf.setIgnoringElementContentWhitespace(true);
        }
        catch(Exception e) {
          logger.warning("Could not set up XML Schema validation for speciciation file.");
        }
      }
      else {
        logger.warning("Could not set up XML Schema validation for speciciation file.");
      }
      Document doc = dbf.newDocumentBuilder().parse(in);
      Node root = doc.getDocumentElement();
      if(root.getNodeName() == "dataset") {
        processElementDataset(root);
      }
      else {
        throw new UnableToComplyException("Experiment specification has incorrect document element: " + root.getNodeName());
      }
    }
    catch(FileNotFoundException e) {
      throw new UnableToComplyException("Can't open specification file.", e);
    }
    catch(SAXException e) {
      throw new UnableToComplyException("Error parsing specification file.", e);
    }
    catch(IOException e) {
      throw new UnableToComplyException("IO Exception loading specification file.", e);
    }
    catch(ParserConfigurationException e) {
      throw new UnableToComplyException("Parser Configuration Error", e);
    }
  }

  /**
   * Process a 'dataset' Element in the XML stream.
   * 
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementDataset(Node cur) throws UnableToComplyException {
    // *** get parameters
    String seedstr = ((Element) cur).getAttribute("random-seed");
    if(seedstr != null && seedstr != "") {
      clusterRandom = new Random((int) (Integer.valueOf(seedstr) * sizescale));
    }
    String testmod = ((Element) cur).getAttribute("test-model");
    if(testmod != null && testmod != "") {
      testAgainstModel = (Integer.valueOf(testmod) != 0);
    }
    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeName() == "cluster") {
        processElementCluster(child);
      }
      else if(child.getNodeName() == "static") {
        processElementStatic(child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'cluster' Element in the XML stream.
   * 
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementCluster(Node cur) throws UnableToComplyException {
    int size = -1;
    double overweight = 1.0;

    String sizestr = ((Element) cur).getAttribute("size");
    if(sizestr != null && sizestr != "") {
      size = (int) (Integer.valueOf(sizestr) * sizescale);
    }

    String name = ((Element) cur).getAttribute("name");

    String dcostr = ((Element) cur).getAttribute("density-correction");
    if(dcostr != null && dcostr != "") {
      overweight = Double.valueOf(dcostr);
    }

    if(size < 0) {
      throw new UnableToComplyException("No valid cluster size given in specification file.");
    }
    if(name == null || name == "") {
      throw new UnableToComplyException("No cluster name given in specification file.");
    }

    // *** add new cluster object
    Random newRand = new Random(clusterRandom.nextLong());
    GeneratorSingleCluster cluster = new GeneratorSingleCluster(name, size, overweight, newRand);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeName() == "uniform") {
        processElementUniform(cluster, child);
      }
      else if(child.getNodeName() == "normal") {
        processElementNormal(cluster, child);
      }
      else if(child.getNodeName() == "gamma") {
        processElementGamma(cluster, child);
      }
      else if(child.getNodeName() == "rotate") {
        processElementRotate(cluster, child);
      }
      else if(child.getNodeName() == "translate") {
        processElementTranslate(cluster, child);
      }
      else if(child.getNodeName() == "clip") {
        processElementClipping(cluster, child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }

    gen.addCluster(cluster);
  }

  /**
   * Process a 'uniform' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementUniform(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    double min = 0.0;
    double max = 1.0;

    String minstr = ((Element) cur).getAttribute("min");
    if(minstr != null && minstr != "") {
      min = Double.valueOf(minstr);
    }
    String maxstr = ((Element) cur).getAttribute("max");
    if(maxstr != null && maxstr != "") {
      max = Double.valueOf(maxstr);
    }

    // *** new uniform generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new UniformDistribution(min, max, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'normal' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementNormal(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    double mean = 0.0;
    double stddev = 1.0;
    String meanstr = ((Element) cur).getAttribute("mean");
    if(meanstr != null && meanstr != "") {
      mean = Double.valueOf(meanstr);
    }
    String stddevstr = ((Element) cur).getAttribute("stddev");
    if(stddevstr != null && stddevstr != "") {
      stddev = Double.valueOf(stddevstr);
    }

    // *** New normal distribution generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new NormalDistribution(mean, stddev, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'gamma' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementGamma(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    double k = 1.0;
    double theta = 1.0;
    String kstr = ((Element) cur).getAttribute("k");
    if(kstr != null && kstr != "") {
      k = Double.valueOf(kstr);
    }
    String thetastr = ((Element) cur).getAttribute("theta");
    if(thetastr != null && thetastr != "") {
      theta = Double.valueOf(thetastr);
    }

    // *** New normal distribution generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new GammaDistribution(k, theta, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'rotate' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementRotate(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    int axis1 = 0;
    int axis2 = 0;
    double angle = 0.0;

    String a1str = ((Element) cur).getAttribute("axis1");
    if(a1str != null && a1str != "") {
      axis1 = Integer.valueOf(a1str);
    }
    String a2str = ((Element) cur).getAttribute("axis2");
    if(a2str != null && a2str != "") {
      axis2 = Integer.valueOf(a2str);
    }
    String anstr = ((Element) cur).getAttribute("angle");
    if(anstr != null && anstr != "") {
      angle = Double.valueOf(anstr);
    }
    if(axis1 <= 0 || axis1 > cluster.getDim()) {
      throw new UnableToComplyException("Invalid axis1 number given in specification file.");
    }
    if(axis1 <= 0 || axis1 > cluster.getDim()) {
      throw new UnableToComplyException("Invalid axis2 number given in specification file.");
    }
    if(axis1 == axis2) {
      throw new UnableToComplyException("Invalid axis numbers given in specification file.");
    }

    // Add rotation to cluster.
    cluster.addRotation(axis1 - 1, axis2 - 1, Math.toRadians(angle));

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'translate' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementTranslate(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    Vector offset = null;
    String vstr = ((Element) cur).getAttribute("vector");
    if(vstr != null && vstr != "") {
      offset = parseVector(vstr);
    }
    if(offset == null) {
      throw new UnableToComplyException("No translation vector given.");
    }

    // *** add new translation
    cluster.addTranslation(offset);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'clipping' Element in the XML stream.
   * 
   * @param cluster
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementClipping(GeneratorSingleCluster cluster, Node cur) throws UnableToComplyException {
    Vector cmin = null;
    Vector cmax = null;

    String minstr = ((Element) cur).getAttribute("min");
    if(minstr != null && minstr != "") {
      cmin = parseVector(minstr);
    }
    String maxstr = ((Element) cur).getAttribute("max");
    if(maxstr != null && maxstr != "") {
      cmax = parseVector(maxstr);
    }
    if(cmin == null || cmax == null) {
      throw new UnableToComplyException("No or incomplete clipping vectors given.");
    }

    // *** set clipping
    cluster.setClipping(cmin, cmax);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'static' cluster Element in the XML stream.
   * 
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementStatic(Node cur) throws UnableToComplyException {
    String name = ((Element) cur).getAttribute("name");
    if(name == null) {
      throw new UnableToComplyException("No cluster name given in specification file.");
    }

    LinkedList<Vector> points = new LinkedList<Vector>();
    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeName() == "point") {
        processElementPoint(points, child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
    // *** add new cluster object
    GeneratorStatic cluster = new GeneratorStatic(name, points);

    gen.addCluster(cluster);
    if(logger.isVerbose()) {
      logger.verbose("Loaded cluster " + cluster.name + " from specification.");
    }
  }

  /**
   * Parse a 'point' element (point vector for a static cluster)
   * 
   * @param points current list of points (to append to)
   * @param cur Current document nod
   * @throws UnableToComplyException
   */
  private void processElementPoint(LinkedList<Vector> points, Node cur) throws UnableToComplyException {
    Vector point = null;
    String vstr = ((Element) cur).getAttribute("vector");
    if(vstr != null && vstr != "") {
      point = parseVector(vstr);
    }
    if(point == null) {
      throw new UnableToComplyException("No translation vector given.");
    }

    // *** add new point
    points.add(point);

    // TODO: check for unknown attributes.
    for(Node child : new XMLNodeIterator(cur.getFirstChild())) {
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        logger.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Parse a string into a vector.
   * 
   * TODO: move this into utility package?
   * 
   * @param s String to parse
   * @return Vector
   * @throws UnableToComplyException
   */
  private Vector parseVector(String s) throws UnableToComplyException {
    String[] entries = WHITESPACE_PATTERN.split(s);
    double[] d = new double[entries.length];
    for(int i = 0; i < entries.length; i++) {
      try {
        d[i] = Double.valueOf(entries[i]);
      }
      catch(NumberFormatException e) {
        throw new UnableToComplyException("Could not parse vector.");
      }
    }
    return new Vector(d);
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
     * The configuration file.
     */
    File specfile = null;

    /**
     * Parameter for scaling the cluster sizes.
     */
    double sizescale = 1.0;

    /**
     * Random generator used for initializing cluster generators.
     */
    private Random clusterRandom = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Specification file
      final FileParameter cfgparam = new FileParameter(CONFIGFILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(cfgparam)) {
        specfile = cfgparam.getValue();
      }
      // Cluster size scaling
      final DoubleParameter scalepar = new DoubleParameter(SIZE_SCALE_ID, 1.0);
      if(config.grab(scalepar)) {
        sizescale = scalepar.getValue();
      }
      // Random generator
      final IntParameter seedpar = new IntParameter(RANDOMSEED_ID, true);
      if(config.grab(seedpar)) {
        clusterRandom = new Random(seedpar.getValue());
      }
    }

    @Override
    protected GeneratorXMLDatabaseConnection makeInstance() {
      return new GeneratorXMLDatabaseConnection(specfile, sizescale, clusterRandom);
    }
  }
}