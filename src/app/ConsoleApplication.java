package app;

import backend.ARMTransformer;
import backend.FourTuple;
import backend.SymbolTable;
import exception.PLDLAnalysisException;
import exception.PLDLAssemblingException;
import exception.PLDLParsingException;
import generator.Generator;
import lexer.Lexer;
import lexer.NFA;
import lexer.SimpleREApply;
import parser.AnalysisTree;
import parser.CFG;
import parser.CFGProduction;
import parser.TransformTable;
import pldl.*;
import symbol.Symbol;
import symbol.SymbolPool;
import translator.MovementCreator;
import translator.Translator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConsoleApplication {

    private File []testfolders = {
            new File("compiler2021/公开用例与运行时库/function_test2020"),
            new File("compiler2021/公开用例与运行时库/function_test2021"),
            new File("compiler2021/公开用例与运行时库/functional_test"),
            new File("compiler2021/公开用例与运行时库/performance_test2021_pre"),
            new File("compiler2021/公开用例与运行时库/mytest"),
            new File("compiler2021/公开用例与运行时库/初赛用例/functional"),
            new File("compiler2021/公开用例与运行时库/初赛用例/h_functional"),
            new File("compiler2021/公开用例与运行时库/初赛用例/performance"),
            new File("compiler2021/公开用例与运行时库/初赛用例/h_performance"),
            new File("compiler2021/公开用例与运行时库/决赛隐藏用例/h_performance"),
    };

    private Lexer lexer = null;
    private CFG cfg = null;
    private TransformTable table = null;
    private Set<Character> emptyChars = null;
    private Translator translator = null;
    private Generator generator = null;

    private final String assembler = "arm-linux-gnueabi-gcc";
    private final String assembleConfig = "-static -L./res -lsysy -march=armv7-a";
    private final String outputConfig = "-o";
    protected final String[] args;

    public ConsoleApplication(String[] args) {
        this.args = args;
    }

    protected void LLParse(InputStream codeInputStream, OutputStream fourTupleOutputStream, OutputStream assembleOutputStream)
            throws PLDLAnalysisException, PLDLParsingException, IOException, PLDLAssemblingException {
        int size = codeInputStream.available();
        byte[] buffer = new byte[size];
        int readin = codeInputStream.read(buffer);
        if (readin != size){
            throw new IOException("代码文件读取大小与文件大小不一致。");
        }
        codeInputStream.close();
        String codestr = new String(buffer, StandardCharsets.UTF_8);
        //不支持中文
        codestr = codestr.replaceAll("[^\\x00-\\x7F]", "");
        List<Symbol> symbols = lexer.analysis(codestr, emptyChars);
        symbols = cfg.revertToStdAbstractSymbols(symbols);
        symbols = cfg.eraseComments(symbols);
        AnalysisTree tree = table.getAnalysisTree(symbols);
        translator.checkMovementsMap();
        translator.doTreesMovements(tree);
        List<String> comm = new ArrayList<>();
        generator.doTreesMovements(tree, comm);
        //建立四元式
        List<FourTuple> rt4 = new ArrayList<>();
        for (int i = 0; i < comm.size(); ++i){
            rt4.add(new FourTuple(comm.get(i), i));
        }
        //输出四元式
        if (fourTupleOutputStream != null) {
            PrintStream fourTuplePrintStream = new PrintStream(fourTupleOutputStream);
            for (FourTuple fourTuple : rt4) {
                fourTuplePrintStream.println(fourTuple);
            }
            fourTuplePrintStream.close();
        }

        //建立符号表
        SymbolTable varSymbolTable = new SymbolTable();
        //第一遍：生成汇编丢弃，为了获取所有变量定义
        ARMTransformer transformer = new ARMTransformer(rt4, true, varSymbolTable);
        transformer.setDivBySoftware(false);
        transformer.parse();
//        transformer.optimizeRegisters();
//        transformer.optimizeDeadTuples();
        transformer.clear();
        //第二遍：重新生成汇编
        transformer.setSettingUpVariableTable(false);
//        for (FourTuple line: rt4){
//            System.out.println(line + ": " + line.getIsValid() + ": " + Arrays.toString(line.getSymbol().toArray()));
//        }
        List<String> results = transformer.parse();
        //输出生成的汇编
        PrintStream assemblePrintStream = new PrintStream(assembleOutputStream);
        for (String s: results){
            assemblePrintStream.println(s);
        }
        assemblePrintStream.close();
    }

    protected int executeCmd(String command) throws Exception {
        System.out.println("Execute command : " + command);
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(new String[]{"sh", "-c", command});
        process.waitFor();
        int retVal = process.exitValue();
        Scanner sc = new Scanner(process.getErrorStream());
        while (sc.hasNextLine()){
            System.out.println(sc.nextLine());
        }
        return retVal;
    }

    protected boolean executeProgram(String command, String input, String stdOutput, String userOutput) throws Exception {
        if (!new File(input).exists()){
            input = "";
        }
        else {
            input = " < " + input;
        }
        command = command + input + " > " + userOutput;
        int retVal = executeCmd(command);
        if (retVal == 139){
            return false;
        }

        if (new File(stdOutput).exists() && new File(userOutput).exists()) {
            Scanner sc1 = new Scanner(new FileInputStream(stdOutput));
            Scanner sc2 = new Scanner(new FileInputStream(userOutput));

            while (sc1.hasNextLine() && sc2.hasNextLine()) {
                String str1 = sc1.nextLine();
                String str2 = sc2.nextLine();
                if (!str1.equals(str2)) {
                    System.out.println("不匹配:" + str1 + " 与 " + str2);
                    return false;
                }
            }

            String str1 = sc1.nextLine();
            if (Integer.parseInt(str1) % 256 != retVal) {
                System.out.println("不匹配:" + str1 + " 与 " + retVal);
                return false;
            }
        }
        return true;
    }

    protected void prepare() throws Exception {
        Language sysy = new SysYLanguage();
        translator = new Translator();
        generator = new Generator();
        Set<String> terminals = new HashSet<>();
        Set<String> nonterminals = new HashSet<>();
        Set<String> comments = new HashSet<>();
        List<String> prods = new ArrayList<>();
        List<List<AnalysisTree> > movementsTrees = new ArrayList<>();
        List<List<AnalysisTree> > beforeGenerationsTrees = new ArrayList<>();
        List<List<AnalysisTree> > afterGenerationsTrees = new ArrayList<>();
        List<Map.Entry<String, NFA> > terminalsNFA = new ArrayList<>();
        for (GrammarDefinition grammarDefinition : sysy.getGrammarDefinition()){
            String production = grammarDefinition.getProduction().trim();
            String []movements = grammarDefinition.getMovements();
            String []beforemovements = grammarDefinition.getBeforeMovements();
            String []aftermovements = grammarDefinition.getAfterMovements();
            prods.add(production);
            nonterminals.add(production.split("->")[0].trim());
            movementsTrees.add(getAnalysisTreeList(movements, translator));
            beforeGenerationsTrees.add(getAnalysisTreeList(beforemovements, generator));
            afterGenerationsTrees.add(getAnalysisTreeList(aftermovements, generator));
        }
        for (TerminalRegex terminalRegex: sysy.getTerminalRegexes()){
            terminals.add(terminalRegex.getTerminal());
        }
        for (CommentRegex commentRegex: sysy.getCommentRegexes()){
            comments.add(commentRegex.getComment());
        }
        for (String prod: prods){
            String []afters = prod.split("->")[1].trim().split(" +");
            for (String s: afters){
                String after = s.trim();
                if (!after.equals("null") && !comments.contains(after) && !nonterminals.contains(after) && !terminals.contains(after)){
                    terminalsNFA.add(new AbstractMap.SimpleEntry<>(after, NFA.fastNFA(after)));
                    terminals.add(after);
                }
            }
        }
        for (TerminalRegex terminalRegex: sysy.getTerminalRegexes()){
            terminalsNFA.add(new AbstractMap.SimpleEntry<>(terminalRegex.getTerminal(), new SimpleREApply(terminalRegex.getRegex()).getNFA()));
        }
        for (CommentRegex commentRegex: sysy.getCommentRegexes()){
            terminalsNFA.add(new AbstractMap.SimpleEntry<>(commentRegex.getComment(), new SimpleREApply(commentRegex.getRegex()).getNFA()));
        }

        SymbolPool pool = new SymbolPool();
        pool.initTerminalString(terminals);
        pool.initNonterminalString(nonterminals);
        for (String comment: comments){
            pool.addCommentStr(comment);
        }
        Set<CFGProduction> productions = new HashSet<>();
        for (int i = 0; i < prods.size(); ++i){
            CFGProduction production = CFGProduction.getCFGProductionFromCFGString(prods.get(i), pool);
            production.setSerialNumber(i + 1);
            productions.add(production);
            translator.addToMovementsMap(production, movementsTrees.get(i));
            generator.addToMovementsMap(production, beforeGenerationsTrees.get(i), afterGenerationsTrees.get(i));
        }
        cfg = new CFG(pool, productions, sysy.getMarkInStr());
        lexer = new Lexer(terminalsNFA, null);

        emptyChars = new HashSet<>();
        emptyChars.add(' ');
        emptyChars.add('\t');
        emptyChars.add('\n');
        emptyChars.add('\r');
        emptyChars.add('\f');
        table = cfg.getTable();
    }

    protected List<AnalysisTree> getAnalysisTreeList(String[] movements, MovementCreator creator) throws PLDLAnalysisException, PLDLParsingException {
        if (movements == null){
            return new ArrayList<>();
        }
        else {
            List<AnalysisTree> movementsTree = new ArrayList<>();
            for (String movement : movements) {
                movementsTree.add(creator.getMovementTree(movement.trim()));
            }
            return movementsTree;
        }
    }

    protected void testFiles() throws Exception {
        int counter = 0;
        int truecounter = 0;
        for (File folder: testfolders){
            File []testfiles = folder.listFiles();
            Arrays.sort(testfiles, new Comparator<File>() {
                @Override
                public int compare(File file, File t1) {
                    return file.getAbsolutePath().compareTo(t1.getAbsolutePath());
                }
            });
            for (File f: testfiles){
                if (f.getName().endsWith(".sy")){
                    try {
                        System.out.println(f.getAbsolutePath());
                        ++counter;
                        clearfile(f);
                        runFile(f);
                        ++truecounter;
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("PASS: " + truecounter + "/" + counter);
    }

    protected void clearFiles(){
        for (File folder: testfolders){
            File []testfiles = folder.listFiles();
            for (File f: testfiles){
                clearfile(f);
            }
        }
        System.out.println("Clean Finished.");
    }

    protected void runFile(File f) throws Exception {
        String codeFileName, fourTupleFileName, assembleFileName, outFileName, outFileInputName, outFileOutputName, outFileStdOutputName;

        codeFileName = f.getAbsolutePath();
        String pureFileName = codeFileName.substring(0, codeFileName.length() - 3);
        fourTupleFileName = pureFileName + ".4tu";
        assembleFileName = pureFileName + ".s";
        outFileName = pureFileName + ".exe";
        outFileInputName = pureFileName + ".in";
        outFileStdOutputName = pureFileName + ".out";
        outFileOutputName = pureFileName + ".myout";
        //compile
        LLParse(new FileInputStream(codeFileName), new FileOutputStream(fourTupleFileName), new FileOutputStream(assembleFileName));
        //assemble
        boolean t1 = (executeCmd(assembler + " " + assembleFileName + " " + outputConfig + " " + outFileName + " " + assembleConfig) == 0);
        //run
        boolean t2 = executeProgram(outFileName, outFileInputName, outFileStdOutputName, outFileOutputName);
        if (!t1 || !t2){
            throw new Exception(null, null);
        }
    }

    protected void compilefile(String input, String output) throws Exception {
        LLParse(new FileInputStream(input), null, new FileOutputStream(output));
    }

    protected void calculate() throws Exception {
        //compiler -S -o testcase.s testcase.sy
        String inputfile = null, outputfile = null;
        for (int i = 0; i < args.length; ++i){
            String arg = args[i];
            if (arg.trim().equals("-o")){
                outputfile = args[i + 1];
            }
            else if (!arg.trim().equals("-S") && !arg.trim().contains("-O")){
                inputfile = args[i];
            }
        }
        this.compilefile(inputfile, outputfile);
    }

    protected void clearfile(File f){
        String codeFileName, fourTupleFileName, assembleFileName, outFileName, outFileInputName, outFileOutputName, outFileStdOutputName;

        codeFileName = f.getAbsolutePath();
        String pureFileName = codeFileName.substring(0, codeFileName.length() - 3);
        fourTupleFileName = pureFileName + ".4tu";
        assembleFileName = pureFileName + ".s";
        outFileName = pureFileName + ".exe";
        outFileOutputName = pureFileName + ".myout";

        new File(fourTupleFileName).delete();
        new File(assembleFileName).delete();
        new File(outFileName).delete();
        new File(outFileOutputName).delete();
    }

}
