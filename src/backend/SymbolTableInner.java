package backend;

import java.util.List;

class SymbolTableInner {
    List<SymbolTableInner> children;
    List<Symbol> variables;
    SymbolTableInner parent;

    public void setChildren(List<SymbolTableInner> newChildren) {
        children = newChildren;
    }
}
