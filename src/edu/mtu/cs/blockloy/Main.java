/*
 * Elijah Cobb
 * ejcobb@mtu.edu
 * blockloy-alloy-integration
 */

package edu.mtu.cs.blockloy;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.Module;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;
import edu.mit.csail.sdg.alloy4viz.VizGUI;

public final class Main {

	public static void main(String[] args) throws Err {

		A4Reporter rep = new A4Reporter() {
			@Override public void warning(ErrorWarning msg) {
				System.out.print("Relevance Warning:\n"+(msg.toString().trim())+"\n\n");
				System.out.flush();
			}
		};

		String filename = "/home/elijah/Documents/trafficsol.als";
		Module world = CompUtil.parseEverything_fromFile(rep, null, filename);

		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J;

		for (Command command: world.getAllCommands()) {

			A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);

			if (ans.satisfiable()) {

				String outFilePath = "example_output.xml";
				ans.writeXML(outFilePath);
				new VizGUI(false, outFilePath, null);

			} else System.out.println("Model was not satisfiable.");

		}
	}
}
