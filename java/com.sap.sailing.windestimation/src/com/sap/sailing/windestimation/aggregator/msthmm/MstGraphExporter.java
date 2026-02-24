package com.sap.sailing.windestimation.aggregator.msthmm;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.windestimation.aggregator.graph.DijkstraShortestPathFinderImpl;
import com.sap.sailing.windestimation.aggregator.graph.DijsktraShortestPathFinder;
import com.sap.sailing.windestimation.aggregator.graph.ElementAdjacencyQualityMetric;
import com.sap.sailing.windestimation.aggregator.graph.InnerGraphSuccessorSupplier;
import com.sap.sailing.windestimation.aggregator.hmm.GraphLevelInference;
import com.sap.sailing.windestimation.aggregator.hmm.GraphNode;
import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;

import java.util.function.Supplier;

/**
 * Exports MST graph data to JSON format for visualization.
 * The output can be consumed by a Python visualization script.
 * 
 * @author Generated for visualization purposes using Claude Opus 4.5
 */
public class MstGraphExporter {

    private final MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public MstGraphExporter(MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) {
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
    }

    /**
     * Exports the MST graph components to JSON format.
     * 
     * @param graphComponents The MST graph to export
     * @param writer The writer to output JSON to
     * @throws IOException if writing fails
     */
    public void exportToJson(MstManeuverGraphComponents graphComponents, Writer writer) throws IOException {
        // Collect all best paths for highlighting
        final Set<String> bestPathEdges = collectBestPathEdges(graphComponents);
        final Map<String, String> bestNodePerLevel = collectBestNodePerLevel(graphComponents);
        writer.write("{\n");
        writer.write("  \"nodes\": [\n");
        // Export all nodes starting from root
        final MstGraphLevel root = graphComponents.getRoot();
        final boolean[] firstNode = {true};
        final Map<MstGraphLevel, Integer> levelToId = new HashMap<>();
        final int[] nodeIdCounter = {0};
        exportNode(root, writer, firstNode, levelToId, nodeIdCounter, 0);
        writer.write("\n  ],\n");
        writer.write("  \"edges\": [\n");
        // Export edges between nodes
        final boolean[] firstEdge = {true};
        exportEdges(root, writer, firstEdge, levelToId, bestPathEdges);
        writer.write("\n  ],\n");
        // Export best path information
        writer.write("  \"bestPaths\": {\n");
        boolean firstBest = true;
        for (Map.Entry<String, String> entry : bestNodePerLevel.entrySet()) {
            if (!firstBest) {
                writer.write(",\n");
            }
            firstBest = false;
            writer.write("    \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
        }
        writer.write("\n  }\n");
        writer.write("}\n");
    }

    private void exportNode(MstGraphLevel level, Writer writer, boolean[] firstNode, 
            Map<MstGraphLevel, Integer> levelToId, int[] nodeIdCounter, int depth) throws IOException {
        final int nodeId = nodeIdCounter[0]++;
        levelToId.put(level, nodeId);
        if (!firstNode[0]) {
            writer.write(",\n");
        }
        firstNode[0] = false;
        final ManeuverForEstimation maneuver = level.getManeuver();
        writer.write("    {\n");
        writer.write("      \"id\": " + nodeId + ",\n");
        writer.write("      \"depth\": " + depth + ",\n");
        writer.write("      \"timestamp\": \"" + DATE_FORMAT.format(maneuver.getManeuverTimePoint().asDate()) + "\",\n");
        writer.write("      \"position\": {\"lat\": " + maneuver.getManeuverPosition().getLatDeg() + 
                     ", \"lon\": " + maneuver.getManeuverPosition().getLngDeg() + "},\n");
        // distanceToParent is actually the "compound distance" = sum of two predicted standard deviations
        // It's used for the Gaussian-based transition probability calculation
        writer.write("      \"compoundDistanceToParent\": " + level.getDistanceToParent() + ",\n");
        // Also export actual spatial distance and time difference to parent
        final MstGraphLevel parent = level.getParent();
        if (parent != null) {
            final ManeuverForEstimation parentManeuver = parent.getManeuver();
            final double spatialDistanceMeters = maneuver.getManeuverPosition()
                    .getDistance(parentManeuver.getManeuverPosition()).getMeters();
            final long timeDiffMillis = Math.abs(maneuver.getManeuverTimePoint().asMillis() 
                    - parentManeuver.getManeuverTimePoint().asMillis());
            writer.write("      \"spatialDistanceToParentMeters\": " + spatialDistanceMeters + ",\n");
            writer.write("      \"timeDiffToParentSeconds\": " + (timeDiffMillis / 1000.0) + ",\n");
        }
        writer.write("      \"compartments\": [\n");
        // Export each maneuver type compartment
        boolean firstCompartment = true;
        for (GraphNode<MstGraphLevel> node : level.getLevelNodes()) {
            if (!firstCompartment) {
                writer.write(",\n");
            }
            firstCompartment = false;
            final ManeuverTypeForClassification type = node.getManeuverType();
            final WindCourseRange windRange = node.getValidWindRange();
            final double confidence = node.getConfidence();
            writer.write("        {\n");
            writer.write("          \"type\": \"" + type.name() + "\",\n");
            writer.write("          \"confidence\": " + confidence + ",\n");
            writer.write("          \"windRangeFrom\": " + windRange.getFromPortside() + ",\n");
            writer.write("          \"windRangeWidth\": " + windRange.getAngleTowardStarboard() + ",\n");
            
            // Calculate single wind direction estimate for TACK/JIBE (width ~0)
            double windEstimate;
            if (windRange.getAngleTowardStarboard() < 1.0) {
                windEstimate = windRange.getFromPortside();
            } else {
                // For HEAD_UP/BEAR_AWAY, use middle of range
                windEstimate = windRange.getFromPortside() + windRange.getAngleTowardStarboard() / 2.0;
                if (windEstimate >= 360) {
                    windEstimate -= 360;
                }
            }
            writer.write("          \"windEstimate\": " + windEstimate + ",\n");
            writer.write("          \"tackAfter\": \"" + node.getTackAfter() + "\"\n");
            writer.write("        }");
        }
        writer.write("\n      ]\n");
        writer.write("    }");
        // Recursively export children
        for (MstGraphLevel child : level.getChildren()) {
            exportNode(child, writer, firstNode, levelToId, nodeIdCounter, depth + 1);
        }
    }

    private void exportEdges(MstGraphLevel level, Writer writer, boolean[] firstEdge,
            Map<MstGraphLevel, Integer> levelToId, Set<String> bestPathEdges) throws IOException {
        final int parentId = levelToId.get(level);
        for (final MstGraphLevel child : level.getChildren()) {
            final int childId = levelToId.get(child);
            // Export edges between all compartment pairs
            for (final GraphNode<MstGraphLevel> parentNode : level.getLevelNodes()) {
                for (final GraphNode<MstGraphLevel> childNode : child.getLevelNodes()) {
                    // Calculate transition probability
                    final double transitionProb = transitionProbabilitiesCalculator.getTransitionProbability(
                            childNode, parentNode, child.getDistanceToParent());
                    final String edgeKey = parentId + "_" + parentNode.getManeuverType().ordinal() + 
                                    "_" + childId + "_" + childNode.getManeuverType().ordinal();
                    final boolean isBestPath = bestPathEdges.contains(edgeKey);
                    if (!firstEdge[0]) {
                        writer.write(",\n");
                    }
                    firstEdge[0] = false;
                    writer.write("    {\n");
                    writer.write("      \"from\": " + parentId + ",\n");
                    writer.write("      \"fromType\": \"" + parentNode.getManeuverType().name() + "\",\n");
                    writer.write("      \"to\": " + childId + ",\n");
                    writer.write("      \"toType\": \"" + childNode.getManeuverType().name() + "\",\n");
                    writer.write("      \"transitionProbability\": " + transitionProb + ",\n");
                    writer.write("      \"distance\": " + child.getDistanceToParent() + ",\n");
                    writer.write("      \"isBestPath\": " + isBestPath + "\n");
                    writer.write("    }");
                }
            }
            // Recursively export edges for children
            exportEdges(child, writer, firstEdge, levelToId, bestPathEdges);
        }
    }

    private Set<String> collectBestPathEdges(MstManeuverGraphComponents graphComponents) {
        final Set<String> bestPathEdges = new HashSet<>();
        final Map<MstGraphLevel, Integer> levelToId = new HashMap<>();
        final int[] nodeIdCounter = {0};
        assignNodeIds(graphComponents.getRoot(), levelToId, nodeIdCounter);
        final ElementAdjacencyQualityMetric<GraphNode<MstGraphLevel>> edgeQualityMetric = (previousNode, currentNode) -> {
            return transitionProbabilitiesCalculator.getTransitionProbability(currentNode, previousNode,
                    previousNode.getGraphLevel() == null ? 0.0 : previousNode.getGraphLevel().getDistanceToParent());
        };
        for (final MstGraphLevel leaf : graphComponents.getLeaves()) {
            final InnerGraphSuccessorSupplier<GraphNode<MstGraphLevel>, MstGraphLevel> innerGraphSuccessorSupplier =
                    new InnerGraphSuccessorSupplier<>(graphComponents,
                            (final Supplier<String> nameSupplier) -> new GraphNode<MstGraphLevel>(
                                    null, null, new WindCourseRange(0, 360), 1.0, 0, null) {
                                @Override
                                public String toString() {
                                    return nameSupplier.get();
                                }
                            });
            final DijsktraShortestPathFinder<GraphNode<MstGraphLevel>> dijkstra = 
                    new DijkstraShortestPathFinderImpl<>(
                            innerGraphSuccessorSupplier.getArtificialLeaf(leaf),
                            innerGraphSuccessorSupplier.getArtificialRoot(),
                            innerGraphSuccessorSupplier, edgeQualityMetric);
            GraphNode<MstGraphLevel> prev = null;
            for (final GraphNode<MstGraphLevel> node : dijkstra.getShortestPath()) {
                if (prev != null && prev.getGraphLevel() != null && node.getGraphLevel() != null) {
                    Integer prevId = levelToId.get(prev.getGraphLevel());
                    Integer nodeId = levelToId.get(node.getGraphLevel());
                    if (prevId != null && nodeId != null) {
                        // Dijkstra goes from leaf to root (child to parent)
                        // We export edges from parent to child, so store the edge in that direction
                        String edgeKey = nodeId + "_" + node.getManeuverType().ordinal() + 
                                        "_" + prevId + "_" + prev.getManeuverType().ordinal();
                        bestPathEdges.add(edgeKey);
                    }
                }
                prev = node;
            }
        }
        return bestPathEdges;
    }

    private Map<String, String> collectBestNodePerLevel(MstManeuverGraphComponents graphComponents) {
        final Map<String, String> bestNodePerLevel = new HashMap<>();
        final Map<MstGraphLevel, Integer> levelToId = new HashMap<>();
        final int[] nodeIdCounter = {0};
        assignNodeIds(graphComponents.getRoot(), levelToId, nodeIdCounter);
        // Use the MstBestPathsCalculatorImpl to get the best nodes
        final MstBestPathsCalculatorImpl calculator = new MstBestPathsCalculatorImpl(transitionProbabilitiesCalculator);
        for (final GraphLevelInference<MstGraphLevel> inference : calculator.getBestNodes(graphComponents)) {
            final MstGraphLevel level = inference.getGraphNode().getGraphLevel();
            if (level != null) {
                Integer nodeId = levelToId.get(level);
                if (nodeId != null) {
                    bestNodePerLevel.put(String.valueOf(nodeId), inference.getGraphNode().getManeuverType().name());
                }
            }
        }
        return bestNodePerLevel;
    }

    private void assignNodeIds(MstGraphLevel level, Map<MstGraphLevel, Integer> levelToId, int[] nodeIdCounter) {
        levelToId.put(level, nodeIdCounter[0]++);
        for (final MstGraphLevel child : level.getChildren()) {
            assignNodeIds(child, levelToId, nodeIdCounter);
        }
    }
}
