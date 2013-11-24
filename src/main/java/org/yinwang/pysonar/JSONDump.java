package org.yinwang.pysonar;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.FunctionDef;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSONDump {

    private static final int MAX_PATH_LENGTH = 900;

	private static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static Set<String> seenDef = new HashSet<>();
    private static Set<String> seenRef = new HashSet<>();
    private static Set<String> seenDocs = new HashSet<>();

	private static String dirname(String path) {
		return new File(path).getParent();
	}


	private static Indexer newIndexer(String srcpath, String[] inclpaths) throws Exception {
		Indexer idx = new Indexer();
		for (String inclpath : inclpaths) {
			idx.addPath(inclpath);
		}

		idx.loadFileRecursive(srcpath);
		idx.finish();

		if (idx.semanticErrors.size() > 0) {
			log.info("Indexer errors:");
			for (Entry<String, List<Diagnostic>> entry : idx.semanticErrors.entrySet()) {
				String k = entry.getKey();
				log.info("  Key: " + k);
				List<Diagnostic> diagnostics = entry.getValue();
				for (Diagnostic d : diagnostics) {
					log.info("    " + d);
				}
			}
		}

		return idx;
	}


	private static void writeSymJson(Def def, JsonGenerator json) throws IOException {
		Binding binding = def.getBinding();
        if (def.getStart() < 0) {
            return;
        }

        String name = def.getName();
		boolean isExported = !(
                Binding.Kind.VARIABLE == binding.getKind() ||
                Binding.Kind.PARAMETER == binding.getKind() ||
                Binding.Kind.SCOPE == binding.getKind() ||
                Binding.Kind.ATTRIBUTE == binding.getKind() ||
                (def.hasName() && (name.length() == 0 || name.charAt(0) == '_' || name.startsWith("lambda%"))));

//        boolean isExported = true;

        String path = binding.getQname().replace('.', '/').replace("%20", ".");

//        if (binding.getKind() == Binding.Kind.MODULE) {
//            Util.msg("module! path: " + path);
//        }

        if (!seenDef.contains(path)) {
            seenDef.add(path);
            json.writeStartObject();
            json.writeStringField("name", name);
            json.writeStringField("path", path);
            json.writeStringField("file", def.getFileOrUrl());
            json.writeNumberField("identStart", def.getStart());
            json.writeNumberField("identEnd", def.getEnd());
            json.writeNumberField("defStart", def.getBodyStart());
            json.writeNumberField("defEnd", def.getBodyEnd());
            json.writeBooleanField("exported", isExported);
            json.writeStringField("kind", def.getBinding().getKind().toString());

            if (Binding.Kind.FUNCTION == binding.getKind() ||
                    Binding.Kind.METHOD == binding.getKind() ||
                    Binding.Kind.CONSTRUCTOR == binding.getKind()) {
                json.writeObjectFieldStart("funcData");

                // get args expression
                String argExpr = null;
                Type t = def.getBinding().getType();

                if (t.isUnionType()) {
                    t = t.asUnionType().firstUseful();
                }

                if (t != null && t.isFuncType()) {
                    FunctionDef func = t.asFuncType().getFunc();

                    if (func != null) {
                        StringBuilder args = new StringBuilder();
                        args.append("(");
                        boolean first = true;

                        for (Node n : func.getArgs()) {
                            if (!first) {
                                args.append(", ");
                            }
                            first = false;
                            args.append(n.toDisplay());
                        }

                        if (func.getVararg() != null) {
                            if (!first) args.append(", ");
                            first = false;
                            args.append("*" + func.getVararg().toDisplay());
                        }

                        if (func.getKwarg() != null) {
                            if (!first) args.append(", ");
                            first = false;
                            args.append("**" + func.getKwarg().toDisplay());
                        }

                        args.append(")");

                        argExpr = args.toString();
                    }
                }

                String typeExpr = def.getBinding().getType().toString();

                json.writeNullField("params");

                String signature = argExpr==null? "" : argExpr + "\n" + typeExpr;
                json.writeStringField("signature", signature);
                json.writeEndObject();
            }

            json.writeEndObject();
        }
    }

    private static void writeRefJson(Ref ref, Binding binding, JsonGenerator json) throws IOException {
        Def def = binding.getSingle();
        if (def.getFile() != null) {
            String path = Util.moduleQname(def.getFile()).replace(".", "/").replace("%20", ".") + "/" + def.getName();

            if (def.getStart() >= 0 && ref.start() >= 0 && !binding.isBuiltin()) {
                json.writeStartObject();
                json.writeStringField("sym", path);
                json.writeStringField("file", ref.getFile());
                json.writeNumberField("start", ref.start());
                json.writeNumberField("end", ref.end());
                json.writeBooleanField("builtin", binding.isBuiltin());
                json.writeEndObject();
            }
        }
    }


    private static void writeDocJson(Def def, Indexer idx, JsonGenerator json) throws Exception {
        String path = def.getBinding().getQname().replace('.', '/').replace("%20", ".");
//        Util.msg("path: " + path);

        if (!seenDocs.contains(path)) {
            seenDocs.add(path);

            if (def.docstring != null) {
                json.writeStartObject();
    			json.writeStringField("sym", path);
                json.writeStringField("file", def.getFileOrUrl());
                json.writeStringField("body", def.docstring);
                json.writeNumberField("start", def.docstringStart);
                json.writeNumberField("end", def.docstringEnd);
                json.writeEndObject();
            } else if (def.getBinding().getKind() == Binding.Kind.MODULE) {
                AstCache.DocstringInfo info = idx.getModuleDocstringInfoForFile(def.getFileOrUrl());
                if (info != null) {
                    json.writeStartObject();
                    json.writeStringField("sym", path);
                    json.writeStringField("file", def.getFileOrUrl());
                    json.writeStringField("body", info.docstring);
                    json.writeNumberField("start", info.start);
                    json.writeNumberField("end", info.end);
                    json.writeEndObject();
                }
            }
        }
    }


    private static boolean shouldEmit(@NotNull String pathToMaybeEmit, String srcpath) {
		return Util.unifyPath(pathToMaybeEmit).startsWith(Util.unifyPath(srcpath));
	}


	/*
	 * Precondition: srcpath and inclpaths are absolute paths
	 */
	private static void graph(String srcpath,
                              String[] inclpaths,
                              OutputStream symOut,
                              OutputStream refOut,
                              OutputStream docOut) throws Exception {
		// Compute parent dirs, sort by length so potential prefixes show up first
		List<String> parentDirs = Lists.newArrayList(inclpaths);
		parentDirs.add(dirname(srcpath));
		Collections.sort(parentDirs, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				int diff = s1.length() - s2.length();
				if (0 == diff) {
					return s1.compareTo(s2);
				}
				return diff;
			}
		});

		Indexer idx = newIndexer(srcpath, inclpaths);
        idx.multilineFunType = true;
		JsonFactory jsonFactory = new JsonFactory();
		JsonGenerator symJson = jsonFactory.createGenerator(symOut);
		JsonGenerator refJson = jsonFactory.createGenerator(refOut);
		JsonGenerator docJson = jsonFactory.createGenerator(docOut);
		JsonGenerator[] allJson = {symJson, refJson, docJson};
		for (JsonGenerator json : allJson) {
            json.writeStartArray();
        }

        for (List<Binding> bindings : idx.getAllBindings().values()) {
            for (Binding b : bindings) {
                for (Def def : b.getDefs()) {
                    if (def.getFile() != null) {
                        if (shouldEmit(def.getFile(), srcpath)) {
                            writeSymJson(def, symJson);
                            writeDocJson(def, idx, docJson);
                        }
                    }
                }

                for (Ref ref : b.getRefs()) {
                    if (ref.getFile() != null) {
                        String key = ref.getFile() + ":" + ref.start();
                        if (!seenRef.contains(key) && shouldEmit(ref.getFile(), srcpath)) {
                            writeRefJson(ref, b, refJson);
                            seenRef.add(key);
                        }
                    }
                }
            }
        }

        for (JsonGenerator json : allJson) {
			json.writeEndArray();
			json.close();
		}
	}

    private static void info(Object msg) {
        System.out.println(msg);
    }

	private static void usage() {
		info("Usage: java org.yinwang.pysonar.dump <source-path> <include-paths> <out-root> [verbose]");
		info("  <source-path> is path to source unit (package directory or module file) that will be graphed");
		info("  <include-paths> are colon-separated paths to included libs");
		info("  <out-root> is the prefix of the output files.  There are 3 output files: <out-root>-doc, <out-root>-sym, <out-root>-ref");
		info("  [verbose] if set, then verbose logging is used (optional)");
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3 || args.length > 4) {
			usage();
			return;
		}

		log.setLevel(Level.SEVERE);
		if (args.length >= 4) {
			log.setLevel(Level.ALL);
			log.info("LOGGING VERBOSE");
			log.info("ARGS: " + Arrays.toString(args));
		}

		String srcpath = args[0];
		String[] inclpaths = args[1].split(":");
		String outroot = args[2];

		String symFilename = outroot + "-sym";
		String refFilename = outroot + "-ref";
		String docFilename = outroot + "-doc";
		OutputStream symOut = null, refOut = null, docOut = null;
		try {
			docOut = new BufferedOutputStream(new FileOutputStream(docFilename));
			symOut = new BufferedOutputStream(new FileOutputStream(symFilename));
			refOut = new BufferedOutputStream(new FileOutputStream(refFilename));
            Util.msg("graphing: " + srcpath);
            graph(srcpath, inclpaths, symOut, refOut, docOut);
			docOut.flush();
			symOut.flush();
			refOut.flush();
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file: " + e);
			return;
		} finally {
			if (docOut != null) {
				docOut.close();
			}
			if (symOut != null) {
				symOut.close();
			}
			if (refOut != null) {
				refOut.close();
			}
		}
		log.info("SUCCESS");
	}
}
