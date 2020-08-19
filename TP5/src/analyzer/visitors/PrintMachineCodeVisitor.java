package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    private Integer REG = 256; // default register limitation
    private ArrayList<String> RETURNED = new ArrayList<String>(); // returned variables from the return statement
    private ArrayList<MachLine> CODE = new ArrayList<MachLine>(); // representation of the Machine Code in Machine lines (MachLine)
    private ArrayList<String> LOADED = new ArrayList<String>(); // could be use to keep which variable/pointer are loaded/ defined while going through the intermediate code
    private ArrayList<String> MODIFIED = new ArrayList<String>(); // could be use to keep which variable/pointer are modified while going through the intermediate code

    private HashMap<String, String> OP; // map to get the operation name from it's value

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OP = new HashMap<>();
        OP.put("+", "ADD");
        OP.put("-", "MIN");
        OP.put("*", "MUL");
        OP.put("/", "DIV");


    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        // Visiter les enfants
        node.childrenAccept(this, null);

        compute_LifeVar(); // first Life variables computation (should be recalled when machine code generation)
        compute_NextUse(); // first Next-Use computation (should be recalled when machine code generation)
        compute_machineCode(); // generate the machine code from the CODE array (the CODE array should be transformed

        for (int i = 0; i < CODE.size(); i++) // print the output
            m_writer.println(CODE.get(i));
        return null;
    }


    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        REG = ((ASTIntValue) node.jjtGetChild(0)).getValue(); // get the limitation of register
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNED.add("@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue()); // returned values (here are saved in "@*somthing*" format, you can change that if you want.
            CODE.get(CODE.size() - 1).Life_OUT.add(RETURNED.get(i));
        }
        for (String var : MODIFIED) {
            if (RETURNED.contains(var)) {
                ArrayList<String> mem_load = new ArrayList<>();
                mem_load.add("ST");
                mem_load.add(var.replace("@", ""));
                mem_load.add(var);
                CODE.add(new MachLine(mem_load));
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);
        String op_name = OP.get(node.getOp());

        ArrayList<String> line = new ArrayList<>();
        line.add(op_name);
        line.add(assigned);
        line.add(left);
        if (!LOADED.contains(left) && !left.matches("^@{1}t{1}[0-9]*$") && !left.contains("#")) {
            ArrayList<String> load_left = new ArrayList<>();
            load_left.add("LD");
            load_left.add(left);
            load_left.add(left.replace("@", ""));
            CODE.add(new MachLine(load_left));
            LOADED.add(left);
        }

        line.add(right);
        if (!LOADED.contains(right) && !right.matches("^@{1}t{1}[0-9]*$") && !right.contains("#")) {
            ArrayList<String> load_right = new ArrayList<>();
            load_right.add("LD");
            load_right.add(right);
            load_right.add(right.replace("@", ""));
            CODE.add(new MachLine(load_right));
            LOADED.add(right);
        }

        if (!MODIFIED.contains(assigned) && !assigned.matches("^@{1}t{1}[0-9]*$")) {
            MODIFIED.add(assigned);
        }

        CODE.add(new MachLine(line));
        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        ArrayList<String> line = new ArrayList<>();
        line.add("MIN");
        line.add(assigned);
        line.add("#0");
        line.add(left);
        if (!LOADED.contains(left) && !left.matches("^@{1}t{1}[0-9]*$") && !left.contains("#")) {
            ArrayList<String> load = new ArrayList<>();
            load.add("LD");
            load.add(left);
            load.add(left.replace("@", ""));
            CODE.add(new MachLine(load));
            LOADED.add(left);
        }

        if (!MODIFIED.contains(assigned) && !assigned.matches("^@{1}t{1}[0-9]*$")) {
            MODIFIED.add(assigned);
        }

        CODE.add(new MachLine(line));
        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        ArrayList<String> line = new ArrayList<>();
        line.add("ADD");
        line.add(assigned);
        line.add("#0");
        line.add(left);
        if (!LOADED.contains(left) && !left.matches("^@{1}t{1}[0-9]*$") && !left.contains("#")) {
            ArrayList<String> load = new ArrayList<>();
            load.add("LD");
            load.add(left);
            load.add(left.replace("@", ""));
            CODE.add(new MachLine(load));
            LOADED.add(left);
        }

        if (!MODIFIED.contains(assigned) && !assigned.matches("^@{1}t{1}[0-9]*$")) {
            MODIFIED.add(assigned);
        }

        CODE.add(new MachLine(line));
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        //nothing to do here
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        //nothing to do here
        return "#" + String.valueOf(node.getValue());
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        //nothing to do here
        return "@" + node.getValue();
    }


    private class NextUse {
        // NextUse class implementation: you can use it or redo it you're way
        public HashMap<String, ArrayList<Integer>> nextuse = new HashMap<String, ArrayList<Integer>>();

        public NextUse() {

        }

        public NextUse(HashMap<String, ArrayList<Integer>> nextuse) {
            this.nextuse = nextuse;
        }

        public void add(String s, int i) {
            if (!nextuse.containsKey(s)) {
                nextuse.put(s, new ArrayList<Integer>());
            }
            nextuse.get(s).add(i);
        }

        public String toString() {
            String buff = "";
            boolean first = true;
            for (String k : set_ordered(nextuse.keySet())) {
                if (!first) {
                    buff += ", ";
                }
                buff += k + ":" + nextuse.get(k);
                first = false;
            }
            return buff;
        }

        public Object clone() {
            return new NextUse((HashMap<String, ArrayList<Integer>>) nextuse.clone());
        }
    }


    private class MachLine {
        List<String> line;
        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();
        public HashSet<Integer> SUCC = new HashSet<Integer>();
        public HashSet<Integer> PRED = new HashSet<Integer>();
        public HashSet<String> Life_IN = new HashSet<String>();
        public HashSet<String> Life_OUT = new HashSet<String>();

        public NextUse Next_IN = new NextUse();
        public NextUse Next_OUT = new NextUse();

        public MachLine(List<String> s) {
            this.line = s;
            int size = CODE.size();

            // PRED, SUCC, REF, DEF already computed (cadeau
            if (size > 0) {
                PRED.add(size - 1);
                CODE.get(size - 1).SUCC.add(size);
            }
            this.DEF.add(s.get(1));
            for (int i = 2; i < s.size(); i++)
                if (s.get(i).charAt(0) == '@')
                    this.REF.add(s.get(i));
        }

        public String toString() {
            String buff = "";

            // print line :
            buff += line.get(0) + " " + line.get(1);
            for (int i = 2; i < line.size(); i++)
                buff += ", " + line.get(i);
            buff += "\n";
            buff += "// Life_IN  : " + set_ordered(Life_IN).toString() + "\n";
            buff += "// Life_OUT : " + set_ordered(Life_OUT).toString() + "\n";
            buff += "// Next_IN  : " + Next_IN.toString() + "\n";
            buff += "// Next_OUT : " + Next_OUT.toString() + "\n";
            return buff;
        }
    }

    private void compute_LifeVar() {
        Stack<Integer> work_stack = new Stack<>();
        work_stack.push(CODE.size() - 1);
        ArrayList<String> lastLine = new ArrayList<>(RETURNED);
        lastLine.removeAll(MODIFIED);
        CODE.get(CODE.size() - 1).Life_OUT.addAll(lastLine);
        while (!work_stack.isEmpty()) {
            Integer current_line = work_stack.pop();
            for (Integer succ_line : CODE.get(current_line).SUCC) {
                CODE.get(current_line).Life_OUT.addAll(CODE.get(succ_line).Life_IN);
            }

            HashSet<String> OLD_IN = (HashSet<String>) CODE.get(current_line).Life_IN.clone();

            //(OUT[ node ] − DEF[ node ]) union REF[ node ]
            HashSet<String> out_minus_def = (HashSet<String>) CODE.get(current_line).Life_OUT.clone();
            out_minus_def.removeAll(CODE.get(current_line).DEF);
            out_minus_def.addAll(CODE.get(current_line).REF);

            //IN [ node ] = ( OUT[ node ] − DEF[ node ] ) union REF[ node ]
            CODE.get(current_line).Life_IN = (HashSet<String>) out_minus_def.clone();

            if (!(CODE.get(current_line).Life_IN.equals(OLD_IN))) {
                for (Integer pred_line : CODE.get(current_line).PRED) {
                    work_stack.push(pred_line);
                }
            }
        }
    }

    private void compute_NextUse() {
        Stack<Integer> work_stack = new Stack<>();
        work_stack.push(CODE.size() - 1);
        while (!work_stack.isEmpty()) {
            Integer current_line = work_stack.pop();
            for (Integer succ_line : CODE.get(current_line).SUCC) {
                CODE.get(current_line).Next_OUT.nextuse.putAll(CODE.get(succ_line).Next_IN.nextuse);
            }

            for (Map.Entry<String, ArrayList<Integer>> entry : CODE.get(current_line).Next_OUT.nextuse.entrySet()) {
                if (!CODE.get(current_line).DEF.contains(entry.getKey())) {
                    CODE.get(current_line).Next_IN.nextuse.put(entry.getKey(), entry.getValue());
                }
            }

            for (String node : CODE.get(current_line).REF) {
                ArrayList<Integer> line_value = new ArrayList<>();
                line_value.add(current_line);
                if (CODE.get(current_line).Next_IN.nextuse.get(node) != null)
                    line_value.addAll(CODE.get(current_line).Next_IN.nextuse.get(node));
                CODE.get(current_line).Next_IN.nextuse.put(node, line_value);
            }

            for (Integer pred_line : CODE.get(current_line).PRED) {
                work_stack.push(pred_line);
            }
        }
    }

    public void compute_machineCode() {
        boolean doneColoring = false;
        while (!doneColoring) {
            initGraph();
            doneColoring = colorGraphRegister();
        }

        //Remplacer les pointeurs avec registres
        for (int i = 0; i < CODE.size(); i++) {
            for (int j = 0; j < CODE.get(i).line.size(); j++) {
                if (color_map.containsKey(CODE.get(i).line.get(j)))
                    CODE.get(i).line.set(j, "R" + color_map.get(CODE.get(i).line.get(j)));
            }
        }
    }

    public List<String> set_ordered(Set<String> s) {
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    // TODO: add any class you judge necessary, and explain them in the report. GOOD LUCK!
    private HashMap<String, Integer> color_map; //Pour associer un pointeur a un registre
    private Graph graph; //Pour representer le graph d'interference

    //Methode pour construire le graph d'interference
    private void initGraph() {
        graph = new Graph();
        color_map = new HashMap<>();

        //Init noeuds
        for (int i = 0; i < CODE.size(); i++) {
            for (Map.Entry<String, ArrayList<Integer>> entry : CODE.get(i).Next_OUT.nextuse.entrySet()) {
                if (!graph.graph_nodes.contains(entry.getKey())) {
                    graph.graph_nodes.add(entry.getKey());
                }
            }
        }

        //Init arretes
        for (int i = 0; i < CODE.size(); i++) {
            for (Map.Entry<String, ArrayList<Integer>> entryA : CODE.get(i).Next_OUT.nextuse.entrySet()) {
                for (Map.Entry<String, ArrayList<Integer>> entryB : CODE.get(i).Next_OUT.nextuse.entrySet()) {
                    String nodeA = entryA.getKey();
                    String nodeB = entryB.getKey();
                    if (!nodeA.equals(nodeB)) {
                        graph.graph_edges.add(new Edge(nodeA, nodeB));
                    }
                }
            }
        }
    }

    //Coloriage du graph d'interferance pour l'assignation des registres.
    //Retourne false si un noeud a ete spill, true si le coloriage a reussi.
    private boolean colorGraphRegister() {
        HashSet<Edge> temp_edges = (HashSet<Edge>) graph.graph_edges.clone();
        ArrayList<String> temp_nodes = (ArrayList<String>) graph.graph_nodes.clone();
        Stack<String> node_stack = new Stack<>();
        HashSet<String> valid_max_nodes = new HashSet<>();
        HashSet<String> spill_max_nodes = new HashSet<>();

        //--- 1) Retirer le noeud ayant le plus de voisins et l'inserer dans la stack ---//
        while (!temp_nodes.isEmpty()) {

            //Identifier les noeuds ayant le plus de voisins en dessous de REG (valid_max_nodes)
            // et au dessus (spill_max_nodes)
            String selected_node = "";
            int max_neighbor_count = 0;
            for (String node : temp_nodes) {
                int current_neighbor_count = 0;
                for (Edge edge : temp_edges) { //Compter le nombre de voisins de chaque noeud
                    if (edge.nodeA.equals(node) || edge.nodeB.equals(node)) {
                        current_neighbor_count++;
                    }
                }
                if (current_neighbor_count > max_neighbor_count) { //Si nouveau maximum...
                    max_neighbor_count = current_neighbor_count;
                    if (current_neighbor_count < REG) {
                        valid_max_nodes.clear(); //Nouvel ensemble de noeuds valides
                        valid_max_nodes.add(node);
                    } else {
                        spill_max_nodes.clear(); //Nouvel ensemble de noeuds a spill
                        spill_max_nodes.add(node);
                    }
                } else if (current_neighbor_count == max_neighbor_count) { //Si meme valeur que le maximum
                    if (current_neighbor_count < REG) {
                        valid_max_nodes.add(node); //Un noeud de plus dans l'ensemble valide
                    } else {
                        spill_max_nodes.add(node); //Un noeud de plus dans l'ensemble a spill
                    }
                }
            }

            //Selectioner le noeud ayant le plus de voisins
            if (valid_max_nodes.size() != 0) { //Si on a des noeuds valides
                selected_node = set_ordered(valid_max_nodes).get(0);
                valid_max_nodes.clear();
            } else {
                selected_node = set_ordered(spill_max_nodes).get(0);
                if (doSpill(selected_node)) { //noeud a ete spill
                    return false;
                } else { //noeud n'a pas pu etre spill
                    spill_max_nodes.clear();
                }
            }

            //Retirer les noeuds et ses arretes
            temp_nodes.remove(selected_node);
            HashSet<Edge> remove_edges = new HashSet<>();
            for (Edge edge : temp_edges) {
                if (edge.nodeA.equals(selected_node) || edge.nodeB.equals(selected_node)) {
                    remove_edges.add(edge);
                }
            }
            temp_edges.removeAll(remove_edges);

            //Push to stack
            node_stack.push(selected_node);
        }

        //---------- 2) Reconstruire et colorier le graph noeud par noeud ---------------//
        while (!node_stack.isEmpty()) {
            String cur_node = node_stack.pop();

            //Remettre le noeud et ses voisins dans le graph
            temp_nodes.add(cur_node);
            for (Edge edge : graph.graph_edges) {
                if (edge.nodeA.equals(cur_node)) {
                    if (temp_nodes.contains(edge.nodeB))
                        temp_edges.add(edge);
                } else if (edge.nodeB.equals(cur_node)) {
                    if (temp_nodes.contains(edge.nodeA))
                        temp_edges.add(edge);
                }
            }

            //Trouver les voisins et ajouter leur couleur a une liste
            int color = 0;
            ArrayList<Integer> color_list = new ArrayList<>();
            for (Map.Entry entry : color_map.entrySet()) {
                String evaluated_node = (String) entry.getKey();
                for (Edge edge : temp_edges) {
                    if (edge.nodeA.equals(cur_node)) {
                        if (edge.nodeB.equals(evaluated_node)) {
                            color_list.add((Integer) entry.getValue());
                        }
                    } else if (edge.nodeB.equals(cur_node)) {
                        if (edge.nodeA.equals(evaluated_node)) {
                            color_list.add((Integer) entry.getValue());
                        }
                    }
                }
            }

            //Assigner une couleur qui n'est pas deja assigner aux voisins
            while (color_list.contains(color)) {
                color++;
            }
            color_map.put(cur_node, color);
            color_list.clear();
        }
        return true;
    }

    //Execute l'algorithme de spill. Retourne true si le noeud a ete spill, false sinon.
    private boolean doSpill(String node) {
        //Find first reference to node in OP line
        int first = 0;
        int added_counter = 0;
        for (int i = 0; i < CODE.size(); i++) {
            if (CODE.get(i).line.get(0).matches("ADD|MIN|MUL|DIV")) {
                if (CODE.get(i).line.contains(node)) {
                    first = i;
                    break;
                }
            }
        }

        //Si le noeud est modifie lors de sa premiere reference
        if (CODE.get(first).line.get(1).equals(node)) {
            ArrayList<String> currentLine = new ArrayList<>();
            currentLine.add("ST");
            currentLine.add(node.replace("!", "").replace("@", ""));
            currentLine.add(node);
            CODE.add(first + 1, new MachLine(currentLine));
            added_counter++;
        }

        //Si le noeud peut etre spill
        if (CODE.get(first).Next_OUT.nextuse.get(node) != null) {
            ArrayList<String> currentLine = new ArrayList<>();
            currentLine.add("LD");
            currentLine.add(node + "!");
            currentLine.add(node.replace("!", "").replace("@", ""));
            CODE.add(
                    CODE.get(first).Next_OUT.nextuse.get(node).get(0) + added_counter,
                    new MachLine(currentLine)
            );
            added_counter++;
            for (int i = CODE.get(first).Next_OUT.nextuse.get(node).get(0) + added_counter; i < CODE.size(); i++) {
                if (CODE.get(i).line.contains(node)) {
                    Collections.replaceAll(CODE.get(i).line, node, node + "!");
                }
            }
        } else {
            recompute_code();
            return false;
        }
        recompute_code();
        return true;
    }

    //Recalcule les attributs de CODE en le reconstruisant ligne par ligne
    private void recompute_code() {
        //Since PRED and SUCC need to be recomputed:
        ArrayList<ArrayList<String>> temp_lines = new ArrayList<>();
        for (int i = 0; i < CODE.size(); i++) {
            temp_lines.add(new ArrayList<>(CODE.get(i).line));
        }
        CODE.clear();
        for (int i = 0; i < temp_lines.size(); i++) {
            CODE.add(new MachLine(temp_lines.get(i)));
        }
        compute_LifeVar();
        compute_NextUse();
    }

    //Pour representer le graph d'intersection
    private static class Graph {
        public HashSet<Edge> graph_edges; //On utilise HashSet pour eliminer les doublons
        public ArrayList<String> graph_nodes;

        public Graph() {
            graph_edges = new HashSet<>();
            graph_nodes = new ArrayList<>();
        }
    }

    //Pour representer les arretes dans le graph d'intersection
    private static class Edge {
        public String nodeA;
        public String nodeB;

        public Edge(String nodeA, String nodeB) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
        }

        @Override
        public String toString() {
            return "[ " + nodeA + "; " + nodeB + " ]";
        }

        @Override
        public boolean equals(Object obj) {
            Edge other = (Edge) obj;
            return (this.nodeA.equals(other.nodeA) && this.nodeB.equals(other.nodeB)) ||
                    (this.nodeA.equals(other.nodeB) && this.nodeB.equals(other.nodeA));
        }

        //Necessaire pour eliminer les doublons dans le HashSet de Graph
        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < this.nodeA.length(); i++) {
                hash += this.nodeA.charAt(i);
            }
            for (int i = 0; i < this.nodeB.length(); i++) {
                hash += this.nodeB.charAt(i);
            }
            return hash;
        }
    }
}
