package com.jarone.litterary;

import org.nd4j.linalg.api.ndarray.INDArray;

class SalesmanSolver {

    static {
        System.loadLibrary("jniortools");
    }

    static class GPSPhotoIndex {
        public GPSPhotoIndex(INDArray array) {
            xs = array.getColumn(1).data().asInt();
            ys = array.getColumn(2).data().asInt();

        }

        public long run(int firstIndex, int secondIndex) {
            return Math.abs(xs[firstIndex] - xs[secondIndex]) +
                    Math.abs(ys[firstIndex] - ys[secondIndex]);
        }

        private int[] xs;
        private int[] ys;
    }

    static void solve(INDArray GPSPhotoLocations) {
//        RoutingModel routing = new RoutingModel(GPSPhotoLocations.rows(), 1);
//        // Setting first solution heuristic (cheapest addition).
//        routing.setFirstSolutionStrategy(RoutingModel.ROUTING_PATH_CHEAPEST_ARC);
//
//        // Setting the cost function.
//        // Put a permanent callback to the distance accessor here. The callback
//        // has the following signature: ResultCallback2<int64, int64, int64>.
//        // The two arguments are the from and to node inidices.
//        GPSPhotoIndex distances = new GPSPhotoIndex(GPSPhotoLocations);
//        routing.setCost(distances);
//
//        // Solve, returns a solution if any (owned by RoutingModel).
//        Assignment solution = routing.solve();
//        if (solution != null) {
//            // Solution cost.
//            System.out.println("Cost = " + solution.objectiveValue());
//            // Inspect solution.
//            // Only one route here; otherwise iterate from 0 to routing.vehicles() - 1
//            int route_number = 0;
//            for (long node = routing.start(route_number);
//                 !routing.isEnd(node);
//                 node = solution.value(routing.nextVar(node))) {
//                System.out.print("" + node + " -> ");
//            }
//            System.out.println("0");
//        }
    }
}
