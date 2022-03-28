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
import edu.mit.csail.sdg.alloy4.Computer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class BlockloyConnector {

	private void sendErrorToPipe(String message, int retCode) {
		System.err.println(message);
		System.exit(retCode);
	}
	private void sendErrorToPipe(String message) {
		sendErrorToPipe(message, 1);
	}

	private void sendCompileErrorToPipe(String message, Pos position, int retCode) {
		System.out.printf("{\"msg\":\"%s\",\"x1\":%d,\"x2\":%d,\"y1\":%d,\"y2\":%d}", message.replaceAll("\n", " ").trim(), position.x, position.x2, position.y, position.y2);
		System.exit(retCode);
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
			this.sendCompileErrorToPipe(e.msg, e.pos, 2);
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
				//try {	System.out.println(new String(Files.readAllBytes(Paths.get(outFilePath))));
				//} catch(Exception e) { sendErrorToPipe("........"); }
				//System.out.println("-------");

				class BasicComputer implements Computer {

					A4Solution ans;
					VizGUI viz;
					public BasicComputer(A4Solution _ans) {
						ans = _ans;
					}
					public void setViz( VizGUI _viz) {
						viz = _viz;
					}

					private String writeout(A4Solution aans) throws Exception {
						StringWriter out    = new StringWriter();
						PrintWriter  writer = new PrintWriter(out);
						aans.writeXML(writer, null, null);
						writer.flush();
						return out.toString();
					}

					@Override public String compute(Object input) throws Exception {
						ans = ans.next();
						if(ans.satisfiable()) {
							String res = writeout(ans);
							String opath = getTempFileLocation();
							ans.writeXML(opath);
							viz.loadXML(opath, true);
							return writeout(ans);
						} else {
							throw new Exception("No more satisfying instances found");
						}
					}
				}


				// I genuinely have no idea if this is how it's supposed to be done
				//   because there's SFA for documentation
				BasicComputer enumerator = new BasicComputer(ans);
				BasicComputer evaluator = new BasicComputer(ans);
				VizGUI viz = new VizGUI(true, outFilePath, null, enumerator, evaluator);
				enumerator.setViz(viz);
				evaluator.setViz(viz);
				satisfied = true;
				//for(A4Solution ayn = ans; ayn.satisfiable(); ayn = ayn.next()) {}
			}

		}

		if (!satisfied) {
			System.exit(7);
			//sendErrorToPipe("Model was not satisfiable.", 7);
		}

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
