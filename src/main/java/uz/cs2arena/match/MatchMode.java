package uz.cs2arena.match;

public enum MatchMode {
    FIVE_V_FIVE("5v5", 5),
    WINGMAN("2v2 Wingman", 2),
    DUEL("1v1 Duel", 1);

    private final String label;
    private final int teamSize;

    MatchMode(String label, int teamSize) {
        this.label = label;
        this.teamSize = teamSize;
    }

    public String label() { return label; }
    public int teamSize() { return teamSize; }
}
