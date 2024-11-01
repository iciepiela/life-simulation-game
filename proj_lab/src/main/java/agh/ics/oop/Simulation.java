package agh.ics.oop;

import agh.ics.oop.model.*;
import agh.ics.oop.model.elements.*;
import agh.ics.oop.model.maps.*;
import agh.ics.oop.model.observers.MapChangeListener;
import agh.ics.oop.model.util.RandomPositionsGenerator;
import agh.ics.oop.parameters.SimulationParameters;

import java.io.FileNotFoundException;
import java.util.*;


public class Simulation implements Runnable {
    private final List<MapChangeListener> observers = new ArrayList<>();
    private final ArrayList<Animal> animalsAlive = new ArrayList<>(0);
    private final List<Animal> animalsDead = new ArrayList<>();
    private final Map<Genome,List<Animal>> genomeList = new HashMap<>();
    private final WorldMap map;
    private final SimulationParameters simulationParameters;
    private final RandomPositionsGenerator randomPositionsGenerator = new RandomPositionsGenerator();
    private boolean isRunning = false;
    private int dayCounter = 1;

    private float averageEnergy;
    private float averageLifespan;
    private Genome mostPopularGenome;
    private float averageChildrenCount;
    private int deadAnimalsCount;
  
    public Simulation(SimulationParameters simulationParameters) throws FileNotFoundException {
        this.simulationParameters = simulationParameters;
        map = simulationParameters.getMap();


        placeAnimals();

        ArrayList<Vector2d> grassPositions = randomPositionsGenerator.kPositionsNoRepetition
                (map.getMapBorders().allPositions(), simulationParameters.startingGrassAmount());

        for(Vector2d position: grassPositions) { //rozkladanie trawy na mapie
            Grass grass = new Grass(position);
            this.map.placeGrass(grass);
        }
        this.updateStats();
    }
    public void addObserver(MapChangeListener observer){
        observers.add(observer);
    }

    private void mapChanged(String message){
        for(MapChangeListener observer: observers){
            observer.mapChanged(this,message);
        }
    }
    public boolean isRunning(){
        return isRunning;
    }

    public void placeAnimals(){
        ArrayList<Vector2d> animalPositions = randomPositionsGenerator.kPositionsWithRepetition
                (map.getMapBorders().allPositions(), simulationParameters.startingAnimalAmount());

        for(Vector2d position: animalPositions){
            Animal animal = new Animal(simulationParameters.getGenome(),
                    position,
                    simulationParameters.energyParameters().startingEnergy());
            this.map.placeAnimal(animal);
            animalsAlive.add(animal);
            addGenome(animal);
        }
    }

    public WorldMap getMap() {
        return map;
    }

    public void removeIfDead(Animal animal) {
        if (animal.getEnergy()<=0){
            this.animalsAlive.remove(animal);
            map.removeAnimal(animal);
            animalsDead.add(animal);
            animal.setDeathDay(dayCounter);
            System.out.println("Zwierze umarlo na pozycji "+animal.getPosition()+". Żyło "+animal.getLifeSpan());
        }
    }
    public void reproduce(Vector2d position){
        if(map.animalsAt(position).size()>=2){
            List<Animal> parents = map.kWinners(position,2);
            if(parents.get(1).getEnergy()>=simulationParameters.energyParameters().energyToFull()){
                Animal child = new Animal(parents.get(0),parents.get(1),simulationParameters);
                map.placeAnimal(child);
                animalsAlive.add(child);

                addGenome(child);
                System.out.println("Nowe zwierze na pozycji: "+child.getPosition()+", "+child.getGenome());
            }
        }
    }
    public void addGenome(Animal animal){
        Genome genome = animal.getGenome();
        if (!genomeList.containsKey(genome)){
            genomeList.put(genome,new ArrayList<>());
        }
        genomeList.get(genome).add(animal);
    }
    public int getDayCounter(){
        return dayCounter;
    }
    public float getAverageEnergy() {
        return averageEnergy;
    }
    public float getAverageLifespan() {
        return averageLifespan;
    }
    public Genome getMostPopularGenome() {
        return mostPopularGenome;
    }
    public float getAverageChildrenCount() {
        return averageChildrenCount;
    }
    public int getDeadAnimalsCount() {
        return deadAnimalsCount;
    }
    public SimulationParameters getSimulationParameters(){
        return this.simulationParameters;
    }

    private void updateMostPopularGenome() {
        Genome resGenome = new DefaultGenome(1);
        int currMaxSize = 0;
        for(Genome genome : genomeList.keySet() ){
            if (genomeList.get(genome).size() > currMaxSize) {
                currMaxSize = genomeList.get(genome).size();
                resGenome = genome;
            }
        }
        this.mostPopularGenome=resGenome;
    }
    private void updateAverageEnergy() {
        if (animalsAlive.isEmpty()) this.averageEnergy = 0;
        else this.averageEnergy = (float)animalsAlive.stream().mapToInt(Animal::getEnergy).sum()/animalsAlive.size();
    }
    private void updateAverageLifeSpan() {
        if (animalsDead.isEmpty()) this.averageLifespan= 0;
        else this.averageLifespan =  (float) animalsDead.stream().mapToInt(Animal::getLifeSpan).sum()/animalsDead.size();
    }
    private void updateAverageChildrenCount() {
        if(animalsAlive.isEmpty()) this.averageChildrenCount = 0;
        else this.averageChildrenCount = (float) animalsAlive.stream()
                .mapToInt(animal -> animal.getChildren().size())
                .sum() /animalsAlive.size();
    }
    private void updateDeadAnimalsCount(){
        this.deadAnimalsCount = animalsDead.size();
    }
    private void updateStats(){
        updateDeadAnimalsCount();
        updateAverageChildrenCount();
        updateAverageEnergy();
        updateAverageLifeSpan();
        updateMostPopularGenome();
    }
    public int getAliveAnimalsCount(){
        return animalsAlive.size();
    }

    public List<Animal> animalsWithMostPopularGenome(){
        return genomeList.get(this.mostPopularGenome);
    }
    public void startRunning(){
        this.isRunning = true;
    }
    public void stopRunning(){
        this.isRunning = false;
    }
    @Override
    public void run() {
        while(isRunning) {
            mapChanged("Dzień: "+this.dayCounter);
            System.out.println("Dzień: "+this.dayCounter);
//            if(this.dayCounter%25 == 0) System.out.println(getStatistics());

            //usuniecie martwych zwierzakow
            new ArrayList<>(animalsAlive).forEach(this::removeIfDead);

            //ruszanie sie zwierzakow
            animalsAlive.forEach(map::move);

            //jedzenie roslin
            new ArrayList<>(map.getGrasses().values()).forEach(map::eatGrass);

            //rozmnazanie sie najedzonych zwierzakow
            map.getMapBorders().allPositions().forEach(this::reproduce); // do sprawdzenia

            //nowe rosliny
            map.growGrass(simulationParameters.dailyGrassGrowth());



            this.dayCounter+=1;

            this.updateStats();

                try {
                    if (isRunning) {
                        Thread.sleep(700);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
