package OOParser;

/*
 program -> { function }+ end
function -> int id pars '{' body '}'
pars -> '(' ')' | '(' int id { ',' int id } ')'
pars -> '(' ')' | '(' parslist ')'
parslist -> int id [',' parslist]
 body -> [ decls ] stmts
 decls -> int idlist ';'
idlist -> id [',' idlist ]
stmts -> stmt [ stmts ]
stmt -> assign ';' | cond | loop | cmpd |
 return expr ';'
assign -> id '=' expr
cond -> if '(' relexp ')' stmt [ else stmt ]
loop -> while '(' relexp ')' stmt
cmpd -> '{' stmts '}'

relexp -> expr ('<' | '>' | '<=' | '>=' | '==' | '!= ') expr
expr -> term [ ('+' | '-') expr ]
term -> factor [ ('*' | '/') term ]
factor -> int_lit | id | '(' expr ')' | funcall
funcall -> id '(' [ exprlist ] ')'
exprlist -> expr [ ',' exprlist ]
 */

public class Parser {
	static Program p;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		p = new Program();
	}
}

class Program {		// program -> { function }+ end
	static Function f;
	public Program() {
		while (Lexer.nextToken == Token.KEY_INT) {
			SymTab.setName("int ");
			Lexer.lex();
			SymTab.init();
			Code.init();
			f = new Function();
			SymTab.getName();
			Code.output();
			//System.out.println("\n");
		}
	}
}

class Function{ //function -> int id pars '{' body '}'
	String id;
	Pars pars;
	Body body;
	Program p;

	public Function() {
		id = Lexer.ident;
		SymTab.setName(id);
		Lexer.lex(); // skip over id
		if(Lexer.nextToken == Token.LEFT_PAREN) {
			pars = new Pars();
			if(Lexer.nextToken == Token.LEFT_BRACE) {
				Lexer.lex(); //skip over {
				body = new Body();
				if(Lexer.nextToken == Token.RIGHT_BRACE) {
					Lexer.lex(); // skip over }
				}
			}
		}
	}
}

class Pars{		//pars -> '(' ')' | '(' int id { ',' int id } ')'
	String id;
	Pars pars;

	public Pars() {
		Lexer.lex(); //skip over (
		SymTab.setName("(");
		if(Lexer.nextToken == Token.KEY_INT) {
			SymTab.setName("int");
			Lexer.lex(); // skip over int
			id = Lexer.ident;
			SymTab.add(id); //store id
			Lexer.lex(); //skip over id
			while(Lexer.nextToken == Token.COMMA) {
				SymTab.setName(",");
				Lexer.lex(); //skip over ,
				SymTab.setName("int");
				Lexer.lex(); //skip over int
				id = Lexer.ident;
				SymTab.add(id);
				Lexer.lex(); //skip over id
			}
		}
		Lexer.lex(); //skip over )
		SymTab.setName(");");
	}
}

class Body{		//body -> [ decls ] stmts
	Decls decls;
	Stmts stmts;

	public Body() {
		if(Lexer.nextToken == Token.KEY_INT) {
			decls = new Decls();
		}
		stmts = new Stmts();
		/*if (Lexer.nextToken == Token.KEY_END) {
			return;
		}*/
	}
}

class Decls {	//decls   ->  int idlist ';'
	Idlist idlist;

	public Decls() {
		if(Lexer.nextToken == Token.KEY_INT) {
			Lexer.lex(); //parse int
			idlist = new Idlist();
		}
	}

}

class Idlist{	//id [',' idlist ]
	Idlist idlist;
	String id;

	public Idlist() {
			id = Lexer.ident;
			SymTab.add(id);
			Lexer.lex();   //Parse over id
			if(Lexer.nextToken == Token.COMMA) {
				Lexer.lex();
				idlist = new Idlist();
			}else {
				Lexer.lex(); //skip ;
			}
	}

}

class Stmts{		//stmt [ stmts ]
	Stmts stmts;
	Stmt stmt;

	public Stmts() {
		stmt = new Stmt();
		if(Lexer.nextToken == Token.ID || Lexer.nextToken == Token.KEY_IF
				|| Lexer.nextToken == Token.KEY_WHILE || Lexer.nextToken == Token.LEFT_BRACE || Lexer.nextToken == Token.KEY_RETURN) {
			stmts = new Stmts();
		}
	}
}

class Stmt{		//stmt -> assign ';' | cond | loop | cmpd | return expr ';'
	Assign assign;
	Cond cond;
	Loop loop;
	Cmpd cmpd;
	Expr expr;
	//Return return;

	public Stmt() {
		switch(Lexer.nextToken) {
		case Token.ID :
			assign = new Assign();
			Lexer.lex();
			break;
		case Token.KEY_IF :
			cond = new Cond();
			break;
		case Token.KEY_WHILE :
			loop = new Loop();
			break;
		case Token.LEFT_BRACE :
			cmpd = new Cmpd();
			Lexer.lex();
			break;
		case Token.KEY_RETURN :
			Lexer.lex(); //skip return
			expr = new Expr();
			Lexer.lex(); //skip over ;
			Code.gen("ireturn");
		default : break;
		}
	}
}

class Assign {		//assign  ->  id '=' expr
	Expr expr;
	String id;
	public Assign() {
			id = Lexer.ident;
			 //Collect id
			Lexer.lex(); //Parse over id

			if(Lexer.nextToken == Token.ASSIGN_OP) {
				Lexer.lex(); //Parse over =
				expr = new Expr();
				Code.storeId(id);
			}
	}

}

class Cond {		//cond    ->  if '(' relexp ')' stmt [ else stmt ]
	Rexpr rexpr;
	Stmt stmt1, stmt2;

	public Cond() {
		int a =  -1;
		int if_ptr = -1;
		int goto_ptr = -1;
		Lexer.lex(); //Parse if
		if(Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex(); //Parse (
			rexpr = new Rexpr();
			if_ptr = Code.codeptr-3;
			Lexer.lex(); // Parse )
			stmt1 = new Stmt();
		}
		if(Lexer.nextToken == Token.KEY_ELSE) {
			goto_ptr = Code.codeptr+3;
			Code.backPatch(if_ptr, goto_ptr);
			Code.gen("goto ");
			Code.skip(2);
			a = Code.codeptr-3;
			Lexer.lex(); // Parse else

			stmt2 = new Stmt();
			Code.backPatch(a, -1);
		}
		if(goto_ptr == -1) {
			Code.backPatch(if_ptr, -1);
		}
	}

}

class Loop{		//loop    ->  while '(' relexp ')' stmt
	Rexpr rexpr;
	Stmt stmt;

	public Loop() {
		int if_ptr = -1;
		int a = -1;
		int x = -1;
		int goto_ptr = -1;
		Lexer.lex(); //Parse while
		if(Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex(); //Parse (
			x = Code.codeptr;
			rexpr = new Rexpr();
			if_ptr = Code.codeptr-3;
			Lexer.lex(); //Parse over )
			stmt = new Stmt();
			Code.gen("goto ");
			Code.skip(2);
			goto_ptr = Code.codeptr - 3;
			Code.backPatch(goto_ptr, x);
		}
		if(a == -1) {
			Code.backPatch(if_ptr, -1);
		}

	}

}

class Cmpd {			//cmpd    ->  '{' stmts '}'
	Stmts stmts;

	public Cmpd() {
		if(Lexer.nextToken == Token.LEFT_BRACE) {
			Lexer.lex();
			stmts = new Stmts();
		}
	}

}

class Return { // return expr ;
	String id;
	Expr expr;

	public Return() {

	}
}

class Rexpr {			//relexp ->  expr ('<' | '>' | '<=' | '>=' | '==' | '!= ') expr
	Expr expr1, expr2;
	String op;

	public Rexpr() {
			expr1 = new Expr();
			switch(Lexer.nextToken) {
			case Token.LESSER_OP :
				op = "<";
				break;
			case Token.GREATER_OP :
				op = ">";
				break;
			case Token.LESSEQ_OP :
				op = "<=";
				break;
			case Token.GREATEREQ_OP :
				op = ">=";
				break;
			case Token.EQ_OP :
				op = "==";
				break;
			case Token.NOT_EQ :
				op = "!=";
				break;
			default : break;
			}
			Lexer.lex();
			expr2 = new Expr();
			Code.gen(Code.if_icpm(op));
			Code.skip(2);
		}
}

class Expr { 				 // expr -> term (+ | -) expr | term
	Term t;
	char op;
	Expr e;

	public Expr() {	 // C is an inherited attribute for Expr
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();    // scan over op
			e = new Expr();
			Code.gen(op);      // generate the byte-code for op
		}
	}
}

class Term { 				// term -> factor (* | /) term | factor
	Factor f;
	char op;
	Term t;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();     // scan over op
			t = new Term();
			Code.gen(op);
		}
	}
}

class Factor { 				// factor  ->  int_lit | id | '(' expr ')' | funcall
	int i;
	Expr e;
	String id;
	Funcall funcall;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Code.gen(i);            // generate byte-code for i
			Lexer.lex();       // scan over int
			break;
		case Token.ID: // id
			id = Lexer.ident;
			if(SymTab.isPresent(id)) {
				Code.loadId(id);
				Lexer.lex();
			}else {
				funcall = new Funcall();
			}
			break;
		case Token.LEFT_PAREN:
			Lexer.lex();        // scan over '('
			e = new Expr();
			Lexer.lex();        // scan over ')'
			break;
		default:
			break;
		}
	}
}

class Funcall{		//funcall -> id '(' [ exprlist ] ')'
	String id;
	int f;
	Exprlist exprlist;

	public Funcall() {
		id = Lexer.ident;
		Lexer.lex(); //skip over id
		Code.gen("aload_0");
		if(Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex(); //skip over (
			if(Lexer.nextToken == Token.ID) {
				exprlist = new Exprlist();
			}
			Lexer.lex(); //skip over )
			if(SymTab.isPresent2(id)) {
				f = SymTab.getFunctionPos(id);
			}else {
				SymTab.addFunction(id);
				f = SymTab.getFunctionPos(id);
			}
			Code.invokeFunction(f);
			Code.skip(2);
		}

	}
}

class Exprlist{		//expr [ ',' exprlist ]
	Expr expr;
	Exprlist exprlist;
	String id;

	public Exprlist() {
		id = Lexer.ident;
		expr = new Expr();
		//Lexer.lex(); //skip over id, this is happening in factor
		if(Lexer.nextToken == Token.COMMA) {
			Lexer.lex(); //skip over ,
			exprlist = new Exprlist();
		}
	}
}

class SymTab {		// This will have an String array storing all ids
	public static String[] symbol;
	public static int SymbolPosition;
	public static String[] symFun = new String[300];
	public static int FunPosition = 0;
	public static String name;

	public static void init() {
		symbol = new String[300];
		SymbolPosition = 0;
		name = new String("int ");
	}

	public static void add(String id) {
		symbol[SymbolPosition] = id;
		SymbolPosition++;
	}

	public static int getPosition(String id) {
		for (int a=0; a < symbol.length; a++) {
			//String s = symbol.get(a);
			if(id.equals(symbol[a])) {
				return a;
			}

		}
		return -1;

	}

	public static int getFunctionPos(String id) {
		for (int a = 0; a < symFun.length; a++) {
			if(id.equals(symFun[a])) {
				return a+2;
			}
		}
		return -1;
	}

	public static boolean isPresent(String id) {
		for(int a=0; a < symbol.length; a++) {
			if(id.equals(symbol[a])) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPresent2(String id) {
		for(int a=0; a < symFun.length; a++) {
			if(id.equals(symFun[a])) {
				return true;
			}
		}
		return false;
	}

	public static void addFunction(String id) {
		symFun[FunPosition] = id;
		FunPosition++;
	}

	public static void setName(String s) {
		name = name + s;
	}

	public static void getName() {
		System.out.println(name);
	}
}

class Code {
public static String[] code;

	public static int codeptr;

	public static void init() {
		code = new String[300];
		codeptr = 0;
	}

	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}

	public static void gen(char c) {
		gen(opcode(c));
	}

	public static void gen(int i) {
		if (i < 6 && i > -1)
			gen("iconst_" + i);
		else if (i < 128) {
			gen("bipush " + i);
			skip(1);
		} else {
			gen("sipush " + i);
			skip(2);
		}
	}

	public static void skip(int n) {
		codeptr = codeptr + n;
	}

	public static String opcode(char c) {
		switch (c) {
		case '+':
			return "iadd";
		case '-':
			return "isub";
		case '*':
			return "imul";
		case '/':
			return "idiv";
		default:
			return "";
		}
	}

	public static void storeId(String id) {
		for(int i = 0; i < SymTab.SymbolPosition; i++) {
			if(SymTab.symbol[i].equals(id)) {
				i = i+1; //to start istore with 1
				if(i < 4) {
					gen("istore_" + i);
					//skip(1);
					//return;
				}else {
					gen("istore " + i);
					skip(1);
				}
			}
		}
	}

	public static void loadId(String id) {
		for(int i = 0; i < SymTab.SymbolPosition; i++) {
			if(SymTab.symbol[i].equals(id)) {
				i = i+1; // to start iload with 1
				if(i < 4) {
					gen("iload_" + i);
				}else {
					gen("iload " + i);
					skip(1);
				}
			}
		}
	}

	public static void invokeFunction(String id) {
		for(int i =2; i< SymTab.FunPosition; i++) {
			if(SymTab.symFun[i].equals(id)) {
				gen("InvokeVirtual" + "# " + i);
			}
		}
	}

	public static void invokeFunction(int id) {
		gen("invokevirtual " + "#" + id);
	}

	public static void backPatch(int track,int ptr){
		if (ptr != -1){
			code[track] = code[track]+ ptr;
		}else{
		code[track] = code[track]+ codeptr;
		}
	}

	public static String if_icpm(String c) {
		switch(c){
		case "<" :{
			return "if_icmpge ";
		}
		case ">" :{
			return "if_icmple ";
		}
		case "<=" :{
			return "if_icmpgt ";
		}
		case ">=" :{
			return "if_icmplt ";
		}
		case "==" :{
			return "if_icmpne ";
		}
		case "!=" :{
			return "if_icmpeq ";
		}
		default: return "";

		}
	}

	public static void output() {
		//System.out.println("\n");
		//SymTab.getName();
		//System.out.println("\n");
		System.out.println("Code:");

		for (int i = 0; i < codeptr; i++)
			if (code[i] != null && code[i] != "")
				System.out.println("     " + i + ": " + code[i]);
				System.out.println();
	}
}
