package projects.semiDisplayOpticNet.nodes.nodeImplementations;

import java.util.ArrayList;

import projects.opticalNet.nodes.messages.HasMessage;
import projects.opticalNet.nodes.messages.NewMessage;
import projects.opticalNet.nodes.messages.OpticalNetMessage;
import projects.opticalNet.nodes.messages.RoutingInfoMessage;
import projects.opticalNet.nodes.models.Direction;
import projects.opticalNet.nodes.models.InfraNode;
import projects.opticalNet.nodes.models.Rotation;
import projects.opticalNet.nodes.nodeImplementations.NetworkController;
import projects.opticalNet.nodes.nodeImplementations.NetworkNode;

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
    private boolean seq = true;
    /**
     * Initializes the CBNetController and makes a call for it's parent constructor.
     * This constructor builds the network as a balanced BST.
     * @param numNodes      Number of nodes in the network
     * @param switchSize    Number of input/output ports in the switch
     * @param netNodes      Array with the initialized NetworkNodes
     */
    public SemiDisplayNetController (int numNodes, int switchSize, ArrayList<NetworkNode> netNodes) {
        super(numNodes, switchSize, netNodes);
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
        int numNodes, int switchSize, ArrayList<NetworkNode> netNodes, ArrayList<Integer> edgeList
    ) {
        super(numNodes, switchSize, netNodes, edgeList);
        this.projectName = "semiDisplayOpticNet";
    }

    @Override
    public void controllerStep () {
        super.controllerStep();
    }

    /**
     * Getter for the rotation a node should perfomed to rout a message to the destination node.
     * if the message is at one hop away to it's destination or to the LCA between the src node
     * and the dst node the message is simply routed one time. Else, it returns the appropriated
     * rotation based on the direction the message needs to be routed and the network topology
     * surrounding the involved nodes.
     * @param x         InfraNode with the message
     * @param dstNode   destination InfraNode
     * @return          the decided rotation
     */
    @Override
    protected Rotation getRotationToPerform (InfraNode x, InfraNode dstNode) {
        Direction direction = x.getRoutingDirection(dstNode);

        if (direction == Direction.PARENTROUT) {
            return Rotation.NULL;

        } else if (direction == Direction.LEFTROUT) {
            return Rotation.NULL;

        } else if (direction == Direction.RIGHTROUT) {
            return Rotation.NULL;

        } else if (
            direction == Direction.PARENT &&
            !(this.isValidNode(x.getParent()) && this.isValidNode(x.getParent().getParent()))
        ) {
            return Rotation.NULL;

        }

        /*bottom-up - BEGIN*/
        if (direction == Direction.PARENT) {
            InfraNode y = x.getParent();
            InfraNode z = y.getParent();
            if (
                this.isValidNode(y.getLeftChild()) && x == y.getLeftChild() &&
                this.isValidNode(z.getLeftChild()) && y == z.getLeftChild()
            ) {
                return Rotation.ZIGZIGLEFT_BOTTOMUP;

            } else if (
                this.isValidNode(y.getRightChild()) && x == y.getRightChild() &&
                this.isValidNode(z.getRightChild()) && y == z.getRightChild()
            ) {
                return Rotation.ZIGZIGRIGHT_BOTTOMUP;

            } else if (
                this.isValidNode(y.getRightChild()) && x == y.getRightChild() &&
                this.isValidNode(z.getLeftChild()) && y == z.getLeftChild()
            ) {
                return Rotation.ZIGZAGLEFT_BOTTOMUP;

            } else if (
                this.isValidNode(y.getLeftChild()) && x == y.getLeftChild() &&
                this.isValidNode(z.getRightChild()) && y == z.getRightChild()
            ) {
                return Rotation.ZIGZAGRIGHT_BOTTOMUP;

            } else {
                Tools.fatalError("Network topology for BottomUp not expected");

            }

        /* Top-Down - LEFT - BEGIN */
        } else if (direction == Direction.LEFT) {
            InfraNode y = x.getRoutingNode(dstNode);
            InfraNode z = y.getRoutingNode(dstNode);

            if (x.getLeftChild() == y && y.getLeftChild() == z) {
                return Rotation.ZIGZIGLEFT_TOPDOWN;

            } else if (x.getLeftChild() == y && y.getRightChild() == z) {
                return Rotation.ZIGZAGLEFT_TOPDOWN;

            } else {
                Tools.fatalError("Network topology for Left TopDown not expected");

            }

        /* Top-Down - RIGHT - BEGIN */
        } else if (direction == Direction.RIGHT) {
            InfraNode y = x.getRoutingNode(dstNode);
            InfraNode z = y.getRoutingNode(dstNode);

            if (x.getRightChild() == y && y.getRightChild() == z) {
                return Rotation.ZIGZIGRIGHT_TOPDOWN;

            } else if (x.getRightChild() == y && y.getLeftChild() == z) {
                return Rotation.ZIGZAGRIGHT_TOPDOWN;

            } else {
                Tools.fatalError("Network topology for Right TopDown not expected");

            }
        }

        Tools.fatalError("Unexpected rotation");

        return null;
    }

    /**
     * This method handles the message a CBNetCOntroller receives. If it is a OpticalNetMessage,
     * it means that this message has reached it's destination, so the number of completed
     * messages is incremented, the LoggerLayer reports this message information and the weigth
     * in the path between the src and destination node is updated. If it is a NewMessage the
     * number of received messages is incremented. If it is a HasMessage, the sender node is
     * marked as a nodeWithMessage for the controllerStep. If it is a RoutingInfoMessage, the
     * sender node is marked as a routerNode.
     */
    @Override
    public void handleMessages (Inbox inbox) {
        while (inbox.hasNext()) {
            Message msg = inbox.next();

            if (msg instanceof OpticalNetMessage) {
                OpticalNetMessage optmsg = (OpticalNetMessage) msg;
                this.logIncrementCompletedRequests();
                this.logMessageRouting(optmsg.getRouting());

                this.cmpMsgs++;
                this.seq = true;

            } else if (msg instanceof NewMessage) {
                this.rcvMsgs++;

            } else if (msg instanceof HasMessage) {
                HasMessage hasmsg = (HasMessage) msg;
                this.nodesWithMsg.add(hasmsg);

            } else if (msg instanceof RoutingInfoMessage) {
                RoutingInfoMessage routmsg = (RoutingInfoMessage) msg;
                this.routingNodes.add(routmsg);

            }

        }
    }

    /**
     * Getter for the seq flag.
     * @return  True if there is a message in the network false if there isn't
     */
    public boolean getSeq () {
        return this.seq;
    }

    /**
     * Sets the seq flag as false, called when a message reaches it's destination
     */
    public void setSeq () {
        this.seq = false;
    }

}
