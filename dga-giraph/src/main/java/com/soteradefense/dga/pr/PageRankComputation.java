package com.soteradefense.dga.pr;

import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

import java.io.IOException;

public class PageRankComputation extends BasicComputation<Text, DoubleWritable, Text, DoubleWritable> {

    public static final String MAX_EPSILON = "com.soteradefense.dga.max.epsilon";
    public static final String DAMPING_FACTOR = "damping.factor";
    public static final float DAMPING_FACTOR_DEFAULT_VALUE = 0.85f;


    @Override
    public void compute(Vertex<Text, DoubleWritable, Text> vertex, Iterable<DoubleWritable> messages) throws IOException {

        float dampingFactor = this.getConf().getFloat(DAMPING_FACTOR, DAMPING_FACTOR_DEFAULT_VALUE);

        long step = getSuperstep();

        if (step == 0) {
            //set initial value
            vertex.setValue(new DoubleWritable(1.0 / getTotalNumVertices()));
        } else { // go until no one votes to continue

            double rank = 0;
            for (DoubleWritable partial : messages) {
                rank += partial.get();
            }
            rank = ((1 - dampingFactor) / getTotalNumVertices()) + (dampingFactor * rank);
            double vertexValue = vertex.getValue().get();
            double delta = Math.abs(rank - vertexValue) / vertexValue;
            aggregate(MAX_EPSILON, new DoubleWritable(delta));
            vertex.setValue(new DoubleWritable(rank));
        }
        distributeRank(vertex);
    }

    private void distributeRank(Vertex<Text, DoubleWritable, Text> vertex) {
        double rank = vertex.getValue().get() / vertex.getNumEdges();
        sendMessageToAllEdges(vertex, new DoubleWritable(rank));
    }

}
