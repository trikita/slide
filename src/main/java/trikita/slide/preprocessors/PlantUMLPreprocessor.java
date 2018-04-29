package trikita.slide.preprocessors;

import android.text.TextUtils;

import net.sourceforge.plantuml.code.AsciiEncoder;
import net.sourceforge.plantuml.code.URLEncoder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;

public class PlantUMLPreprocessor implements PresentationPreprocessor {
    private Presentation currentPresentation;

    @Override
    public Presentation process(Presentation p) {
        currentPresentation = p;
        List<String> finalPar = new ArrayList<>();
        for(String par : joinPlantUMLDiagrams(p.splitParagraphs())) {
            finalPar.add(parsePlantUML(par));
        }
        return ImmutablePresentation.copyOf(p).withText(
            p.joinParagraphs(finalPar.toArray(new String[]{}))
        );
    }

    private String[] joinPlantUMLDiagrams(String[] paragraphs) {
        List<String> finalParagraphs = new ArrayList<>();

        boolean inUMLDiagram = false;

        for (String par : paragraphs) {
            if(inUMLDiagram) {
                String p = finalParagraphs.get(finalParagraphs.size()-1);
                p += "\n" + par;
                finalParagraphs.set(finalParagraphs.size()-1, p);
            } else {
                finalParagraphs.add(par);
            }

            for(String line : par.split("\n")) {
                if(isStartUML(line)) inUMLDiagram = true;
                else if(isEndUML(line)) inUMLDiagram = false;
            }
        }

        return finalParagraphs.toArray(new String[]{});
    }

    private final String startUML = "@startuml";

    private boolean isStartUML(String s) {
        return s.trim().toLowerCase().startsWith(startUML);
    }

    private boolean isEndUML(String s) {
        return s.trim().toLowerCase().startsWith("@enduml");
    }

    private String parsePlantUML(String par) {
        List<String> outLines = new ArrayList<>();

        boolean inUML = false;
        List<String> umlLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inUML) {
                if (isEndUML(line)) {
                    outLines.add(processPlantUML(TextUtils.join("\n", umlLines)) + bgArgs);
                    inUML = false;
                    umlLines = null;
                    bgArgs = "";
                } else {
                    umlLines.add(line);
                }
            } else {
                if (isStartUML(line)) {
                    umlLines = new ArrayList<>();
                    inUML = true;
                    bgArgs = line.trim().substring(startUML.length());
                    if(!bgArgs.isEmpty() && bgArgs.charAt(0) != ' ') bgArgs = " " + bgArgs;
                } else {
                    outLines.add(line);
                }
            }
        }

        if(umlLines != null) {
           outLines.add(processPlantUML(TextUtils.join("\n", umlLines)) + bgArgs);
        }

        return TextUtils.join("\n", outLines);
    }

    private String processPlantUML(String s) {
        try {
            String payload = currentPresentation.plantUMLTemplateBefore() + s + currentPresentation.plantUMLTemplateAfter();
            return "@" + currentPresentation.plantUMLEndPoint() +
                    "/" + encodePlantUML(payload.trim());
        } catch (Exception e) {
            return "@http://s2.quickmeme.com/img/17/17637236ce6b1eb8a807f5b871c81b0269d72ef2a89265e1b23cf3f8e741a6d2.jpg";
        }
    }

    private String encodePlantUML(String s) throws UnsupportedEncodingException {
        // Deflate
        byte[] input = s.getBytes("UTF-8");
        byte[] output = new byte[input.length];
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION, true);
        compressor.setInput(input);
        compressor.finish();
        int compressedSize = compressor.deflate(output);
        compressor.end();

        byte[] finalOutput = new byte[compressedSize];
        System.arraycopy(output, 0, finalOutput, 0, compressedSize);

        URLEncoder encoder = new AsciiEncoder();
        return encoder.encode(finalOutput);
    }
}
