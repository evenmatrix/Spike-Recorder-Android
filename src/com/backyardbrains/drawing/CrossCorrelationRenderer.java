package com.backyardbrains.drawing;

import android.content.Intent;
import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.view.ofRectangle;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;

public class CrossCorrelationRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = "CrossCorrelationRenderer";

    private boolean bDrawThumbs = true;

    // ----------------------------------------------------------------------------------------
    public CrossCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    public void setDrawThumbs(boolean b) {
        if (bDrawThumbs != b && getContext() != null) {
            Intent i = new Intent();
            i.setAction("BYBRenderAnalysis");
            i.putExtra("requestRender", true);
            getContext().sendBroadcast(i);
            bDrawThumbs = b;
        }
    }

    @Override protected void thumbRectClicked(int i) {
        if (bDrawThumbs) {
            bDrawThumbs = false;
            super.thumbRectClicked(i);
        }
    }

    ;

    public boolean isDrawThumbs() {
        return bDrawThumbs;
    }

    // ----------------------------------------------------------------------------------------
    @Override protected void drawingHandler(GL10 gl) {
        initGL(gl);
        int margin = 20;
        int maxSpikeTrains = 3;
        if (getAnalysisManager() != null) {
            ArrayList<ArrayList<Integer>> CC = getAnalysisManager().getCrossCorrelation();
            if (CC != null) {
                if (bDrawThumbs) {

                    float d = (Math.min(width, height) / (float) (maxSpikeTrains + 1)) * 0.2f;
                    if (d < margin) {
                        margin = (int) d;
                    }
                    float w = (width - margin * (maxSpikeTrains + 1)) / (float) maxSpikeTrains;
                    float h = (height - (margin * 1.5f) * (maxSpikeTrains + 1)) / (float) maxSpikeTrains;

                    thumbRects = new ofRectangle[maxSpikeTrains * maxSpikeTrains];

                    for (int i = 0; i < maxSpikeTrains; i++) {
                        for (int j = 0; j < maxSpikeTrains; j++) {
                            thumbRects[i * maxSpikeTrains + j] =
                                new ofRectangle(j * (w + margin) + margin, (h + (margin * 1.5f)) * i + (margin * 1.5f),
                                    w, h);
                        }
                    }

                    for (int i = 0; i < CC.size(); i++) {
                        graphIntegerList(gl, CC.get(i), thumbRects[i], BYBColors.getColorAsGlById(i), true);
                    }
                } else {
                    int s = selected;
                    if (selected < 0 || selected >= maxSpikeTrains * maxSpikeTrains || selected >= CC.size()) {
                        s = 0;
                    }
                    mainRect = new ofRectangle(margin, margin, width - 2 * margin, height - 2 * margin);
                    graphIntegerList(gl, CC.get(s), mainRect, BYBColors.getColorAsGlById(s), true);
                }
            }
        }
    }
}
