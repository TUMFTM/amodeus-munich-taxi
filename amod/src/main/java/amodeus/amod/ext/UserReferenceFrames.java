/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod.ext;

import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Unit;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public enum UserReferenceFrames implements ReferenceFrame {
    SANFRANCISCO( //
            "EPSG:26743", //
            Unit.of("ft")), //
    BERLIN( //
            "EPSG:31468", SI.METER), //
    SANTIAGO_DE_CHILE( //
            "EPSG:32719", SI.METER), //
    AUCKLAND( //
            "EPSG:2193",SI.METER), //
    TEL_AVIV( //
            "EPSG:2039",SI.METER), //
    CHICAGO( //
            "EPSG:3435", SI.METER),
    MUNICH(
            "EPSG:25832", SI.METER);
    ;
    // ---
    private final CoordinateTransformation coords_toWGS84;
    private final CoordinateTransformation coords_fromWGS84;
    private final MathTransform geom_toWGS84;
    private final MathTransform geom_fromWGS84;
    private final Unit unit;

    UserReferenceFrames(String referenceCoordinateSystem, Unit unit) {
        this.coords_toWGS84 = new GeotoolsTransformation(referenceCoordinateSystem, "WGS84");
        this.coords_fromWGS84 = new GeotoolsTransformation("WGS84", referenceCoordinateSystem);
        CoordinateReferenceSystem referenceCRS = MGC.getCRS(referenceCoordinateSystem);
        CoordinateReferenceSystem WGS84CRS = MGC.getCRS("WGS84");
        try {
            this.geom_toWGS84 = CRS.findMathTransform(referenceCRS, WGS84CRS,true);
            this.geom_fromWGS84 = CRS.findMathTransform(WGS84CRS, referenceCRS, true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
        this.unit = unit;

    }


    @Override
    public CoordinateTransformation coords_fromWGS84() {
        return coords_fromWGS84;
    }

    @Override
    public CoordinateTransformation coords_toWGS84() {
        return coords_toWGS84;
    }

    public MathTransform geom_toWGS84() {
        return geom_toWGS84;
    }

    public MathTransform geom_fromWGS84() {
        return geom_fromWGS84;
    }

    @Override
    public Unit unit() {
        return unit;
    }
}
