package uz.cs2arena.match;

public enum Team {
    A, B;

    public Team opposite() {
        return this == A ? B : A;
    }
}
