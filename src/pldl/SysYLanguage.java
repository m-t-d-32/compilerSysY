package pldl;

public class SysYLanguage implements Language{
    public GrammarDefinition[] getGrammarDefinition() {
        return grammarDefinition;
    }

    public TerminalRegex[] getTerminalRegexes() {
        return terminalRegexes;
    }

    public CommentRegex[] getCommentRegexes() {
        return commentRegexes;
    }

    private GrammarDefinition[] grammarDefinition = {
            new GrammarDefinition("Program -> CompUnit",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("CompUnit -> CompUnitOrNo Decl",new String[]{"go($1)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("CompUnit -> CompUnitOrNo FuncDef",new String[]{"go($1)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("CompUnitOrNo -> CompUnit",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("CompUnitOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("Decl -> ConstDecl",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("Decl -> VarDecl",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstDecl -> const int ConstDef ConstDefMulti ;",new String[]{"go($4)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstDefMulti -> , ConstDef ConstDefMulti",new String[]{"go($3)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstDefMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("ConstDef -> ConstAllIdentDef = ConstInitVal",new String[]{"go($1)","$3(varname) = $1(varname)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstAllIdentDef -> Ident ConstExpMulti",new String[]{"go($1)","go($2)","$$(varname) = $1(val)"},new String[]{},new String[]{"gen(constdefine, $2(arraylen), NULL, $1(val))"}),
            new GrammarDefinition("ConstExpMulti -> ConstExpMulti [ ConstExp ]",new String[]{"$$(arraylen) = newTemp(arraylen)","$1(lastlen) = $$(arraylen)","go($1)","go($3)"},new String[]{},new String[]{"gen(arrayjoin, $3(val), $$(lastlen), $$(arraylen))"}),
            new GrammarDefinition("ConstExpMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("ConstInitVal -> ConstExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{"gen(constinit, $$(val), NULL, $$(varname))"}),
            new GrammarDefinition("ConstInitVal -> { ConstInitValPlusOrNo }",new String[]{"$2(varname) = $$(varname)","go($2)"},new String[]{"gen(innerconstinitnew, NULL, NULL, $$(varname))"},new String[]{"gen(outerconstinitnew, NULL, NULL, $$(varname))"}),
            new GrammarDefinition("ConstInitValPlusOrNo -> ConstInitVal ConstInitValMulti",new String[]{"$1(varname) = $$(varname)","$2(varname) = $$(varname)","go($1)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstInitValPlusOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("ConstInitValMulti -> , ConstInitVal ConstInitValMulti",new String[]{"$2(varname) = $$(varname)","$3(varname) = $$(varname)","go($2)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("ConstInitValMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("VarDecl -> int VarDef VarDefMulti ;",new String[]{"go($2)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("VarDefMulti -> , VarDef VarDefMulti",new String[]{"go($2)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("VarDefMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("VarDef -> AllIdentDef",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("VarDef -> AllIdentDef = InitVal",new String[]{"go($1)","$3(varname) = $1(varname)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("AllIdentDef -> Ident ConstExpMulti",new String[]{"go($1)","go($2)","$$(varname) = $1(val)"},new String[]{},new String[]{"gen(define, $2(arraylen), NULL, $1(val))"}),
            new GrammarDefinition("InitVal -> Exp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{"gen(init, $$(val), NULL, $$(varname))"}),
            new GrammarDefinition("InitVal -> { InitValOrNo }",new String[]{"$2(varname) = $$(varname)","go($2)"},new String[]{"gen(innerinitnew, NULL, NULL, $$(varname))"},new String[]{"gen(outerinitnew, NULL, NULL, $$(varname))"}),
            new GrammarDefinition("InitValOrNo -> InitVal InitValMulti",new String[]{"$1(varname) = $$(varname)","$2(varname) = $$(varname)","go($1)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("InitValOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("InitValMulti -> , InitVal InitValMulti",new String[]{"$2(varname) = $$(varname)","$3(varname) = $$(varname)","go($2)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("InitValMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("FuncDef -> int Func ( FuncFParamsOrNo ) FuncBlock",new String[]{"go($2)", "$$(name) = $2(val)","$4(funcname) = $$(name)","go($4)","go($6)"},new String[]{"gen(func, NULL, $1(val), $2(val))","gen(in, NULL, NULL, NULL)"},new String[]{"gen(out, NULL, NULL, NULL)","gen(funcend, NULL, $1(val), $2(val))"}),
            new GrammarDefinition("FuncDef -> void Func ( FuncFParamsOrNo ) FuncBlock",new String[]{"go($2)", "$$(name) = $2(val)","$4(funcname) = $$(name)","go($4)","go($6)"},new String[]{"gen(func, NULL, $1(val), $2(val))","gen(in, NULL, NULL, NULL)"},new String[]{"gen(out, NULL, NULL, NULL)","gen(funcend, NULL, $1(val), $2(val))"}),
            new GrammarDefinition("FuncFParamsOrNo -> FuncFParams",new String[]{"$1(funcname) = $$(funcname)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("FuncFParamsOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("FuncFParams -> FuncFParam FuncFParamMulti",new String[]{"$1(funcname) = $$(funcname)","$2(funcname) = $$(funcname)","go($2)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("FuncFParamMulti -> , FuncFParam FuncFParamMulti",new String[]{"$2(funcname) = $$(funcname)","$3(funcname) = $$(funcname)","go($2)","go($3)"},new String[]{},new String[]{}),
            new GrammarDefinition("FuncFParamMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("FuncFParam -> int Ident ExpMultiOrNo",new String[]{"go($2)", "go($3)"},new String[]{},new String[]{"gen(param, $$(funcname), $1(val), $2(val))"}),
            new GrammarDefinition("ExpMultiOrNo -> [ ] ExpMulti",new String[]{"$$(arraylen) = newTemp(arraylen)","$3(lastlen) = $$(arraylen)","go($3)"},new String[]{"gen(arrayjoin, NULL, NULL, $$(arraylen))"},new String[]{}),
            new GrammarDefinition("ExpMultiOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("ExpMulti -> ExpMulti [ Exp ]",new String[]{"$$(arraylen) = newTemp(arraylen)","$1(lastlen) = $$(arraylen)","go($1)","go($3)"},new String[]{},new String[]{"gen(arrayjoin, $3(val), $$(lastlen), $$(arraylen))"}),
            new GrammarDefinition("ExpMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("Block -> { BlockItemMulti }",new String[]{"$2(loopstart) = $$(loopstart)","$2(loopend) = $$(loopend)","go($2)"},new String[]{"gen(in, NULL, NULL, NULL)"},new String[]{"gen(out, NULL, NULL, NULL)"}),
            new GrammarDefinition("FuncBlock -> { BlockItemMulti }",new String[]{"$2(loopstart) = newTemp(label)","$2(loopend) = newTemp(label)","go($2)"},new String[]{"gen(subparams, NULL, NULL, NULL)"},new String[]{}),
            new GrammarDefinition("BlockItemMulti -> BlockItem BlockItemMulti",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","$2(loopstart) = $$(loopstart)","$2(loopend) = $$(loopend)","go($1)","go($2)"},new String[]{},new String[]{}),
            new GrammarDefinition("BlockItemMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("BlockItem -> Decl",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("BlockItem -> Stmt",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("Stmt -> LVal = Exp ;",new String[]{"go($1)","go($3)","$$(val) = $1(val)"},new String[]{},new String[]{"gen(assign, $3(val), NULL, $1(val))"}),
            new GrammarDefinition("Stmt -> Exp ;",new String[]{"go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("Stmt -> ;",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("Stmt -> Block",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("Stmt -> if ( IfCond ) ThenStmt ElseStmtOrNo",new String[]{"$5(loopstart) = $$(loopstart)","$5(loopend) = $$(loopend)","$6(loopstart) = $$(loopstart)","$6(loopend) = $$(loopend)","go($3)","go($5)","go($6)","$3(elsestart) = $6(start)","$5(elseend) = $6(end)"},new String[]{},new String[]{}),
            new GrammarDefinition("ThenStmt -> Stmt",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","go($1)"},new String[]{},new String[]{"gen(b, NULL, NULL, $$(elseend))"}),
            new GrammarDefinition("ElseStmtOrNo -> else Stmt",new String[]{"$2(loopstart) = $$(loopstart)","$2(loopend) = $$(loopend)","$$(start) = newTemp(label)","$$(end) = newTemp(label)","go($2)"},new String[]{"gen(label, NULL, NULL, $$(start))"},new String[]{"gen(label, NULL, NULL, $$(end))"}),
            new GrammarDefinition("ElseStmtOrNo -> null",new String[]{"$$(start) = newTemp(label)","$$(end) = $$(start)"},new String[]{"gen(label, NULL, NULL, $$(start))"},new String[]{}),
            new GrammarDefinition("Stmt -> while ( WhileCond ) WhileStmt",new String[]{"go($3)","$5(while1start) = $3(start)","$5(end) = newTemp(label)","$5(loopstart) = $3(start)","$5(loopend) = $5(end)","$3(while2end) = $5(end)","go($5)"},new String[]{},new String[]{}),
            new GrammarDefinition("WhileStmt -> Stmt",new String[]{"$1(loopstart) = $$(loopstart)","$1(loopend) = $$(loopend)","go($1)"},new String[]{},new String[]{"gen(b, NULL, NULL, $$(while1start))","gen(label, NULL, NULL, $$(end))"}),
            new GrammarDefinition("Stmt -> break ;",new String[]{},new String[]{},new String[]{"gen(b, NULL, NULL, $$(loopend))"}),
            new GrammarDefinition("Stmt -> continue ;",new String[]{},new String[]{},new String[]{"gen(b, NULL, NULL, $$(loopstart))"}),
            new GrammarDefinition("Stmt -> return Exp ;",new String[]{"go($2)"},new String[]{},new String[]{"gen(ret, NULL, NULL, $2(val))"}),
            new GrammarDefinition("Stmt -> return ;",new String[]{},new String[]{},new String[]{"gen(ret, NULL, NULL, NULL)"}),
            new GrammarDefinition("Exp -> AddExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("IfCond -> LOrExp",new String[]{"go($1)"},new String[]{},new String[]{"gen(cmp, $1(val), 0, NULL)","gen(beq, NULL, NULL, $$(elsestart))"}),
            new GrammarDefinition("WhileCond -> LOrExp",new String[]{"$$(start) = newTemp(label)","go($1)"},new String[]{"gen(label, NULL, NULL, $$(start))"},new String[]{"gen(cmp, $1(val), 0, NULL)","gen(beq, NULL, NULL, $$(while2end))"}),
            new GrammarDefinition("LVal -> Ident",new String[]{"go($1)", "$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("LVal -> Ident LinkExpMulti",new String[]{"go($1)", "$2(val) = newTemp(link)","go($2)","$$(val) = $2(val)"},new String[]{},new String[]{"gen(getvar, $2(val), NULL, $1(val))"}),
            new GrammarDefinition("LinkExpMulti -> LinkExpMulti [ Exp ]",new String[]{"go($1)","go($3)","$$(val) = newTemp(link)"},new String[]{},new String[]{"gen(link, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("LinkExpMulti -> [ Exp ]",new String[]{"go($2)","$$(val) = newTemp(link)"},new String[]{},new String[]{"gen(link, NULL, $2(val), $$(val))"}),
            new GrammarDefinition("PrimaryExp -> ( Exp )",new String[]{"go($2)","$$(val) = $2(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("PrimaryExp -> LVal",new String[]{"go($1)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(putvar, $1(val), NULL, $$(val))"}),
            new GrammarDefinition("PrimaryExp -> Number",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("Number -> int_const",new String[]{"$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("UnaryExp -> PrimaryExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("UnaryExp -> Func ( FuncRParamsOrNo )",new String[]{"go($1)", "$3(funcname) = $1(val)","go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(call, $1(val), NULL, $$(val))"}),
            new GrammarDefinition("FuncRParamsOrNo -> FuncRParams",new String[]{"$1(funcname) = $$(funcname)","go($1)"},new String[]{},new String[]{}),
            new GrammarDefinition("FuncRParamsOrNo -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("UnaryExp -> - UnaryExp",new String[]{"go($2)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(neg, $2(val), NULL, $$(val))"}),
            new GrammarDefinition("UnaryExp -> + UnaryExp",new String[]{"go($2)","$$(val) = $2(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("UnaryExp -> ! UnaryExp",new String[]{"go($2)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(not, $2(val), NULL, $$(val))"}),
            new GrammarDefinition("FuncRParams -> Exp CommaExpMulti",new String[]{"$2(funcname) = $$(funcname)","go($1)","go($2)"},new String[]{},new String[]{"gen(pushvar, $1(val), NULL, $$(funcname))"}),
//            new GrammarDefinition("Exp -> StrConst",new String[]{"$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("CommaExpMulti -> , Exp CommaExpMulti",new String[]{"$3(funcname) = $$(funcname)","go($2)","go($3)"},new String[]{},new String[]{"gen(pushvar, $2(val), NULL, $$(funcname))"}),
            new GrammarDefinition("CommaExpMulti -> null",new String[]{},new String[]{},new String[]{}),
            new GrammarDefinition("MulExp -> UnaryExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("MulExp -> MulExp * UnaryExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(mul, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("MulExp -> MulExp / UnaryExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(div, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("MulExp -> MulExp % UnaryExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(mod, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("AddExp -> MulExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("AddExp -> AddExp + MulExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(add, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("AddExp -> AddExp - MulExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(sub, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("RelExp -> AddExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("RelExp -> RelExp < AddExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(lt, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("RelExp -> RelExp > AddExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(gt, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("RelExp -> RelExp <= AddExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(lte, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("RelExp -> RelExp >= AddExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(gte, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("EqExp -> RelExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("EqExp -> EqExp == RelExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(equ, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("EqExp -> EqExp != RelExp",new String[]{"go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(neq, $1(val), $3(val), $$(val))"}),
            new GrammarDefinition("LAndExp -> EqExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("LAndExp -> LAndNextExp && LAndEndExp",new String[]{"$$(end) = newTemp(label)","$$(end2) = newTemp(label)","$1(end) = $$(end)","$3(end) = $$(end)","go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(add, 0, $3(val), $$(val))","gen(b, NULL, NULL, $$(end2))","gen(label, NULL, NULL, $$(end))","gen(clr, NULL, NULL, $$(val))","gen(label, NULL, NULL, $$(end2))"}),
            new GrammarDefinition("LAndEndExp -> EqExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("LAndNextExp -> LAndExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{"gen(cmp, $1(val), 0, NULL)","gen(beq, NULL, NULL, $$(end))"}),
            new GrammarDefinition("LOrExp -> LAndExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("LOrExp -> LOrNextExp || LOrEndExp",new String[]{"$$(end) = newTemp(label)","$$(end2) = newTemp(label)","$1(end) = $$(end)","$3(end) = $$(end)","go($1)","go($3)","$$(val) = newTemp(var)"},new String[]{},new String[]{"gen(add, 0, $3(val), $$(val))","gen(b, NULL, NULL, $$(end2))","gen(label, NULL, NULL, $$(end))","gen(str, NULL, NULL, $$(val))","gen(label, NULL, NULL, $$(end2))"}),
            new GrammarDefinition("LOrEndExp -> LAndExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("LOrNextExp -> LOrExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{"gen(cmp, $1(val), 0, NULL)","gen(bne, NULL, NULL, $$(end))"}),
            new GrammarDefinition("ConstExp -> AddExp",new String[]{"go($1)","$$(val) = $1(val)"},new String[]{},new String[]{}),
            new GrammarDefinition("Ident -> var_name", new String[]{"$$(addname) = str(uservar)", "$$(val) = $$(addname) + $1(val)"}, new String[]{}, new String[]{}),
            new GrammarDefinition("Func -> var_name", new String[]{"$$(addname) = str(userfunc)", "$$(val) = $$(addname) + $1(val)"}, new String[]{}, new String[]{})
    };
    private TerminalRegex terminalRegexes[] = {
            new TerminalRegex("var_name","[_a-zA-Z][_a-zA-Z0-9]*"),
            new TerminalRegex("int_const","[1-9][0-9]*|0[0-7]*|0[xX][0-9a-fA-F]+"),
//            new TerminalRegex("StrConst","\"[^\"]*\"")
    };
    private CommentRegex commentRegexes[] = {
            new CommentRegex("comment1","/\\*([^\\*]|(\\*)*[^\\*/])*(\\*)*\\*/"),
            new CommentRegex("comment2","//[^\\r\\n]*(\\r\\n|\\r|\\n)")
    };

    public String getMarkInStr(){
        return "Program";
    }
}

