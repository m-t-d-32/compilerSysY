package backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FuncParam {
    public boolean isUseful() {
        return isUseful;
    }

    public void setUseful(boolean useful) {
        isUseful = useful;
    }

    private boolean isUseful = false;
    private String name;
    private TypeName returnType;
    private List<Symbol> params = new ArrayList<>();
    private List<String> assemblyARMStrings = new ArrayList<>();

    public List<FourTuple> getFourTuples() {
        return fourTuples;
    }

    private List<FourTuple> fourTuples = new ArrayList<>();

    public List<Symbol> getLocalVars() {
        return localVars;
    }

    public void setLocalVars(List<Symbol> localVars) {
        this.localVars = localVars;
    }

    private List<Symbol> localVars = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeName getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeName returnType) {
        this.returnType = returnType;
    }

    public List<Symbol> getParams() {
        return params;
    }

    public List<String> getAssemblyARMStrings() {
        return assemblyARMStrings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FuncParam)) return false;
        FuncParam funcParam = (FuncParam) o;
        return Objects.equals(name, funcParam.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, params);
    }

    public void addTuples(FourTuple line) {
        fourTuples.add(line);
    }
}
