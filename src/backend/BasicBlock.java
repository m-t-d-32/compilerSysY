package backend;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
    private final List<BasicBlock> parents;
    private final List<BasicBlock> children;

    public List<BasicBlock> getParents() {
        return parents;
    }

    public List<BasicBlock> getChildren() {
        return children;
    }

    public void addFourTuple(FourTuple fourTuple){
        fourTuple.setBasicBlock(this);
        fourTuples.add(fourTuple);
    }

    public List<FourTuple> getFourTuples() {
        return fourTuples;
    }

    public String getLabel() {
        return labelname;
    }

    public String getFuncname() {
        return funcname;
    }

    private final List<FourTuple> fourTuples;
    private final String labelname;
    private final String funcname;

    BasicBlock(String funcname, String labelname){
        this.labelname = labelname;
        this.funcname = funcname;
        parents = new ArrayList<>();
        children = new ArrayList<>();
        fourTuples = new ArrayList<>();
    }
}
