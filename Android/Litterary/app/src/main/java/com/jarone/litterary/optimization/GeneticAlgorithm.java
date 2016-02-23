/*
* GA.java
* Manages algorithms for evolving population
*/

package com.jarone.litterary.optimization;

public class GeneticAlgorithm {
    /* GeneticAlgorithm parameters */
    private static final double mutationRate = 0.015;
    private static final int tournamentSize = 5;
    private static final boolean elitism = true;

    // Evolves a population over one generation
    public static Population evolvePopulation(Population pop) {
        Population newPopulation = new Population(pop.populationSize(), false);
        // Keep our best individual if elitism is enabled
        int elitismOffset = 0;
        if (elitism) {
            newPopulation.saveRoute(0, pop.getFittest());
            elitismOffset = 1;
        }

        // Crossover population
        // Loop over the new population's size and create individuals from
        // Current population
        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            // Select parents
            Route parent1 = tournamentSelection(pop);
            Route parent2 = tournamentSelection(pop);
            // Crossover parents
            Route child = crossover(parent1, parent2);
            // Add child to new population
            newPopulation.saveRoute(i, child);
        }

        // Mutate the new population a bit to add some new genetic material
        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            mutate(newPopulation.getRoute(i));
        }

        return newPopulation;
    }

    // Applies crossover to a set of parents and creates offspring
    public static Route crossover(Route parent1, Route parent2) {
        // Create new child route
        Route child = new Route();

        // Get start and end sub route positions for parent1's route
        int startPos = (int) (Math.random() * parent1.routeSize());
        int endPos = (int) (Math.random() * parent1.routeSize());

        // Loop and add the sub route from parent1 to our child
        for (int i = 0; i < child.routeSize(); i++) {
            // If our start position is less than the end position
            if (startPos < endPos && i > startPos && i < endPos) {
                child.setPhotoPoint(i, parent1.getPhotoPoint(i));
            } // If our start position is larger
            else if (startPos > endPos) {
                if (!(i < startPos && i > endPos)) {
                    child.setPhotoPoint(i, parent1.getPhotoPoint(i));
                }
            }
        }

        // Loop through parent2's photoPoint route
        for (int i = 0; i < parent2.routeSize(); i++) {
            // If child doesn't have the photoPoint add it
            if (!child.containsPhotoPoint(parent2.getPhotoPoint(i))) {
                // Loop to find a spare position in the child's route
                for (int ii = 0; ii < child.routeSize(); ii++) {
                    // Spare position found, add photoPoint
                    if (child.getPhotoPoint(ii) == null) {
                        child.setPhotoPoint(ii, parent2.getPhotoPoint(i));
                        break;
                    }
                }
            }
        }
        return child;
    }

    // Mutate a route using swap mutation
    private static void mutate(Route route) {
        // Loop through route cities
        for(int routePos1 = 0; routePos1 < route.routeSize(); routePos1++){
            // Apply mutation rate
            if(Math.random() < mutationRate){
                // Get a second random position in the route
                int routePos2 = (int) (route.routeSize() * Math.random());

                // Get the cities at target position in route
                PhotoPoint point1 = route.getPhotoPoint(routePos1);
                PhotoPoint point2 = route.getPhotoPoint(routePos2);

                // Swap them around
                route.setPhotoPoint(routePos2, point1);
                route.setPhotoPoint(routePos1, point2);
            }
        }
    }

    // Selects candidate route for crossover
    private static Route tournamentSelection(Population pop) {
        // Create a tournament population
        Population tournament = new Population(tournamentSize, false);
        // For each place in the tournament get a random candidate route and
        // add it
        for (int i = 0; i < tournamentSize; i++) {
            int randomId = (int) (Math.random() * pop.populationSize());
            tournament.saveRoute(i, pop.getRoute(randomId));
        }
        // Get the fittest route
        return tournament.getFittest();
    }
}