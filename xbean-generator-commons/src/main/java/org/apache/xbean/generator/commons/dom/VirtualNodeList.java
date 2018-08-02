package org.apache.xbean.generator.commons.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;

public class VirtualNodeList implements NodeList {

    private final List<Node> nodes;

    public VirtualNodeList(Node ... nodes) {
        this(Arrays.asList(nodes));
    }

    public VirtualNodeList(List<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Node item(int index) {
        return nodes.get(index);
    }

    @Override
    public int getLength() {
        return nodes.size();
    }

}
