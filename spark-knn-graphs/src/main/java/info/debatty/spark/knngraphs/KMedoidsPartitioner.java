/*
 * The MIT License
 *
 * Copyright 2017 tibo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.debatty.spark.knngraphs;

import info.debatty.java.graphs.NeighborList;
import info.debatty.java.graphs.Node;
import info.debatty.java.graphs.SimilarityInterface;
import java.util.ArrayList;
import onlineknn.spark.kmedoids.Clusterer;
import onlineknn.spark.kmedoids.Similarity;
import onlineknn.spark.kmedoids.Solution;
import onlineknn.spark.kmedoids.neighborgenerator.WindowNeighborGenerator;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

/**
 *
 * @author tibo
 * @param <T>
 */
public class KMedoidsPartitioner<T> implements Partitioner<T> {

    private final SimilarityInterface<T> similarity;
    private final int partitions;
    private Budget budget;

    /**
     *
     * @param similarity
     * @param partitions
     */
    public KMedoidsPartitioner(
            final SimilarityInterface<T> similarity, final int partitions) {
        this.similarity = similarity;
        this.partitions = partitions;
    }

    /**
     *
     * @param graph
     * @return
     */
    public final Partitioning<T> partition(
            final JavaPairRDD<Node<T>, NeighborList> graph) {

        if (budget == null) {
            throw new IllegalStateException("Budget is undefined!");
        }

        Partitioning<T> solution = new Partitioning<T>();

        Clusterer<Node<T>> clusterer = new Clusterer<Node<T>>();
        clusterer.setK(partitions);
        clusterer.setSimilarity(new ClusteringSimilarityAdapter<T>(similarity));
        clusterer.setNeighborGenerator(new WindowNeighborGenerator<Node<T>>());
        clusterer.setBudget(BudgetAdapter.get(budget));
        Solution<Node<T>> medoids = clusterer.cluster(graph.keys());

        // Assign each node to the most similar medoid
        solution.graph =
                graph.mapToPair(new AssignToMedoidFunction<T>(
                        medoids.medoids,
                        similarity));
        solution.graph.count();
        solution.end_time = System.currentTimeMillis();
        return solution;
    }

    public void setBudget(Budget budget) {
        this.budget = budget;
    }
}

class BudgetAdapter {
    public static onlineknn.spark.kmedoids.Budget get(final Budget budget) {
        if (budget.getClass() == TimeBudget.class) {
            return new onlineknn.spark.kmedoids.Budget() {
                public boolean isExhausted(Solution solution) {
                    return (System.currentTimeMillis() - solution.start_time) / 1000 >=
                            ((TimeBudget) budget).getValue();
                }
            };
        }

        throw new IllegalArgumentException("Unsupported budget type");
    }
}

/**
 *
 * @author tibo
 * @param <T>
 */
class AssignToMedoidFunction<T>
        implements PairFunction<
            Tuple2<Node<T>, NeighborList>,
            Node<T>,
            NeighborList> {

    private final ArrayList<Node<T>> medoids;
    private final SimilarityInterface<T> similarity;

    AssignToMedoidFunction(
            final ArrayList<Node<T>> medoids,
            final SimilarityInterface<T> similarity) {
        this.medoids = medoids;
        this.similarity = similarity;
    }



    public Tuple2<Node<T>, NeighborList> call(
            final Tuple2<Node<T>, NeighborList> tuple) {

        int most_similar = 0;
        double highest_similarity = 0;

        for (int i = 0; i < medoids.size(); i++) {
            Node<T> medoid = medoids.get(i);
            double sim = similarity.similarity(tuple._1.value, medoid.value);

            if (sim > highest_similarity) {
                highest_similarity = sim;
                most_similar = i;
            }

        }

        tuple._1.setAttribute(NodePartitioner.PARTITION_KEY, most_similar);
        return tuple;
    }

}

class ClusteringSimilarityAdapter<T> implements Similarity<Node<T>> {

    private final SimilarityInterface<T> similarity;

    ClusteringSimilarityAdapter(final SimilarityInterface<T> similarity) {
        this.similarity = similarity;
    }

    public double similarity(final Node<T> obj1, final Node<T> obj2) {
        return similarity.similarity(obj1.value, obj2.value);
    }
}


