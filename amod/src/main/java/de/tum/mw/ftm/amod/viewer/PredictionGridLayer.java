package de.tum.mw.ftm.amod.viewer;

import amodeus.amod.ext.UserReferenceFrames;
import amodeus.amodeus.gfx.*;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationObject;
import amodeus.amodeus.util.gui.RowPanel;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.operation.TransformException;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Objects;

public class PredictionGridLayer extends ViewerLayer {
    private static final Scalar _224 = RealScalar.of(224);
    public static final Color COLOR = new Color(128, 153 / 2, 0, 128);
    // ---
    private UniformGridWithBorderPredictions uniformGridWithBorderPredictions;

    public boolean drawPredictionGrid;


    public PredictionGridLayer(AmodeusComponent amodeusComponent, UniformGridWithBorderPredictions uniformGridWithBorderPredictions) {
        super(amodeusComponent);
        this.uniformGridWithBorderPredictions = uniformGridWithBorderPredictions;
    }

    public void setUniformGridWithBorderPredictions(UniformGridWithBorderPredictions uniformGridWithBorderPredictions) {
        this.uniformGridWithBorderPredictions = uniformGridWithBorderPredictions;
    }


    private static Color withAlpha(Color myColor, int alpha) {
        return new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), alpha);
    }


    @Override
    protected void paint(Graphics2D graphics, SimulationObject ref)  {
        final MatsimAmodeusDatabase db = amodeusComponent.getDb();
        if (Objects.isNull(uniformGridWithBorderPredictions))
            return;
        if(drawPredictionGrid) {
            final Tensor center = Tensors.vectorInt( //
                    amodeusComponent.getWidth() / 2, //
                    amodeusComponent.getHeight());

            graphics.setColor(new Color(155, 155, 155, 25));

            for (GridCell gridCell : uniformGridWithBorderPredictions.getGridCells()) {
                try {
                    Shape shape = createShape(JTS.transform(JTS.toGeometry(gridCell), ((UserReferenceFrames) db.referenceFrame).geom_toWGS84()));
                    graphics.fill(shape);
                    graphics.draw(shape);
                }
                catch (TransformException e){
                    //Do nothing here
                }

            }
            for (org.locationtech.jts.geom.Polygon borderPolygon : uniformGridWithBorderPredictions.getBorderPolygons().values()) {
                try{
                    Shape shape = createShape(JTS.transform(borderPolygon, ((UserReferenceFrames) db.referenceFrame).geom_toWGS84()));
                    graphics.fill(shape);
                    graphics.draw(shape);
                }
                catch (TransformException e){
                    //Do nothing here
                }
            }
        }
    }

    private void setDrawPredictionGrid(boolean selected) {
        drawPredictionGrid = selected;
        amodeusComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("predictionGrid");
            jCheckBox.setToolTipText("Demand prediction grid");
            jCheckBox.setSelected(drawPredictionGrid);
            jCheckBox.addActionListener(e -> setDrawPredictionGrid(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

    private static Color halfAlpha(Color color) {
        int rgb = color.getRGB() & 0xffffff;
        int alpha = color.getAlpha() / 2;
        return new Color(rgb | (alpha << 24), true);
    }

    private Point transformToMapCoordiantes(Coordinate coordinate){
        return amodeusComponent.getMapPosition(coordinate.y, coordinate.x);
    }

    private Shape createShape(Geometry geometry) {
       Path2D path2d = new Path2D.Double();
        boolean init = false;
        for (Coordinate coordinate: geometry.getCoordinates()) {
            Point point = transformToMapCoordiantes(coordinate);
            if (!init) {
                init = true;
                path2d.moveTo(point.getX(), point.getY());
            } else
                path2d.lineTo(point.getX(), point.getY());
        }
        path2d.closePath();
        return path2d;
    }

}
