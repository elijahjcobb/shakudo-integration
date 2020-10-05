/*
 * Elijah Cobb
 * ejcobb@mtu.edu
 * blockloy-alloy-integration
 */

package edu.mtu.cs.blockloy;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.Module;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;
import edu.mit.csail.sdg.alloy4viz.VizGUI;

import java.io.File;

public final class BlockloyConnector {

	private void sendErrorToPipe(String message) {
		System.err.println(message);
		System.exit(1);
	}

	private void sendCompileErrorToPipe(String message, Pos position) {
		System.out.printf("{\"msg\":\"%s\",\"x1\":%d,\"x2\":%d,\"y1\":%d,\"y2\":%d}", message.replaceAll("\n", " ").trim(), position.x, position.x2, position.y, position.y2);
		System.exit(2);
	}

	private File getFile(String[] args) {

		if (args.length < 1) sendErrorToPipe("Path to source file was not provided.");
		String sourceFilePath = args[0];
		if (sourceFilePath.isEmpty()) sendErrorToPipe("Path to source file was empty.");
		File sourceFile = new File(sourceFilePath);
		if (!sourceFile.exists()) sendErrorToPipe("Path did not resolve to file.");

		return sourceFile;

	}

	public void compute(String[] args) throws Err {

		File sourceFile = getFile(args);

		A4Reporter rep = new A4Reporter() {
			@Override public void warning(ErrorWarning msg) {
				sendErrorToPipe(msg.toString().trim());
			}
		};

		String absoluteFilePath = sourceFile.getAbsolutePath();
		Module world;

		try {
			world = CompUtil.parseEverything_fromFile(rep, null, absoluteFilePath);
		} catch (Err e) {
			this.sendCompileErrorToPipe(e.msg, e.pos);
			return;
		}

		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J;

		boolean satisfied = false;

		for (Command command: world.getAllCommands()) {

			A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);

			if (ans.satisfiable()) {

				String outFilePath = getTempFileLocation();
				ans.writeXML(outFilePath);
				new VizGUI(false, outFilePath, null);

				satisfied = true;

			}

		}

		if (!satisfied) sendErrorToPipe("Model was not satisfiable.");

	}

	private static String getTempFileLocation() {
		String tmpDir = System.getProperty("java.io.tmpdir");
		String fileSeparator = File.separator;
		return tmpDir + fileSeparator + "blockloy-analysis-" + System.currentTimeMillis() + ".xml";
	}

	public static void main(String[] args) {

		BlockloyConnector blockloyConnector = new BlockloyConnector();

		try {

			blockloyConnector.compute(args);

		} catch (Err e) {

			blockloyConnector.sendErrorToPipe(e.getMessage());
			System.exit(1);

		}

	}
}
