package games;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class MazeGenerator {

    public static final int PATH = 0;
    public static final int WALL = 1;
    public static final int OBSTACLE = 2;

    private final int rows;
    private final int cols;
    private final Random random = new Random();

    private int[][] maze;
    private final int startRow = 1;
    private final int startCol = 1;
    private int exitRow;
    private int exitCol;

    public MazeGenerator(int rows, int cols) {
        this.rows = normalizeSize(rows);
        this.cols = normalizeSize(cols);
        generateMaze();
    }

    public final void generateMaze() {
        maze = new int[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                maze[row][col] = WALL;
            }
        }

        carvePaths();
        exitRow = rows - 2;
        exitCol = cols - 2;
        maze[startRow][startCol] = PATH;
        maze[exitRow][exitCol] = PATH;
        addObstacles();
    }

    public int[][] getMaze() {
        return maze;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getExitRow() {
        return exitRow;
    }

    public int getExitCol() {
        return exitCol;
    }

    private int normalizeSize(int size) {
        int normalized = Math.max(11, size);
        if (normalized % 2 == 0) {
            normalized++;
        }
        return normalized;
    }

    private void carvePaths() {
        Deque<Point> stack = new ArrayDeque<>();
        stack.push(new Point(startCol, startRow));
        maze[startRow][startCol] = PATH;

        int[] dRows = {-2, 2, 0, 0};
        int[] dCols = {0, 0, -2, 2};

        while (!stack.isEmpty()) {
            Point current = stack.peek();
            List<Integer> availableDirections = new ArrayList<>();

            for (int i = 0; i < dRows.length; i++) {
                int nextRow = current.y + dRows[i];
                int nextCol = current.x + dCols[i];
                if (isInsideInnerBounds(nextRow, nextCol) && maze[nextRow][nextCol] == WALL) {
                    availableDirections.add(i);
                }
            }

            if (availableDirections.isEmpty()) {
                stack.pop();
                continue;
            }

            int selected = availableDirections.get(random.nextInt(availableDirections.size()));
            int targetRow = current.y + dRows[selected];
            int targetCol = current.x + dCols[selected];
            int betweenRow = current.y + (dRows[selected] / 2);
            int betweenCol = current.x + (dCols[selected] / 2);

            maze[betweenRow][betweenCol] = PATH;
            maze[targetRow][targetCol] = PATH;
            stack.push(new Point(targetCol, targetRow));
        }
    }

    private void addObstacles() {
        List<Point> candidates = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                if (maze[row][col] != PATH) {
                    continue;
                }
                if ((row == startRow && col == startCol) || (row == exitRow && col == exitCol)) {
                    continue;
                }
                candidates.add(new Point(col, row));
            }
        }

        Collections.shuffle(candidates, random);
        int targetObstacles = Math.max(3, (rows * cols) / 90);
        int placed = 0;

        for (Point candidate : candidates) {
            if (placed >= targetObstacles) {
                break;
            }

            maze[candidate.y][candidate.x] = OBSTACLE;
            if (hasReachableExit()) {
                placed++;
            } else {
                maze[candidate.y][candidate.x] = PATH;
            }
        }
    }

    private boolean hasReachableExit() {
        boolean[][] visited = new boolean[rows][cols];
        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(new Point(startCol, startRow));
        visited[startRow][startCol] = true;

        int[] dRows = {-1, 1, 0, 0};
        int[] dCols = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (current.y == exitRow && current.x == exitCol) {
                return true;
            }

            for (int i = 0; i < dRows.length; i++) {
                int nextRow = current.y + dRows[i];
                int nextCol = current.x + dCols[i];
                if (!isInsideBounds(nextRow, nextCol) || visited[nextRow][nextCol]) {
                    continue;
                }
                if (maze[nextRow][nextCol] != PATH) {
                    continue;
                }

                visited[nextRow][nextCol] = true;
                queue.offer(new Point(nextCol, nextRow));
            }
        }

        return false;
    }

    private boolean isInsideInnerBounds(int row, int col) {
        return row > 0 && row < rows - 1 && col > 0 && col < cols - 1;
    }

    private boolean isInsideBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
