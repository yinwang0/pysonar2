package org.yinwang.pysonar;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.jetbrains.annotations.NotNull;

public class JSONDump {

	private static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static Set<String> seenDef = new HashSet<>();
    private static Set<String> seenRef = new HashSet<>();
    private static Set<String> seenDocs = new HashSet<>();

	private static String dirname(String path) {
		return new File(path).getParent();
	}

	private static String noExtension(String path) {
		String ext = Files.getFileExtension(path);
		if (ext.length() == 0) {
			return path;
		}
		return path.substring(0, path.length() - ext.length() - 1);
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


	private static void writeSymJson(Def def, List<String> parentDirs, JsonGenerator json) throws IOException {
		Binding binding = def.getBinding();
        if (def.getStart() < 0) {
            return;
        }
//        Util.msg("def:" + def.getName());
		String name = def.getName();
		if (null == name || name.length() == 0) {
			String[] nameComponents = binding.getQname().replace('@', '.').split("\\.");
			if (nameComponents.length == 0) return;
			name = nameComponents[nameComponents.length - 1];
		}

		boolean isExported = !(
                Binding.Kind.VARIABLE == binding.getKind() ||
                Binding.Kind.PARAMETER == binding.getKind() ||
                (def.isName() && (name.length() == 0 || name.charAt(0) == '_')));

        String path = symPath(def, parentDirs);

        if (!path.isEmpty() && !seenDef.contains(path)) {
            seenDef.add(path);

            json.writeStartObject();
//            json.writeStringField("qname", binding.getQname());
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
                // TODO(bliu)
                json.writeNullField("params");
                json.writeStringField("signature", def.getBinding().getType().toString());
                json.writeEndObject();
            }

            json.writeEndObject();
        }
    }

    private static void writeRefJson(Ref ref, Binding binding, List<String> parentDirs, JsonGenerator json) throws IOException {
        Def def = binding.getSingle();
        if (def.getStart() >= 0 && def.getFile() != null && ref.start() >= 0 && !binding.isBuiltin()) {
            json.writeStartObject();
            json.writeStringField("sym", symPath(def, parentDirs));
            json.writeStringField("file", ref.getFile());
            json.writeNumberField("start", ref.start());
            json.writeNumberField("end", ref.end());
            json.writeBooleanField("builtin", binding.isBuiltin());
            json.writeEndObject();
        }
    }


    private static void writeDocJson(Def def, Indexer idx, List<String> parentDirs, JsonGenerator json) throws Exception {
        String path = symPath(def, parentDirs);
//        Util.msg("def: " + def + ", docstring: " + def.docstring);

        if (!path.isEmpty() && !seenDocs.contains(path)) {
            seenDocs.add(path);

            if (def.docstring != null) {
//                Util.msg("found docstring: " + def.docstring);
                json.writeStartObject();
//			json.writeStringField("sym", def.getBinding().getQname());
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
//				json.writeStringField("sym", def.getBinding().getQname());
                    json.writeStringField("sym", symPath(def, parentDirs));
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

	// Kludge to compute global symbol path. In the future, it would probably be best to have the Indexer to compute this directly.
	private static String symPath(Def def, List<String> parentDirs) {
		String file = def.getFile();
		Binding binding = def.getBinding();
		String qnamePath = binding.getQname().replace('.', '/');
		if (binding.isBuiltin()) {
			return qnamePath;
		}

		String modulePrefix = new File(file).getParent();

		for (String parent : parentDirs) {
			if (modulePrefix.startsWith(parent + "/")) {
				String relModulePrefix = modulePrefix.substring(parent.length() + 1);
//				if (qnamePath.startsWith(relModulePrefix)) {
					log.info("making symPath from parent " + parent + " and qnamePath " + qnamePath);
					return modulePrefix + "/" + qnamePath;
//				}
			}
		}

		// We can get here if a module defines a new attribute on a builtin type.
		// TODO(bliu): This may not be the correct thing to do.
		String prefix = noExtension(file);
		return prefix + "/" + qnamePath;
	}

	/*
	 * Precondition: srcpath and inclpaths are absolute paths
	 */
	private static void graph(String srcpath, String[] inclpaths, OutputStream symOut, OutputStream refOut, OutputStream docOut) throws Exception {
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
                            writeSymJson(def, parentDirs, symJson);
                            writeDocJson(def, idx, parentDirs, docJson);
                        }
                    }
                }

                for (Ref ref : b.getRefs()) {
                    if (ref.getFile() == null) continue;
                    String key = ref.getFile() + ":" + ref.start();

                    if (!seenRef.contains(key) && shouldEmit(ref.getFile(), srcpath)) {
                        writeRefJson(ref, b, parentDirs, refJson);
                        seenRef.add(key);
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
