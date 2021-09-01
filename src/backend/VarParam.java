package backend;

import exception.PLDLAssemblingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

abstract class Symbol {
    public static final int INT_TYPE_LENGTH = 4;

    public boolean getIsDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    private boolean isDefined = false;  //是否真正定义了（第二遍遍历）
    private String innername;
    private String outername;
    private int offset;
    private boolean isGlobal;

    public boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(boolean valid) {
        isValid = valid;
    }

    private boolean isValid = false; //是否有用

    public List<FourTuple> getFourTuple() {
        return fourTuples;
    }

    public List<Symbol> getParents(){
        return parents;
    }

    public void addToParents(Symbol s){
        parents.add(s);
    }

    public void addToParents(Collection<Symbol> s){
        parents.addAll(s);
    }

    private List<FourTuple> fourTuples = new ArrayList<>();

    private List<Symbol> parents = new ArrayList<>();

    abstract boolean getIsVar();

    abstract TypeName getType();

    abstract int getSize();

    abstract boolean hasOffset();

    void setOffset(int offset) throws PLDLAssemblingException {
        this.offset = offset;
    }

    int getOffset() throws PLDLAssemblingException {
        return offset;
    }

    String getInnername() {
        return innername;
    }

    void setInnername(String innername) {
        this.innername = innername;
    }

    String getOutername() {
        return outername;
    }

    void setOutername(String outername) {
        this.outername = outername;
    }

    boolean getIsGlobal() {
        return isGlobal;
    }

    void setIsGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public boolean getIsInRegister() {
        return isInRegister;
    }

    public void setInRegister(boolean inRegister) {
        isInRegister = inRegister;
    }

    boolean isInRegister = false;

    public String getRegister() {
        return register;
    }

    public void setRegister(String register) {
        this.register = register;
    }

    String register = null;

    private String getSymbolArrayString(List<Symbol> symbols){
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol: symbols){
            sb.append(symbol.getOutername()).append(" ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "isValid=" + isValid +
                ", isDefined=" + isDefined +
                ", innername='" + innername + '\'' +
                ", outername='" + outername + '\'' +
                ", offset=" + offset +
                ", isGlobal=" + isGlobal +
                ", parents=" + getSymbolArrayString(parents) +
                ", isInRegister=" + isInRegister +
                ", register='" + register + '\'' +
                '}';
    }
}

abstract class Var extends Symbol {
    @Override
    boolean getIsVar() {
        return true;
    }

    @Override
    boolean hasOffset() {
        return true;
    }
}

abstract class Const extends Symbol {
    boolean getIsVar() {
        return false;
    }
}

class IntVar extends Var {
    TypeName getType() {
        return TypeName.INT;
    }

    int getSize() {
        return INT_TYPE_LENGTH;
    }
}

class IntArrayVar extends Var {
    private Integer[] lengths;

    TypeName getType() {
        return TypeName.INTARRAY;
    }

    int getSize() {
        int size = INT_TYPE_LENGTH;
        for (Integer length : lengths) {
            size *= length;
        }
        return size;
    }

    Integer[] getLengths() {
        return lengths;
    }

    void setLengths(Integer[] lengths) {
        this.lengths = lengths;
    }
}

class IntConst extends Const {
    private int val;

    TypeName getType() {
        return TypeName.INT;
    }

    boolean hasOffset() {
        return true;
    }

    int getSize() {
        return INT_TYPE_LENGTH;
    }
    int getVal() {
        return val;
    }

    void setVal(int val) {
        this.val = val;
    }
}

class ImmediateInt extends IntConst {
    @Override
    boolean hasOffset(){
        return false;
    }

    @Override
    void setOffset(int offset) throws PLDLAssemblingException {
        throw new PLDLAssemblingException("不能给立即数设置地址", null);
    }

    @Override
    int getOffset() throws PLDLAssemblingException {
        throw new PLDLAssemblingException("不能获取立即数的地址", null);
    }
}

class IntArrayConst extends Const {
    private Integer[] lengths;
    private Integer[] vals;

    TypeName getType() {
        return TypeName.INTARRAY;
    }

    int getSize() {
        return vals.length * INT_TYPE_LENGTH;
    }

    @Override
    boolean hasOffset() {
        return true;
    }

    int getVal(List<Integer> indexes) throws PLDLAssemblingException {
        int allLength = getIndex(indexes);
        return vals[allLength];
    }

    void setVal(List<Integer> indexes, int val) throws PLDLAssemblingException {
        int allLength = getIndex(indexes);
        vals[allLength] = val;
    }

    void setVal(int index, int val) {
        vals[index] = val;
    }

    private int getIndex(List<Integer> indexes) throws PLDLAssemblingException {
        if (indexes.size() != lengths.length) {
            throw new PLDLAssemblingException("维度不一致 " + indexes.size() + "<>" + lengths.length, null);
        }
        int allLength = 0;
        for (int i = 0; i < indexes.size(); ++i) {
            int nowLength = indexes.get(i);
            if (nowLength >= lengths[i]) {
                throw new PLDLAssemblingException("数组越界 " + indexes.get(i) + ">=" + lengths[i], null);
            }
            for (int j = i + 1; j < indexes.size(); ++j) {
                nowLength *= lengths[j];
            }
            allLength += nowLength;
        }
        return allLength;
    }

    Integer[] getLengths() {
        return lengths;
    }

    void setLengths(Integer[] lengths) {
        this.lengths = lengths;
        int allLength = 1;
        for (Integer length : lengths) {
            allLength *= length;
        }
        this.vals = new Integer[allLength];
        for (int i = 0; i < allLength; ++i){
            this.vals[i] = 0;
        }
    }
}

class AddressedVar extends Var {
    private Integer[] lengths;

    TypeName getType() {
        return TypeName.ADDR;
    }

    int getSize() {
        return INT_TYPE_LENGTH;
    }

    Integer[] getLengths() {
        return lengths;
    }

    void setLengths(Integer[] lengths) {
        this.lengths = lengths;
    }
}
