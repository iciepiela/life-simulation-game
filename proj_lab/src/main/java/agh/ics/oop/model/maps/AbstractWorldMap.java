package agh.ics.oop.model.maps;

import agh.ics.oop.EnergyParameters;
import agh.ics.oop.model.Vector2d;
import agh.ics.oop.model.elements.Animal;
import agh.ics.oop.model.elements.Grass;
import agh.ics.oop.model.elements.WorldElement;
import agh.ics.oop.model.util.RandomPositionsGenerator;

import java.util.*;

public abstract class AbstractWorldMap implements WorldMap,MoveValidator{

    protected final Map<Vector2d, List<Animal>> animalsAlive = new HashMap<>();

    protected final Map<Vector2d, Grass> grasses=new HashMap<>();
    protected final List<Vector2d> emptyPlacesOnEquator;
    protected final List<Vector2d> emptyPlacesNotOnEquator;
    protected final Boundary mapBorders;
    protected final Boundary equator;
    protected final EnergyParameters energyParameters;
    protected final RandomPositionsGenerator randomPositionsGenerator = new RandomPositionsGenerator();

    public AbstractWorldMap(Boundary mapBorders, EnergyParameters energyParameters) {
        this.mapBorders = mapBorders;
        this.energyParameters = energyParameters;

        int height = mapBorders.upperRight().y()-mapBorders.lowerLeft().y()+1;
        int lowEq = (height%2==0) ? height/2-1 - height/5-1 : height/2 - height/5-1;
        int highEq = height/2 + height/5-1;
        this.equator = new Boundary(new Vector2d(0,lowEq),new Vector2d(mapBorders.upperRight().x(),highEq));

        this.emptyPlacesOnEquator = this.equator.allPositions();
        List<Vector2d> belowEq = new Boundary(mapBorders.lowerLeft(),new Vector2d(equator.upperRight().x(), equator.lowerLeft().y()-1)).allPositions();
        List<Vector2d> aboveEq = new Boundary(new Vector2d(mapBorders.lowerLeft().x(),mapBorders.upperRight().y()+1),mapBorders.upperRight()).allPositions();
        emptyPlacesNotOnEquator = new ArrayList<>(belowEq);
        emptyPlacesNotOnEquator.addAll(aboveEq);
    }
    public Boundary getMapBorders() {
        return mapBorders;
    }
    @Override
    public List<WorldElement> getElements() {
        List<WorldElement> elements = new ArrayList<>(grasses.values());
        for(List<Animal> animalList : animalsAlive.values()){
            elements.addAll(animalList);
        }
        return elements;
    }

    public EnergyParameters getEnergyParameters() {
        return energyParameters;
    }

    @Override
    public void placeGrass(Grass grass) {
        grasses.put(grass.getPosition(),grass);
        if(grass.isOnEquator(equator)) emptyPlacesOnEquator.remove(grass.getPosition());
        else emptyPlacesNotOnEquator.remove(grass.getPosition());
    }
    @Override
    public void removeGrass(Grass grass){
        if(grass.isOnEquator(equator)) emptyPlacesOnEquator.add(grass.getPosition());
        else emptyPlacesNotOnEquator.add(grass.getPosition());
        grasses.remove(grass.getPosition());
    }
    @Override
    public void placeAnimal(Animal animal){
        animalsAlive.get(animal.getPosition()).add(animal);
    }
    @Override
    public void removeAnimal(Animal animal) {
        animalsAlive.get(animal.getPosition()).remove(animal);
    }


    @Override
    public void move(Animal animal) {
        this.removeAnimal(animal);
        animal.move(this);
        this.placeAnimal(animal);

//        animal.changeEnergy(-energyParameters.energyToMove());
//        animal.getOlder();
    }
    @Override
    public List<Animal> animalsAt(Vector2d position) {
        return animalsAlive.get(position);
    }
    @Override
    public List<Animal> kWinners(Vector2d position, int k){
        return animalsAt(position).stream()
                .sorted()
                .limit(k)
                .toList();
    }

    @Override
    public void eatGrass(Grass grass) {
        if (!animalsAt(grass.getPosition()).isEmpty()){
            kWinners(grass.getPosition(),1).get(0).changeEnergy(energyParameters.energyFromEating());
            removeGrass(grass);
        }
    }
    @Override
    public void reproduce(Vector2d position) {
        if(animalsAt(position).size()>=2){
            List<Animal> parents = kWinners(position,2);
            if(parents.get(1).getEnergy()>=energyParameters.energyToFull()){
                // to wciaz trzeba dokodzic
                parents.get(0).changeEnergy(-energyParameters.energyToReproduce());
                parents.get(1).changeEnergy(-energyParameters.energyToReproduce());
            }
        }
    }

    @Override
    public int countGrass() {
        return grasses.size();
    }
    @Override
    public List<Vector2d> emptyPositions() {
        List<Vector2d> places = new ArrayList<>(emptyPlacesOnEquator);
        places.addAll(emptyPlacesNotOnEquator);
        return places;
    }
    public boolean canMove(Animal animal){
        Vector2d newPosition = animal.getPosition().add(animal.getOrientation().toUnitVector());
        return (newPosition.precedes(mapBorders.upperRight()) && newPosition.follows(mapBorders.lowerLeft()));
    }
    public abstract Vector2d nextPosition(Animal animal);

}
