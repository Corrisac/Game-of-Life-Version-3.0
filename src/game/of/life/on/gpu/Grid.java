import processing.core.PApplet;
import java.util.*;


public class Grid {
    private Cell[][] board;
    private int rows;
    private int cols;
    public Grid(int rows, int cols) 
    {
        this.rows = rows;
        this.cols = cols;
        this.board = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) 
            {
                for (int c = 0; c < cols; c++) 
                    {
                        board[r][c] = new Cell(false);
                    }
            }
    }
    public void printBoard() 
    {
        for (int r = 0; r < rows; r++) 
            {
                for (int c = 0; c < cols; c++) 
                    {
                        if (board[r][c].checkAlive()) {
                         System.out.print(" O "); 
                    }  
                    else 
                        {
                            System.out.print(" . "); // Dead cell
                        }
                    }
                System.out.println(); // Moves to the next line after finishing a row
            }
        System.out.println("-------------------------");
    }
    public int countLiveNeighbors(int row, int col) {
        int count = 0;

        // Loop through the 3x3 grid around the target cell
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                
                // Rule A: Make sure we don't look outside the edges of the board
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    
                    // Rule B: Don't count the target cell itself!
                    if (!(r == row && c == col)) {
                        
                        if (board[r][c].checkAlive()) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    // 6. The Time Stepper: Calculates the next frame of the simulation
    public void updateToNextGeneration() {
        // Rule C: We must create a temporary snapshot for the next turn
        boolean[][] nextState = new boolean[rows][cols];

        // Step 1: Scan every cell on the current board
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int neighbors = countLiveNeighbors(r, c);
                boolean currentlyAlive = board[r][c].checkAlive();

                //Conway's Rules of Life
                if (currentlyAlive && (neighbors == 2 || neighbors == 3)) {
                    nextState[r][c] = true;  // Survives
                } else if (!currentlyAlive && neighbors == 3) {
                    nextState[r][c] = true;  // Reproduction
                } else {
                    nextState[r][c] = false; // Dies (underpopulation or overpopulation)
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c].setAlive(nextState[r][c]);
            }
        }
    }
    public void setCellState(int row, int col, boolean isAlive) 
    {
        if (row >= 0 && row < rows && col >= 0 && col < cols) 
            {
            board[row][col].setAlive(isAlive);
            }
    }

    public boolean getCellState(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return board[row][col].checkAlive();
        }
        return false;
    }
// ... (keep your existing variables, constructor, and updateToNextGeneration logic) ...

    // The Erase Tool: Kills every cell instantly
    public void clearBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c].setAlive(false);
            }
        }
    }

    // The Painter: Tells the visual window how to draw the grid
    public void draw(PApplet window, int cellSize) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                
                if (board[r][c].checkAlive()) {
                    window.fill(0); // 0 = Black color for alive
                } else {
                    window.fill(255); // 255 = White color for dead
                }
                
                window.stroke(150); // Light gray for the grid lines
                
                // Draw the actual square on the screen
                window.rect(c * cellSize, r * cellSize, cellSize, cellSize);
            }
        }
    }
}