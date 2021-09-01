package pldl;

public class GrammarDefinition {
    String production;
    String []movements;
    String []beforeMovements;
    String []afterMovements;

    GrammarDefinition(String production, String []movements, String []beforeMovements, String []afterMovements){
        this.production = production;
        this.movements = movements;
        this.beforeMovements = beforeMovements;
        this.afterMovements = afterMovements;
    }

    public String getProduction() {
        return production;
    }

    public String[] getMovements() {
        return movements;
    }

    public String[] getBeforeMovements() {
        return beforeMovements;
    }

    public String[] getAfterMovements() {
        return afterMovements;
    }
}