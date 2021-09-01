package backend;

import exception.PLDLAssemblingException;

import java.math.BigInteger;
import java.util.*;

public class ARMTransformer {
    private boolean isSettingUpVariableTable;
    private boolean divBySoftware = true;
    private final List<FourTuple> inputs;
    public static final int fixedFunctionStackSize = 0;
    public static final String NoNamedStr = "NULL";
    public static final Map<String, FuncParam> preDefinedFunctions = new HashMap<>();
    public static final Map<String, Integer> preDefinedParamCount  = new HashMap<>();
    public static final int resultsMaxCount = 512;
    public static final String lGlobalStringIdentifier = "@global-get: ";
    public static final String lGlobalStringToBeReplaced = "@global-get-undefined";
    public static final String lFuncStringVariableCountIdentifier = "function-sub-stack: ";
    public static final String lFuncStringVariableCountToBeReplaced = "function-sub-stack-undefined";
    public static final String lGotoAfterLtorg = "lGotoAfterLtorg";
    public static final String prefixOfFunction = "userfunc";

    //每xxx句汇编打印一次符号表
    private int resultsCount = 0;
    //全局符号表
    SymbolTable varSymbolTable;
    //当前作用域分区
    List<Integer> nowBlockHeight = new ArrayList<>();

    //数组定义长度：当前定义数组的长度
    List<IntConst> nowArrayJoin = new ArrayList<>();
    boolean defineArray = false;
    //数组取下标：当前取数组下标的长度
    Map<String, List<Symbol> > linkingVars = new HashMap<>();

    //数组初始化：当前需要初始化的变量的深度和步数
    Map<Symbol, Integer[]> initializeTable = new HashMap<>();
    Map<Symbol, Integer> nowLastIndex = new HashMap<>();
    Map<Symbol, Integer> nowFinalIndex = new HashMap<>();

    //函数表
    Map<String, FuncParam> functions = new LinkedHashMap<>();
    //现在解析的函数
    FuncParam nowFunction = null;
    //函数传参
    Map<String, List<Symbol>> funcParams = new HashMap<>();
    //全局作用域字符串列表
    List<String> globalStrings = new ArrayList<>();
    //压缩全局变量：对应表
    LinkedHashMap<Integer, Integer> usingGlobalVarOffsetsTransformTable = new LinkedHashMap<>();
    //全局变量段.Lx
    private int globalDefineCount = 0;
    //最终结果
    List<String> results = new ArrayList<>();

    //函数的返回值
    Map<FuncParam, Set<Symbol>> returnees = new HashMap<>();
    //函数的调用点
    Map<FuncParam, Set<Symbol>> callers = new HashMap<>();
    //函数的传参
    Map<FuncParam, Set<List<Symbol>>> paramers = new HashMap<>();
    //cmp的特殊处理
    Map<FourTuple, Map.Entry<Symbol, Symbol>> cmpLinkingVars = new HashMap<>();
    //映射关系建立
    BasicBlockGraphConstructor constructor;

    static {
        try {
            initPreDefinedFunctions();
        } catch (PLDLAssemblingException e) {
            e.printStackTrace();
        }
    }

    public ARMTransformer(List<FourTuple> inputs, boolean isSettingUpVariableTable, SymbolTable symbolTable) {
        this.isSettingUpVariableTable = isSettingUpVariableTable;
        this.varSymbolTable = symbolTable;
        this.inputs = inputs;
    }

    public void clear() {
        resultsCount = 0;
        varSymbolTable.clear();
        nowBlockHeight.clear();
        nowArrayJoin.clear();
        defineArray = false;
        linkingVars.clear();
        initializeTable.clear();
        nowLastIndex.clear();
        nowFinalIndex.clear();
        functions.clear();
        nowFunction = null;
        funcParams.clear();
        globalStrings.clear();
        usingGlobalVarOffsetsTransformTable.clear();
        globalDefineCount = 0;
        results.clear();
        callers.clear();
        returnees.clear();
        paramers.clear();
        cmpLinkingVars.clear();
    }

    public void setDivBySoftware(boolean divBySoftware) {
        this.divBySoftware = divBySoftware;
    }

    public void setSettingUpVariableTable(boolean settingUpVariableTable) {
        isSettingUpVariableTable = settingUpVariableTable;
    }

    static void initPreDefinedFunctions() throws PLDLAssemblingException {
        FuncParam getint = new FuncParam();
        getint.setName("getint");
        getint.setReturnType(TypeName.INT);

        FuncParam getch = new FuncParam();
        getch.setName("getch");
        getch.setReturnType(TypeName.INT);

        FuncParam getarray = new FuncParam();
        getarray.setName("getarray");
        getarray.setReturnType(TypeName.INT);
        Var var1 = new IntVar();
        var1.setIsGlobal(false);
        var1.setOffset(0);
        getarray.getParams().add(var1);

        FuncParam putint = new FuncParam();
        putint.setName("putint");
        putint.setReturnType(TypeName.INT);
        Var var2 = new IntVar();
        var2.setIsGlobal(false);
        var2.setOffset(0);
        putint.getParams().add(var2);

        FuncParam putch = new FuncParam();
        putch.setName("putch");
        putch.setReturnType(TypeName.INT);
        Var var3 = new IntVar();
        var3.setIsGlobal(false);
        var3.setOffset(0);
        putch.getParams().add(var3);

        FuncParam putarray = new FuncParam();
        putarray.setName("putarray");
        putarray.setReturnType(TypeName.INT);
        Var var4 = new IntVar();
        var4.setIsGlobal(false);
        var4.setOffset(0);
        putarray.getParams().add(var4);
        Var var5 = new IntVar();
        var5.setIsGlobal(false);
        var5.setOffset(4);
        putarray.getParams().add(var5);

        FuncParam starttime = new FuncParam();
        starttime.setName("_sysy_starttime");
        starttime.setReturnType(TypeName.VOID);

        FuncParam stoptime = new FuncParam();
        stoptime.setName("_sysy_stoptime");
        stoptime.setReturnType(TypeName.VOID);
        preDefinedFunctions.put("getint", getint);
        preDefinedFunctions.put("getch", getch);
        preDefinedFunctions.put("getarray", getarray);
        preDefinedFunctions.put("putint", putint);
        preDefinedFunctions.put("putch", putch);
        preDefinedFunctions.put("putarray", putarray);
        preDefinedFunctions.put("starttime", starttime);
        preDefinedFunctions.put("stoptime", stoptime);

        preDefinedParamCount.put("getint", 0);
        preDefinedParamCount.put("getch", 0);
        preDefinedParamCount.put("getarray", 0);
        preDefinedParamCount.put("putint", 0);
        preDefinedParamCount.put("putch", 0);
        preDefinedParamCount.put("putarray", 0);
        preDefinedParamCount.put("starttime", 0);
        preDefinedParamCount.put("stoptime", 0);
    }

    public SymbolTable getVarSymbolTable() {
        return varSymbolTable;
    }

    public void setVarSymbolTable(SymbolTable varSymbolTable) {
        this.varSymbolTable = varSymbolTable;
    }

    private void writeString(String str) {
        if (nowFunction == null) {
            globalStrings.add("\t" + str);
        }
        else {
            nowFunction.getAssemblyARMStrings().add("\t" + str);
        }
    }

    public List<String> parse() throws PLDLAssemblingException {
        for (FourTuple line: inputs){
            if (!line.getIsValid()){
                continue;
            }
            String[] tuple = line.getTuples();
            if (tuple.length != 4) {
                throw new PLDLAssemblingException("四元式" + line + "无效！", null);
            }

            for (int i = 0; i < 4; ++i){
                tuple[i] = tuple[i].trim();
            }
            writeString("// " + Arrays.asList(tuple));
            switch (tuple[0]){
                /* arrayjoin, 常量, 常量或null, 常量结果 */
                case "arrayjoin":   {
                    defineArray = true;
                    IntConst var1;
                    if (!tuple[1].equals(NoNamedStr)){
                        if (Character.isDigit(tuple[1].charAt(0))){
                            var1 = varSymbolTable.addGlobalImmediateInt(Integer_8_10_16_parseInt(tuple[1]), line);
                        }
                        else {
                            var1 = (IntConst) varSymbolTable.getConst(nowBlockHeight, tuple[1], line, isSettingUpVariableTable, true);
                            if (var1 == null) {
                                throw new PLDLAssemblingException("数组维度必须为常量", null);
                            }
                            if (var1.getType() != TypeName.INT) {
                                throw new PLDLAssemblingException("数组维度必须为整数类型", null);
                            }
                        }
                    }
                    else {
                        var1 = varSymbolTable.addGlobalImmediateInt(Integer_8_10_16_parseInt("0"), line);
                    }
                    nowArrayJoin.add(var1);
                }
                break;

                /* constdefine, 数组大小或者null, _, 变量名 */
                case "constdefine": {
                    if (!defineArray) {
                        varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, false);
                    } else {
                        List<Integer> arrayLengths = new ArrayList<>();
                        for (IntConst arrayLength: nowArrayJoin){
                            arrayLengths.add(arrayLength.getVal());
                        }
                        Const var3 = varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INTARRAY, arrayLengths, line, isSettingUpVariableTable, false);
                        for (Symbol s: nowArrayJoin){
                            var3.addToParents(s);
                        }
                    }
                    nowArrayJoin.clear();
                    defineArray = false;
                }
                break;

                /* define, 数组大小或者null, _, 变量名 */
                case "define": {
                    if (!defineArray) {
                        varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, false);
                    } else {
                        List<Integer> arrayLengths = new ArrayList<>();
                        for (IntConst arrayLength: nowArrayJoin){
                            arrayLengths.add(arrayLength.getVal());
                        }
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INTARRAY, arrayLengths, line, isSettingUpVariableTable, false);
                        for (Symbol s: nowArrayJoin){
                            var3.addToParents(s);
                        }
                    }
                    nowArrayJoin.clear();
                    defineArray = false;
                }
                break;

                /* link, 上一维度下标，这一维度下标，这一维度结果 */
                case "link":    {
                    if (tuple[1].equals(NoNamedStr)){
                        linkingVars.put(tuple[3], new ArrayList<>());
                    }
                    else {
                        linkingVars.put(tuple[3], linkingVars.get(tuple[1]));
                    }
                    Symbol var2;
                    if (Character.isDigit(tuple[2].charAt(0))) {
                        var2 = varSymbolTable.addGlobalImmediateInt(Integer_8_10_16_parseInt(tuple[2]), line);
                    } else {
                        var2 = varSymbolTable.getSymbol(nowBlockHeight, tuple[2], line, isSettingUpVariableTable, true);
                    }
                    linkingVars.get(tuple[3]).add(var2);
                }
                break;

                /* getvar, 临时变量, _, 指代变量 */
                case "getvar": {
                    Symbol var3 = varSymbolTable.getSymbol(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null) {
                        throw new PLDLAssemblingException("变量" + tuple[3] + "没有定义: " + line, null);
                    } else if (var3.getType() != TypeName.INTARRAY && var3.getType() != TypeName.ADDR) {
                        throw new PLDLAssemblingException("变量不是数组: " + line, null);
                    } else {
                        //当var3是变量时，或者当nowArrayLink中任何一个是变量时，都返回变量
                        //否则返回常数
                        boolean allIsConst = true;
                        List<Symbol> nowArrayLink = linkingVars.get(tuple[1]);
                        if (var3.getType() != TypeName.INTARRAY || var3.getIsVar()){
                            allIsConst = false;
                        }
                        else if (((IntArrayConst)var3).getLengths().length != nowArrayLink.size()){
                            allIsConst = false;
                        }
                        for (Symbol s : nowArrayLink) {
                            if (s.getIsVar()){
                                allIsConst = false;
                            }
                        }

                        int var3length, var3Size, var3Offset;
                        boolean var3IsGlobal;
                        Integer []var3Lengths;
                        if (var3.getType() == TypeName.INTARRAY && var3.getIsVar()){
                            IntArrayVar varArray3 = (IntArrayVar) var3;
                            var3Lengths = varArray3.getLengths();
                            var3length = var3Lengths.length;
                            var3Size = varArray3.getSize();
                            var3Offset = varArray3.getOffset();
                            var3IsGlobal = varArray3.getIsGlobal();
                        }
                        else if (var3.getType() == TypeName.INTARRAY && !var3.getIsVar()){
                            IntArrayConst varArray3 = (IntArrayConst) var3;
                            var3Lengths = varArray3.getLengths();
                            var3length = var3Lengths.length;
                            var3Size = varArray3.getSize();
                            var3Offset = varArray3.getOffset();
                            var3IsGlobal = varArray3.getIsGlobal();
                        }
                        else if (var3.getType() == TypeName.ADDR){
                            AddressedVar varArray3 = (AddressedVar) var3;
                            var3Lengths = varArray3.getLengths();
                            var3length = var3Lengths.length;
                            var3Size = varArray3.getSize();
                            var3Offset = varArray3.getOffset(); //var3自己的offset
                            var3IsGlobal = varArray3.getIsGlobal();
                        }
                        else {
                            throw new PLDLAssemblingException("变量或常量不支持下标运算" + line, null);
                        }
                        if (var3length < nowArrayLink.size()){
                            throw new PLDLAssemblingException("数组取值维度大于定义维度：" + var3length + "<" + nowArrayLink.size() + ", 在" + line, null);
                        }
                        else {
                            List<Symbol> var1Parents = new ArrayList<>();
                            writeString("MOV R1, #0x0");
                            writeString("MOV R2, #0x0");
                            for (int i = 0; i < nowArrayLink.size(); ++i) {
                                int R1Temp = Symbol.INT_TYPE_LENGTH;
                                for (int j = i + 1; j < var3length; ++j) {
                                    R1Temp *= var3Lengths[j];
                                }
                                loadImmediateIntToRegister(R1Temp, "R1");
                                String nowVarStr = nowArrayLink.get(i).getOutername();
                                Symbol nowVar = getIntValueToRegister(nowVarStr, "R3", line);
                                var1Parents.add(nowVar);
                                writeString("MUL R3, R1, R3");
                                writeString("ADD R2, R3, R2");
                            }
                            if (var3IsGlobal) {
                                loadAddrFromGlobalMemory(var3Offset, "R1");
                            } else {
                                loadAddrFromLocalMemory((var3Offset + var3Size + 4), "R1");
                            }

                            if (var3.getType() == TypeName.ADDR){
                                writeString("LDR R1, [R1]");
                            }
                            writeString("ADD R2, R1, R2");
                            List<Integer> nowVarLengths = new ArrayList<>(Arrays.asList(var3Lengths).subList(nowArrayLink.size(), var3length));

                            Symbol var1;
                            if (allIsConst) {
                                List<Integer> indexes = new ArrayList<>();
                                for (Symbol s : nowArrayLink) {
                                    indexes.add(((IntConst)s).getVal());
                                }
                                int val = ((IntArrayConst)var3).getVal(indexes);
                                var1 = varSymbolTable.addConst(nowBlockHeight, tuple[1], TypeName.INT, null, line, isSettingUpVariableTable, true);
                                ((IntConst)var1).setVal(val);

                                writeString("LDR R2, [R2]");
                            }
                            else {
                                var1 = varSymbolTable.addVar(nowBlockHeight, tuple[1], TypeName.ADDR, nowVarLengths, line, isSettingUpVariableTable, true);
                            }
                            var1.addToParents(var3);
                            var1.addToParents(var1Parents);
                            var3.addToParents(var1);
                            writeRegisterToVar("R2", "R3", var1);
                        }
                    }
                }
                break;

                /* init, 用于初始化的数据, _, 被初始化的变量 */
                case "init": {
                    Var var3 = varSymbolTable.getVar(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null) {
                        throw new PLDLAssemblingException("变量 " + tuple[3] + " 没有定义。" + line, null);
                    }
                    Symbol var1;
                    if (var3.getType() == TypeName.INTARRAY) {
                        IntArrayVar varArray3 = (IntArrayVar) var3;
                        initBarIfNotExist(varArray3);
                        var1 = getIntValueToRegister(tuple[1], "R2", line);
                        int finalIndex = nowFinalIndex.get(varArray3);
                        writeRegisterToVar("R2", "R1", "R3", varArray3, finalIndex);

                        int length = initializeTable.get(varArray3).length;
                        nowFinalIndex.put(varArray3, finalIndex + 1);
                        ++initializeTable.get(varArray3)[length - 1];
                    }
                    else {
                        var1 = getIntValueToRegister(tuple[1], "R2", line);
                        writeRegisterToVar("R2", "R3", var3);
                    }
                    var3.addToParents(var1);
                }
                break;

                /* constinit, 用于初始化的数据, _, 被初始化的常量 */
                /* 注意：const变量只会在constinit时被赋值 */
                /* 但是由于有可能作为函数传参，因而必须在内存中保留值 */
                /* 另一种实现：传参时拷贝 */
                case "constinit": {
                    IntConst var1;
                    Const var3 = varSymbolTable.getConst(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null) {
                        throw new PLDLAssemblingException("常量 " + tuple[3] + " 没有定义。" + line, null);
                    }
                    if (var3.getType() == TypeName.INTARRAY) {
                        IntArrayConst varArray3 = (IntArrayConst) var3;
                        initBarIfNotExist(varArray3);
                        var1 = (IntConst) getIntValueToRegister(tuple[1], "R2", line);
                        int finalIndex = nowFinalIndex.get(varArray3);
                        writeRegisterToVar("R2", "R1", "R3", varArray3, finalIndex);
                        varArray3.setVal(finalIndex, var1.getVal());

                        int length = initializeTable.get(varArray3).length;
                        nowFinalIndex.put(varArray3, finalIndex + 1);
                        ++initializeTable.get(varArray3)[length - 1];
                    }
                    else {
                        var1 = (IntConst) getIntValueToRegister(tuple[1], "R2", line);
                        ((IntConst) var3).setVal(var1.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                    }
                    var3.addToParents(var1);
                }
                break;

                /* innerconstinitnew, _, _, 被初始化的常量 */
                /* innerinitnew, _, _, 被初始化的变量 */
                case "innerconstinitnew":
                case "innerinitnew": {
                    Symbol var3 = varSymbolTable.getSymbol(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null){
                        throw new PLDLAssemblingException("变量" + tuple[3] + "没有定义, 在" + line, null);
                    }
                    initBarIfNotExist(var3);
                    nowLastIndex.put(var3, nowLastIndex.get(var3) + 1);
                    goCarryBackend(var3);
                }
                break;

                /* outerinitnew, _, _, 被初始化的变量 */
                case "outerinitnew": {
                    IntArrayVar var3 = (IntArrayVar) varSymbolTable.getVar(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null) {
                        throw new PLDLAssemblingException("常量 " + tuple[3] + " 没有定义。" + line, null);
                    }
                    initBarIfNotExist(var3);
                    int finalIndex = nowFinalIndex.get(var3);

                    int thisDepthMaxLen = 1;
                    for (int j = nowLastIndex.get(var3) - 1; j < var3.getLengths().length; ++j){
                        thisDepthMaxLen *= var3.getLengths()[j];
                    }
                    int thisDepthLen = 0;
                    for (int j = nowLastIndex.get(var3) - 1; j < var3.getLengths().length; ++j){
                        int addValue = initializeTable.get(var3)[j];
                        for (int k = j + 1; k < var3.getLengths().length; ++k){
                            addValue *= var3.getLengths()[k];
                        }
                        thisDepthLen += addValue;
                    }
                    int length = var3.getLengths().length;
                    for (int i = thisDepthLen; i < thisDepthMaxLen; ++i) {
                        if (!var3.getIsGlobal()) {
                            writeString("MOV R2, #0x0");
                            writeRegisterToVar("R2", "R1", "R3", var3, finalIndex);
                        }
                        ++initializeTable.get(var3)[length - 1];
                        ++finalIndex;
                    }
                    goCarryBackend(var3);
                    nowLastIndex.put(var3, nowLastIndex.get(var3) - 1);
                    nowFinalIndex.put(var3, finalIndex);
                }
                break;

                /* outerconstinitnew, _, _, 被初始化的常量 */
                case "outerconstinitnew": {
                    IntArrayConst var3 = (IntArrayConst) varSymbolTable.getConst(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null) {
                        throw new PLDLAssemblingException("常量 " + tuple[3] + " 没有定义。" + line, null);
                    }
                    initBarIfNotExist(var3);
                    int finalIndex = nowFinalIndex.get(var3);

                    int thisDepthMaxLen = 1;
                    for (int j = nowLastIndex.get(var3) - 1; j < var3.getLengths().length; ++j){
                        thisDepthMaxLen *= var3.getLengths()[j];
                    }
                    int thisDepthLen = 0;
                    for (int j = nowLastIndex.get(var3) - 1; j < var3.getLengths().length; ++j){
                        int addValue = initializeTable.get(var3)[j];
                        for (int k = j + 1; k < var3.getLengths().length; ++k){
                            addValue *= var3.getLengths()[k];
                        }
                        thisDepthLen += addValue;
                    }
                    int length = var3.getLengths().length;
                    for (int i = thisDepthLen; i < thisDepthMaxLen; ++i) {
                        if (!var3.getIsGlobal()) {
                            writeString("MOV R2, #0x0");
                            writeRegisterToVar("R2", "R1", "R3", var3, finalIndex);
                        }
                        var3.setVal(finalIndex, 0);
                        ++initializeTable.get(var3)[length - 1];
                        ++finalIndex;
                    }
                    goCarryBackend(var3);
                    nowLastIndex.put(var3, nowLastIndex.get(var3) - 1);
                    nowFinalIndex.put(var3, finalIndex);
                }
                break;

                /* func, _, 返回值类型, 函数名 */
                case "func": {
                    if (functions.containsKey(tuple[1])) {
                        throw new PLDLAssemblingException("函数" + tuple[3] + "重复定义" + line, null);
                    }

                    FuncParam newFunc = new FuncParam();
                    newFunc.setName(tuple[3]);
                    switch (tuple[2]) {
                        case "void": newFunc.setReturnType(TypeName.VOID); break;
                        case "int": newFunc.setReturnType(TypeName.INT); break;
                        default: throw new PLDLAssemblingException("函数返回值" + tuple[2] + "无效" + line, null);
                    }
                    functions.put(tuple[3], newFunc);
                    nowFunction = newFunc;

                    writeString("PUSH {FP, LR}");
                    writeString("ADD FP, SP, #4");
                }
                break;

                /* subparams, _, _, _ */
                case "subparams":    {
                    if (nowFunction == null){
                        throw new PLDLAssemblingException("内部编译器错误：" + line, null);
                    }
                    writeString(lFuncStringVariableCountIdentifier + "SUB SP, SP, " + lFuncStringVariableCountToBeReplaced);
                }
                break;

                /* funcend, _, 返回值类型, 函数名 */
                case "funcend": {
                    writeString("SUB SP, FP, #4");
                    writeString("POP {FP, PC}");
                    List<SymbolTableInner> symbolTableInners = varSymbolTable.getRoot().children;
                    List<SymbolTableInner> tracingTableInners = varSymbolTable.getTracingForMovein().children;
                    List<Symbol> symbols = varSymbolTable.getAllSymbolsInner(symbolTableInners.get(tracingTableInners.size() - 1));
                    nowFunction.setLocalVars(symbols);
                    nowFunction = null;
                }
                break;

                /* param, 函数名, 类型，变量名 */
                case "param": {
                    FuncParam func = functions.get(tuple[1]);
                    Var varParam;
                    if ("int".equals(tuple[2])) {
                        if (!defineArray){
                            varParam = varSymbolTable.addFuncParam(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                            if (varParam.getIsInRegister()){
                                varParam.setInRegister(false);
                                readVarToRegister(varParam, varParam.getRegister());
                                varParam.setInRegister(true);
                            }
                        }
                        else {
                            List<Integer> arrayLengths = new ArrayList<>();
                            for (IntConst arrayLength: nowArrayJoin){
                                arrayLengths.add(arrayLength.getVal());
                            }
                            varParam = varSymbolTable.addFuncParam(nowBlockHeight, tuple[3], arrayLengths, line, isSettingUpVariableTable, true);
                            for (Symbol s: nowArrayJoin){
                                varParam.addToParents(s);
                            }
                        }
                    } else {
                        throw new PLDLAssemblingException("函数参数类型" + tuple[2] + "无效" + line, null);
                    }
                    nowArrayJoin.clear();
                    defineArray = false;
                    func.getParams().add(varParam);
                }
                break;

                /* in, _, _, _ */
                case "in": {
                    varSymbolTable.moveIn(nowBlockHeight, isSettingUpVariableTable);
                }
                break;

                /* out, _, _, _ */
                case "out": {
                    varSymbolTable.moveOut(nowBlockHeight, isSettingUpVariableTable);
                }
                break;

                /* putvar, 左值, _, 结果 */
                case "putvar":  {
                    //左值被取出，有可能是整型变量，或者整型数组，或者指针
                    Symbol var1 = varSymbolTable.getSymbol(nowBlockHeight, tuple[1], line, isSettingUpVariableTable, true);
                    if (var1 == null){
                        throw new PLDLAssemblingException("变量" + tuple[1] + "没有定义", null);
                    }
                    else {
                        switch(var1.getType()) {
                            case INT: {
                                readVarToRegister(var1, "R2");
                                if (var1.getIsVar()) {
                                    IntVar var3 = (IntVar) varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                } else {
                                    IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                                    var3.setVal(((IntConst) var1).getVal());
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                }
                            }
                            break;

                            //不支持数组传参或者地址传参后用其中的值进行初始化
                            //这是由于基于文法，函数参数必须以int开头而不是const
                            case INTARRAY: {
                                readVarAddrToRegister(var1, "R2");
                                if (var1.getIsVar()) {
                                    AddressedVar var3 = (AddressedVar) varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.ADDR,
                                            Arrays.asList(((IntArrayVar) var1).getLengths()), line, isSettingUpVariableTable, true);
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                    var1.addToParents(var3);
                                }
                                else {
                                    AddressedVar var3 = (AddressedVar) varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.ADDR,
                                            Arrays.asList(((IntArrayConst) var1).getLengths()), line, isSettingUpVariableTable, true);
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                    var1.addToParents(var3);
                                }
                            }
                            break;

                            case ADDR: {
                                if (((AddressedVar) var1).getLengths().length == 0) {
                                    //说明是真正的左值（整数），但是存储位置是地址
                                    //需要取值（解引用）
                                    IntVar var3 = (IntVar) varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT,
                                            null, line, isSettingUpVariableTable, true);
                                    readVarToRegister(var1, "R2");
                                    writeString("LDR R2, [R2]");
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                } else {
                                    //存的是地址，并且不需要解引用
                                    readVarToRegister(var1, "R2");
                                    AddressedVar var3 = (AddressedVar) varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.ADDR,
                                            Arrays.asList(((AddressedVar) var1).getLengths()), line, isSettingUpVariableTable, true);
                                    writeRegisterToVar("R2", "R3", var3);
                                    var3.addToParents(var1);
                                    var1.addToParents(var3);
                                }
                            }
                            break;

                            default:
                                throw new PLDLAssemblingException("变量类型不正确：" + tuple[3] + ", 在" + line, null);
                        }
                    }
                }
                break;

                /* assign, 用来赋值的值, _, 左值 */
                case "assign": {
                    //这个左值被赋值，因而只能是整型变量（或者时整数数组的一个），但是不会是常量，常量初始化在constinit
                    Symbol var3 = varSymbolTable.getVar(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (var3 == null){
                        throw new PLDLAssemblingException("变量" + tuple[3] + "没有定义", null);
                    }
                    else {
                        Symbol var1;
                        if (var3.getType() == TypeName.ADDR && ((AddressedVar) var3).getLengths().length == 0) {
                            var1 = getIntValueToRegister(tuple[1], "R2", line);
                            var3 = getIntValueToRegister(tuple[3], "R3", line);
                            writeString("STR R2, [R3]");
                            var3.addToParents(var1);
                        } else if (var3.getType() == TypeName.INT) {
                            var1 = getIntValueToRegister(tuple[1], "R2", line);
                            writeRegisterToVar("R2", "R3", var3);
                            var3.addToParents(var1);
                        } else {
                            throw new PLDLAssemblingException("变量未定义或类型不正确：" + tuple[3] + ", 在" + line, null);
                        }
                    }
                }
                break;

                /* b, _, _, 标签 */
                case "b":   {
                    writeString("B " + tuple[3]);
                }
                break;

                /* label, _, _, 标签 */
                case "label":   {
                    writeString(tuple[3] + ":");
                }
                break;

                /* ret, _, _, 返回值 */
                case "ret": {
                    if (!tuple[3].equals(NoNamedStr)){
                        Symbol var3 = getIntValueToRegister(tuple[3], "R0", line);
                        if (!returnees.containsKey(nowFunction)){
                            returnees.put(nowFunction, new HashSet<>());
                        }
                        returnees.get(nowFunction).add(var3);
                        if (nowFunction.getName().equals(prefixOfFunction + "main")){
                            //main函数的返回值也是有用的
                            var3.setIsValid(true);
                            nowFunction.setUseful(true);
                        }
                    }
                    writeString("SUB SP, FP, #4");
                    writeString("POP {FP, PC}");
                }
                break;

                /* cmp, 操作数1, 操作数2, _ */
                case "cmp": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R2", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R1", line);
                    writeString("CMP R2, R1");
                    var1.addToParents(var2);
                    var2.addToParents(var1);
                    cmpLinkingVars.put(line, new AbstractMap.SimpleEntry<>(var1, var2));
                }
                break;

                /* beq, _, _, 标签 */
                case "beq":   {
                    writeString("BEQ " + tuple[3]);
                }
                break;

                /* bne, _, _, 标签 */
                case "bne":   {
                    writeString("BNE " + tuple[3]);
                }
                break;

                /* call, 函数名, _, 返回值保存的变量 */
                case "call":    {
                    FuncParam func;
                    boolean isPredefined = false;
                    String tuple1 = tuple[1];
                    if (functions.containsKey(tuple1)){
                        func = functions.get(tuple1);
                    }
                    else {
                        tuple1 = tuple1.substring(prefixOfFunction.length());
                        if (preDefinedFunctions.containsKey(tuple1)){
                            func = preDefinedFunctions.get(tuple1);
                        }
                        else {
                            throw new PLDLAssemblingException("函数 " + tuple1 + " 没有定义 " + line, null);
                        }
                        if (preDefinedParamCount.containsKey(tuple1)){
                            preDefinedParamCount.put(tuple1, 0);
                        }
                        isPredefined = true;
                    }
                    if (!funcParams.containsKey(tuple1)){
                        funcParams.put(tuple1, new ArrayList<>());
                    }
                    List<Symbol> trueparams = funcParams.get(tuple1);
                    if (trueparams.size() != func.getParams().size()){
                        throw new PLDLAssemblingException("函数 " + tuple1 + " 参数传递个数不正确 " +
                                "函数需要 " + func.getParams().size() + "个，传递了 " + trueparams.size() + "个，在" + line, null);
                    }
                    for (Symbol trueparam: trueparams){
                        if (trueparam.getType() != TypeName.INT){
                            trueparam.getFourTuple().add(line);
                            line.getSymbol().add(trueparam);
                        }
                    }

                    //对starttime和stoptime做处理
                    if (tuple1.equals("starttime") || tuple1.equals("stoptime")){
                        writeString("LDR R0, =1");
                    }
                    writeString("BL " + func.getName());
                    if (func.getReturnType() == TypeName.INT) {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        writeRegisterToVar("R0", "R3", var3);
                        if (!callers.containsKey(func)){
                            callers.put(func, new HashSet<>());
                        }
                        callers.get(func).add(var3);
                    }

                    int paramSize;
                    if (isPredefined){
                        paramSize = (trueparams.size() - 4) * Symbol.INT_TYPE_LENGTH;
                    }
                    else {
                        paramSize = trueparams.size() * Symbol.INT_TYPE_LENGTH;
                    }
                    if (paramSize > 0) {
                        writeString("LDR R3, =" + paramSize);
                        writeString("ADD SP, SP, R3");
                    }

                    if (!paramers.containsKey(func)){
                        paramers.put(func, new HashSet<>());
                    }
                    paramers.get(func).add(new ArrayList<>(trueparams));
                    trueparams.clear();
                }
                break;

                /* pushvar, 变量, _, 函数名 */
                case "pushvar": {
                    String tuple3 = tuple[3];
                    if (functions.containsKey(tuple3)) {
                        if (!funcParams.containsKey(tuple3)) {
                            funcParams.put(tuple3, new ArrayList<>());
                        }
                        Symbol var = getIntValueToRegister(tuple[1], "R3", line);
                        funcParams.get(tuple3).add(var);
                        writeString("PUSH {R3}");
                    }
                    else {
                        tuple3 = tuple3.substring(prefixOfFunction.length());
                        if (preDefinedFunctions.containsKey(tuple3)) {
                            if (!funcParams.containsKey(tuple3)) {
                                funcParams.put(tuple3, new ArrayList<>());
                            }
                            Symbol var;
                            if (preDefinedParamCount.get(tuple3) > 3) {
                                var = getIntValueToRegister(tuple[1], "R3", line);
                                funcParams.get(tuple3).add(var);
                                writeString("PUSH {R3}");
                            } else {
                                int nowRegister = preDefinedFunctions.get(tuple3).getParams().size() - preDefinedParamCount.get(tuple3) - 1;
                                var = getIntValueToRegister(tuple[1], "R" + nowRegister, line);
                                funcParams.get(tuple3).add(var);
                            }
                            if (isUsefulPreFunctions(tuple3)){
                                //预先定义的函数参数是有用的
                                var.setIsValid(true);
                                nowFunction.setUseful(true);
                            }
                            preDefinedParamCount.put(tuple3, preDefinedParamCount.get(tuple3) + 1);
                        } else {
                            throw new PLDLAssemblingException("函数 " + tuple3 + " 没有定义 " + line, null);
                        }
                    }
                }
                break;

                /* neg, 变量, _, 结果 */
                case "neg": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R2", line);
                    if (!var1.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(-((IntConst)var1).getVal());
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateNegByOptimize(var1);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                    }
                }
                break;

                /* not, 变量, _, 结果 */
                case "not": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R2", line);
                    if (!var1.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() == 0 ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateNotByOptimize(var1);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                    }
                }
                break;

                /* mul, 乘数1, 乘数2, 结果 */
                case "mul": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() * ((IntConst)var2).getVal());
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateMulByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* div, 被除数, 除数, 结果 */
                case "div": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R0", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R1", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() / ((IntConst)var2).getVal());
                        writeString("LDR R0, =" + var3.getVal());
                        writeRegisterToVar("R0", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateDivByOptimize(var1, var2);
                        writeRegisterToVar("R0", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* mod, 被除数, 除数, 结果 */
                case "mod": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R0", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R1", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() % ((IntConst)var2).getVal());
                        writeString("LDR R1, =" + var3.getVal());
                        writeRegisterToVar("R1", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateModByOptimize(var1, var2);
                        writeRegisterToVar("R1", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* add, 加数1, 加数2, 结果 */
                case "add": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() + ((IntConst)var2).getVal());
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateAddByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* sub, 被减数, 减数, 结果 */
                case "sub": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() - ((IntConst)var2).getVal());
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateSubByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* lt, 变量1, 变量2, 结果 */
                case "lt": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() < ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateLtByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* gt, 变量1, 变量2, 结果 */
                case "gt": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() > ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateGtByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* lte, 变量1, 变量2, 结果 */
                case "lte": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() <= ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateLteByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* gte, 变量1, 变量2, 结果 */
                case "gte": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() >= ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateGteByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* equ, 变量1, 变量2, 结果 */
                case "equ": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() == ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateEquByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* neq, 变量1, 变量2, 结果 */
                case "neq": {
                    Symbol var1 = getIntValueToRegister(tuple[1], "R1", line);
                    Symbol var2 = getIntValueToRegister(tuple[2], "R2", line);
                    if (!var1.getIsVar() && !var2.getIsVar()){
                        IntConst var3 = (IntConst) varSymbolTable.addConst(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        var3.setVal(((IntConst)var1).getVal() != ((IntConst)var2).getVal() ? 1 : 0);
                        writeString("LDR R2, =" + var3.getVal());
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                    else {
                        Var var3 = varSymbolTable.addVar(nowBlockHeight, tuple[3], TypeName.INT, null, line, isSettingUpVariableTable, true);
                        generateNeqByOptimize(var1, var2);
                        writeRegisterToVar("R2", "R3", var3);
                        var3.addToParents(var1);
                        var3.addToParents(var2);
                    }
                }
                break;

                /* str, _, _, 结果 */
                case "str": {
                    loadImmediateIntToRegister(1, "R2");
                    Symbol var3 = varSymbolTable.getSymbol(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (!var3.getIsVar()){
                        ((IntConst)var3).setVal(1);
                    }
                    writeRegisterToVar("R2", "R3", var3);
                }
                break;

                /* clr, _, _, 结果 */
                case "clr": {
                    loadImmediateIntToRegister(0, "R2");
                    Symbol var3 = varSymbolTable.getSymbol(nowBlockHeight, tuple[3], line, isSettingUpVariableTable, true);
                    if (!var3.getIsVar()){
                        ((IntConst)var3).setVal(0);
                    }
                    writeRegisterToVar("R2", "R3", var3);
                }
                break;
            }
            if (nowFunction != null) {
                nowFunction.addTuples(line);
            }
        }

        results.add("\t .arch armv7-a");
        if (!divBySoftware){
            results.add("\t .arch armv7ve");
            results.add("\t .fpu softvfp");
        }

        //先处理COMM
        SymbolTableInner now = varSymbolTable.getRoot();
        for (Symbol s: now.variables){
            if (s.hasOffset()) {
                results.add("\t .global " + s.getInnername());
                results.add("\t .bss");
                results.add("\t .align 2");
                results.add("\t .type " + s.getInnername() + ", %object");
                results.add("\t .size " + s.getInnername() + ", " + s.getSize());
                results.add(s.getInnername() + ":");
                results.add("\t .space " + s.getSize());
                results.add("");
            }
        }

        for (String s: functions.keySet()){
            FuncParam func = functions.get(s);
            results.add("\t.text");
            results.add("\t.global " + func.getName());
            results.add("\t.type " + func.getName() + ", %function");
            results.add(func.getName() + ":");
            int localSymbolSize = 0;
            List<Symbol> localSymbols = func.getLocalVars();
            for (Symbol symbol: localSymbols){
                localSymbolSize += symbol.getSize();
            }
            for (String funcString : func.getAssemblyARMStrings()) {
                resultsGlobalAdd(funcString, localSymbolSize);
            }
            results.add("");
        }
        results.add("\t.text");
        results.add("\t.global main");
        results.add("\t.type main, %function");
        results.add("main:");
        results.add("\tPUSH {FP, LR}");
        results.add("\tADD FP, SP, #4");
        for (String globalString: globalStrings){
            resultsGlobalAdd(globalString, 0);
        }
        results.add("\tBL " + prefixOfFunction + "main");
        results.add("\tSUB SP, FP, #4");
        results.add("\tPOP {FP, PC}");

        results.add("\t .ltorg");
        results.add(".L" + globalDefineCount + ":");
        for (Integer symbolIndex : usingGlobalVarOffsetsTransformTable.keySet()) {
            Symbol s = now.variables.get(symbolIndex);
            if (s.getIsVar() || s.getType() == TypeName.INTARRAY) {
                results.add("\t.word " + s.getInnername());
            }
        }
        return results;
    }

    private void generateNotByOptimize(Symbol var1) {
        writeString("CMP R2, #0");
        writeString("MOVEQ R2, #1");
        writeString("MOVNE R2, #0");
    }

    private void generateNegByOptimize(Symbol var1) {
        writeString("RSB R2, R2, #0");
    }

    private void generateNeqByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVNE R2, #1");
        writeString("MOVEQ R2, #0");
    }

    private void generateEquByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVEQ R2, #1");
        writeString("MOVNE R2, #0");
    }

    private void generateGteByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVGE R2, #1");
        writeString("MOVLT R2, #0");
    }

    private void generateLteByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVLE R2, #1");
        writeString("MOVGT R2, #0");
    }

    private void generateGtByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVGT R2, #1");
        writeString("MOVLE R2, #0");
    }

    private void generateLtByOptimize(Symbol var1, Symbol var2) {
        writeString("CMP R1, R2");
        writeString("MOVLT R2, #1");
        writeString("MOVGE R2, #0");
    }

    private void generateModByOptimize(Symbol var1, Symbol var2) {
        if (!var2.getIsVar()) {
            int const2val = ((IntConst) var2).getVal();
            const2val = Math.abs(const2val);
            if (const2val == 1){
                writeString("MOV R1, =0");
            }
            else if ((const2val & (const2val - 1)) == 0){
                //是2的整数次幂
                writeString("MOV R2, #0");
                writeString("CMP R0, #0");
                writeString("MOVLT R2, #1");
                writeString("CMP R2, #1");
                writeString("RSBEQ R0, R0, #0");
                writeString("CMP R1, #0");
                writeString("RSBLT R1, R1, #0");
                writeString("SUB R1, R1, #1");
                writeString("AND R1, R0, R1");
                writeString("CMP R2, #1");
                writeString("RSBEQ R1, R1, #0");
            }
            else {
                if (divBySoftware){
                    writeString("BL __aeabi_idivmod");
                }
                else {
                    writeString("SDIV R2, R0, R1"); //R2 = R0 / R1
                    writeString("MUL R1, R2, R1"); //R1 = R2 * R1
                    writeString("SUB R1, R0, R1"); //R1 = R0 - R1
                }
            }
        }
        else {
            if (divBySoftware){
                writeString("BL __aeabi_idivmod");
            }
            else {
                writeString("SDIV R2, R0, R1"); //R2 = R0 / R1
                writeString("MUL R1, R2, R1"); //R1 = R2 * R1
                writeString("SUB R1, R0, R1"); //R1 = R0 - R1
            }
        }
    }

    private void generateDivByOptimize(Symbol var1, Symbol var2) {
        if (!var2.getIsVar()) {
            int const2val = ((IntConst) var2).getVal();
            const2val = Math.abs(const2val);
            if (const2val == 1){
                /* Do nothing */
            }
            else if ((const2val & (const2val - 1)) == 0){
                int log2 = 0, val = const2val;
                while (val != 1){
                    val = val >>> 1;
                    ++log2;
                }
                //是2的整数次幂
                writeString("MOV R2, #0");
                writeString("CMP R0, #0");
                writeString("MOVLT R2, #1");
                writeString("CMP R2, #1");
                writeString("RSBEQ R0, R0, #0");
                writeString("MOV R0, R0, LSR#" + log2);
                writeString("CMP R2, #1");
                writeString("RSBEQ R0, R0, #0");
                writeString("CMP R1, #0");
                writeString("RSBLT R0, R0, #0");
            }
            else {
                if (divBySoftware){
                    writeString("BL __aeabi_idiv");
                }
                else {
                    writeString("SDIV R0, R0, R1");
                }
            }
        }
        else {
            if (divBySoftware){
                writeString("BL __aeabi_idiv");
            }
            else {
                writeString("SDIV R0, R0, R1");
            }
        }
    }

    private void generateMulByOptimize(Symbol var1, Symbol var2) {
        if (!var1.getIsVar()){
            IntConst const1 = (IntConst) var1;
            if (const1.getVal() == 0){
                writeString("MOV R2, =0");
            }
            else if ((const1.getVal() & (const1.getVal() - 1)) == 0){
                int log2 = 0, val = const1.getVal();
                while (val != 1){
                    val = val >>> 1;
                    ++log2;
                }
                //是2的整数次幂
                writeString("MOV R2, R2, LSL#" + log2);
            }
            else {
                writeString("MUL R2, R1, R2");
            }
        }
        else if (!var2.getIsVar()) {
            IntConst const2 = (IntConst) var2;
            if (const2.getVal() == 0){
                writeString("MOV R2, =0");
            }
            else if ((const2.getVal() & (const2.getVal() - 1)) == 0){
                int log2 = 0, val = const2.getVal();
                while (val != 1){
                    val = val >>> 1;
                    ++log2;
                }
                //是2的整数次幂
                writeString("MOV R2, R1, LSL#" + log2);
            }
            else {
                writeString("MUL R2, R1, R2");
            }
        }
        else {
            writeString("MUL R2, R1, R2");
        }
    }

    private void generateSubByOptimize(Symbol var1, Symbol var2) {
        if (var1 == var2){
            writeString("MOV R2, =0");
        }
        else {
            writeString("SUB R2, R1, R2");
        }
    }

    private void generateAddByOptimize(Symbol var1, Symbol var2) {
        if (var1 == var2){
            writeString("MOV R2, R1, LSL#1");
        }
        else {
            writeString("ADD R2, R1, R2");
        }
    }

    private void goCarryBackend(Symbol symbol) {
        Integer []lengths;
        if (symbol.getIsVar()){
            lengths = ((IntArrayVar)symbol).getLengths();
        }
        else {
            lengths = ((IntArrayConst)symbol).getLengths();
        }
        if (nowLastIndex.get(symbol) > 1){
            for (int j = initializeTable.get(symbol).length - 1; j >= nowLastIndex.get(symbol) - 1; --j){
                if (initializeTable.get(symbol)[j] >= lengths[j]){
                    initializeTable.get(symbol)[j - 1] += initializeTable.get(symbol)[j] / lengths[j];
                    initializeTable.get(symbol)[j] %= lengths[j];
                }
            }
        }
    }

    private void resultsGlobalAdd(String str, int localVarSize) {
        ++resultsCount;
        if (resultsCount > resultsMaxCount) {
            results.add("\t B " + lGotoAfterLtorg + globalDefineCount);
            results.add("\t .ltorg");

            SymbolTableInner now = varSymbolTable.getRoot();
            results.add(".L" + globalDefineCount + ":");
            for (Integer symbolIndex : usingGlobalVarOffsetsTransformTable.keySet()) {
                Symbol s = now.variables.get(symbolIndex);
                if (s.getIsVar() || s.getType() == TypeName.INTARRAY) {
                    results.add("\t.word " + s.getInnername());
                }
            }
            results.add(lGotoAfterLtorg + globalDefineCount + ":");
            resultsCount = 0;
            ++globalDefineCount;
            usingGlobalVarOffsetsTransformTable.clear();
        }

        if (str.contains(lGlobalStringIdentifier)){
            str = str.replace(lGlobalStringIdentifier, "");
            String originalOffsetStr = "";
            for (int i = str.lastIndexOf(lGlobalStringToBeReplaced) + lGlobalStringToBeReplaced.length() + 1; i < str.length(); ++i){
                if (Character.isDigit(str.charAt(i))) {
                    originalOffsetStr += str.charAt(i);
                }
                else {
                    break;
                }
            }
            int originalOffset = Integer.parseInt(originalOffsetStr), transformedOffset;
            if (usingGlobalVarOffsetsTransformTable.containsKey(originalOffset)){
                transformedOffset = usingGlobalVarOffsetsTransformTable.get(originalOffset);
            }
            else {
                transformedOffset = usingGlobalVarOffsetsTransformTable.size() * Symbol.INT_TYPE_LENGTH;
                usingGlobalVarOffsetsTransformTable.put(originalOffset, transformedOffset);
            }
            str = str.replace(lGlobalStringToBeReplaced + "+" + originalOffsetStr, ".L" + globalDefineCount + "+" + transformedOffset);
        }

        if (str.contains(lFuncStringVariableCountIdentifier)){
            results.add("\tLDR R3, =" + (localVarSize + fixedFunctionStackSize));
            str = str.replace(lFuncStringVariableCountIdentifier, "");
            str = str.replace(lFuncStringVariableCountToBeReplaced, "R3");
        }
        results.add(str);
    }

    private int Integer_8_10_16_parseInt(String str) {
        int radix;
        if (str.startsWith("0x") || str.startsWith("0X")){
            radix = 16;
            str = str.substring(2).toLowerCase();
        }
        else if (str.startsWith("0")){
            radix = 8;
        }
        else {
            radix = 10;
        }
        return new BigInteger(str, radix).intValue();
    }

    private void initBarIfNotExist(Symbol var) {
        int length;
        if (var.getIsVar()){
            length = ((IntArrayVar)var).getLengths().length;
        }
        else {
            length = ((IntArrayConst)var).getLengths().length;
        }

        if (!initializeTable.containsKey(var)){
            initializeTable.put(var, new Integer[length]);
            for (int i = 0; i < length; ++i){
                initializeTable.get(var)[i] = 0;
            }
        }
        if (!nowLastIndex.containsKey(var)) {
            nowLastIndex.put(var, 0);
        }
        if (!nowFinalIndex.containsKey(var)) {
            nowFinalIndex.put(var, 0);
        }
    }

    private Symbol getIntValueToRegister(String varname, String register, FourTuple line) throws PLDLAssemblingException {
        Symbol var;
        if (Character.isDigit(varname.charAt(0))) {
            var = varSymbolTable.addGlobalImmediateInt(Integer_8_10_16_parseInt(varname), line);
            loadImmediateIntToRegister(Integer_8_10_16_parseInt(varname), register);
        } else {
            var = varSymbolTable.getSymbol(nowBlockHeight, varname, line, isSettingUpVariableTable, true);
            if (var == null) {
                throw new PLDLAssemblingException("变量 " + varname + " 没有定义。", null);
            } else if (var.getType() == TypeName.INT || var.getType() == TypeName.ADDR) {
                readVarToRegister(var, register);
            } else {
                throw new PLDLAssemblingException("不能读取数组类型", null);
            }
        }
        return var;
    }

    private void writeRegisterToVar(String register, String tempRegister1, String tempRegister2, Symbol nowVar, int index) throws PLDLAssemblingException {
        //数组类型
        if (nowVar.getIsGlobal()) {
            loadAddrFromGlobalMemory(nowVar.getOffset(), tempRegister1);
            writeString("LDR " + tempRegister2 + ", =" + Symbol.INT_TYPE_LENGTH * index);
            writeString("ADD " + tempRegister1 + ", " + tempRegister1 + ", " + tempRegister2);
            writeString("STR " + register + ", [" + tempRegister1 + "]");
        } else {
            storeToLocalMemory(register, tempRegister1, (nowVar.getOffset() + nowVar.getSize() - Symbol.INT_TYPE_LENGTH * index + 4));
        }
    }

    private void writeRegisterToVar(String register, String tempRegister, Symbol nowVar) throws PLDLAssemblingException {
        //整数类型
        if (nowVar.getType() != TypeName.INTARRAY){
            if (!nowVar.getIsGlobal()){
                if (nowVar.getIsInRegister()){
                    writeString("MOV " + nowVar.getRegister() + ", " + register);
                }
                else {
                    storeToLocalMemory(register, tempRegister, (nowVar.getOffset() + nowVar.getSize() + 4));
                }
            }
            else if (nowVar.getIsVar()) {
                loadAddrFromGlobalMemory(nowVar.getOffset(), tempRegister);
                writeString("STR " + register + ", [" + tempRegister + "]");
            }
            else {
                /* Do nothing */
            }
        }
        else {
            throw new PLDLAssemblingException("不能读取数组类型", null);
        }
    }

    private void readVarToRegister(Symbol nowVar, String register) throws PLDLAssemblingException {
        //保证是整型
        if (nowVar.getType() != TypeName.INTARRAY){
            if (!nowVar.getIsGlobal()){
                if (nowVar.getIsInRegister()){
                    writeString("MOV " + register + ", " + nowVar.getRegister());
                }
                else {
                    loadFromLocalMemory((nowVar.getOffset() + nowVar.getSize() + 4), register);
                }
            }
            else if (nowVar.getIsVar()) {
                loadAddrFromGlobalMemory(nowVar.getOffset(), register);
                writeString("LDR " + register + ", [" + register + "]");
            }
            else {
                loadImmediateIntToRegister(((IntConst)nowVar).getVal(), register);
            }
        }
        else {
            throw new PLDLAssemblingException("不能读取数组类型", null);
        }
    }

    private void readVarAddrToRegister(Symbol nowVar, String register) throws PLDLAssemblingException {
        if (!nowVar.getIsGlobal()){
            loadAddrFromLocalMemory((nowVar.getOffset() + nowVar.getSize() + 4), register);
        }
        else if (nowVar.getIsVar() || nowVar.getType() == TypeName.INTARRAY){
            //全局变量或全局常量数组有地址
            loadAddrFromGlobalMemory(nowVar.getOffset(), register);
        }
        else {
            throw new PLDLAssemblingException("全局常量没有地址", null);
        }
    }

    private void loadAddrFromGlobalMemory(int offset, String register) {
        writeString(lGlobalStringIdentifier + "LDR " + register + ", " + lGlobalStringToBeReplaced + "+" + offset);
    }
    private void loadAddrFromLocalMemory(int offset, String register) {
        loadImmediateIntToRegister(offset, register);
        writeString("SUB " + register + ", FP, " + register);
    }
    private void loadFromLocalMemory(int offset, String register) {
        loadAddrFromLocalMemory(offset, register);
        writeString("LDR " + register + ", [" + register + "]");
    }
    private void storeToLocalMemory(String register, String tempRegister, int offset) {
        loadAddrFromLocalMemory(offset, tempRegister);
        writeString("STR " + register + ", [" + tempRegister + "]");
    }

    private void loadImmediateIntToRegister(int val, String register) {
        writeString("LDR " + register + ", =" + val);
    }

    //以下是优化

    //块内分配寄存器
    public void optimizeInBlock(){
        Map<BasicBlock, List<Symbol>> scopedInOneBasicBlockVars = new HashMap<>();
        Map<Symbol, Integer> minIndexOfBasicBlock = new HashMap<>();
        Map<Symbol, Integer> maxIndexOfBasicBlock = new HashMap<>();
        List<Symbol> allSymbols = varSymbolTable.getAllSymbolsInner(varSymbolTable.getRoot());

        for (Symbol symbol: allSymbols){
            if (symbol.getIsGlobal() || symbol.getType() != TypeName.INT){
                continue;
            }
            BasicBlock scopedBasicBlock = getScopedInOneBasicBlock(symbol);
            if (scopedBasicBlock != null){
                if (!scopedInOneBasicBlockVars.containsKey(scopedBasicBlock)){
                    scopedInOneBasicBlockVars.put(scopedBasicBlock, new ArrayList<>());
                }
                scopedInOneBasicBlockVars.get(scopedBasicBlock).add(symbol);
                int max = Integer.MIN_VALUE;
                int min = Integer.MAX_VALUE;

                for (FourTuple fourTuple: symbol.getFourTuple()){
                    int index = scopedBasicBlock.getFourTuples().indexOf(fourTuple);
                    if (index > max){
                        max = index;
                    }
                    if (index < min){
                        min = index;
                    }
                }
                minIndexOfBasicBlock.put(symbol, min);
                maxIndexOfBasicBlock.put(symbol, max + 1);
            }
        }

        for (BasicBlock basicBlock: scopedInOneBasicBlockVars.keySet()){
            Queue<String> idleRegisters = new ArrayDeque<>();
            idleRegisters.add("R4");
            idleRegisters.add("R5");
            idleRegisters.add("R6");
            idleRegisters.add("R7");
            idleRegisters.add("R8");
            idleRegisters.add("R9");
            idleRegisters.add("R10");
            for (int i = 0; i < basicBlock.getFourTuples().size(); ++i){
                List<Symbol> symbols = scopedInOneBasicBlockVars.get(basicBlock);
                for (Symbol symbol: symbols){
                    if (i == maxIndexOfBasicBlock.get(symbol)){
                        if (symbol.getIsInRegister()){
                            idleRegisters.add(symbol.getRegister());
                        }
                    }
                    else if (i == minIndexOfBasicBlock.get(symbol)){
                        if (!idleRegisters.isEmpty()){
                            String register = idleRegisters.poll();
                            symbol.setInRegister(true);
                            symbol.setRegister(register);
                        }
                    }
                }
            }
        }
    }

    private BasicBlock getScopedInOneBasicBlock(Symbol symbol) {
        BasicBlock result = null;

        for (FourTuple fourTuple: symbol.getFourTuple()){
            if (result == null){
                result = fourTuple.getBasicBlock();
            }
            else if (result != fourTuple.getBasicBlock()){
                return null;
            }
        }
        return result;
    }

    //压缩栈空间
    public void reConstructVars() throws PLDLAssemblingException {
        SymbolTableInner root = varSymbolTable.getRoot();
        for (int i = 0; i < root.children.size(); ++i){
            SymbolTableInner funcroot = root.children.get(i);
            int counter = 0;
            List<Symbol> allSymbols = varSymbolTable.getAllSymbolsInner(funcroot);
            for (Symbol symbol: allSymbols){
                if (symbol.getOffset() >= 0){
                    if (symbol.getIsInRegister()){
                        symbol.setOffset(-1);
                    }
                    else {
                        symbol.setOffset(counter);
                        counter += symbol.getSize();
                    }
                }
            }
        }
    }

    //消除死代码
    public void markDeadFourTuples(){
        List<Symbol> allSymbols = varSymbolTable.getAllSymbolsInner(varSymbolTable.getRoot());
        for (Symbol symbol: allSymbols){
            if (symbol.getIsValid()){
                rr_upToParents(symbol);
            }
        }
        for (Symbol symbol: allSymbols){
            for (FourTuple fourTuple: symbol.getFourTuple()){
                if (!fourTuple.getIsValid()){
                    continue;
                }
                else if (shouldBeInvalid(fourTuple)){
                    fourTuple.setIsValid(false);
                }
            }
        }
        for (String funcname: functions.keySet()){
            FuncParam func = functions.get(funcname);
            if (!func.isUseful()){
                for (FourTuple fourTuple: func.getFourTuples()){
                    fourTuple.setIsValid(false);
                }
            }
            else {
                for (FourTuple fourTuple: func.getFourTuples()){
                    //修复ret, ret的作用有结束当前函数，不能删除
                    /* ret, _, _, 返回值 */
                    //修复define, constdefine和arrayjoin
                    if (fourTuple.getTuples()[0].equals("ret") && !fourTuple.getIsValid()){
                        fourTuple.getTuples()[3] = "NULL";
                        fourTuple.setIsValid(true);
                    }
                    else if (fourTuple.getTuples()[0].equals("arrayjoin") ||
                            fourTuple.getTuples()[0].equals("define") ||
                            fourTuple.getTuples()[0].equals("constdefine")){
                        fourTuple.setIsValid(true);
                    }
                }
            }
        }
    }

    private boolean shouldBeInvalid(FourTuple fourTuple) {
        boolean result = true;
        for (Symbol s: fourTuple.getSymbol()){
            if (s.getIsValid()) {
                String funcname = fourTuple.getBasicBlock().getFuncname();
                if (functions.containsKey(funcname)) {
                    functions.get(funcname).setUseful(true);
                }
                result = false;
                break;
            }
        }
        return result;
    }

    private void rr_upToParents(Symbol root) {
        Queue<Symbol> symbols = new ArrayDeque<>();
        HashSet<Symbol> usedsymbols = new HashSet<>();
        symbols.add(root);
        while (!symbols.isEmpty()){
            Symbol symbol = symbols.poll();
            if (usedsymbols.contains(symbol)){
                continue;
            }
            symbol.setIsValid(true);
            usedsymbols.add(symbol);
            symbols.addAll(symbol.getParents());
        }
    }

    private List<List<Integer>> tarjanSearch(List<List<Integer>> intGraph, int nodeCount) {
        int counter = 0;
        boolean []isInStack = new boolean[nodeCount];
        Stack<Integer> dfsStack = new Stack<>();
        int []dfn = new int[nodeCount];
        int []low = new int[nodeCount];
        Arrays.fill(dfn, -1);
        Arrays.fill(low, -1);
        List<List<Integer> > clusterResults = new ArrayList<>();

        for (int i = 0; i < nodeCount; i++) {
            if (dfn[i] == -1) {
                counter = rr_TarjanSearch(i, intGraph, isInStack, dfsStack, dfn, low, clusterResults, counter);
            }
        }
        return clusterResults;
    }


    private int rr_TarjanSearch(int current, List<List<Integer> > intGraph, boolean []isInStack, Stack<Integer> dfsStack,
                                int []dfn, int []low, List<List<Integer>> clusterResults, int counter) {
        dfn[current] = low[current] = counter;
        counter = counter + 1;
        isInStack[current] = true;
        dfsStack.push(current);

        for (int i = 0; i < intGraph.get(current).size(); i++) {
            int next = intGraph.get(current).get(i);
            if (dfn[next] == -1) {
                counter = rr_TarjanSearch(next, intGraph, isInStack, dfsStack, dfn, low, clusterResults, counter);
                low[current] = Math.min(low[current], low[next]);
            } else if (isInStack[next]) {
                low[current] = Math.min(low[current], dfn[next]);
            }
        }

        if (low[current] == dfn[current]) {
            ArrayList<Integer> temp = new ArrayList<>();
            int j = -1;
            while (current != j) {
                j = dfsStack.pop();
                isInStack[j] = false;
                temp.add(j);
            }
            clusterResults.add(temp);
        }
        return counter;
    }

    public void optimizeRegisters() throws PLDLAssemblingException {
        //建立四元式与基本块之间的映射关系
        constructor = new BasicBlockGraphConstructor(inputs);
        constructor.resetFlowGraph();
        constructor.buildFlowGraph(ConstructMode.CALLOUTOFBLOCK);
        //对符号表进行优化：将符号放入寄存器
        this.optimizeInBlock();
        //压缩栈空间
        this.reConstructVars();
    }

    public void optimizeDeadTuples() {
        //建立四元式与基本块之间的映射关系
        constructor = new BasicBlockGraphConstructor(inputs);
        constructor.resetFlowGraph();
        constructor.buildFlowGraph(ConstructMode.CALLINBLOCK);
        this.linkParamsAcrossFunctionCall();
        this.linkReturnsAcrossFunctionCall();
        this.addVarsUseCompareFourTuples();
        this.markDeadFourTuples();
        this.fixCallingAndParams();
    }

    public void fixCallingAndParams() {
        //pushvar, param, call
        /* pushvar, 变量, _, 函数名 */
        /* call, 函数名, _, 返回值保存的变量 */
        /* param, 函数名, 类型，变量名 */

        /* func, _, 返回值类型, 函数名 */
        /* funcend, _, 返回值类型, 函数名 */

        for (FourTuple line: inputs){
            switch (line.getTuples()[0]){
                case "pushvar": {
                    String funcname = line.getTuples()[3];
                    if (functions.containsKey(funcname)){
                        FuncParam func = functions.get(funcname);
                        if (func.isUseful() && !line.getIsValid()){
                            line.getTuples()[1] = "0";
                            line.setIsValid(true);
                        }
                        else if (!func.isUseful()){
                            line.setIsValid(false);
                        }
                    }
                    else {
                        line.setIsValid(true);
                    }
                }
                break;

                case "call":
                case "param":   {
                    String funcname = line.getTuples()[1];
                    if (functions.containsKey(funcname)){
                        FuncParam func = functions.get(funcname);
                        line.setIsValid(func.isUseful());
                    }
                    else {
                        line.setIsValid(true);
                    }
                }
                break;

                case "func":
                case "funcend": {
                    String funcname = line.getTuples()[3];
                    FuncParam func = functions.get(funcname);
                    line.setIsValid(func.isUseful());
                }
                break;
            }
        }

        //调整符号表
        int counter = 0;
        List<SymbolTableInner> newInnerTable = new ArrayList<>();

        for (String funcname: functions.keySet()){
            FuncParam func = functions.get(funcname);
            if (func.isUseful()) {
                newInnerTable.add(varSymbolTable.getRoot().children.get(counter));
            }
            counter++;
        }
        varSymbolTable.getRoot().setChildren(newInnerTable);
    }

    private void linkReturnsAcrossFunctionCall() {
        //callers和returnees多对多
        for (FuncParam func: callers.keySet()) {
            if (!returnees.containsKey(func)){
                continue;
            }
            Set<Symbol> callersymbols = callers.get(func);
            for (Symbol callersymbol : callersymbols) {
                callersymbol.addToParents(returnees.get(func));
            }
        }
    }

    private void linkParamsAcrossFunctionCall() {
        //callparams和params多对一，且一一对应
        for (FuncParam func: paramers.keySet()){
            if (!preDefinedFunctions.containsKey(func.getName())) {
                Set<List<Symbol>> callersymbolsSet = paramers.get(func);
                for (List<Symbol> callersymbols : callersymbolsSet) {
                    for (int i = 0; i < func.getParams().size(); ++i) {
                        //从右往左进栈，所以是反的
                        Symbol callersymbol = callersymbols.get(func.getParams().size() - 1 - i);
                        Symbol paramsymbol = func.getParams().get(i);
                        paramsymbol.addToParents(callersymbol);
                        if (paramsymbol.getType() != TypeName.INT) {
                            callersymbol.addToParents(paramsymbol);
                        }
                    }
                }
            }
        }
    }

    private void addVarsUseCompareFourTuples() {
        //cmp, var1, var2, _
        //其中var1和var2是子基本块的所有变量的父亲变量

        //把basicblock转数字
        Map<Integer, BasicBlock> intToBasicBlockMap = new HashMap<>();
        Map<BasicBlock, Integer> basicBlockToIntMap = new HashMap<>();
        int counter = 0;
        for (String funcname: constructor.getBasicBlocks().keySet()){
            for (String labelname: constructor.getBasicBlocks().get(funcname).keySet()){
                intToBasicBlockMap.put(counter, constructor.getBasicBlocks().get(funcname).get(labelname));
                basicBlockToIntMap.put(constructor.getBasicBlocks().get(funcname).get(labelname), counter);
                ++counter;
            }
        }
        List<List<Integer> > inputToTarjan = new ArrayList<>();
        for (int i = 0; i < counter; ++i){
            List<BasicBlock> childBlocks = intToBasicBlockMap.get(i).getChildren();
            inputToTarjan.add(new ArrayList<>());
            for (BasicBlock childBlock: childBlocks){
                inputToTarjan.get(i).add(basicBlockToIntMap.get(childBlock));
            }
        }
        List<List<Integer> > resultsSet = tarjanSearch(inputToTarjan, counter);
        List<List<Integer> > results = new ArrayList<>();
        for (int i = 0; i < counter; ++i){
            results.add(new ArrayList<>());
        }
        for (List<Integer> result: resultsSet){
            for (Integer x: result){
                results.get(x).addAll(result);
            }
        }
        for (FourTuple line: inputs){
            if (!line.getIsValid()){
                continue;
            }
            String []tuple = line.getTuples();
            if (tuple[0].equals("cmp")){
                Map.Entry<Symbol, Symbol> symbol2 = cmpLinkingVars.get(line);
                Symbol var1 = symbol2.getKey();
                Symbol var2 = symbol2.getValue();
                List<BasicBlock> childblocks = line.getBasicBlock().getChildren();
                Set<FourTuple> childlines = new HashSet<>();
                for (BasicBlock childblock: childblocks){
                    List<Integer> connectedblocks = results.get(basicBlockToIntMap.get(childblock));
                    for (Integer x: connectedblocks){
                        childlines.addAll(intToBasicBlockMap.get(x).getFourTuples());
                    }
                }
                Set<Symbol> childsymbols = new HashSet<>();
                for (FourTuple childline: childlines){
                    childsymbols.addAll(childline.getSymbol());
                }
                for (Symbol childsymbol: childsymbols){
                    childsymbol.addToParents(var1);
                    childsymbol.addToParents(var2);
                }
            }
        }
    }

    private boolean isUsefulPreFunctions(String funcname) {
        switch (funcname){
            case "putint":
            case "putch":
            case "putarray":
                return true;
        }
        return false;
    }

}
