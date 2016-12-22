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
 * Configuration for searching bands in an image.
 */
public class CarSnapshotGraph extends Graph {

    private static double peakFootConstant =
            GlobalState.getInstance().getConfigurator().getDoubleProperty("carsnapshotgraph_peakfootconstant"); // 0.55
    private static double peakDiffMultiplicationConstant =
            GlobalState.getInstance().getConfigurator().getDoubleProperty("carsnapshotgraph_peakDiffMultiplicationConstant"); // 0.1
    private CarSnapshot handle;

    public CarSnapshotGraph(CarSnapshot handle) {
        this.handle = handle;
    }

    public Vector<Peak> findPeaks(int count) {
        Vector<Peak> outPeaks = new Vector<Peak>();
        for (int c = 0; c < count; c++) {
            float maxValue = 0.0f;
            int maxIndex = 0;
            for (int i = 0; i < this.yValues.size(); i++) { // left to right
                if (this.allowedInterval(outPeaks, i)) {
                    if (this.yValues.elementAt(i) >= maxValue) {
                        maxValue = this.yValues.elementAt(i);
                        maxIndex = i;
                    }
                }
            }
            // we found the biggest peak
            int leftIndex = this.indexOfLeftPeakRel(maxIndex, CarSnapshotGraph.peakFootConstant);
            int rightIndex = this.indexOfRightPeakRel(maxIndex, CarSnapshotGraph.peakFootConstant);
            int diff = rightIndex - leftIndex;
            leftIndex -= CarSnapshotGraph.peakDiffMultiplicationConstant * diff;
            rightIndex += CarSnapshotGraph.peakDiffMultiplicationConstant * diff;
            outPeaks.add(new Peak(Math.max(0, leftIndex), maxIndex, Math.min(this.yValues.size() - 1, rightIndex)));
        }
        Collections.sort(outPeaks, new PeakComparer(this.yValues));
        super.peaks = outPeaks;
        return outPeaks;
    }

    public class PeakComparer implements Comparator<Object> {

        private Vector<Float> yValues = null;

        public PeakComparer(Vector<Float> yValues) {
            this.yValues = yValues;
        }

        /**
         * @param peak the peak
         * @return its value according to intensity
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
