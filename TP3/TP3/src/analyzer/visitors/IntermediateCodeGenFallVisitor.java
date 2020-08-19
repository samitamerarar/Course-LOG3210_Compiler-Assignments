package analyzer.visitors;

import analyzer.ast.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import sun.awt.Symbol;

import java.awt.*;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;


/**
 * Created: 19-02-15
 * Last Changed: 19-10-20
 * Author: Félix Brunet & Doriane Olewicki
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class IntermediateCodeGenFallVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private final PrintWriter m_writer;

    public IntermediateCodeGenFallVisitor(PrintWriter writer) {
        m_writer = writer;
    }
    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    private int id = 0;
    private int label = 0;
    private final String fall = "fall";
    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genId() {
        return "_t" + id++;
    }

    //génère un nouveau Label qu'il est possible de print.
    private String genLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        return null;
    }

    /*
    Code fournis pour remplir la table de symbole.
    Les déclarations ne sont plus utile dans le code à trois adresse.
    elle ne sont donc pas concervé.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        VarType t;
        if(node.getValue().equals("bool")) {
            t = VarType.Bool;
        } else {
            t = VarType.Number;
        }
        SymbolTable.put(id.getValue(), t);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        if(node.jjtGetNumChildren() == 2){
            BoolLabel B = new BoolLabel();
            B.lTrue = fall;
            String S_next = genLabel();
            B.lFalse = S_next;

            node.jjtGetChild(0).jjtAccept(this, B);
            node.jjtGetChild(1).jjtAccept(this, S_next);
            m_writer.println(S_next);
        }
        else if (node.jjtGetNumChildren() == 3){
            BoolLabel B = new BoolLabel();
            B.lTrue = fall;
            B.lFalse = genLabel();
            String S_next = genLabel();

            node.jjtGetChild(0).jjtAccept(this, B);
            node.jjtGetChild(1).jjtAccept(this, S_next);
            m_writer.println("goto " + S_next);
            m_writer.println(B.lFalse);
            node.jjtGetChild(2).jjtAccept(this, S_next);
            m_writer.println(S_next);
        }
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        BoolLabel B = new BoolLabel();
        String begin = genLabel();
        B.lTrue = fall;
        String S_next = genLabel();
        B.lFalse = S_next;

        m_writer.println(begin);
        node.jjtGetChild(0).jjtAccept(this, B);
        node.jjtGetChild(1).jjtAccept(this, begin);
        m_writer.println("goto " + begin);
        m_writer.println(S_next);
        return null;
    }

    /*
     *  la difficulté est d'implémenter le "short-circuit" des opérations logiques combinez à l'enregistrement des
     *  valeurs booléennes dans des variables.
     *
     *  par exemple,
     *  a = b || c && !d
     *  deviens
     *  if(b)
     *      t1 = 1
     *  else if(c)
     *      if(d)
     *         t1 = 1
     *      else
     *         t1 = 0
     *  else
     *      t1 = 0
     *  a = t1
     *
     *  qui est équivalent à :
     *
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //Assign
     *  LTrue
     *  a = 1
     *  goto LEnd
     *  LFalse
     *  a = 0
     *  LEnd
     *  //End Assign
     *
     *  mais
     *
     *  a = 1 * 2 + 3
     *
     *  deviens
     *
     *  //expr
     *  t1 = 1 * 2
     *  t2 = t1 + 3
     *  //expr
     *  a = t2
     *
     *  et
     *
     *  if(b || c && !d)
     *
     *  deviens
     *
     *  //expr
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //expr
     *  //if
     *  LTrue
     *  codeS1
     *  goto lEnd
     *  LFalse
     *  codeS2
     *  LEnd
     *  //end if
     *
     *
     *  Il faut donc dès le départ vérifier dans la table de symbole le type de la variable à gauche, et généré du
     *  code différent selon ce type.
     *
     *  Pour avoir l'id de la variable de gauche de l'assignation, il peut être plus simple d'aller chercher la valeur
     *  du premier enfant sans l'accepter.
     *  De la sorte, on accept un noeud "Identifier" seulement lorsqu'on l'utilise comme référence (à droite d'une assignation)
     *  Cela simplifie le code de part et d'autre.
     *
     *  Aussi, il peut être pertinent d'extraire le code de l'assignation dans une fonction privée, parce que ce code
     *  sera utile pour les noeuds de comparaison (plus d'explication au commentaire du noeud en question.)
     *  la signature de la fonction que j'ai utilisé pour se faire est :
     *  private String generateAssignCode(Node node, String tId);
     *  ou "node" est le noeud de l'expression représentant la valeur, et tId est le nom de la variable ou assigné
     *  la valeur.
     *
     *  Il est normal (et probablement inévitable concidérant la structure de l'arbre)
     *  de généré inutilement des labels (ou des variables temporaire) qui ne sont pas utilisé ni imprimé dans le code résultant.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String cur_id = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if(SymbolTable.get(cur_id) == VarType.Number){
            String address = ((String) node.jjtGetChild(1).jjtAccept(this, data));
            m_writer.println(cur_id + " = " + address);
        } else {
            boolAssignmentPrint(node, 1, cur_id);
        }
        return null;
    }

    // Imprime le code intermediaire pour l'assignation bool
    private void boolAssignmentPrint(SimpleNode node, int i, String cur_id){
        BoolLabel B = new BoolLabel();
        B.lTrue = fall;
        B.lFalse = genLabel();
        String S_next = genLabel();
        node.jjtGetChild(i).jjtAccept(this, B);
        m_writer.println(cur_id + " = 1");
        m_writer.println("goto " + S_next);
        m_writer.println(B.lFalse);
        m_writer.println(cur_id + " = 0");
        m_writer.println(S_next);
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    //Expression arithmétique
    /*
    Les expressions arithmétique add et mult fonctionne exactement de la même manière. c'est pourquoi
    il est plus simple de remplir cette fonction une fois pour avoir le résultat pour les deux noeuds.

    On peut bouclé sur "ops" ou sur node.jjtGetNumChildren(),
    la taille de ops sera toujours 1 de moins que la taille de jjtGetNumChildren
     */
    public String exprCodeGen(SimpleNode node, Object data, Vector<String> ops) {
        String previous_addr = ((String) node.jjtGetChild(0).jjtAccept(this, data));
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String cur_addr = ((String) node.jjtGetChild(i).jjtAccept(this, data));
            String id = genId();
            m_writer.println(id + " = " + previous_addr + " " + ops.get(i - 1) + " " + cur_addr);
            previous_addr = id;
        }
        return previous_addr;
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else {
            return exprCodeGen(node, data, node.getOps());
        }
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else {
            return exprCodeGen(node, data, node.getOps());
        }
    }

    //UnaExpr est presque pareil au deux précédente. la plus grosse différence est qu'il ne va pas
    //chercher un deuxième noeud enfant pour avoir une valeur puisqu'il s'agit d'une opération unaire.
    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        String address = ((String) node.jjtGetChild(0).jjtAccept(this, data));
        if(node.getOps().size() % 2 == 1) {
            for (int i = 0; i < node.getOps().size(); i++) {
                String cur_id = genId();
                m_writer.println(cur_id + " = - " + address);
                address = cur_id;
            }
        }
        return address;
    }

    //expression logique

    /*

    Rappel, dans le langague, le OU et le ET on la même priorité, et sont associatif à droite par défaut.
    ainsi :
    "a = a || || a2 || b && c || d" est interprété comme "a = a || a2 || (b && (c || d))"
     */
    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        if(node.getOps().size() > 0){
            for(int i = 0; i < node.getOps().size(); i++) {
                BoolLabel B = ((BoolLabel) data);
                BoolLabel B1 = new BoolLabel();
                BoolLabel B2 = new BoolLabel();

                switch (node.getOps().get(0).toString()) {
                    case "||":
                        if(B.lTrue.equals(fall)){
                            B1.lTrue = genLabel();
                        } else{
                            B1.lTrue = B.lTrue;
                        }
                        B1.lFalse = fall;
                        B2.lTrue = B.lTrue;
                        B2.lFalse = B.lFalse;
                        if(B.lTrue.equals(fall)) {
                            node.jjtGetChild(0).jjtAccept(this, B1);
                            node.jjtGetChild(1).jjtAccept(this, B2);
                            m_writer.println(B1.lTrue);
                        } else {
                            node.jjtGetChild(0).jjtAccept(this, B1);
                            node.jjtGetChild(1).jjtAccept(this, B2);
                        }
                        break;
                    case "&&":
                        B1.lTrue = fall;
                        if(B.lFalse.equals(fall)){
                            B1.lFalse = genLabel();
                        } else{
                            B1.lFalse = B.lFalse;
                        }
                        B2.lTrue = B.lTrue;
                        B2.lFalse = B.lFalse;
                        if(B.lFalse.equals(fall)) {
                            node.jjtGetChild(0).jjtAccept(this, B1);
                            node.jjtGetChild(1).jjtAccept(this, B2);
                            m_writer.println(B1.lFalse);
                        } else {
                            node.jjtGetChild(0).jjtAccept(this, B1);
                            node.jjtGetChild(1).jjtAccept(this, B2);
                        }
                        break;
                    case "!":
                        B1.lTrue = B.lTrue;
                        B1.lFalse = B.lFalse;
                        node.jjtGetChild(0).jjtAccept(this, B1);
                        break;
                }
            }
        } else {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        return null;
    }


    //cette fonction privé est utile parce que le code pour généré le goto pour les opérateurs de comparaison est le même
    //que celui pour le référencement de variable booléenne.
    //le code est très simple avant l'optimisation, mais deviens un peu plus long avec l'optimisation.
    private void genCodeRelTestJump(String labelTrue, String labelfalse, String strSegment) {
        if (labelTrue != null && labelfalse != null) {
            m_writer.println("if " + strSegment + " goto " + labelTrue);
            m_writer.println("goto " + labelfalse);
        } else if (labelTrue != null) {
            m_writer.println("if " + strSegment + " goto " + labelTrue);
        } else if (labelfalse != null) {
            m_writer.println("if " + strSegment + " goto " + labelfalse);
        }
    }


    //une partie de la fonction à été faite pour donner des pistes, mais comme tous le reste du fichier, tous est libre
    //à modification.
    /*
    À ajouté : la comparaison est plus complexe quand il s'agit d'une comparaison de booléen.
    Le truc est de :
    1. vérifier qu'il s'agit d'une comparaison de nombre ou de booléen.
        On peut Ce simplifier la vie et le déterminer simplement en regardant si les enfants retourne une valeur ou non, à condition
        de s'être assurer que les valeurs booléennes retourne toujours null.
     2. s'il s'agit d'une comparaison de nombre, on peut faire le code simple par "genCodeRelTestJump(B, test)"
     3. s'il s'agit d'une comparaison de booléen, il faut enregistrer la valeur gauche et droite de la comparaison dans une variable temporaire,
        en utilisant le même code que pour l'assignation, deux fois. (mettre ce code dans une fonction deviens alors pratique)
        avant de faire la comparaison "genCodeRelTestJump(B, test)" avec les deux variables temporaire.

        notez que cette méthodes peut sembler peu efficace pour certain cas, mais qu'avec des passes d'optimisations subséquente, (que l'on
        ne fera pas dans le cadre du TP), on pourrait s'assurer que le code produit est aussi efficace qu'il peut l'être.
     */
    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else if(node.jjtGetNumChildren() > 1){
            if(data != null){
                BoolLabel B = ((BoolLabel) data);
                String addr1 = ((String)node.jjtGetChild(0).jjtAccept(this, null));
                String addr2 = ((String)node.jjtGetChild(1).jjtAccept(this, null));
                if(SymbolTable.get(addr1) != null){
                    VarType op_type = SymbolTable.get(addr1);
                    if(op_type == VarType.Bool){
                        String _t1 = genId();
                        String _t2 = genId();
                        boolAssignmentPrint(node, 0, _t1);
                        boolAssignmentPrint(node, 1, _t2);
                        fallthroughCompChecks(B, _t1, _t2, node.getValue());
                    } else {
                        if(!B.lTrue.equals(fall) && !B.lFalse.equals(fall)){
                            genCodeRelTestJump(B.lTrue, B.lFalse, (addr1 + " " + node.getValue() + " " + addr2));
                        } else {
                            fallthroughCompChecks(B, addr1, addr2, node.getValue());
                        }
                    }
                } else {
                    fallthroughCompChecks(B, addr1, addr2, node.getValue());
                }
            }
        }
        return null;
    }

    // Imprime le code intermediaire selon les recquis du fallthrough
    private void fallthroughCompChecks(BoolLabel labels, String id1, String id2, String op){
        if(!labels.lTrue.equals(fall) && !labels.lFalse.equals(fall)){
            genCodeRelTestJump(labels.lTrue, labels.lFalse, (id1 + " " + op + " " + id1));
        } else {
            if(!labels.lTrue.equals(fall)){
                genCodeRelTestJump(labels.lTrue, null, (id1 + " " + op + " " + id2));
            } else {
                if(!labels.lFalse.equals(fall)){
                    m_writer.println("ifFalse " + id1 + " " + op + " " + id2 + " goto " + labels.lFalse);
                } else {
                    //error
                }
            }
        }
    }

    /*
    Même si on peut y avoir un grand nombre d'opération, celle-ci s'annullent entre elle.
    il est donc intéressant de vérifier si le nombre d'opération est pair ou impaire.
    Si le nombre d'opération est pair, on peut simplement ignorer ce noeud.
     */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /*
    BoolValue ne peut pas simplement retourné sa valeur à son parent contrairement à GenValue et IntValue,
    Il doit plutôt généré des Goto direct, selon sa valeur.
     */
    @Override
    public Object visit(ASTBoolValue node, Object data) {
        if(data != null){
            BoolLabel B = ((BoolLabel)data);
            if(node.getValue()){
                if(!B.lTrue.equals(fall)){
                    m_writer.println("goto " + B.lTrue);
                }
            } else {
                if(!B.lFalse.equals(fall)){
                    m_writer.println("goto " + B.lFalse);
                }
            }
        }
        return node.getValue().toString();
    }


    /*
    si le type de la variable est booléenne, il faudra généré des goto ici.
    le truc est de faire un "if value == 1 goto Label".
    en effet, la structure "if valeurBool goto Label" n'existe pas dans la syntaxe du code à trois adresse.
     */
    @Override
    public Object visit(ASTIdentifier node, Object data) {
        VarType type = SymbolTable.get(node.getValue());
        if(type == VarType.Bool){
            if(data != null){
                BoolLabel B = ((BoolLabel) data);
                if(!B.lTrue.equals(fall) && !B.lFalse.equals(fall)){
                    m_writer.println("if " + node.getValue() + " == 1 goto " + B.lTrue);
                    m_writer.println("goto " + B.lFalse);
                } else{
                    if(!B.lTrue.equals(fall)){
                        m_writer.println("if " + node.getValue() + " == 1 goto " + B.lTrue);
                    } else {
                        if(!B.lFalse.equals(fall)){
                            m_writer.println("ifFalse " + node.getValue() + " == 1 goto " + B.lFalse);
                        } else {
                            //error
                        }
                    }
                }
            }
        }
        return node.getValue();
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return String.valueOf(node.getValue());
    }


    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        String S_check = genLabel();
        String S_next = genLabel();
        m_writer.println("goto " + S_check);
        String[] labels_list = new String[node.jjtGetNumChildren() - 1];
        String[] values_list = new String[node.jjtGetNumChildren() - 1];
        for(int i = 1; i < node.jjtGetNumChildren(); i++){
            labels_list[i-1] = genLabel();
            values_list[i-1] = ((String)node.jjtGetChild(i).jjtAccept(this, labels_list[i-1]));
            m_writer.println("goto " + S_next);
        }
        m_writer.println(S_check);
        for(int i = 1; i < node.jjtGetNumChildren(); i++){
            if(node.jjtGetChild(i).toString().equals("CaseStmt")){
                m_writer.println("if " + node.jjtGetChild(0).jjtAccept(this, null) +
                        " == " + values_list[i-1] + " goto " + labels_list[i-1]);
            } else {
                m_writer.println("goto " + labels_list[i-1]);
            }
        }
        m_writer.println(S_next);
        return S_next;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        String case_label = ((String) data);
        m_writer.println(case_label);
        node.childrenAccept(this, data);
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {
        String default_label = ((String) data);
        m_writer.println(default_label);
        node.childrenAccept(this, data);
        return null;
    }

    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }

    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class BoolLabel {
        public String lTrue = null;
        public String lFalse = null;
    }


}
