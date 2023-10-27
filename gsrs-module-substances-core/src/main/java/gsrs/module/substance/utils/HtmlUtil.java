package gsrs.module.substance.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Utility class for working with HTML strings.
 *
 * Created by epuzanov on 7/25/22.
 */
public final class HtmlUtil {
    private static final Set<String> safetags = Stream.of("i", "small", "sub", "sup").collect(Collectors.toSet());
    private static class TruncateVisitor implements NodeVisitor {
        private int maxLen = 0;
        private Element dst;
        private Element cur;
        private boolean stop = false;

        public TruncateVisitor (Element dst, int maxLen) {
            this.maxLen = maxLen;
            this.dst = dst;
            this.cur = dst;
        }

        public void head(Node node, int depth) {
            if (depth > 0) {
                int resHtmlLen = dst.html().getBytes(StandardCharsets.UTF_8).length;
                int nodeHtmlLen = node.outerHtml().getBytes(StandardCharsets.UTF_8).length;
                int maxNodeLen = maxLen - resHtmlLen;
                if (node instanceof Element) {
                    String tagName = ((Element) node).tagName();
                    if (safetags.contains(tagName)) {
                        nodeHtmlLen = tagName.length() * 2 + 5;
                        if (resHtmlLen + nodeHtmlLen > maxLen) {
                            throw new IllegalStateException();
                        } else {
                            cur = cur.appendElement(tagName);
                        }
                    }
                } else if (node instanceof TextNode) {
                    String parentTag = ((Element)node.parent()).tagName();
                    if (safetags.contains(parentTag) || (parentTag == "body" && depth == 1)) {
                        String curText = ((TextNode) node).getWholeText();
                        if (resHtmlLen + nodeHtmlLen > maxLen) {
                            StringBuilder sb = new StringBuilder(curText);
                            int curHtmlLen = maxNodeLen + 1;
                            sb.setLength(curHtmlLen);
                            sb.setLength(sb.length() - (Long.valueOf(sb.chars().filter(c -> c == '&').count()).intValue() * 4));
                            sb.setLength(sb.length() - (Long.valueOf(sb.chars().filter(c -> (c == '<' || c == '>')).count()).intValue() * 3));
                            while (curHtmlLen > maxNodeLen) {
                                sb.setLength(sb.length() - 1);
                                curHtmlLen = sb.toString().getBytes(StandardCharsets.UTF_8).length;
                                curHtmlLen += Long.valueOf(sb.chars().filter(c -> c == '&').count()).intValue() * 4;
                                curHtmlLen += Long.valueOf(sb.chars().filter(c -> (c == '<' || c == '>')).count()).intValue() * 3;
                            }
                            cur.appendText(sb.toString());
                            throw new IllegalStateException();
                        } else {
                            cur.appendText(curText);
                        }
                    }
                }
            }
        }

        public void tail(Node node, int depth) {
            if (depth > 0 && node instanceof Element && safetags.contains(((Element)node).tagName())) {
                cur = cur.parent();
            }
        }
    }

    public static String truncate(String s, int len){
        Document srcDoc = Parser.parseBodyFragment(s, "");
        srcDoc.outputSettings().prettyPrint(false);

        int maxLength = len-3; //for final ...

        Document dstDoc = Document.createShell(srcDoc.baseUri());
        dstDoc.outputSettings().prettyPrint(false);
        dstDoc.outputSettings().charset("UTF-8");
        Element dst = dstDoc.body();
        NodeVisitor v = new TruncateVisitor(dst,maxLength);

        try {
            NodeTraversor t = new NodeTraversor();
            t.traverse(v, srcDoc.body());
        } catch (IllegalStateException ex) {}

        String htmlReturn = dst.html();
        if(htmlReturn.length() < s.length() && htmlReturn.length() <= maxLength){
            return htmlReturn + "...";
        }else{
            return htmlReturn;
        }
    }

    public static String clean(String content, String charset) {
        Safelist sl = Safelist.none().addTags(safetags.toArray(new String[safetags.size()]));
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        settings.charset(charset);
        settings.escapeMode(Entities.EscapeMode.base);
        String safeHtml = Jsoup.clean(content, "", sl, settings);
        return(safeHtml);
    }

    public static String cleanToText(String content, String charset) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        settings.charset(charset);
        settings.escapeMode(Entities.EscapeMode.base);
        String safeText = Jsoup.clean(content, "", Safelist.none(), settings);
        return(safeText);
    }

    public static boolean isValid(String content) {
        Safelist sl = Safelist.none().addTags(safetags.toArray(new String[safetags.size()]));
        return Jsoup.isValid(content, sl);
    }
}
