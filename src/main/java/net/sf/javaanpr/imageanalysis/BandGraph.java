/*
 * Copyright 2013 JavaANPR contributors
 * Copyright 2006 Ondrej Martinsky
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package net.sf.javaanpr.imageanalysis;

import net.sf.javaanpr.configurator.Configurator;
import net.sf.javaanpr.configurator.GlobalState;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Processing of the horizontal projection of the detected region of the plate.
 * <p/>
 * The {@code peak} and {@code peakfoot} are detected on the X axis and multiplied with the {@code
 * peakDiffMultiplicationConstant}
 */
public class BandGraph extends Graph {

    private static double peakFootConstant =
            GlobalState.getInstance().getConfigurator().getDoubleProperty("bandgraph_peakfootconstant"); // 0.75
    private static double peakDiffMultiplicationConstant =
            GlobalState.getInstance().getConfigurator().getDoubleProperty("bandgraph_peakDiffMultiplicationConstant"); // 0.2

    /**
     * The Band to which this Graph is related.
     */
    private Band handle;

    public BandGraph(Band handle) {
        this.handle = handle;
    }

    public Vector<Peak> findPeaks(int count) {
        Vector<Graph.Peak> outPeaks = new Vector<Graph.Peak>();
        for (int c = 0; c < count; c++) {
            float maxValue = 0.0f;
            int maxIndex = 0;
            for (int i = 0; i < this.yValues.size(); i++) { // left to right
                if (allowedInterval(outPeaks, i)) {
                    if (this.yValues.elementAt(i) >= maxValue) {
                        maxValue = this.yValues.elementAt(i);
                        maxIndex = i;
                    }
                }
            }
            // we found the biggest peak, let's do the first cut
            int leftIndex = this.indexOfLeftPeakRel(maxIndex, BandGraph.peakFootConstant);
            int rightIndex = this.indexOfRightPeakRel(maxIndex, BandGraph.peakFootConstant);
            int diff = rightIndex - leftIndex;
            leftIndex -= BandGraph.peakDiffMultiplicationConstant * diff;
            rightIndex += BandGraph.peakDiffMultiplicationConstant * diff;
            outPeaks.add(new Peak(Math.max(0, leftIndex), maxIndex, Math.min(this.yValues.size() - 1, rightIndex)));
        }
        // filter the candidates that don't correspond with plate proportions
        Vector<Peak> outPeaksFiltered = new Vector<Peak>();
        for (Peak p : outPeaks) {
            if ((p.getDiff() > (2 * this.handle.getHeight())) // plate too thin
                    && (p.getDiff() < (15 * this.handle.getHeight()))) { // plate too wide
                outPeaksFiltered.add(p);
            }
        }
        Collections.sort(outPeaksFiltered, new PeakComparer(this.yValues));
        super.peaks = outPeaksFiltered;
        return outPeaksFiltered;
    }

    public int indexOfLeftPeakAbs(int peak, double peakFootConstantAbs) {
        int index = peak;
        for (int i = peak; i >= 0; i--) {
            index = i;
            if (this.yValues.elementAt(index) < peakFootConstantAbs) {
                break;
            }
        }
        return Math.max(0, index);
    }

    public int indexOfRightPeakAbs(int peak, double peakFootConstantAbs) {
        int index = peak;
        for (int i = peak; i < this.yValues.size(); i++) {
            index = i;
            if (this.yValues.elementAt(index) < peakFootConstantAbs) {
                break;
            }
        }
        return Math.min(this.yValues.size(), index);
    }

    public class PeakComparer implements Comparator<Object> {

        private Vector<Float> yValues = null;

        public PeakComparer(Vector<Float> yValues) {
            this.yValues = yValues;
        }

        /**
         * @param peak the peak
         * @return the peak size
         */
        private float getPeakValue(Object peak) {
            return this.yValues.elementAt(((Peak) peak).getCenter());
        }

        @Override
        public int compare(Object peak1, Object peak2) {
            double comparison = this.getPeakValue(peak2) - this.getPeakValue(peak1);
            if (comparison < 0) {
                return -1;
            }
            if (comparison > 0) {
                return 1;
            }
            return 0;
        }
    }
}
