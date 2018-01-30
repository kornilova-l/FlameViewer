const TreeDrawer = require('./TreeDrawer');

module.exports.CallTracesDrawer = class CallTracesDrawer extends TreeDrawer.TreeDrawer {
    constructor(tree) {
        super(tree);
        const visibleDepth = tree.getVisibleDepth();
        if (visibleDepth !== 0) { // if only part of tree displayed
            this.moveSectionUp();
        }
        this._assignChildIndexRecursively(tree.getBaseNode());
    }

    moveSectionUp() {
        const invisibleLayersCount = this.tree.getDepth() - this.tree.getVisibleDepth();
        this.$section.css("bottom", invisibleLayersCount * (LAYER_HEIGHT + LAYER_GAP) + "px");
    }

    /**
     * Indices are needed to get hidden nodes when node is zoomed.
     * @param node
     */
    _assignChildIndexRecursively(node) {
        const children = node.getNodesList();
        for (let i = 0; i < children.length; i++) {
            children[i].index = i;
            this._assignChildIndexRecursively(children[i]);
        }
    }

    /**
     * @override
     */
    draw() {
        super.draw();
        const $main = $("main");
        // noinspection JSValidateTypes
        $main.scrollTop($main[0].scrollHeight);
    }
};