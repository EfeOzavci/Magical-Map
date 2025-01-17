import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class OzNavigator {

    private BufferedWriter writer;                              // Writer to output logs or information
    private int maxX, maxY;                                     // Dimensions of the grid
    private Node[][] grid;                                      // 2D array of Node objects representing the map
    private HashMap<String, ArrayList<Edge>> adj = new HashMap<>(); // Adjacency list mapping "x-y" to a list of Edges
    private Integer lineOfSightRadius;                          // Radius around the current position to reveal impassable nodes
    private int startX, startY;                                 // Starting coordinates
    private ArrayList<Objective> objectives = new ArrayList<>();// List of objectives (goals) to reach

    private ArrayList<Node> allRevealedNodes = new ArrayList<>();// Keeps track of all nodes revealed (turned impassable)

    // Current position of the navigator
    private int currentX, currentY;

    public OzNavigator(BufferedWriter writer) {
        this.writer = writer;                                   // Store the provided BufferedWriter for later output
    }

    public void readNodeFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // Read dimensions (maxX, maxY)
            String[] dim = br.readLine().trim().split("\\s+");
            maxX = Integer.parseInt(dim[0]);
            maxY = Integer.parseInt(dim[1]);

            // Initialize the grid of nodes
            grid = new Node[maxX][maxY];

            String line;
            // For each line, create a Node object and store it in the grid
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int t = Integer.parseInt(parts[2]);
                grid[x][y] = new Node(x, y, t, true);           // Initially 'true' for passable if type allows
            }
        }
    }

    public void readEdgesFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            // Each line describes an edge between two nodes and the travel time between them
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                String[] nodeCoords = parts[0].split(",");
                double time = Double.parseDouble(parts[1]);

                // Parse the coordinates of the two connected nodes
                int[] coord1 = parseCoordinate(nodeCoords[0]);
                int[] coord2 = parseCoordinate(nodeCoords[1]);

                // Add edges in both directions (undirected graph)
                addEdge(coord1[0], coord1[1], coord2[0], coord2[1], time);
                addEdge(coord2[0], coord2[1], coord1[0], coord1[1], time);
            }
        }
    }

    // Parse a coordinate string in the form "x-y" into integer x and y
    private int[] parseCoordinate(String coord) {
        String[] xy = coord.split("-");
        int x = Integer.parseInt(xy[0]);
        int y = Integer.parseInt(xy[1]);
        return new int[]{x, y};
    }

    // Add an edge from (x1,y1) to (x2,y2) with the given travel time
    private void addEdge(int x1, int y1, int x2, int y2, double time) {
        String key = x1 + "-" + y1;                             // Convert coordinates to a string key
        adj.putIfAbsent(key, new ArrayList<>());                // Ensure list exists
        adj.get(key).add(new Edge(x2, y2, time));               // Add the edge
    }

    public void readObjFile(String filename) throws IOException {
        // This reads the objectives file, including line-of-sight radius, start position, and objectives
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            lineOfSightRadius = (int) Double.parseDouble(br.readLine().trim()); // First line: line of sight radius

            // Data structures to hold parsed objective information before creating Objective objects
            ArrayList<Integer> xList = new ArrayList<>();
            ArrayList<Integer> yList = new ArrayList<>();
            ArrayList<ArrayList<Integer>> optsList = new ArrayList<>();

            optsList.add(0, new ArrayList<>());                 // Initialize optsList with an empty list at index 0

            // Read starting coordinates
            String[] startCoords = br.readLine().trim().split("\\s+");
            startX = Integer.parseInt(startCoords[0]);
            startY = Integer.parseInt(startCoords[1]);

            String line;
            // Read each objective line, parse coordinates and options
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                int ox = Integer.parseInt(parts[0]);
                xList.add(ox);
                int oy = Integer.parseInt(parts[1]);
                yList.add(oy);
                ArrayList<Integer> opts = new ArrayList<>();

                // If there are additional integers, they are options
                if (parts.length > 2) {
                    for (int i = 2; i < parts.length; i++) {
                        opts.add(Integer.parseInt(parts[i]));
                    }
                } else {
                    opts = new ArrayList<>();
                }

                optsList.add(opts);
                opts = new ArrayList<>();
            }

            // Create Objective objects from parsed data
            for (int i = 0; i < xList.size(); i++){
                objectives.add(new Objective(xList.get(i), yList.get(i), optsList.get(i)));
            }

        }
    }

    public void writeOutput() {
        try {
            writer.flush();                                      // Flush the writer to ensure data is written out
        } catch (IOException e) {
            System.err.println("Error writing to file.");
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        // Set the current position to the start coordinates
        currentX = startX;
        currentY = startY;

        ArrayList<Node> revealedNodes;                          // Temporarily holds nodes revealed each step
        ArrayList<Node> changedNodes;                           // Temporarily holds nodes whose type changes
        ArrayList<Integer> bestOptions = new ArrayList<>();     // Track best options chosen so far
        ArrayList<int[]> path;                                  // Stores a path of coordinates from current to objective

        // Iterate through each objective
        for (int i = 0; i < objectives.size(); i++) {

            Objective obj = objectives.get(i);
            int targetX = obj.x;
            int targetY = obj.y;

            changedNodes = new ArrayList<>();

            int bestOption = -1;
            double bestDist = Double.POSITIVE_INFINITY;

            // Reveal around the current position to mark any newly discovered impassable nodes
            revealedNodes = revealAround(currentX,currentY);

            // If the objective has options, try them to find the best one
            if (!obj.options.isEmpty()) {

                // For each option, temporarily alter nodes of that type to 0 (passable),
                // find a path, measure distance, and revert changes
                for (int opt : obj.options) {

                    for (Node[] temp : grid) {
                        for (Node node : temp) {
                            if (node.getType() == opt) {
                                node.setTypeToZero();
                                changedNodes.add(node);
                            }
                        }
                    }

                    revealedNodes = revealAround(currentX, currentY);
                    path = dijkstra(currentX, currentY, targetX, targetY);

                    double totalDistance = calculateTotalPathDistance(path, adj);

                    // Update bestOption if this option yields a shorter path
                    if (totalDistance < bestDist) {
                        bestDist = totalDistance;
                        bestOption = opt;
                    }

                    // Revert nodes to original type after testing this option
                    for (Node node : changedNodes) {
                        node.setType(opt);
                    }

                    // If any revealed nodes were previously made passable, make sure they remain impassable
                    for (Node node : changedNodes){
                        if (allRevealedNodes.contains(node)){
                            node.passable = false;
                        }
                    }

                    changedNodes.clear();
                }

                // Finally, set all nodes of the bestOption type to 0 (passable) permanently
                for (Node[] temp : grid) {
                    for (Node node : temp) {
                        if (node.getType() == bestOption) {
                            node.setTypeToZero();
                            changedNodes.add(node);
                        }
                    }
                }

                writer.write("Number " + bestOption + " is chosen!"+ "\n");
                // Note: bestOptions is not updated here (commented out in the code)
            }

            // Reveal nodes around current position again
            revealedNodes = revealAround(currentX, currentY);

            // Compute the path to the objective using Dijkstra
            path = dijkstra(currentX, currentY, targetX, targetY);

            // If any revealed (impassable) node lies in the path, recalculate
            if (isAnyNodeInPath(revealedNodes, path)) {
                writer.write("Path is impassable!"+ "\n");
                path = dijkstra(currentX, currentY, targetX, targetY);
            }

            // Begin traversing the computed path
            int idx = 0;
            if (!path.isEmpty()) {
                int[] first = path.get(0);
                // If the first node is the current position, start from the next node in the path
                if (first[0] == currentX && first[1] == currentY) {
                    idx = 1;
                }

                revealedNodes = revealAround(currentX, currentY);

                // Check path impassability again after revealing more nodes
                if (isAnyNodeInPath(revealedNodes, path)) {
                    writer.write("Path is impassable!"+ "\n");
                    idx = 1;
                    path = dijkstra(currentX, currentY, targetX, targetY);
                }
            }

            // Move along the path step by step
            while (idx < path.size()) {

                revealedNodes = revealAround(currentX, currentY);

                // If any revealed node blocks the path, recalculate path
                if (isAnyNodeInPath(revealedNodes, path)) {
                    writer.write("Path is impassable!"+ "\n");
                    idx = 1;
                    path = dijkstra(currentX, currentY, targetX, targetY);
                }

                int[] step = path.get(idx);
                int nx = step[0];
                int ny = step[1];

                // Move to the next node in the path
                currentX = nx;
                currentY = ny;
                writer.write("Moving to " + nx + "-" + ny+ "\n");
                idx++;
            }

            // Once we've followed the path, we consider the objective reached
            writer.write("Objective " + (i + 1) + " reached!"+ "\n");
        }

    }

    // Calculate the total distance of a given path using the adjacency information
    public double calculateTotalPathDistance(ArrayList<int[]> path, HashMap<String, ArrayList<Edge>> adj) {
        if (path == null || path.size() < 2) {
            return 0.0; // No path or a single-node path means zero distance
        }

        double totalDistance = 0.0;

        // Sum the travel times of each consecutive edge in the path
        for (int i = 0; i < path.size() - 1; i++) {
            int[] current = path.get(i);
            int[] next = path.get(i + 1);

            int x1 = current[0], y1 = current[1];
            int x2 = next[0], y2 = next[1];

            String key = x1 + "-" + y1;
            ArrayList<Edge> edges = adj.getOrDefault(key);

            boolean edgeFound = false;
            // Find the edge leading from (x1,y1) to (x2,y2)
            for (Edge edge : edges) {
                if (edge.nx == x2 && edge.ny == y2) {
                    totalDistance += edge.time;
                    edgeFound = true;
                    break;
                }
            }

            if (!edgeFound) {
                System.out.println("No edge found between (" + x1 + ", " + y1 + ") and (" + x2 + ", " + y2 + ")");
            }
        }

        return totalDistance;
    }

    // Reveal nodes around (cx,cy) within lineOfSightRadius that are type >= 2, marking them impassable
    public ArrayList<Node> revealAround(int cx, int cy) {

        if (cx < 0 || cx >= maxX || cy < 0 || cy >= maxY) return new ArrayList<>();

        ArrayList<Node> myRevealedNodes = new ArrayList<>();     // Nodes whose passability changes now

        for (int x = Math.max(cx - (int)Math.ceil(lineOfSightRadius), 0);
             x < Math.min(cx + (int)Math.ceil(lineOfSightRadius) + 1, maxX);
             x++) {
            for (int y = Math.max(cy - (int)Math.ceil(lineOfSightRadius), 0);
                 y < Math.min(cy + (int)Math.ceil(lineOfSightRadius) + 1, maxY);
                 y++) {

                double dx = x - cx;
                double dy = y - cy;
                double distSquared = dx * dx + dy * dy;

                // If within lineOfSightRadius and type >= 2, make node impassable
                if (grid[x][y] != null && distSquared <= lineOfSightRadius * lineOfSightRadius && grid[x][y].type >= 2) {
                    if (grid[x][y].passable) {
                        grid[x][y].passable = false;
                        myRevealedNodes.add(grid[x][y]);
                    }
                }
            }
        }

        // Add these newly revealed nodes to the global list
        if (!myRevealedNodes.isEmpty()) {
            for (Node node : myRevealedNodes){
                allRevealedNodes.add(node);
            }
        }

        return myRevealedNodes;
    }

    // Check if any of the revealed impassable nodes are part of the given path
    public boolean isAnyNodeInPath(ArrayList<Node> revealedNodes, ArrayList<int[]> path) {
        ArrayList<String> pathSet = new ArrayList<>();
        // Convert path coordinates to strings for easier searching
        for (int[] p : path) {
            pathSet.add(p[0] + "-" + p[1]);
        }

        // If any revealed node matches a coordinate in the path, return true
        for (Node node : revealedNodes) {
            String key = node.x + "-" + node.y;
            if (pathSet.contains(key)) {
                return true;
            }
        }

        return false;
    }

    // Dijkstra's algorithm to find the shortest path from (sx,sy) to (tx,ty)
    public ArrayList<int[]> dijkstra(int sx, int sy, int tx, int ty) {
        double[][] dist = new double[maxX][maxY];
        // Initialize distances to infinity except the start node
        for (int i = 0; i < maxX; i++) {
            for (int j = 0; j < maxY; j++) {
                dist[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        dist[sx][sy] = 0.0;

        // PriorityQueue for managing nodes to explore; compares by distance
        PriorityQueue<State> pq = new PriorityQueue<>(new Comparator<State>() {
            @Override
            public int compare(State s1, State s2) {
                return Double.compare(s1.dist, s2.dist);
            }
        });
        pq.add(new State(sx, sy, 0.0));

        // parentMap to reconstruct the path after Dijkstra finishes
        HashMap<String, String> parentMap = new HashMap<>();
        parentMap.put(sx + "-" + sy, null);

        ArrayList<int[]> path = new ArrayList<>();

        // Main Dijkstra loop
        while (!pq.isEmpty()) {
            State cur = pq.poll();

            // If we've reached the target, reconstruct the path
            if (cur.x == tx && cur.y == ty) {
                String current = tx + "-" + ty;
                while (current != null) {
                    String[] parts = current.split("-");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    path.add(new int[]{x, y});
                    current = parentMap.get(current);
                }

                // Reverse the path so it goes from start to target
                for (int left = 0, right = path.size() - 1; left < right; left++, right--) {
                    int[] temp = path.get(left);
                    path.set(left, path.get(right));
                    path.set(right, temp);
                }

                return path;
            }

            // Explore neighbors (edges) of the current node
            String key = cur.x + "-" + cur.y;
            ArrayList<Edge> edges = adj.getOrDefault(key);
            if (edges == null) {
                continue;
            }
            for (Edge e : edges) {
                // Only consider if destination node is passable
                if (grid[e.nx][e.ny] == null || !grid[e.nx][e.ny].isPassable()) {
                    continue;
                }

                double ndist = cur.dist + e.time;
                // If we found a shorter path to (e.nx,e.ny), update and push to queue
                if (ndist < dist[e.nx][e.ny]) {
                    dist[e.nx][e.ny] = ndist;
                    pq.add(new State(e.nx, e.ny, ndist));
                    parentMap.put(e.nx + "-" + e.ny, cur.x + "-" + cur.y);
                }
            }
        }

        // If target not reached, return empty path
        return new ArrayList<>();
    }

    // Inner classes:

    public static class Node {
        int x, y;
        int type;
        boolean passable;

        Node(int x, int y, int t, boolean passable) {
            this.x = x;
            this.y = y;
            this.type = t;

            // Type 1 = impassable, Type 0 or >=2 = passable initially
            if (type == 1){
                this.passable = false;
            }
            if (type == 0 || type >= 2) {
                this.passable = true;
            }

        }

        public boolean isPassable() {
            return passable;
        }

        public int getType(){
            return type;
        }

        public void setType(int newType){
            type = newType;
            this.passable = (type == 0 || type >= 2);  // Passable if 0 or >=2
        }

        public void setTypeToZero(){
            type = 0;
            this.passable = true;
        }

    }

    public static class Edge {
        int nx, ny;
        double time;
        Edge(int nx, int ny, double time) {
            this.nx = nx;
            this.ny = ny;
            this.time = time;
        }
    }

    public static class Objective {
        int x, y;
        ArrayList<Integer> options;
        Objective(int x, int y, ArrayList<Integer> options) {
            this.x = x;
            this.y = y;
            this.options = options;  // Possible type options to try for this objective
        }
    }

    public static class State {
        int x, y;
        double dist;

        public State(int x, int y, double dist) {
            this.x = x;
            this.y = y;
            this.dist = dist;        // Distance so far from the start
        }
    }
}