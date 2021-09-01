package backend;

import util.StringGenerator;
import java.util.*;

enum ConstructMode {
    CALLINBLOCK,
    CALLOUTOFBLOCK
};

public class BasicBlockGraphConstructor {
    private final List<FourTuple> fourTuples;
    private static final String startLabel = "start";
    private static final String startFunc = "global";
    private static final String tempLabel = "opti_tmp_";
    private final Set<String> preDefinedFunctions;
    private final String prefix_userfunc = "userfunc";
    private final Map<String, Set<BasicBlock>> funcWillReturnBlocks;

    public Map<String, Map<String, BasicBlock>> getBasicBlocks() {
        return basicBlocks;
    }

    private final Map<String, Map<String, BasicBlock>> basicBlocks;

    public BasicBlockGraphConstructor(List<FourTuple> fourTuples) {
        this.fourTuples = new ArrayList<>();
        this.fourTuples.addAll(fourTuples);
        this.basicBlocks = new HashMap<>();
        this.funcWillReturnBlocks = new HashMap<>();
        this.preDefinedFunctions = new HashSet<String>(){{
            add(prefix_userfunc + "getint");
            add(prefix_userfunc + "getch");
            add(prefix_userfunc + "getarray");
            add(prefix_userfunc + "putint");
            add(prefix_userfunc + "putch");
            add(prefix_userfunc + "putarray");
            add(prefix_userfunc + "starttime");
            add(prefix_userfunc + "stoptime");
        }};
    }

    private BasicBlock getBasicBlock(String nowFuncName, String nowLabelName){
        if (!basicBlocks.containsKey(nowFuncName)){
            basicBlocks.put(nowFuncName, new HashMap<>());
        }
        Map<String, BasicBlock> outerPointer = basicBlocks.get(nowFuncName);
        if (!outerPointer.containsKey(nowLabelName)){
            outerPointer.put(nowLabelName, new BasicBlock(nowFuncName, nowLabelName));
        }
        return outerPointer.get(nowLabelName);
    }

    private void addToLinkedList(LinkedHashSet<BasicBlock> blocks, BasicBlock block){
        if (!blocks.contains(block)){
            blocks.add(block);
        }
    }

    public void resetFlowGraph(){
        funcWillReturnBlocks.clear();
        basicBlocks.clear();
        for (FourTuple line: fourTuples){
            line.setBasicBlock(null);
        }
    }

    public void buildFlowGraph(ConstructMode mode) {
        String nowFuncName = startFunc;
        String newLabelName = startLabel;
        BasicBlock globalBlock = getBasicBlock(nowFuncName, newLabelName);
        if (mode == ConstructMode.CALLINBLOCK) {
            BasicBlock pointer = globalBlock;
            LinkedHashSet<BasicBlock> results = new LinkedHashSet<>();

            for (FourTuple line : fourTuples) {
                String[] tuple = line.getTuples();
                switch (tuple[0]) {
                    case "func": {
                        nowFuncName = tuple[3];
                        newLabelName = startLabel;
                        addToLinkedList(results, pointer);
                        pointer = getBasicBlock(nowFuncName, newLabelName);
                        pointer.addFourTuple(line);
                    }
                    break;

                    case "funcend": {
                        pointer.addFourTuple(line);
                        addToLinkedList(results, pointer);
                        pointer = globalBlock;
                    }
                    break;

                    case "beq":
                    case "bne": {
                        String objLabelName = tuple[3];
                        BasicBlock objBlock = getBasicBlock(nowFuncName, objLabelName);
                        pointer.getChildren().add(objBlock);
                        objBlock.getParents().add(pointer);
                        pointer.addFourTuple(line);
                        newLabelName = tempLabel + StringGenerator.getNextCode();
                        BasicBlock nextBlock = getBasicBlock(nowFuncName, newLabelName);
                        pointer.getChildren().add(nextBlock);
                        nextBlock.getParents().add(pointer);
                        addToLinkedList(results, pointer);
                        pointer = nextBlock;
                    }
                    break;

                    case "b": {
                        String objLabelName = tuple[3];
                        BasicBlock objBlock = getBasicBlock(nowFuncName, objLabelName);
                        pointer.getChildren().add(objBlock);
                        objBlock.getParents().add(pointer);
                        pointer.addFourTuple(line);
                        newLabelName = tempLabel + StringGenerator.getNextCode();
                        BasicBlock nextBlock = getBasicBlock(nowFuncName, newLabelName);
                        addToLinkedList(results, pointer);
                        pointer = nextBlock;
                    }
                    break;

                    case "label": {
                        newLabelName = tuple[3];
                        BasicBlock nextBlock = getBasicBlock(nowFuncName, newLabelName);
                        pointer.getChildren().add(nextBlock);
                        nextBlock.getParents().add(pointer);
                        addToLinkedList(results, pointer);
                        pointer = nextBlock;
                        pointer.addFourTuple(line);
                    }
                    break;

                    default:
                        pointer.addFourTuple(line);
                }
            }
            addToLinkedList(results, pointer);
        }
        else {
            BasicBlock pointer = globalBlock;
            LinkedHashSet<BasicBlock> results = new LinkedHashSet<>();

            for (FourTuple line: fourTuples){
                String []tuple = line.getTuples();
                switch (tuple[0]){
                    case "func" : {
                        nowFuncName = tuple[3];
                        newLabelName = startLabel;
                        addToLinkedList(results, pointer);
                        pointer = getBasicBlock(nowFuncName, newLabelName);
                        pointer.addFourTuple(line);
                    }
                    break;

                    case "funcend":{
                        pointer.addFourTuple(line);
                        addToLinkedList(results, pointer);
                        pointer = globalBlock;
                    }
                    break;

                    case "ret": {
                        if (!funcWillReturnBlocks.containsKey(nowFuncName)){
                            funcWillReturnBlocks.put(nowFuncName, new HashSet<>());
                        }
                        funcWillReturnBlocks.get(nowFuncName).add(pointer);
                        pointer.addFourTuple(line);
                    }
                    break;

                    case "beq":
                    case "bne":
                    case "b":{
                        String objLabelName = tuple[3];
                        BasicBlock objBlock = getBasicBlock(nowFuncName, objLabelName);
                        pointer.getChildren().add(objBlock);
                        objBlock.getParents().add(pointer);
                        pointer.addFourTuple(line);
                        newLabelName = tempLabel + StringGenerator.getNextCode();
                        BasicBlock nextBlock = getBasicBlock(nowFuncName, newLabelName);
                        pointer.getChildren().add(nextBlock);
                        nextBlock.getParents().add(pointer);
                        addToLinkedList(results, pointer);
                        pointer = nextBlock;
                    }
                    break;

                    case "label": {
                        newLabelName = tuple[3];
                        BasicBlock nextBlock = getBasicBlock(nowFuncName, newLabelName);
                        pointer.getChildren().add(nextBlock);
                        nextBlock.getParents().add(pointer);
                        addToLinkedList(results, pointer);
                        pointer = nextBlock;
                        pointer.addFourTuple(line);
                    }
                    break;

                    /* call, 函数名, _, 返回值保存的变量 */
                    case "call": {
                        String objFuncName = tuple[1];
                        if (preDefinedFunctions.contains(objFuncName)){
                            pointer.addFourTuple(line);
                        }
                        else {
                            String objLabelName = startLabel;
                            BasicBlock nextBlock = getBasicBlock(objFuncName, objLabelName);
                            pointer.getChildren().add(nextBlock);
                            nextBlock.getParents().add(pointer);
                            pointer.addFourTuple(line);
                            newLabelName = tempLabel + StringGenerator.getNextCode();
                            addToLinkedList(results, pointer);
                            pointer = getBasicBlock(nowFuncName, newLabelName);
                            if (funcWillReturnBlocks.containsKey(objFuncName)) {
                                for (BasicBlock willReturnBlock : funcWillReturnBlocks.get(objFuncName)) {
                                    willReturnBlock.getChildren().add(pointer);
                                    pointer.getParents().add(willReturnBlock);
                                }
                            }
                        }
                    }
                    break;

                    default:
                        pointer.addFourTuple(line);
                }
            }
            addToLinkedList(results, pointer);
        }
    }
}
