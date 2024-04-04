package projects.bstOpticalNet.nodes.infrastructureImplementations;

import java.util.ArrayList;
import java.util.Collections;

import sinalgo.tools.logging.Logging;
import sinalgo.tools.statistics.DataSeries;
import projects.bstOpticalNet.nodes.models.InfraNode;

/**
 * This abstract class is responsible to log the simulation results
 */
public abstract class LoggerLayer extends SynchronizerLayer {
    protected String projectName;

    private DataSeries rotationCounter = new DataSeries();
    private DataSeries linkAlterationCounter = new DataSeries();
    private DataSeries swtAlterationCounter = new DataSeries();
    private DataSeries messageRoutingCounter = new DataSeries();
    private DataSeries activeRequestsCounter = new DataSeries();

    private ArrayList<Long> activePortsPerSwitchRound;
    private ArrayList<Long> roundAlterationsPerSwitch;

    private long activeRequests = 0;
    private long currentRoundRotations = 0;
    private long roundCompletedRequests = 0;

    private long completedRequests = 0;

    // LOGS
    private Logging rotationLog;
    private Logging routingLog;
    private Logging alterationLog;
    private Logging activeRequestsPerRound;

    private Logging switchesActivePortsPerRound;

    private Logging routingPerMessage;

    private Logging throughputLog;

    private Logging simulationLog;
    private Logging operationsLog;

    public abstract int getNumNodes ();

    public abstract int getNumSwitches ();

    public abstract int getNumClustersType1 ();

    public abstract int getSwitchesPerClusterType1 ();

    public abstract int getNumClustersType2 ();

    public abstract int getSwitchesPerClusterType2 ();

    protected abstract int getRoutingSwitchId (InfraNode fromNode, InfraNode toNode);

    protected abstract boolean isValidNode (InfraNode node);

    /* Logger Functions */

    /**
     * This functions logs the round results. It logs the rotation, alteration and
     * routings performed this round, as well as this metrics distributed over the
     * NetworkNode's and Switches.
     */
    @Override
    public void logRoundResults () {
        this.rotationLog.logln(this.projectName + "," + this.currentRoundRotations);
        this.activeRequestsPerRound.logln(this.projectName + "," + this.activeRequests);
        this.activeRequestsCounter.addSample(this.activeRequests);

        this.logSparceLogger(this.switchesActivePortsPerRound, this.activePortsPerSwitchRound);

        int roundSwitchAlterations = 0;
        for (int swtIdx = 0; swtIdx < this.roundAlterationsPerSwitch.size(); swtIdx++) {
            roundSwitchAlterations += this.roundAlterationsPerSwitch.get(swtIdx);
        }
        this.swtAlterationCounter.addSample(roundSwitchAlterations);

        if (this.roundCompletedRequests > 0) {
            this.throughputLog.logln(
                this.projectName + "," + this.getCurrentRound() + "," + this.roundCompletedRequests
            );
        }

        this.resetRoundInfo();

    }

    /**
     * Logs the results stored on the rotation, alteration, routing and
     * active requests counter on the operations.txt file.
     */
    public void logEndOfSimulation () {
        this.operationsLog.logln("name,sum,mean,std_dvt,min,max");
        this.printRotationCounter();
        this.printSwtAlterationCounter();
        this.printLinkAlterationCounter();
        this.printMessageRoutingCounter();
        this.printActiveRequestsCounter();

        this.printSimulationInfo();
    }

    /* End of Logger Functions */

    /* Setter Functions */

    /**
     * Increment round actions per node or switch as a sparse matrix to save space.
     * @param logger        Logging object for the parameter
     * @param logArray      Array containing information about the round
     */
    private void logSparceLogger (Logging logger, ArrayList<Long> logArray) {
        for (int indx = 0; indx < logArray.size(); indx++) {
            Long value = logArray.get(indx);
            if (value != 0) {
                logger.logln(
                    this.projectName + "," + this.getCurrentRound() + "," + indx + "," + value
                );
            }

        }

    }

    /**
     * Increment the number of active ports on the switch_swtid. Active ports
     * are ports where the connection InputNode -> OutputNode are representatives
     * of a real edge on our graph.
     * @param swtId         the switch id
     */
    public void logIncrementActivePorts (int swtId) {
        long value = this.activePortsPerSwitchRound.get(swtId);
        this.activePortsPerSwitchRound.set(swtId, value + 1);

    }

    /**
     * Decrement the number of active ports on the switch_swtid. Active ports
     * are ports where the connection InputNode -> OutputNode are representatives
     * of a real edge on our graph.
     * @param swtId         the switch id
     */
    public void logDecrementActivePorts (int swtId) {
        long value = this.activePortsPerSwitchRound.get(swtId);
        this.activePortsPerSwitchRound.set(swtId, value - 1);

    }

    /**
     * Increment the number of alterations performed on a given round, updating this counter
     * for the parent node, child node and switch involved. An alteration occurs when you remove
     * one edge on our graph and add another.
     * @param swtId         the switch id
     * @param fromNode      the node connected to the input port
     */
    public void logIncrementAlterations (int swtId, InfraNode fromNode) {
        this.linkAlterationCounter.addSample(1);
        this.roundAlterationsPerSwitch.set(swtId, 1L);

        this.alterationLog.logln(
            this.projectName + "," + this.getCurrentRound() + "," + fromNode.getId() + "," + swtId
        );
    }

    /**
     * Increment the number of routings that the nodes fromNode, toNode and the switch that
     * implements their edge are involed in this round.
     * @param netFromNodeId the node that started the routing
     * @param netToNodeId   the node that received the message
     */
    public void logIncrementRouting (InfraNode fromNode, InfraNode toNode) {
        int swtId = this.getRoutingSwitchId(fromNode, toNode);

        this.routingLog.logln(
            this.projectName + "," + this.getCurrentRound() + "," +
            fromNode.getId() + "," + toNode.getId() + "," + swtId
        );
    }

    /**
     * Update the message-routing counter with the number of times a message has been
     * routed.
     * @param num   how many times the message was routed
     */
    public void logMessageRouting (long num) {
        this.messageRoutingCounter.addSample(num);
        this.routingPerMessage.logln(this.projectName + "," + num);

    }

    /**
     * Update rotation counter and the number of rotations the nodesInvolved performed
     * @param num           how many rotations this call represents
     * @param nodesInvolved which nodes are involved in it
     */
    public void logRotation (long num) {
        this.rotationCounter.addSample(num);
        this.currentRoundRotations += num;
    }

    /**
     * Increment the number of active requests for the round
     */
    public void logIncrementActiveRequests () {
        this.activeRequests++;

    }

    /**
     * Increment the number of completed requests for the round
     */
    public void logIncrementCompletedRequests () {
        this.roundCompletedRequests++;
        this.completedRequests++;

    }

    /* End of Setters Functions */

    /* Getters */
    /**
     * Getter for the number of completed requests
     * @return The number of completed requests so far
     */
    public long getCompletedRequests () {
        return completedRequests;

    }

    /**
     * Getter for the alteration counter
     * @return The alteration Counter
     */
    public DataSeries getAlterationCounterSeries () {
        return this.linkAlterationCounter;

    }

    /**
     * Getter for the rotation counter
     * @return The rotation Counter
     */
    public DataSeries getRotationCounterSeries () {
        return this.rotationCounter;

    }

    /* End of Getters */

    /* Printer Functions */
    /**
     * Print Active Requests counter informations.
     */
    public void printActiveRequestsCounter () {
        this.printCounter(this.activeRequestsCounter, "active-requests");

    }

    /**
     * Print Link Alteration counter informations.
     */
    public void printLinkAlterationCounter () {
        this.printCounter(this.linkAlterationCounter, "link-alteration");
    }

    /**
     * Print Switch Alteration counter informations.
     */
    public void printSwtAlterationCounter () {
        this.printCounter(this.swtAlterationCounter, "swt-alteration");
    }

    /**
     * Print Rotation counter informations.
     */
    public void printRotationCounter () {
        this.printCounter(this.rotationCounter, "rotation");

    }

    /**
     * Print MessageRouting counter informations.
     */
    public void printMessageRoutingCounter () {
        this.printCounter(this.messageRoutingCounter, "message-routing");

    }

    /**
     * Print the information of counter in the order: (sum, mean, std, min, max).
     * @param counter       the DataSeries counter
     * @param operation     the identification string of the counter
     */
    public void printCounter (DataSeries counter, String operation) {
        System.out.println(operation);
        System.out.println("Number of request " + counter.getNumberOfSamples());
        System.out.println("Number of operations " + counter.getSum());
        System.out.println("Mean: " + counter.getMean());
        System.out.println(
            "Standard Deviation: " + counter.getStandardDeviation()
        );
        System.out.println("Min: " + counter.getMinimum());
        System.out.println("Max: " + counter.getMaximum());

        this.operationsLog.logln(
            operation + "," +
            counter.getSum() + "," +
            counter.getMean() + "," +
            counter.getStandardDeviation() + "," +
            counter.getMinimum() + "," +
            counter.getMaximum()
        );

    }

    /**
     * Reports the simulation info, the number and size of clusters of type 1 and 2.
     */
    public void printSimulationInfo () {
        this.simulationLog.logln("num-rounds," + this.getCurrentRound());
        this.simulationLog.logln(
            "num-cluster-1," + this.getNumClustersType1() +
            ",size-cluster-1," + this.getSwitchesPerClusterType1()
        );
        this.simulationLog.logln(
            "num-cluster-2," + this.getNumClustersType2() +
            ",size-cluster-2," + this.getSwitchesPerClusterType2()
        );
    }

    /* End of Printer Functions */

    /* Auxiliary Functions */

    /**
     * Configure the path of the logger files with the path provided by the output parameter
     * in the simulation.
     * @param path      path to folder where the logger files are stored
     */
    public void setLogPath (String path) {
        this.rotationLog = Logging.getLogger(path + "/rotations.csv");
        this.routingLog = Logging.getLogger(path + "/routings.csv");
        this.alterationLog = Logging.getLogger(path + "/alterations.csv");
        this.activeRequestsPerRound = Logging.getLogger(path + "/active_requests_per_round.csv");

        this.switchesActivePortsPerRound = Logging.getLogger(
            path + "/switches_active_ports_per_round.csv"
        );

        this.routingPerMessage = Logging.getLogger(path + "/routing_per_message.csv");

        this.simulationLog = Logging.getLogger(path + "/simulation_info.txt");
        this.throughputLog = Logging.getLogger(path + "/throughput.csv");
        this.operationsLog = Logging.getLogger(path + "/operations.csv");

        this.initSimulationLog();
    }

    /**
     * Prints the collumn names for th log files
     */
    public void initSimulationLog () {
        this.rotationLog.logln("project,rotation");

        this.routingLog.logln("project,round,from_node,to_node,switch");
        this.alterationLog.logln("project,round,node,switch");

        this.switchesActivePortsPerRound.logln("project,round,switch,active_ports");

        this.routingPerMessage.logln("project,times");
        this.throughputLog.logln("project,round,completed_requests");
        this.activeRequestsPerRound.logln("project,active_requests");
    }

    /* End of Auxiliary Functions */

    /* Reset counter Functions */
    /**
     * Called by the end of round to reset round related information.
     */
    public void resetRoundInfo () {
        this.activePortsPerSwitchRound = new ArrayList<>(
            Collections.nCopies(this.getNumSwitches(), 0L)
        );
        this.roundAlterationsPerSwitch = new ArrayList<>(
            Collections.nCopies(this.getNumSwitches(), 0L)
        );

        this.activeRequests = 0;
        this.currentRoundRotations = 0;
        this.roundCompletedRequests = 0;

    }
    /* End of Reset counter Functions */
}
