package projects.semiDisplayOpticNet.nodes.nodeImplementations;

import java.util.ArrayList;

import projects.bstOpticalNet.nodes.nodeImplementations.NetworkController;
import projects.bstOpticalNet.nodes.nodeImplementations.NetworkNode;

import projects.bstOpticalNet.nodes.messages.HasMessage;
import projects.bstOpticalNet.nodes.messages.RoutingInfoMessage;
import projects.bstOpticalNet.nodes.models.Rotation;
import projects.bstOpticalNet.nodes.models.Direction;
import projects.bstOpticalNet.nodes.models.InfraNode;
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
            Direction parentDir = y.getRoutingDirection(dstNode);

            if (parentDir != Direction.PARENT) {
                return Rotation.ZIG_BOTTOMUP;

            } else if (
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
        } else if (direction == Direction.LEFT) {
            InfraNode y = x.getRoutingNode(dstNode);
            InfraNode z = y.getRoutingNode(dstNode);

            if (x.getLeftChild() == y && y.getLeftChild() == z) {
                return Rotation.SEMI_ZIGZIGLEFT_TOPDOWN;

            } else if (x.getLeftChild() == y && y.getRightChild() == z) {
                return Rotation.SEMI_ZIGZAGLEFT_TOPDOWN;

            } else {
                Tools.fatalError("Network topology for Left TopDown not expected");

            }

        /* Top-Down - RIGHT - BEGIN */
        } else if (direction == Direction.RIGHT) {
            InfraNode y = x.getRoutingNode(dstNode);
            InfraNode z = y.getRoutingNode(dstNode);

            if (x.getRightChild() == y && y.getRightChild() == z) {
                return Rotation.SEMI_ZIGZIGRIGHT_TOPDOWN;

            } else if (x.getRightChild() == y && y.getLeftChild() == z) {
                return Rotation.SEMI_ZIGZAGRIGHT_TOPDOWN;

            } else {
                Tools.fatalError("Network topology for Right TopDown not expected");

            }
        }

        Tools.fatalError("Unexpected rotation");
        return Rotation.NULL;
    }

    @Override
    protected void updateConn () {
        this.lockRoutingNodes();

        while (!this.nodesWithMsg.isEmpty()) {
            HasMessage hasmsg = this.nodesWithMsg.poll();
            int nodeId = hasmsg.getCurrId();

            InfraNode node = this.getInfraNode(nodeId);
            InfraNode dstNode = this.getInfraNode(hasmsg.getDst());

            Rotation op = this.getRotationToPerform(node, dstNode);

            switch (op) {
                case NULL:
                    this.allowRouting(node, dstNode, 1);
                    break;

                case ZIG_BOTTOMUP:
                	if (this.zigBottomUp(node)) {
                		System.out.println("zigBottomUp");
                        this.logIncrementActiveRequests();

                	}
                    break;

                case ZIGZIGLEFT_BOTTOMUP:
                case ZIGZIGRIGHT_BOTTOMUP:
                    if (this.zigZigBottomUp(node)) {
                        System.out.println("semiZigZigBottomUp");
                        this.logIncrementActiveRequests();

                    }
                    break;

                case ZIGZAGLEFT_BOTTOMUP:
                case ZIGZAGRIGHT_BOTTOMUP:
                    if (this.zigZagBottomUp(node)) {
                        System.out.println("zigZagBottomUp");
                        this.logIncrementActiveRequests();

                    }
                    break;

                case SEMI_ZIGZIGLEFT_TOPDOWN:
                    if (this.semiZigZigLeftTopDown(node)) {
                        System.out.println("semiZigZigLeftTopDown");
                        this.logIncrementActiveRequests();

                        InfraNode rfrshNode = this.getInfraNode(nodeId);
                        InfraNode nxtNode = rfrshNode.getRoutingNode(dstNode);

                        this.configureRoutingMessage(rfrshNode, nxtNode, new RoutingInfoMessage(2));

                    }
                    break;

                case SEMI_ZIGZAGLEFT_TOPDOWN:
                    if (this.zigZagLeftTopDown(node)) {
                        System.out.println("semiZigZagLeftTopDown");
                        this.logIncrementActiveRequests();

                        InfraNode rfrshNode = this.getInfraNode(nodeId);
                        InfraNode nxtNode = rfrshNode.getRoutingNode(dstNode);

                        if (nxtNode == rfrshNode.getParent()) {
                            this.configureRoutingMessage(
                                rfrshNode, nxtNode, new RoutingInfoMessage(3)
                            );

                        } else {
                            this.configureRoutingMessage(
                                rfrshNode, nxtNode, new RoutingInfoMessage(1)
                            );

                        }

                    }
                    break;

                case SEMI_ZIGZIGRIGHT_TOPDOWN:
                    if (this.semiZigZigRightTopDown(node)) {
                        System.out.println("semiZigZigRightTopDown");
                        this.logIncrementActiveRequests();

                        InfraNode rfrshNode = this.getInfraNode(nodeId);
                        InfraNode nxtNode = rfrshNode.getRoutingNode(dstNode);

                        this.configureRoutingMessage(rfrshNode, nxtNode, new RoutingInfoMessage(2));

                    }
                    break;

                case SEMI_ZIGZAGRIGHT_TOPDOWN:
                    if (this.zigZagRightTopDown(node)) {
                        System.out.println("semiZigZagRightTopDown");
                        this.logIncrementActiveRequests();

                        InfraNode rfrshNode = this.getInfraNode(nodeId);
                        InfraNode nxtNode = rfrshNode.getRoutingNode(dstNode);

                        if (nxtNode == rfrshNode.getParent()) {
                            this.configureRoutingMessage(
                                rfrshNode, nxtNode, new RoutingInfoMessage(3)
                            );

                        } else {
                            this.configureRoutingMessage(
                                rfrshNode, nxtNode, new RoutingInfoMessage(1)
                            );

                        }

                    }
                    break;

                default:
                	Tools.fatalError("Rotation not treated");
                    break;
            }

            this.areAvailableNodes(node);
        }
    }
}