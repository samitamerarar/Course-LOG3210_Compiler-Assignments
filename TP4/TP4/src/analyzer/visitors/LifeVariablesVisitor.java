package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;


/**
 * Created: 19-02-15
 * Last Changed: 19-11-14
 * Author: Félix Brunet & Doriane Olewicki
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class LifeVariablesVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private /*final*/ PrintWriter m_writer;

    public LifeVariablesVisitor(PrintWriter writer) {
        m_writer = writer;
    }
    public HashMap<String, StepStatus> allSteps = new HashMap<>();

    private int step = 0;
    private HashSet<String> previous_step = new HashSet<>();

    /*Afin de pouvoir garder en memoire les variables a ajouter au REF*/
    private HashSet<String> current_ref_ids = new HashSet<>();
    
    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genStep() {
        return "_step" + step++;
    }
    
    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        compute_IN_OUT();
        for (int i = 0; i < step; i++) {
            m_writer.write("===== STEP " + i + " ===== \n" + allSteps.get("_step" + i).toString());
        }
        return null;
    }

    @Override
    public Object visit(ASTDeclaration node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        String step = genStep();
        StepStatus s = new StepStatus();
        allSteps.put(step, s);

        for (String e : previous_step) {
            allSteps.get(step).PRED.add(e);
            allSteps.get(e).SUCC.add(step);
        }

        // Update previous step to current
        previous_step = new HashSet<>();
        previous_step.add(step);

        node.childrenAccept(this, previous_step);
        return previous_step;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        //Expression

        //Ajouter les variables referencees dans la condition
        //a l'ensemble REF (current_ref_ids est remplie au visiteur ASTIdentifier)
        current_ref_ids.clear();
        node.jjtGetChild(0).jjtAccept(this, data);
        allSteps.get("_step" + (step-1)).REF = (HashSet<String>) current_ref_ids.clone();

        HashSet<String> mem_previous_step = (HashSet<String>) previous_step.clone();
        HashSet<String> if_previous_step;
        if (node.jjtGetNumChildren() == 2) {
            if_previous_step = (HashSet<String>) previous_step.clone(); // if no else
        }
        else {
            if_previous_step = new HashSet<>(); // if there is a else
        }
        for(int i=1; i < node.jjtGetNumChildren(); i++ ){
            //reset previous for each block
            previous_step = (HashSet<String>) mem_previous_step.clone();
            node.jjtGetChild(i).jjtAccept(this,data);
            for (String e : previous_step) {
                if_previous_step.add(e);
            }
        }

        previous_step = (HashSet<String>) if_previous_step.clone();
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        String current_step = "_step" + (step-1);

        //Ajouter les variables referencees dans la condition
        // a l'ensemble REF (current_ref_ids est remplie au visiteur ASTIdentifier)
        current_ref_ids.clear();
        node.jjtGetChild(0).jjtAccept(this, data);
        allSteps.get(current_step).REF = (HashSet<String>) current_ref_ids.clone();

        //Accepter tous les enfants autres que la condition
        for(int i=1; i < node.jjtGetNumChildren(); i++ ){
            node.jjtGetChild(i).jjtAccept(this,data);
        }

        //Definition des ensembles PRED et SUCC
        allSteps.get(current_step).PRED.addAll(previous_step);
        for(String e: allSteps.get(current_step).PRED){
            allSteps.get(e).SUCC.add(current_step);
        }

        //Assignation du previous_step
        HashSet<String> while_previous_step = new HashSet<>();
        while_previous_step.add(current_step);
        previous_step = (HashSet<String>)while_previous_step.clone();
        return null;
    }


    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String current_step = "_step" + (step-1);
        
        //Ajouter la variable qui se fait assigner une valeur a l'ensemble DEF
        node.jjtGetChild(0).jjtAccept(this, data);
        allSteps.get(current_step).DEF.add(((ASTIdentifier)node.jjtGetChild(0)).getValue());

        //Ajouter les variables referencees a l'ensemble REF (current_ref_ids est remplie au visiteur ASTIdentifier)
        current_ref_ids.clear();
        node.jjtGetChild(1).jjtAccept(this, data);
        allSteps.get(current_step).REF = (HashSet<String>) current_ref_ids.clone();
        return null;
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBoolValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        node.childrenAccept(this, data);
        current_ref_ids.add("" + node.getValue());
        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    } 

    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class StepStatus {
        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();
        public HashSet<String> IN  = new HashSet<String>();
        public HashSet<String> OUT = new HashSet<String>();

        public HashSet<String> SUCC  = new HashSet<String>();
        public HashSet<String> PRED  = new HashSet<String>();

        public String toString() {
            String buff = "";
            buff += "REF : " + set_ordered(REF) +"\n";
            buff += "DEF : " + set_ordered(DEF) +"\n";
            buff += "IN  : " + set_ordered(IN) +"\n";
            buff += "OUT : " + set_ordered(OUT) +"\n";

            buff += "SUCC: " + set_ordered(SUCC) +"\n";
            buff += "PRED: " + set_ordered(PRED) +"\n";
            buff += "\n";
            return buff;
        }

        public String set_ordered(HashSet<String> s) {
            List<String> list = new ArrayList<String>(s);
            Collections.sort(list);

            return list.toString();
        }
    }

    //Implementation 1 a 1 de l'algorithme fourni dans l'enonce
    private void compute_IN_OUT() {
        Stack<String> work_stack = new Stack<>();
        work_stack.push("_step" + (step - 1));
        while(!work_stack.isEmpty()){
            String current_node = work_stack.pop();
            for(String succ_node: allSteps.get(current_node).SUCC){
                allSteps.get(current_node).OUT.addAll(allSteps.get(succ_node).IN);
            }
            HashSet<String> OLD_IN = (HashSet<String>) allSteps.get(current_node).IN.clone();

            //OUT[ node ] − DEF[ node ]
            HashSet<String> out_minus_def = (HashSet<String>) allSteps.get(current_node).OUT.clone();
            out_minus_def.removeAll(allSteps.get(current_node).DEF);
            out_minus_def.addAll(allSteps.get(current_node).REF);

            //IN [ node ] = ( OUT[ node ] − DEF[ node ] ) union REF[ node ]
            allSteps.get(current_node).IN = (HashSet<String>) out_minus_def.clone();

            if(!(allSteps.get(current_node).IN.equals(OLD_IN))){
                for(String pred_node: allSteps.get(current_node).PRED){
                    work_stack.push(pred_node);
                }
            }
        }
        return;
    }
}
