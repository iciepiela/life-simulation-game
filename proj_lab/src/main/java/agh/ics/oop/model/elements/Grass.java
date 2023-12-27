package agh.ics.oop.model.elements;

import agh.ics.oop.model.Vector2d;
import agh.ics.oop.model.elements.WorldElement;

public class Grass implements WorldElement {
    private final Vector2d position;

    public Grass(Vector2d position) {
        this.position = position;
    }
    @Override
    public Vector2d getPosition() {
        return position;
    }
    @Override
    public String toString() {
        return "*";
    }
    @Override
    public boolean isAt(Vector2d position) {
        return this.position.equals(position);
    }

}
