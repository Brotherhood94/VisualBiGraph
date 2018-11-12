/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.layout.plugin.random.RandomLayout;

/**
 *
 * @author alessandro
 */
public class Layouts {
    
    private static void RandomLayout(GraphModel graphModel){
        RandomLayout rl = new RandomLayout(null, 500.0D);
        rl.setGraphModel(graphModel);
        rl.initAlgo();
        if(rl.canAlgo())
            rl.goAlgo();
        rl.endAlgo(); 
    }
    
    private static void ConvergedYifanHuLayout(GraphModel graphModel){
        YifanHuLayout yhl = new YifanHuLayout(null , new StepDisplacement(0f) );
        yhl.setGraphModel(graphModel);
        yhl.initAlgo();
        yhl.resetPropertiesValues();
        yhl.setOptimalDistance(100f);
        yhl.setRelativeStrength(0.2F);
        yhl.setInitialStep(20.0f);
        yhl.setStepRatio(0.95f);
        yhl.setAdaptiveCooling(Boolean.TRUE);
        yhl.setConvergenceThreshold(0.001f);
        yhl.setQuadTreeMaxLevel(10);
        yhl.setBarnesHutTheta(1.2f);
        if(yhl.canAlgo())
            while(!yhl.isConverged())
                yhl.goAlgo();
        yhl.endAlgo();
    }
    
    public static void Random_YifanHu(GraphModel graphModel){
        Layouts.RandomLayout(graphModel);
        Layouts.ConvergedYifanHuLayout(graphModel);
    }
}
