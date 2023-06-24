import java.util.*;

/**
 * PS4 Solution - Extended Graph Library for Kevin Bacon Game
 * @author Reed Levinson, Spring 2023
 */
public class GraphLibPlus {
    /**
     * Conducts a Breadth First Search on a graph from a given source and returns
     * a new graph equivalent to a tree with "child" vertices pointing back to
     * the parents back up to the root ("source")
     * @param g graph of all connections in universe
     * @param source root of path tree
     * @return path tree with the shortest path determined by BFS
     */
    public static <V,E> Graph<V,E> bfs(Graph<V,E> g, V source) {
        Graph<V,E> pathTree = new AdjacencyMapGraph<>(); //initialize path tree
        pathTree.insertVertex(source); // insert source root into path tree graph
        Set<V> visited = new HashSet<>(); //Set to track which vertices have already been visited
        Queue<V> queue = new LinkedList<>(); //queue to implement BFS

        queue.add(source); // enqueue start vertex
        visited.add(source); // add start to visited Set
        while (!queue.isEmpty()) { // loop until no more vertices
            V u = queue.remove(); // dequeue
            for (V v : g.outNeighbors(u)) { // loop over out neighbors
                if (!visited.contains(v)) { // if neighbor not visited, then neighbor is discovered from this vertex
                    visited.add(v); // add neighbor to visited Set
                    queue.add(v); // enqueue neighbor
                    pathTree.insertVertex(v); // create new vertex for neighbor
                    pathTree.insertDirected(v, u, g.getLabel(u, v)); // create directed edge from child to parent
                }
            }
        }

        return pathTree;
    }

    /**
     * Returns a path within the BFS tree from a child up to the root
     * @param tree path tree determined by BFS
     * @param v child element to be pathed back to root
     * @return path from child to root
     */
    public static <V,E> List<V> getPath(Graph<V,E> tree, V v) {
        // Instantiates the path as a list and an element to keep track of the current element
        List<V> path = new ArrayList<>();
        V curr = v;

        // If the desired child is not found in the BFS tree,
        // prints that no path can be found and returns an empty path
        if (!tree.hasVertex(v)) {
            System.out.println("No path found");
            return new ArrayList<>();
        }

        // Works its way up the path tree from the child to root,
        // adding each element that it tracks to the beginning of the path list
        // and resetting current to the next in line
        while (tree.outDegree(curr) != 0) {
            path.add(0, curr);
            for (V u: tree.outNeighbors(curr)) {
                curr = u;
            }
        }

        // Adds the root to path and returns the path
        path.add(0, curr);
        return path;
    }

    /**
     * Takes an overall universe and a BFS subgraph and sees which elements
     * are not in the subgraph
     * @param graph overall universe of connections
     * @param subgraph sub universe narrowed by BFS
     * @return set of all missing vertices from tree from BFS
     */
    public static <V,E> Set<V> missingVertices(Graph<V,E> graph, Graph<V,E> subgraph) {
        // Creates empty set to store missing vertices
        Set<V> missingV = new HashSet<>();

        // Iterates over all vertices in overall graph, then checks each against subgraph
        // If not present, adds it to set
        for (V v: graph.vertices()) {
            if (!subgraph.hasVertex(v)) missingV.add(v);
        }

        return missingV;
    }

    /**
     * Recursive function to determine the average separation between parent of BFS tree and children
     * @param tree path tree determined by BFS
     * @param root center of universe
     * @return average separation between center of universe and all connections
     */
    public static <V,E> double averageSeparation(Graph<V,E> tree, V root) {
        // Recursive call on helper function that returns summed separation then divides by tree size
        return averageSeparationHelper(tree, root, 0) / (double) (tree.numVertices() - 1);
    }

    /**
     * Recursive helper function that sums the total separation of all children from a given root in BFS tree
     * @param tree subgraph of original BFS tree
     * @param root root of each call of the function (new "top" of this subgraph)
     * @param depth how far down the BFS tree this method call is
     * @return total sum of separation of children under root
     */
    public static <V, E> double averageSeparationHelper(Graph<V, E> tree, V root, int depth) {
        // Sets the current sum for each element as the depth of the child this subgraph is starting at
        int sum = depth;
        // For every neighbor (child of root), recursively calls this function to add to the original sum
        for (V neighbor: tree.inNeighbors(root)) {
            sum += averageSeparationHelper(tree, neighbor, depth + 1);
        }
        return sum;
    }

    /**
     * Returns the degree of separation between a given element and the root of graph tree
     * @param tree path tree
     * @param v child element
     * @return degree of separation of child from root
     */
    public static <V,E> int getDegreeOfSep(Graph<V,E> tree, V v) {
        // Generates the path between the two elements
        List<V> path = getPath(tree, v);
        // Returns the size of the path - 1 to account for inclusion of original element
        return (path.size() - 1);
    }
}
