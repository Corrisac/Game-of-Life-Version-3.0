public class Cell {
    private boolean isAlive;
    public Cell(boolean isAlive) {
        this.isAlive = isAlive;
    }
    public boolean checkAlive() {
        return this.isAlive;
    }
    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }
}

