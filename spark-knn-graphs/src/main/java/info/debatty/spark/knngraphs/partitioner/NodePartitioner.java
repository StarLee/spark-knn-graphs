/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
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

package info.debatty.spark.knngraphs.partitioner;


import info.debatty.spark.knngraphs.Node;
import org.apache.spark.Partitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Partition the graph using a specific node attribute.
 * Example usage:
 * graph = graph.mapToPair(new RandomizeFunction(partitions))
 *              .partitionBy(new NodePartitioner(partitions));
 * @author Thibault Debatty
 */
public class NodePartitioner extends Partitioner {

    private final int partitions;

    /**
     *
     * @param partitions
     */
    public NodePartitioner(final int partitions) {
        this.partitions = partitions;
    }

    @Override
    public final int numPartitions() {
        return partitions;
    }

    @Override
    public final int getPartition(final Object obj) {
        Node node = (Node) obj;
        return node.partition;
    }
}
