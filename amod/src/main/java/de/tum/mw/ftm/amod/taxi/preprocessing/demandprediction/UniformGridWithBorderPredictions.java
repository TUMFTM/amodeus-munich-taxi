package de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction;

import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import javax.swing.border.Border;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UniformGridWithBorderPredictions implements GridDemandPrediction {
    private Logger logger = Logger.getLogger(UniformGridWithBorderPredictions.class);
    private LocalDateTime timestampStart;
    private LocalDateTime timestampStop;
    private int numberOfColumns;
    private int numberOfRows;
    private TreeMap<Double, Map<GridCell, double[]>> gridPredictions;
    private TreeMap<Double, Map<AmodeusUtil.BorderOrientation, double[]>> borderPredictions;
    private int predictionWindows;
    private double predictionHorizon;
    private GridCell[] gridCells;
    private Map<AmodeusUtil.BorderOrientation, Polygon> borderPolygons;

    private UniformGridWithBorderPredictions(){

    }

    public UniformGridWithBorderPredictions(int numberOfRows,
                                            int numberOfColumns,
                                            float minX,
                                            float maxX,
                                            float minY,
                                            float maxY,
                                            float borderWidth,
                                            int predictionWindows,
                                            long predictionHorizon,
                                            TreeMap<Double, Map<GridCell, double[]>> gridPredictions,
                                            TreeMap<Double, Map<AmodeusUtil.BorderOrientation, double[]>> borderPredictions) {
        this.predictionWindows = predictionWindows;
        this.predictionHorizon = predictionHorizon;
        this.gridPredictions = gridPredictions;
        this.borderPredictions = borderPredictions;
        setGridCells(numberOfRows, numberOfColumns, minX, maxX, minY, maxY, borderWidth);

    }
    private void setGridCells(GridCell[] gridCells){
        this.gridCells = gridCells;
    }

    private void setGridCells(int numberOfRows,
                              int numberOfColumns,
                              float minX,
                              float maxX,
                              float minY,
                              float maxY,
                              float borderWidth){
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.gridCells = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);
        GeometryFactory geometryFactory = new GeometryFactory();
        this.borderPolygons = new HashMap<>();
        for (AmodeusUtil.BorderOrientation orientation : AmodeusUtil.BorderOrientation.values()) {
            borderPolygons.put(
                    orientation,
                    AmodeusUtil.getBorderPolygon(orientation, borderWidth, minX, maxX,
                            minY, maxY, geometryFactory)
            );
        }
    }

    private void setPredictionWindows(int predictionWindows) {
        this.predictionWindows = predictionWindows;
    }

    private void setPredictionHorizon(double predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
    }

    public LocalDateTime getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(LocalDateTime timestampStart) {
        this.timestampStart = timestampStart;
    }

    public LocalDateTime getTimestampStop() {
        return timestampStop;
    }

    public void setTimestampStop(LocalDateTime timestampStop) {
        this.timestampStop = timestampStop;
    }

    public int getPredictionWindows() {
        return predictionWindows;
    }

    public double getPredictionHorizon() {
        return predictionHorizon;
    }

    public GridCell[] getGridCells() {
        return gridCells;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public Map<AmodeusUtil.BorderOrientation, Polygon> getBorderPolygons() {
        return borderPolygons;
    }

    @Override
    public double[] getDemandPredictionForCell(double time, GridCell cell){
        double[] predictions;
        if(!this.gridPredictions.containsKey(time)){
            logger.debug("There is no entry in demand Predictions for the requested timeStep. " +
                    "The floor entry is used instead. In some cases this may not what you want.");
        }
        predictions = this.gridPredictions.floorEntry(time).getValue().get(cell);
        return predictions;
    }

    public ArrayList<double[]> getDemandPredictionsForAllCells(double time){
        ArrayList<double[]> predictions = new ArrayList<>();
        if(!this.gridPredictions.containsKey(time)){
            logger.debug("There is no entry in demand Predictions for the requested timeStep. " +
                    "The floor entry is used instead. In some cases this may not what you want.");
        }
        for(int i = 0;i  < this.gridCells.length ; i++){
            predictions.add(i,getDemandPredictionForCell(time, this.gridCells[i]));
        }
        return predictions;
    }

    public double[] getDemandPredictionsForThisTimeStepForAllCells(double time){
        double[] predictions = new double[this.gridCells.length];
        for(int i = 0;i  < this.gridCells.length ; i++){
           predictions[i] = getDemandPredictionForCell(time, this.gridCells[i])[0];
        }
        return predictions;
    }

    public double[] getDemandPredictionForBorderCell(double time, AmodeusUtil.BorderOrientation orientation){
        double[] predictions;
        if(!this.gridPredictions.containsKey(time)){
            logger.debug("There is no entry in demand Predictions for the requested timeStep. " +
                    "The floor entry is used instead. In some cases this may not what you want.");
        }
        predictions = this.borderPredictions.floorEntry(time).getValue().get(orientation);
        return predictions;
    }

    @Override
    public GridCell getCellWithHighestDemand(double time) {
        if(!this.gridPredictions.containsKey(time)){
            logger.debug("There is no entry in demand Predictions for the requested timeStep. " +
                    "The floor entry is used instead. In some cases this may not what you want.");
        }
        GridCell cell = Collections.max(this.gridPredictions.floorEntry(time).getValue().entrySet(), Comparator.comparingDouble(entry -> entry.getValue()[0])).getKey();
        return cell;
    }

    public Map<GridCell, double[]> putGridPredictions(Double timestamp, Map<GridCell, double[]> gridPredictions){
        return this.gridPredictions.put(timestamp, gridPredictions);
    }

    public Map<AmodeusUtil.BorderOrientation, double[]> putBorderPredictions(Double timestamp, Map<AmodeusUtil.BorderOrientation, double[]> borderPredictions){
        return this.borderPredictions.put(timestamp, borderPredictions);
    }

    public static UniformGridWithBorderPredictions fromXML(final String filename){
        UniformGridWithBorderPredictions uniformGridWithBorderPredictions = new UniformGridWithBorderPredictions();
        new UniformGridWithBorderPredictionsXMLReader(uniformGridWithBorderPredictions).readFile(filename);
        return uniformGridWithBorderPredictions;
    }



    private static class UniformGridWithBorderPredictionsXMLReader extends MatsimXmlParser{
        private static final String ELEMENT_ROOT = "uniform_grid_with_border_predictions";
        private static final String ATTRIBUTE_START_DATE = "start_date";
        private static final String ATTRIBUTE_STOP_DATE = "stop_date";
        private static final String ATTRIBUTE_N_GRID_ROWS = "n_grid_rows";
        private static final String ATTRIBUTE_N_GRID_COLUMNS = "n_grid_columns";
        private static final String ATTRIBUTE_BORDER_WIDTH = "borderWidth";
        private static final String ATTRIBUTE_X_MIN = "x_min";
        private static final String ATTRIBUTE_X_MAX = "x_max";
        private static final String ATTRIBUTE_Y_MIN = "y_min";
        private static final String ATTRIBUTE_Y_MAX = "y_max";
        private static final String ATTRIBUTE_PREDICTION_WINDOWS = "prediction_windows";
        private static final String ATTRIBUTE_PREDICTION_HORIZON = "prediction_horizon";

        private static final String ELEMENT_PREDICTION = "prediction";
        private static final String ATTRIBUTE_TIME = "time";

        private static final String ELEMENT_GRID_CELLS = "grid_cells";
        private static final String ELEMENT_BORDER_CELLS = "border_cells";
        private static final String ELEMENT_CELL = "cell";
        private static final String ATTRIBUTE_CELL_ID = "id";

        private static final String ELEMENT_Y_HAT = "y_hat";
        private static final String ATTRIBUTE_T = "t";
        private static final String ATTRIBUTE_VALUE = "value";

        private UniformGridWithBorderPredictions uniformGridWithBorderPredictions;

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


        private Double actualTimeStep;
        private Map<GridCell, double[]> actualGridPredictions;
        private Map<AmodeusUtil.BorderOrientation, double[]> actualBorderPredictions;
        private GridCell actualCell;
        private double[] actualYhats;
        private AmodeusUtil.BorderOrientation actualBorderOrientation;

        private UniformGridWithBorderPredictionsXMLReader(UniformGridWithBorderPredictions uniformGridWithBorderPredictions) {
            this.uniformGridWithBorderPredictions = uniformGridWithBorderPredictions;
        }

        @Override
        public void startTag(String name, Attributes atts, Stack<String> context) {
            switch (name){
                case ELEMENT_ROOT:
                    this.uniformGridWithBorderPredictions.setGridCells(
                            Integer.parseInt(atts.getValue(ATTRIBUTE_N_GRID_ROWS)),
                            Integer.parseInt(atts.getValue(ATTRIBUTE_N_GRID_COLUMNS)),
                            Float.parseFloat(atts.getValue(ATTRIBUTE_X_MIN)),
                            Float.parseFloat(atts.getValue(ATTRIBUTE_X_MAX)),
                            Float.parseFloat(atts.getValue(ATTRIBUTE_Y_MIN)),
                            Float.parseFloat(atts.getValue(ATTRIBUTE_Y_MAX)),
                            Float.parseFloat(atts.getValue(ATTRIBUTE_BORDER_WIDTH)));
                    this.uniformGridWithBorderPredictions.setPredictionWindows(Integer.parseInt(atts.getValue(ATTRIBUTE_PREDICTION_WINDOWS)));
                    this.uniformGridWithBorderPredictions.setPredictionHorizon(Double.parseDouble(atts.getValue(ATTRIBUTE_PREDICTION_HORIZON)));
                    this.uniformGridWithBorderPredictions.setTimestampStart(LocalDateTime.parse(atts.getValue(ATTRIBUTE_START_DATE), formatter));
                    this.uniformGridWithBorderPredictions.setTimestampStop(LocalDateTime.parse(atts.getValue(ATTRIBUTE_STOP_DATE), formatter));
                    this.uniformGridWithBorderPredictions.gridPredictions = new TreeMap<>();
                    this.uniformGridWithBorderPredictions.borderPredictions = new TreeMap<>();
                    break;
                case ELEMENT_PREDICTION:
                    actualTimeStep = Double.parseDouble(atts.getValue(ATTRIBUTE_TIME));
                    break;
                case ELEMENT_GRID_CELLS:
                    actualGridPredictions = new HashMap<>();
                    break;
                case ELEMENT_BORDER_CELLS:
                    actualBorderPredictions = new HashMap<>();
                    break;
                case  ELEMENT_CELL:
                    if(context.lastElement().equals(ELEMENT_GRID_CELLS)){
                        Optional<GridCell> optionalVariable = Arrays.stream(this.uniformGridWithBorderPredictions.getGridCells()).filter(gridCell -> gridCell.getId().equals(atts.getValue(ATTRIBUTE_CELL_ID))).findFirst();
                        actualCell = optionalVariable.orElseThrow(() -> new IllegalStateException("Could not find matching cell"));
                    }
                    if(context.lastElement().equals(ELEMENT_BORDER_CELLS)){
                        actualBorderOrientation = borderOrientationFromString(atts.getValue(ATTRIBUTE_CELL_ID));
                    }

                actualYhats = new double[this.uniformGridWithBorderPredictions.getPredictionWindows()];
                    break;
                case ELEMENT_Y_HAT:
                    actualYhats[Integer.parseInt(atts.getValue(ATTRIBUTE_T))] = Double.parseDouble(atts.getValue(ATTRIBUTE_VALUE));
                    break;
            }

        }

        @Override
        public void endTag(String name, String content, Stack<String> context) {
            switch (name){
                case ELEMENT_ROOT:
                    break;
                case ELEMENT_PREDICTION:
                    break;
                case ELEMENT_GRID_CELLS:
                    if(actualGridPredictions.keySet().size() != this.uniformGridWithBorderPredictions.getGridCells().length){
                        throw new IllegalStateException("Number of grid cells and number of predictions are not the same size");
                    }
                    uniformGridWithBorderPredictions.putGridPredictions(actualTimeStep, actualGridPredictions);
                    break;
                case ELEMENT_BORDER_CELLS:
                    if(actualBorderPredictions.keySet().size() != this.uniformGridWithBorderPredictions.getBorderPolygons().size()){
                        throw new IllegalStateException("Number of border cells and number of predictions are not the same size");
                    }
                    uniformGridWithBorderPredictions.putBorderPredictions(actualTimeStep, actualBorderPredictions);
                    break;
                case  ELEMENT_CELL:
                    if(context.get(context.size()-1) == ELEMENT_GRID_CELLS) {
                        actualGridPredictions.put(actualCell, actualYhats);
                    }
                    if(context.get(context.size()-1) == ELEMENT_BORDER_CELLS) {
                        actualBorderPredictions.put(actualBorderOrientation, actualYhats);
                    }
                case ELEMENT_Y_HAT:
                    break;
            }
        }

        private static AmodeusUtil.BorderOrientation borderOrientationFromString(String string){
            switch (string){
                case "north":
                    return AmodeusUtil.BorderOrientation.NORTH;
                case "east":
                    return AmodeusUtil.BorderOrientation.EAST;
                case "south":
                    return AmodeusUtil.BorderOrientation.SOUTH;
                case "west":
                    return AmodeusUtil.BorderOrientation.WEST;
                default:
                    throw new IllegalStateException(String.format("%s Unknown Border Direction", string));
            }
        }
    }
}


