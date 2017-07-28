package com.github.kornilova_l.flamegraph.plugin.server.trees.ser_trees.accumulative_trees.outgoing_calls;

import com.github.kornilova_l.flamegraph.plugin.server.trees.TreeBuilder;
import com.github.kornilova_l.flamegraph.proto.TreeProtos;
import com.github.kornilova_l.flamegraph.proto.TreesProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.kornilova_l.flamegraph.plugin.server.trees.ser_trees.accumulative_trees.AccumulativeTreesHelper.*;

public final class OutgoingCallsBuilder implements TreeBuilder {
    private TreeProtos.Tree.Builder treeBuilder;
    @Nullable private final TreeProtos.Tree tree;
    private int maxDepth = 0;

    public OutgoingCallsBuilder(@NotNull TreesProtos.Trees callTrees) {
        initTreeBuilder();
        for (TreeProtos.Tree callTree : callTrees.getTreesList()) {
            addTree(treeBuilder.getBaseNodeBuilder(), callTree.getBaseNode());
        }
        if (treeBuilder.getBaseNode().getNodesCount() == 0) {
            tree = null;
            return;
        }
        setNodesOffsetRecursively(treeBuilder.getBaseNodeBuilder(), 0);
        setTreeWidth(treeBuilder);
        treeBuilder.setDepth(maxDepth);
        tree = treeBuilder.build();
    }

    @Nullable
    public TreeProtos.Tree getTree() {
        return tree;
    }

    private void initTreeBuilder() {
        treeBuilder = TreeProtos.Tree.newBuilder()
                .setBaseNode(TreeProtos.Tree.Node.newBuilder());
    }

    private void addTree(TreeProtos.Tree.Node.Builder baseNodeInOC,
                                TreeProtos.Tree.Node baseNodeInCT) {
        for (TreeProtos.Tree.Node childNodeInCT : baseNodeInCT.getNodesList()) {
            addNodesRecursively(baseNodeInOC, childNodeInCT, 0);
        }
    }

    private void addNodesRecursively(TreeProtos.Tree.Node.Builder nodeBuilder, // where to append child
                                            TreeProtos.Tree.Node node, // from where get method and it's width
                                            int depth) {
        depth++;
        if (depth > maxDepth) {
            maxDepth = depth;
        }
        nodeBuilder = updateNodeList(nodeBuilder, node, -1);
        for (TreeProtos.Tree.Node childNode : node.getNodesList()) {
            addNodesRecursively(nodeBuilder, childNode, depth);
        }
    }
}
