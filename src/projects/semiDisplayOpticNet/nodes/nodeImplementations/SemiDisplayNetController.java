package projects.semiDisplayOpticNet.nodes.nodeImplementations;

import java.util.ArrayList;

import projects.bstOpticalNet.nodes.messages.HasMessage;
import projects.bstOpticalNet.nodes.messages.NewMessage;
import projects.bstOpticalNet.nodes.messages.OpticalNetMessage;
import projects.bstOpticalNet.nodes.messages.RoutingInfoMessage;
import projects.bstOpticalNet.nodes.models.Direction;
import projects.bstOpticalNet.nodes.models.InfraNode;
import projects.bstOpticalNet.nodes.models.Rotation;
import projects.bstOpticalNet.nodes.nodeImplementations.NetworkController;
import projects.bstOpticalNet.nodes.nodeImplementations.NetworkNode;

import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.Tools;

/**
 * The CBNetController implements the remaining abstract methods left by the NetworkControllers
 * it's constructor calls it's parent class constructor. This layer manages the management of the
 * node weight and counter, and uses as an extra check before performing a rotation.
 * This class implements the blunt of the CBNet algortithm over the OpticalNet framework.
 */
public class SemiDisplayNetController extends NetworkController {
    /**
     * Initializes the CBNetController and makes a call for it's parent constructor.
     * This constructor builds the network as a balanced BST.
     * @param numNodes      Number of nodes in the network
     * @param switchSize    Number of input/output ports in the switch
     * @param netNodes      Array with the initialized NetworkNodes
     */
    public SemiDisplayNetController (
    		int numNodes, int switchSize, ArrayList<NetworkNode> netNodes, boolean mirrored
    ) {
        super(numNodes, switchSize, netNodes, mirrored);
        this.projectName = "semiDisplayOpticNet";
    }

    /**
     * Initializes the CBNetController and makes a call for it's parent constructor. If an
     * edgeList is provided the tree topology follow the specified one. If the edge list
     * can't build an BST, the constructor builds a balanced BST instead.
     * @param numNodes      Number of nodes in the network
     * @param switchSize    Number of input/output ports in the switch
     * @param netNodes      Array with the initialized NetworkNodes
     * @param edgeList      Array with the network edges, if provided.
     */
    public SemiDisplayNetController (
        int numNodes, int switchSize, ArrayList<NetworkNode> netNodes, ArrayList<Integer> edgeList, boolean mirrored
    ) {
        super(numNodes, switchSize, netNodes, edgeList, mirrored);
        this.projectName = "semiDisplayOpticNet";
    }

    @Override
    public void controllerStep () {
        super.controllerStep();
    }

}
