////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.swf.builder.types;

import flash.swf.SwfConstants;
import flash.swf.types.CurvedEdgeRecord;
import flash.swf.types.EdgeRecord;
import flash.swf.types.Shape;
import flash.swf.types.StraightEdgeRecord;
import flash.swf.types.StyleChangeRecord;
import flash.util.Trace;

import java.util.ArrayList;


/**
 * A utility class to help construct a SWF Shape from Java2D AWT Shapes. By default,
 * all co-ordinates are coverted to twips (1/20th of a pixel).
 *
 * @author Peter Farland
 */
public final class ShapeBuilder
{
    /**
     * Constructor. Creates an empty Flash Shape with the pen starting at [0.0,0.0]
     */
    public ShapeBuilder()
    {
        this(new Point());
    }

    /**
     * Constructor.
     * Use this constructor to specify whether co-ordinates will be converted
     * to twips (1/20th of a pixel). The default is to use this conversion.
     *
     * @param useTwips
     */
    public ShapeBuilder(boolean useTwips)
    {
        this();
        convertToTwips = useTwips;
    }

    /**
     * Constructor. Creates an empty Flash Shape. <code>ShapeRecord</code>s can be added
     * manually using the process shape method.
     *
     * @param origin the pen starting point, typically [0.0,0.0]
     * @see #processShape(ShapeIterator)
     */
    public ShapeBuilder(Point origin)
    {
        shape = new Shape();
        shape.shapeRecords = new ArrayList(); //shape record buffer

        if (origin == null)
            origin = new Point();

        this.start = new Point(origin.x, origin.y);
        this.lastMoveTo = new Point(origin.x, origin.y);
        this.pen = new Point(this.lastMoveTo.x, this.lastMoveTo.y);
    }

    public Shape build()
    {
        return shape;
    }

    /**
     * Processes a <code>Shape</code> by converting its general path to a series of
     * <code>ShapeRecord</code>s. The records are not terminated with an <code>EndShapeRecord</code>
     * so that subsequent calls can be made to this method to concatenate more shape paths.
     * <p/>
     * For closed shapes (including character glyphs) there exists a high possibility of rounding
     * errors caused by the conversion of double-precision pixel co-ordinates to integer twips
     * (1 twip = 1/20th pixel at 72 dpi). As such, at each move command this error is checked and
     * corrected.
     * <p/>
     *
     * @param si A ShapeIterator who's path will be converted to records and added to the collection
     */
    public void processShape(ShapeIterator si)
    {
        while (!si.isDone())
        {
            double[] coords = new double[6];
            short code = si.currentSegment(coords);

            switch (code)
            {
                case ShapeIterator.MOVE_TO:
                    correctRoundingErrors();
                    move(coords[0], coords[1]);
                    closed = false; //reset closed flag after move
                    break;

                case ShapeIterator.LINE_TO:
                    straight(coords[0], coords[1]);
                    break;

                case ShapeIterator.QUAD_TO:
                    curved(coords[0], coords[1], coords[2], coords[3]);
                    break;

                case ShapeIterator.CUBIC_TO:
                    approximateCubicBezier(new Point(pen.x, pen.y),
                            new Point(coords[0], coords[1]),
                            new Point(coords[2], coords[3]),
                            new Point(coords[4], coords[5]));
                    break;

                case ShapeIterator.CLOSE:
                    closed = true;
                    close();
                    break;
            }
            si.next();
        }

        correctRoundingErrors();
    }

    /**
     * If a shape is closed then the start and finish records must
     * match exactly to the nearest twip. This method attempts to close the
     * shape by adding a <code>StraightEdgeRecord</code> with a delta
     * equal to the accumulated rounding errors, if such errors exists.
     */
    public void correctRoundingErrors()
    {
        if ((dxSumTwips != 0 || dySumTwips != 0) && (closed || fillStyle0 > 0 || fillStyle1 > 0))
        {
            addLineSubdivideAware(-dxSumTwips, -dySumTwips);

            dxSumTwips = 0;
            dySumTwips = 0;
        }
    }

    /**
     * Moves the current pen position to a new location without drawing. In SWF, this requires a
     * style change record and a delta is calculated between the current position and the
     * new position.
     * <p/>
     * If fill or line style information has changed since the last move command, the new style
     * information is also included in the record.
     *
     * @param x
     * @param y
     */
    public void move(double x, double y)
    {
        double dx = x - start.x; //dx is delta from origin-x to final-x
        double dy = y - start.y; //dy is delta from origin-y to final-y

        if (convertToTwips)
        {
            dx *= SwfConstants.TWIPS_PER_PIXEL;
            dy *= SwfConstants.TWIPS_PER_PIXEL;
        }

        StyleChangeRecord scr = new StyleChangeRecord();
        scr.setMove((int)Math.rint(dx), (int)Math.rint(dy));

        //Reset rounding counters, as this info is only useful per sub-shape/fill closure
        dxSumTwips = 0;
        dySumTwips = 0;

        //Check styles
        if (lineStyleHasChanged)
        {
            scr.setLinestyle(lineStyle);
            lineStyleHasChanged = false;
        }

        if (fillStyle0HasChanged && useFillStyle0)
        {
            scr.setFillStyle0(fillStyle0);
            fillStyle0HasChanged = false;
        }

        if (fillStyle1HasChanged && useFillStyle1)
        {
            scr.setFillStyle1(fillStyle1);
            fillStyle1HasChanged = false;
        }

        lastMoveTo.x = x;
        lastMoveTo.y = y;
        pen.x = x;
        pen.y = y;

        shape.shapeRecords.add(scr);
    }

    /**
     * Calculates the change or 'delta' in position between the current pen location and a
     * given co-ordinate pair. This delta is used to create a straight-edge shape record in
     * SWF, i.e. a simple line.
     *
     * @param x
     * @param y
     */
    public void straight(double x, double y)
    {
        double dx = x - pen.x; //dx is delta from origin-x to final-x
        double dy = y - pen.y; //dy is delta from origin-y to final-y

        if (convertToTwips)
        {
            dx *= SwfConstants.TWIPS_PER_PIXEL;
            dy *= SwfConstants.TWIPS_PER_PIXEL;
        }

        if (dx == 0 && dy == 0)
        {
            return; //For now, we ignore zero length lines
        }
        else
        {
            int intdx = (int)Math.rint(dx);
            int intdy = (int)Math.rint(dy);

            addLineSubdivideAware(intdx, intdy);

            pen.x = x;
            pen.y = y;

            dxSumTwips += intdx;
            dySumTwips += intdy;
        }
    }


    /**
     * Creates a quadratic spline in SWF as a curved-edge record. The current pen position is
     * used as the first anchor point, and is used with the two other points supplied to calculate
     * a delta between the origin and the control point, and the control point and the final anchor point.
     *
     * @param cx - control point-x
     * @param cy - control point-y
     * @param ax - anchor point-x
     * @param ay - anchor point-y
     */
    public void curved(double cx, double cy, double ax, double ay)
    {
        double[] points = new double[]{pen.x, pen.y, cx, cy, ax, ay};

        int[] deltas = addCurveSubdivideAware(points);

        pen.x = ax;
        pen.y = ay;

        dxSumTwips += (deltas[2] + deltas[0]);
        dySumTwips += (deltas[3] + deltas[1]);

    }

    /**
     * Creates a straight-edge record (i.e a straight line) from the current drawing position to the last
     * move-to position. If the delta for the x and y co-ordinates is zero, a line is not necessary and
     * the method does nothing.
     */
    public void close()
    {

        double dx = lastMoveTo.x - pen.x; //dx is delta from lastMoveTo-x to pen-x
        double dy = lastMoveTo.y - pen.y; //dy is delta from lastMoveTo-y to pen-y

        if (convertToTwips)
        {
            dx *= SwfConstants.TWIPS_PER_PIXEL;
            dy *= SwfConstants.TWIPS_PER_PIXEL;
        }

        pen.x = lastMoveTo.x;
        pen.y = lastMoveTo.y;

        if (dx == 0 && dy == 0)
        {
            return; //No action required
        }
        else
        {
            int intdx = (int)Math.rint(dx);
            int intdy = (int)Math.rint(dy);

            addLineSubdivideAware(intdx, intdy);

            dxSumTwips += intdx;
            dySumTwips += intdy;
        }
    }

    private void addLineSubdivideAware(int x, int y)
    {
        int limit = EdgeRecord.MAX_DELTA_IN_TWIPS;

        if (Math.abs(x) > limit || Math.abs(y) > limit)
        {
            int midXLeft = (int)Math.rint(Math.floor(x / 2.0));
            int midYLeft = (int)Math.rint(Math.floor(y / 2.0));
            int midXRight = (int)Math.rint(Math.ceil(x / 2.0));
            int midYRight = (int)Math.rint(Math.ceil(y / 2.0));

            if (Math.abs(midXLeft) > limit || Math.abs(midYLeft) > limit)
            {
                addLineSubdivideAware(midXLeft, midYLeft);
            }
            else
            {
                shape.shapeRecords.add(new StraightEdgeRecord(midXLeft, midYLeft));
            }

            if (Math.abs(midXRight) > limit || Math.abs(midYRight) > limit)
            {
                addLineSubdivideAware(midXRight, midYRight);
            }
            else
            {
                shape.shapeRecords.add(new StraightEdgeRecord(midXRight, midYRight));
            }
        }
        else
        {
            shape.shapeRecords.add(new StraightEdgeRecord(x, y));
        }
    }

    /**
     * Recursively draws smaller sub-sections of a curve until the control-anchor point delta values
     * fit into a SWF EdgeRecord.
     *
     * @param curve An array of 6 values representing the origin x-y, control x-y, anchor x-y
     * @return int[] The four x-y delta values between the two anchor points and one control point.
     * @see EdgeRecord#MAX_DELTA_IN_TWIPS
     */
    private int[] addCurveSubdivideAware(double[] curve)
    {
        int[] delta = curveDeltas(curve);

        if (exceedsEdgeRecordLimit(delta))
        {
            double[] left = new double[6];
            double[] right = new double[6];

            divideQuad(curve, 0, left, 0, right, 0);

            int[] deltaLeft = curveDeltas(left);
            int[] deltaRight = curveDeltas(right);

            if (exceedsEdgeRecordLimit(deltaLeft))
            {
                addCurveSubdivideAware(left);
            }
            else
            {
                curveRecord(deltaLeft);
            }

            if (exceedsEdgeRecordLimit(deltaRight))
            {
                addCurveSubdivideAware(right);
            }
            else
            {
                curveRecord(deltaRight);
            }
        }
        else
        {
            curveRecord(delta);
        }

        return delta;
    }

    /**
     * From java.awt.geom.QuadCurve2D
     */
    public static void divideQuad(double src[], int srcoff, double left[], int loff, double right[], int roff)
    {
        double x1 = src[srcoff + 0];
        double y1 = src[srcoff + 1];
        double ctrlx = src[srcoff + 2];
        double ctrly = src[srcoff + 3];
        double x2 = src[srcoff + 4];
        double y2 = src[srcoff + 5];

        if (left != null)
        {
            left[loff + 0] = x1;
            left[loff + 1] = y1;
        }

        if (right != null)
        {
            right[roff + 4] = x2;
            right[roff + 5] = y2;
        }

        x1 = (x1 + ctrlx) / 2.0;
        y1 = (y1 + ctrly) / 2.0;
        x2 = (x2 + ctrlx) / 2.0;
        y2 = (y2 + ctrly) / 2.0;
        ctrlx = (x1 + x2) / 2.0;
        ctrly = (y1 + y2) / 2.0;

        if (left != null)
        {
            left[loff + 2] = x1;
            left[loff + 3] = y1;
            left[loff + 4] = ctrlx;
            left[loff + 5] = ctrly;
        }

        if (right != null)
        {
            right[roff + 0] = ctrlx;
            right[roff + 1] = ctrly;
            right[roff + 2] = x2;
            right[roff + 3] = y2;
        }
    }

    private void curveRecord(int[] delta)
    {
        CurvedEdgeRecord cer = new CurvedEdgeRecord();
        cer.controlDeltaX = delta[0];
        cer.controlDeltaY = delta[1];
        cer.anchorDeltaX = delta[2];
        cer.anchorDeltaY = delta[3];
        shape.shapeRecords.add(cer);
    }

    private int[] curveDeltas(double[] curve)
    {
        int[] deltas = new int[4];

        double dcx = curve[2] - curve[0]; //dcx is delta from origin-x to control point-x
        double dcy = curve[3] - curve[1]; //dcy is delta from origin-y to control point-y
        double dax = curve[4] - curve[2]; //dax is delta from control point-x to anchor point-x
        double day = curve[5] - curve[3]; //day is delta from control point-y to anchor point-y

        if (convertToTwips)
        {
            dcx *= SwfConstants.TWIPS_PER_PIXEL;
            dcy *= SwfConstants.TWIPS_PER_PIXEL;
            dax *= SwfConstants.TWIPS_PER_PIXEL;
            day *= SwfConstants.TWIPS_PER_PIXEL;
        }

        deltas[0] = (int)Math.rint(dcx);
        deltas[1] = (int)Math.rint(dcy);
        deltas[2] = (int)Math.rint(dax);
        deltas[3] = (int)Math.rint(day);

        return deltas;
    }

    private boolean exceedsEdgeRecordLimit(int[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (Math.abs(values[i]) > EdgeRecord.MAX_DELTA_IN_TWIPS)
                return true;
        }

        return false;
    }

    /**
     * Gets the current line style index.
     * Note that a value of zero represents the empty stroke.
     *
     * @return index to the current line style in the <code>LineStyleArray</code>
     */
    public int getCurrentLineStyle()
    {
        return lineStyle;
    }

    /**
     * Sets the current line style index.
     * Note that a value of zero represents the empty stroke.
     *
     * @param index index to a line style in the <code>LineStyleArray</code>
     */
    public void setCurrentLineStyle(int index)
    {
        if (index != lineStyle)
        {
            lineStyleHasChanged = true;
            lineStyle = index;
        }
    }

    /**
     * Gets the current fill style index.
     * Note that a value of zero represents a blank fill.
     *
     * @return index to the current fill style in the <code>FillStyleArray</code>
     */
    public int getCurrentFillStyle0()
    {
        return fillStyle0;
    }

    /**
     * Sets the current fill style index.
     * Note that a value of zero represents a blank fill.
     *
     * @param index
     */
    public void setCurrentFillStyle0(int index)
    {
        if (index != fillStyle0)
        {
            fillStyle0HasChanged = true;
            fillStyle0 = index;
        }
    }

    /**
     * Gets the current fill style 1 index. A fill style 1 record represents the
     * fill of intersecting shape areas.
     * Note that a value of zero represents a blank fill.
     *
     * @return index to the current fill style in the <code>FillStyleArray</code>
     */
    public int getCurrentFillStyle1()
    {
        return fillStyle1;
    }

    /**
     * Sets the current fill style 1 index. A fill style 1 record represents the
     * fill of intersecting shape areas.
     * Note that a value of zero represents a blank fill.
     *
     * @param index
     */
    public void setCurrentFillStyle1(int index)
    {
        if (index != fillStyle1)
        {
            fillStyle1HasChanged = true;
            fillStyle1 = index;
        }
    }


    /**
     * Gets whether the current paint method should include fill style 1 information, which controls
     * how intersecting shape fills are drawn.
     *
     * @return
     */
    public boolean getUseFillStyle1()
    {
        return useFillStyle1;
    }

    /**
     * Sets the paint method to include fill style 1 information, which controls
     * how intersecting shape fills are drawn.
     *
     * @param b if set to true, fill style 1 information will be used for intersecting shapes
     */
    public void setUseFillStyle1(boolean b)
    {
        useFillStyle1 = b;
    }

    /**
     * Sets the paint method to include fill style 0 information, which
     * is for filling simple shapes.
     *
     * @param b if set to true, fill style 0 information will be used for normal shapes
     */
    public void setUseFillStyle0(boolean b)
    {
        useFillStyle0 = b;
    }

    /**
     * Return a point on a segment [P0, P1] which distance from P0
     * is ratio of the length [P0, P1]
     */
    public static Point getPointOnSegment(Point P0, Point P1, double ratio)
    {
        return new Point((P0.x + ((P1.x - P0.x) * ratio)), (P0.y + ((P1.y - P0.y) * ratio)));
    }

    /**
     * Based on Timothee Groleau's public ActionScript library (which is based on
     * Helen Triolo's approach) using Casteljau's approximation for drawing 3rd-order Cubic
     * curves as a collection of 2nd-order Quadratic curves - with a fixed level of accuracy
     * using just 4 quadratic curves.
     * <p/>
     * The reason this fixed-level approach was chosen is because it is very fast and should
     * provide us with a reasonable approximation for small curves involved in fonts.
     * <p/>
     * &quot;This function will trace a cubic approximation of the cubic Bezier
     * It will calculate a series of [control point/Destination point] which
     * will be used to draw quadratic Bezier starting from P0&quot;
     * <p/>
     *
     * @link http://timotheegroleau.com/Flash/articles/cubic_bezier_in_flash.htm
     */
    private void approximateCubicBezier(final Point P0, final Point P1, final Point P2, final Point P3)
    {
        // calculates the useful base points
        Point PA = getPointOnSegment(P0, P1, 3.0 / 4.0);
        Point PB = getPointOnSegment(P3, P2, 3.0 / 4.0);

        // get 1/16 of the [P3, P0] segment
        double dx = (P3.x - P0.x) / 16.0;
        double dy = (P3.y - P0.y) / 16.0;

        // calculate control point 1
        Point c1 = getPointOnSegment(P0, P1, 3.0 / 8.0);

        // calculate control point 2
        Point c2 = getPointOnSegment(PA, PB, 3.0 / 8.0);
        c2.x = c2.x - dx;
        c2.y = c2.y - dy;

        // calculate control point 3
        Point c3 = getPointOnSegment(PB, PA, 3.0 / 8.0);
        c3.x = c3.x + dx;
        c3.y = c3.y + dy;

        // calculate control point 4
        Point c4 = getPointOnSegment(P3, P2, 3.0 / 8.0);

        // calculate the 3 anchor points (as midpoints of the control segments)
        Point a1 = new Point(((c1.x + c2.x) / 2.0), ((c1.y + c2.y) / 2.0));
        Point a2 = new Point(((PA.x + PB.x) / 2.0), ((PA.y + PB.y) / 2.0));
        Point a3 = new Point(((c3.x + c4.x) / 2.0), ((c3.y + c4.y) / 2.0));

        // draw the four quadratic sub-segments
        curved(c1.x, c1.y, a1.x, a1.y);
        curved(c2.x, c2.y, a2.x, a2.y);
        curved(c3.x, c3.y, a3.x, a3.y);
        curved(c4.x, c4.y, P3.x, P3.y);

        if (Trace.font_cubic)
        {
            Trace.trace("Cubic Curve\n");
            Trace.trace("P0:\t" + P0.x + "\t" + P0.y);
            Trace.trace("c1:\t" + c1.x + "\t" + c1.y + "\t\tP1:\t" + P1.x + "\t" + P1.y);
            Trace.trace("a1:\t" + a1.x + "\t" + a1.y);
            Trace.trace("c2:\t" + c2.x + "\t" + c2.y);
            Trace.trace("a2:\t" + a2.x + "\t" + a2.y);
            Trace.trace("c3:\t" + c3.x + "\t" + c3.y);
            Trace.trace("a3:\t" + a3.x + "\t" + a3.y);
            Trace.trace("c4:\t" + c4.x + "\t" + c4.y + "\t\tP2:\t" + P2.x + "\t" + P2.y);
            Trace.trace("P3:\t" + P3.x + "\t" + P3.y);
        }
    }

    private boolean convertToTwips = true;

    private Shape shape;

    private Point pen;
    private Point lastMoveTo;
    private Point start;

    private int dxSumTwips = 0;
    private int dySumTwips = 0;

    private int lineStyle = -1;
    private int fillStyle0 = -1;
    private int fillStyle1 = -1;
    private boolean useFillStyle1 = true;
    private boolean useFillStyle0 = true;
    private boolean lineStyleHasChanged;
    private boolean fillStyle1HasChanged;
    private boolean fillStyle0HasChanged;

    private boolean closed;

}
