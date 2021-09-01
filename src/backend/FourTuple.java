package backend;

import exception.PLDLAssemblingException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FourTuple {
    public int getSerial() {
        return serial;
    }

    private final int serial;

    private final Set<Symbol> symbolList;

    public Set<Symbol> getSymbol(){
        return symbolList;
    }

    private final String tuples[];

    private boolean isValid;

    public void setIsValid(boolean valid) {
        this.isValid = valid;
    }

    public boolean getIsValid() {
        return isValid;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    private BasicBlock basicBlock;

    public String[] getTuples() {
        return tuples;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public FourTuple(String tuples, int serial) throws PLDLAssemblingException {
        this.serial = serial;
        String []str = tuples.split(",");
        if (str.length != 4){
            throw new PLDLAssemblingException("四元式组成不为4", null);
        }
        for (int i = 0; i < 4; ++i){
            str[i] = str[i].trim();
        }
        this.tuples = str;
        this.symbolList = new HashSet<>();
        this.isValid = true;
    }

    @Override
    public String toString() {
        return Arrays.toString(tuples);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FourTuple fourTuple = (FourTuple) o;
        return serial == fourTuple.serial;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serial);
    }
}
