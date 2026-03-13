package games;

public class GameLogic {

    private final int[][] maze;
    private final Player player;
    private final int exitRow;
    private final int exitCol;
    private boolean completed;

    public GameLogic(MazeGenerator mazeGenerator) {
        this.maze = mazeGenerator.getMaze();
        this.player = new Player(mazeGenerator.getStartRow(), mazeGenerator.getStartCol());
        this.exitRow = mazeGenerator.getExitRow();
        this.exitCol = mazeGenerator.getExitCol();
        this.completed = false;
    }

    public boolean movePlayer(int rowOffset, int colOffset) {
        if (completed) {
            return false;
        }

        int nextRow = player.getRow() + rowOffset;
        int nextCol = player.getCol() + colOffset;

        if (!isWalkable(nextRow, nextCol)) {
            return false;
        }

        player.setPosition(nextRow, nextCol);
        completed = nextRow == exitRow && nextCol == exitCol;
        return true;
    }

    public int[][] getMaze() {
        return maze;
    }

    public Player getPlayer() {
        return player;
    }

    public int getExitRow() {
        return exitRow;
    }

    public int getExitCol() {
        return exitCol;
    }

    public boolean isCompleted() {
        return completed;
    }

    private boolean isWalkable(int row, int col) {
        if (row < 0 || row >= maze.length || col < 0 || col >= maze[0].length) {
            return false;
        }

        int cell = maze[row][col];
        return cell == MazeGenerator.PATH;
    }
}
