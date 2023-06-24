import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * PS4 Solution - Kevin Bacon Game Driver and Generator
 * @author Reed Levinson, Spring 2023
 */
public class BaconGame {
    /**
     * Generates a graph of actors connected to other actors connected by the movies they costarred in
     * Vertices = actors, edges = shared movies
     * Uses data from 3 files which contain data on the actors and their movies
     * Generates a map of actors and their corresponding movies to efficiently generate connections graph
     * @param actorPN filepath of actor-actorID file
     * @param moviesPN filepath of movies-moviesID file
     * @param actorMoviePN filepath of moviesID-actorID file
     * @return graph of all actor-movie connections with actors as vertices and movies as edges
     */
    public static Graph<String, Set<String>> generateActorMovieGraph (String actorPN, String moviesPN, String actorMoviePN) throws Exception{
        BufferedReader actor, movies, actorMovie;

        try { // Open all files, if possible
            actor = new BufferedReader(new FileReader(actorPN));
            movies = new BufferedReader(new FileReader(moviesPN));
            actorMovie = new BufferedReader(new FileReader(actorMoviePN));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new AdjacencyMapGraph<>();
        }

        // Generates a map that maps actorIDs (Integer) to actor names (String)
        Map<Integer, String> actorMap;
        try {
            actorMap = new HashMap<>();
            String line;
            // Reads file line by line, adding each ID-actor pairing to map using "|" as delimiter
            while((line = actor.readLine()) != null) {
                String[] parts = line.split("\\|");
                actorMap.put(Integer.parseInt(parts[0]), parts[1]);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new AdjacencyMapGraph<>();
        }

        // Generates a map that maps movieIDs (Integer) to movie names (String)
        Map<Integer, String> movieMap;
        try {
            movieMap = new HashMap<>();
            String line;
            // Reads file line by line, adding each ID-movie pairing to map using "|" as delimiter
            while((line = movies.readLine()) != null) {
                String[] parts = line.split("\\|");
                movieMap.put(Integer.parseInt(parts[0]), parts[1]);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new AdjacencyMapGraph<>();
        }

        // Generates a map that maps actor names (String) to a set of their movie names (Set of Strings)
        // Uses IDs from other two maps for labelling of actors and movies
        Map<String, Set<String>> actorMovieMap;
        try {
            actorMovieMap = new HashMap<>();
            String line;
            // Reads file line by line, adding each actor-movie pairing to map using "|" as delimiter
            while((line = actorMovie.readLine()) != null) {
                String[] parts = line.split("\\|");
                String movieName = movieMap.get(Integer.parseInt(parts[0]));
                String actorName = actorMap.get(Integer.parseInt(parts[1]));
                // Checks to see if an actor already has a set of movies
                // If so, adds new movie to their set of movies
                // If not, adds actor to map with empty set and adds movie to the set
                if (!actorMovieMap.containsKey(actorName)) {
                    Set<String> tempSet = new HashSet<>();
                    tempSet.add(movieName);
                    actorMovieMap.put(actorName, tempSet);
                } else {
                    actorMovieMap.get(actorName).add(movieName);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new AdjacencyMapGraph<>();
        }

        // Generates a new graph for connecting actors to other actors
        // First adds every actor in the map as a vertex
        Graph<String, Set<String>> actorMovieGraph = new AdjacencyMapGraph<>();
        Set<String> keySet = actorMovieMap.keySet();
        for (String act: keySet) {
            actorMovieGraph.insertVertex(act);
        }

        // Iterates over the list of movies for each actor
        // Then checks the list of movies for every other actor
        // If they have any movies in common, adds to a "shared movies" set
        // If "shared movies" has any elements, adds an edge in the graph between the two movies
        // with the shared movies set as the undirected edge
        for (String a: keySet) {
            Set<String> aMovies = actorMovieMap.get(a);
            for (String b: keySet) {
                if (a.equals(b) || actorMovieGraph.hasEdge(a, b)) continue;
                Set<String> sharedMovies = new HashSet<>();
                for (String c: aMovies) {
                    if (actorMovieMap.get(b).contains(c)) {
                        sharedMovies.add(c);
                    }
                }
                if (sharedMovies.isEmpty()) continue;
                actorMovieGraph.insertUndirected(a, b, sharedMovies);
            }
        }
        return actorMovieGraph;
    }

    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        // Prints the rules for the game
        System.out.println("Welcome to the Bacon Game!\n");
        System.out.println("Commands:");
        System.out.println("c <#>: list top (positive number) or bottom (negative) <#> centers of the universe, sorted by average separation");
        System.out.println("d <low> <high>: list actors sorted by degree, with degree between low and high");
        System.out.println("i: list actors with infinite separation from the current center");
        System.out.println("p <name>: find path from <name> to current center of the universe");
        System.out.println("s <low> <high>: list actors sorted by non-infinite separation from the current center, with separation between low and high");
        System.out.println("u <name>: make <name> the center of the universe");
        System.out.println("q: quit game\n");

        // Hardcoded file path names used to generate the actor-movie graph from their particular files
        // Hardcoded initial source ("Kevin Bacon") as well
        // Runs initial BFS generation with Kevin Bacon as the center of the universe (root of the graph tree)
        String actorPathName = "inputs/bacon/actors.txt";
        String moviesPathName = "inputs/bacon/movies.txt";
        String AMPathName = "inputs/bacon/movie-actors.txt";
        String source = "Kevin Bacon";
        Graph<String, Set<String>> actorMovieGraph = generateActorMovieGraph(actorPathName, moviesPathName, AMPathName);
        Graph<String, Set<String>> pathTree = GraphLibPlus.bfs(actorMovieGraph, source);

        // Prints out initial information about the universe with Kevin Bacon as the center
        System.out.println(source + " is now the center of the acting universe, connected to " + (pathTree.numVertices() - 1)
                + "/" + actorMovieGraph.numVertices() + " actors with average separation " + GraphLibPlus.averageSeparation(pathTree, source) + "\n");

        // Boolean used for gameplay management
        boolean playing = true;
        while (playing) {
            // Takes the next line as the action to be executed
            System.out.println(source + " game >");
            String move = in.nextLine();
            String action = null;
            // Boolean for determining if action is valid (boundary case)
            boolean validKey = true;

            // If what is entered is empty (blank line), sets validKey to false (won't run action)
            try {
                action = move.substring(0, 1);
            } catch (Exception e) {
                validKey = false;
            }

            // Break gameplay iteration if key is invalid
            if (!validKey) {
                System.err.println("Action not acceptable.");
            }
            else {
                // Used to determine (for certain actions) if there is a space after first letter
                // Otherwise will break current gameplay iteration and print error
                boolean singleLetterCheck;
                try {
                    if (move.charAt(1) == ' ') singleLetterCheck = true;
                    else singleLetterCheck = false;
                } catch (Exception e) {
                    singleLetterCheck = false;
                }

                // Code for action "c", takes number and prints the top/bottom centers of the universe
                // Sorted by average separation, positive = top and negative = bottom
                if (action.equals("c")) {
                    // Checks if action call is valid
                    if (singleLetterCheck) {
                        // Generates a list of all actors and a map of all actors mapped to their average separations
                        List<String> actors = new ArrayList<>();
                        Map<String, Double> actorSepMap = new HashMap<>();
                        for (String vertex : pathTree.vertices()) {
                            actors.add(vertex);
                            // Adds each actor to map with key from the average separation from the bfs tree
                            // with them as the center
                            actorSepMap.put(vertex, GraphLibPlus.averageSeparation(GraphLibPlus.bfs(actorMovieGraph, vertex), vertex));
                        }

                        // Special comparator class used to determine if one actors average separation is less than another's
                        class averageSeparationComparator implements Comparator<String> {
                            public int compare(String s1, String s2) {
                                double check = actorSepMap.get(s1) - actorSepMap.get(s2);
                                if (check == 0) {
                                    return 0;
                                } else if (check > 0) {
                                    return 1;
                                } else {
                                    return -1;
                                }
                            }
                        }

                        // Creates new comparator object from above class and sorts actors list based on this ordering
                        Comparator<String> avgSepComp = new averageSeparationComparator();
                        actors.sort(avgSepComp);

                        // Takes number from user input
                        // If number is 0, skip and continue
                        // If number is > 0, take lowest # elements in sorted array list and print them with their separations
                        // If number is < 0, take highest # elements in sorted array list and print them with their separations
                        int num = Integer.parseInt(move.substring(2));
                        if (num == 0) {
                            System.out.println("Need to enter a number greater than or less than 0.");
                            continue;
                        } else if (num > 0) {
                            System.out.println("Here are the top " + num + " actors with the best Bacon numbers/" +
                                    "lowest average separation: ");
                            for (int i = 0; i < num; i++) {
                                String actor = actors.get(i);
                                System.out.println(actor + "\n\tAverage separation of " + actorSepMap.get(actor));
                            }
                        } else {
                            num *= -1;
                            System.out.println("Here are the bottom " + num + " actors with the worst Bacon numbers/" +
                                    "highest average separation: ");
                            for (int i = 0; i < num; i++) {
                                String actor = actors.get(actors.size() - i - 1);
                                System.out.println(actor + "\n\tAverage separation of " + actorSepMap.get(actor));
                            }
                        }
                    } else {
                        System.err.println("Action not acceptable.");
                    }
                }

                // Code for action "d", takes a high and low number and returns a list of actors with degree/number of
                // connections between high and low bounds (inclusive)
                else if (action.equals("d")) {
                    if (singleLetterCheck) {
                        String[] parts = move.split(" ");
                        int low = Integer.parseInt(parts[1]);
                        int high = Integer.parseInt(parts[2]);

                        // Specialized comparator class for comparing the degrees of two actors using actor-movie graph
                        class degreeComparator implements Comparator<String> {
                            public int compare(String s1, String s2) {
                                return actorMovieGraph.inDegree(s1) - actorMovieGraph.inDegree(s2);
                            }
                        }

                        // Creates new comparator object using class and makes a PQ defined by this sorting mechanism
                        Comparator<String> degComp = new degreeComparator();
                        PriorityQueue<String> actorsByDeg = new PriorityQueue<>(degComp);

                        // Iterates over all actors in the graph and checks their degree
                        // If degree is between the bounds, adds the actor to the PQ
                        for (String s : actorMovieGraph.vertices()) {
                            int degree = actorMovieGraph.inDegree(s);
                            if (degree >= low && degree <= high) {
                                actorsByDeg.add(s);
                            }
                        }

                        // Prints each actor in the PQ with their degree until PQ is empty
                        while (!actorsByDeg.isEmpty()) {
                            String actor = actorsByDeg.remove();
                            System.out.println(actor + "\n\tNumber of direct connections: " + actorMovieGraph.inDegree(actor));
                        }
                    } else {
                        System.err.println("Action not acceptable.");
                    }
                }

                // Code for action "i", generates a list of all actors with no connection to current center of universe
                else if (action.equals("i")) {
                    System.out.println("Here are all actors with no connection/infinite separation from the current center: ");
                    // Generates a set of actors using missingVertices function between overall graph and BFS tree from current center
                    Set<String> missingActors = GraphLibPlus.missingVertices(actorMovieGraph, pathTree);
                    // Prints all actors in the set
                    for (String s : missingActors) {
                        System.out.println(s);
                    }
                }

                // Code for action "p", generates a path between an actor and the center of universe
                // Prints all intermediate connections between the two
                else if (action.equals("p")) {
                    if (singleLetterCheck) {
                        String actor = move.substring(2);
                        // Generates the path between the two actors
                        List<String> path = GraphLibPlus.getPath(pathTree, actor);
                        // If the path is empty, returns that path is empty and BN is infinity
                        // Otherwise, prints the path size
                        if (path.size() == 0) {
                            System.out.println("The Bacon Number for " + actor + " is infinity (no connection found).");
                        } else {
                            System.out.println(actor + "'s number is " + (path.size() - 1));
                        }
                        // Prints all intermediate connections along the path
                        for (int i = path.size() - 1; i > 0; i--) {
                            System.out.println(path.get(i) + " appeared in " + pathTree.getLabel(path.get(i), path.get(i - 1)) + " with " + path.get(i - 1));
                        }
                    } else {
                        System.err.println("Action not acceptable.");
                    }
                }

                // Code for action "s", takes a high and low number and returns a list of actors with separation from
                // current center between high and low bounds (inclusive)
                else if (action.equals("s")) {
                    if (singleLetterCheck) {
                        String[] parts = move.split(" ");
                        int low = Integer.parseInt(parts[1]);
                        int high = Integer.parseInt(parts[2]);

                        // Specialized comparator class used to compare the degree of separation between two actors from center
                        Graph<String, Set<String>> tempPathTree = pathTree;
                        class separationComparator implements Comparator<String> {
                            public int compare(String s1, String s2) {
                                return GraphLibPlus.getDegreeOfSep(tempPathTree, s1) - GraphLibPlus.getDegreeOfSep(tempPathTree, s2);
                            }
                        }

                        // Initializes new Comparator object using class above and generates PQ with this comparator's rule
                        Comparator<String> sepComp = new separationComparator();
                        PriorityQueue<String> actorsBySep = new PriorityQueue<>(sepComp);

                        // Iterates over all actors in the graph and checks their separation
                        // If degree is between the bounds, adds the actor to the PQ
                        for (String s : pathTree.vertices()) {
                            int separation = GraphLibPlus.getDegreeOfSep(pathTree, s);
                            if (separation >= low && separation <= high) {
                                actorsBySep.add(s);
                            }
                        }

                        // Prints each actor in the PQ with their degree until PQ is empty
                        while (!actorsBySep.isEmpty()) {
                            String actor = actorsBySep.remove();
                            System.out.println(actor + "\n\tSeparation distance: " + (GraphLibPlus.getPath(pathTree, actor).size() - 1));
                        }
                    } else {
                        System.err.println("Action not acceptable.");
                    }
                }

                // Code for action "u", sets new actor in the graph to the center of the universe
                else if (action.equals("u")) {
                    if (singleLetterCheck) {
                        // Extracts new desired center from user input and checks to see if this actor is in the graph
                        // If so, sets source to the new center's name
                        // Otherwise, prints error and continues
                        String checkSource = move.substring(2);
                        if (!actorMovieGraph.hasVertex(checkSource)) {
                            System.err.println("Actor not found.");
                            continue;
                        }
                        source = checkSource;

                        // Defines the path tree as the new BFS graph with the new center as the root
                        // Then prints information regarding the new center of the universe
                        pathTree = GraphLibPlus.bfs(actorMovieGraph, source);
                        System.out.println(source + " is now the center of the acting universe, connected to "
                                + (pathTree.numVertices() - 1) + "/" + actorMovieGraph.numVertices() +
                                " actors with average separation " + GraphLibPlus.averageSeparation(pathTree, source));
                    } else {
                        System.err.println("Action not acceptable.");
                    }
                }

                // Code for action "q", exits the program
                else if (action.equals("q")) {
                    // Sets gameplay management variable to false in order to end the program
                    System.out.println("Exiting game...");
                    playing = false;
                }

                // Catch for any errant commands
                // If command doesn't fall into any bucket, returns error and resets for new action
                else {
                    System.err.println("Action not acceptable.");
                }
            }
            System.out.println();
        }
    }
}
