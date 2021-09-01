package backend;

import exception.PLDLAssemblingException;
import util.StringGenerator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class SymbolTable {
    public SymbolTableInner getRoot() {
        return root;
    }

    /* Manager类 */
    private final SymbolTableInner root;

    public SymbolTableInner getTracingForMovein() {
        return tracingForMovein;
    }

    private final SymbolTableInner tracingForMovein;
    private Integer functionOffsets = 0;    //每个函数的offset
    private Integer functionParamOffsets = 0; //函数参数的offset

    public List<Symbol> getAllSymbolsInner(SymbolTableInner nowRoot){
        List<Symbol> results = new ArrayList<>();
        Queue<SymbolTableInner> bfsQueue = new ArrayDeque<>();
        bfsQueue.add(nowRoot);
        while (!bfsQueue.isEmpty()){
            SymbolTableInner thisTimeSymbolTableInner = bfsQueue.poll();
            results.addAll(thisTimeSymbolTableInner.variables);
            bfsQueue.addAll(thisTimeSymbolTableInner.children);
        }
        return results;
    }

    public void clear(){
        tracingForMovein.children.clear();
        List<Symbol> allSymbols = getAllSymbolsInner(root);
        for (Symbol symbol: allSymbols){
            symbol.setDefined(false);
        }
        functionOffsets = 0;
        functionParamOffsets = 0;
    }

    private String transformToVarname(List<Integer> blockHeight, String nowName) {
        StringBuilder result = new StringBuilder("named_var");
        for (Integer i : blockHeight) {
            result.append("_").append(i);
        }
        result.append(nowName);
        return result.toString();
    }

    public SymbolTable() {
        tracingForMovein = new SymbolTableInner();
        tracingForMovein.children = new ArrayList<>();

        root = new SymbolTableInner();
        root.parent = null;
        root.children = new ArrayList<>();
        root.variables = new ArrayList<>();
    }

    Symbol getSymbol(List<Integer> blockHeight, String nowName, FourTuple fourTuple, boolean varTracing, boolean mustDefined) {
        SymbolTableInner varSymbolTable = root;
        for (Integer i : blockHeight) {
            varSymbolTable = varSymbolTable.children.get(i);
        }
        List<Symbol> variables = varSymbolTable.variables;

        Symbol result = null;
        while (true) {
            for (Symbol param : variables) {
                if (param.getOutername().equals(nowName) && (!mustDefined || param.getIsDefined())) {
                    result = param;
                    break;
                }
            }
            if (result != null || varSymbolTable.parent == null) {
                break;
            } else {
                varSymbolTable = varSymbolTable.parent;
                variables = varSymbolTable.variables;
            }
        }
        if (result != null && varTracing) {
            result.getFourTuple().add(fourTuple);
            fourTuple.getSymbol().add(result);
        }
        return result;
    }

    Const getConst(List<Integer> blockHeight, String nowName, FourTuple fourTuple, boolean varTracing, boolean mustDefined) {
        Symbol result = getSymbol(blockHeight, nowName, fourTuple, varTracing, mustDefined);
        if (result != null && !result.getIsVar()) {
            return (Const) result;
        } else {
            return null;
        }
    }

    Var getVar(List<Integer> blockHeight, String nowName, FourTuple fourTuple, boolean varTracing, boolean mustDefined) {
        Symbol result = getSymbol(blockHeight, nowName, fourTuple, varTracing, mustDefined);
        if (result != null && result.getIsVar()) {
            return (Var) result;
        } else {
            return null;
        }
    }

    Var addVar(List<Integer> blockHeight, String nowName, TypeName typeName,
               List<Integer> lengths, FourTuple fourTuple, boolean varTracing, boolean linkToFourTuple)
            throws PLDLAssemblingException {
        if (nowName == null){
            throw new NullPointerException();
        }
        if (!varTracing){
            Var varParam = getVar(blockHeight, nowName, fourTuple, varTracing, false);
            varParam.setDefined(true);
            return varParam;
        }
        else {
            Var varParam;
            if (typeName == TypeName.INT) {
                varParam = new IntVar();
            } else if (typeName == TypeName.INTARRAY) {
                IntArrayVar newIntArrayVar = new IntArrayVar();
                newIntArrayVar.setLengths(lengths.toArray(new Integer[0]));
                varParam = newIntArrayVar;
            } else {
                AddressedVar newAddressedVar = new AddressedVar();
                newAddressedVar.setLengths(lengths.toArray(new Integer[0]));
                varParam = newAddressedVar;
            }
            varParam.setDefined(true);
            if (linkToFourTuple) {
                varParam.getFourTuple().add(fourTuple);
                fourTuple.getSymbol().add(varParam);
            }
            varParam.setInnername(transformToVarname(blockHeight, nowName));
            varParam.setOutername(nowName);
            varParam.setIsGlobal(blockHeight.isEmpty());
            SymbolTableInner varSymbolTable = root;
            for (Integer i : blockHeight) {
                varSymbolTable = varSymbolTable.children.get(i);
            }
            List<Symbol> variables = varSymbolTable.variables;

            if (varParam.getIsGlobal()) {
                varParam.setOffset(variables.size());
            } else {
                varParam.setOffset(functionOffsets);
                functionOffsets += varParam.getSize();
            }
            // 查看变量名是否重复
            for (Symbol oldVar : variables) {
                if (oldVar.getOutername().equals(nowName)) {
                    throw new PLDLAssemblingException("变量名重复 " + nowName + " 在分区 " + blockHeight, null);
                }
            }
            variables.add(varParam);
            return varParam;
        }
    }

    Const addConst(List<Integer> blockHeight, String nowName, TypeName typeName,
                   List<Integer> lengths, FourTuple fourTuple, boolean varTracing, boolean linkToFourTuple)
            throws PLDLAssemblingException {
        if (!varTracing){
            Const varParam = getConst(blockHeight, nowName, fourTuple, varTracing, false);
            varParam.setDefined(true);
            return varParam;
        }
        else {
            Const varParam;
            if (typeName == TypeName.INT) {
                varParam = new IntConst();
            } else {
                IntArrayConst newIntArrayConst = new IntArrayConst();
                newIntArrayConst.setLengths(lengths.toArray(new Integer[0]));
                varParam = newIntArrayConst;
            }
            varParam.setDefined(true);
            if (linkToFourTuple) {
                varParam.getFourTuple().add(fourTuple);
                fourTuple.getSymbol().add(varParam);
            }
            varParam.setInnername(transformToVarname(blockHeight, nowName));
            varParam.setOutername(nowName);
            varParam.setIsGlobal(blockHeight.isEmpty());
            SymbolTableInner varSymbolTable = root;
            for (Integer i : blockHeight) {
                varSymbolTable = varSymbolTable.children.get(i);
            }
            List<Symbol> variables = varSymbolTable.variables;

            if (varParam.getIsGlobal()) {
                varParam.setOffset(variables.size());
            } else {
                varParam.setOffset(functionOffsets);
                functionOffsets += varParam.getSize();
            }
            // 查看变量名是否重复
            for (Symbol oldVar : variables) {
                if (oldVar.getOutername().equals(nowName)) {
                    throw new PLDLAssemblingException("变量名重复 " + nowName + " 在分区 " + blockHeight, null);
                }
            }
            variables.add(varParam);
            return varParam;
        }
    }

    IntConst addGlobalImmediateInt(int constVal, FourTuple fourTuple) {
        IntConst varParam = new ImmediateInt();
        String name = "u" + StringGenerator.getNextCode();
        varParam.setDefined(true);
        varParam.setIsGlobal(true);
        varParam.setInnername(name);
        varParam.setOutername(name);
        varParam.setVal(constVal);
        varParam.getFourTuple().add(fourTuple);
        fourTuple.getSymbol().add(varParam);
        root.variables.add(varParam);
        return varParam;
    }

    IntVar addFuncParam(List<Integer> blockHeight, String nowName, FourTuple fourTuple,
                        boolean varTracing, boolean linkToFourTuple) throws PLDLAssemblingException {
        if (!varTracing){
            IntVar varParam = (IntVar) getVar(blockHeight, nowName, fourTuple, varTracing, false);
            varParam.setDefined(true);
            return varParam;
        }
        else {
            IntVar varParam = new IntVar();
            varParam.setDefined(true);
            if (linkToFourTuple) {
                varParam.getFourTuple().add(fourTuple);
                fourTuple.getSymbol().add(varParam);
            }
            varParam.setInnername(transformToVarname(blockHeight, nowName));
            varParam.setOutername(nowName);
            varParam.setIsGlobal(false);
            varParam.setOffset(-functionParamOffsets);
            // 查看变量名是否重复
            SymbolTableInner varSymbolTable = root;
            for (Integer i : blockHeight) {
                varSymbolTable = varSymbolTable.children.get(i);
            }
            List<Symbol> variables = varSymbolTable.variables;
            for (Symbol oldVar : variables) {
                if (oldVar.getOutername().equals(nowName)) {
                    throw new PLDLAssemblingException("变量名重复 " + nowName + " 在分区 " + blockHeight, null);
                }
            }
            variables.add(varParam);
            functionParamOffsets += Symbol.INT_TYPE_LENGTH;
            return varParam;
        }
    }

    AddressedVar addFuncParam(List<Integer> blockHeight, String nowName, List<Integer> nowArrayJoin,
                              FourTuple fourTuple, boolean varTracing, boolean linkToFourTuple) throws PLDLAssemblingException {
        if (!varTracing){
            AddressedVar varParam = (AddressedVar) getVar(blockHeight, nowName, fourTuple, varTracing, false);
            varParam.setDefined(true);
            return varParam;
        }
        else {
            AddressedVar varParam = new AddressedVar();
            varParam.setDefined(true);
            if (linkToFourTuple) {
                varParam.getFourTuple().add(fourTuple);
                fourTuple.getSymbol().add(varParam);
            }
            varParam.setInnername(transformToVarname(blockHeight, nowName));
            varParam.setOutername(nowName);
            varParam.setIsGlobal(false);
            varParam.setOffset(-functionParamOffsets);
            // 查看变量名是否重复
            SymbolTableInner varSymbolTable = root;
            for (Integer i : blockHeight) {
                varSymbolTable = varSymbolTable.children.get(i);
            }
            List<Symbol> variables = varSymbolTable.variables;
            for (Symbol oldVar : variables) {
                if (oldVar.getOutername().equals(nowName)) {
                    throw new PLDLAssemblingException("变量名重复 " + nowName + " 在分区 " + blockHeight, null);
                }
            }
            varParam.setLengths(nowArrayJoin.toArray(new Integer[0]));
            variables.add(varParam);
            functionParamOffsets += Symbol.INT_TYPE_LENGTH;
            return varParam;
        }
    }

    void moveIn(List<Integer> blockHeight, boolean varTracing) {
        if (varTracing) {
            if (blockHeight.isEmpty()) {
                //说明是函数定义
                functionOffsets = 0;
                functionParamOffsets = 12;
            }
            SymbolTableInner varSymbolTable = root;
            SymbolTableInner tracingTable = tracingForMovein;
            for (Integer i : blockHeight) {
                varSymbolTable = varSymbolTable.children.get(i);
                tracingTable = tracingTable.children.get(i);
            }
            SymbolTableInner newChild = new SymbolTableInner();
            SymbolTableInner tracingChild = new SymbolTableInner();
            newChild.parent = varSymbolTable;
            newChild.children = new ArrayList<>();
            tracingChild.children = new ArrayList<>();
            newChild.variables = new ArrayList<>();
            blockHeight.add(tracingTable.children.size());
            tracingTable.children.add(tracingChild);
            varSymbolTable.children.add(newChild);
        }
        else {
            if (blockHeight.isEmpty()) {
                //说明是函数定义
                functionOffsets = 0;
                functionParamOffsets = 12;
            }
            SymbolTableInner tracingTable = tracingForMovein;
            for (Integer i: blockHeight){
                tracingTable = tracingTable.children.get(i);
            }
            SymbolTableInner tracingChild = new SymbolTableInner();
            tracingChild.children = new ArrayList<>();
            blockHeight.add(tracingTable.children.size());
            tracingTable.children.add(tracingChild);
        }
    }

    void moveOut(List<Integer> blockHeight, boolean varTracing) {
        blockHeight.remove(blockHeight.size() - 1);
    }
}
