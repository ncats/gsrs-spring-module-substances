package ix.ginas.models.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Utility class for working with HTML strings.
 *
 * Created by epuzanov on 7/25/22.
 */
public final class HtmlUtil {
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
                if (node instanceof Element) {
                    Element curElement = (Element) node;
                    cur = cur.appendElement(curElement.tagName());
                    String resHtml = dst.html();
                    if (resHtml.length() > maxLen) {
                        cur.remove();
                        throw new IllegalStateException();
                    }
                } else if (node instanceof TextNode) {
                    String curText = ((TextNode) node).getWholeText();
                    String resHtml = dst.html();
                    if (curText.length() + resHtml.length() > maxLen) {
                        cur.appendText(curText.substring(0, maxLen - resHtml.length()));
                        throw new IllegalStateException();
                    } else {
                        cur.appendText(curText);
                    }
                }
            }
        }

        public void tail(Node node, int depth) {
            if (depth > 0 && node instanceof Element) {
                cur = cur.parent();
            }
        }
    }

    public static String truncateString(String s, int len){
        if (s.length() <= len) {
            return s;
        }

        Document srcDoc = Parser.parseBodyFragment(s, "");
        srcDoc.outputSettings().prettyPrint(false);

        Document dstDoc = Document.createShell(srcDoc.baseUri());
        dstDoc.outputSettings().prettyPrint(false);
        dstDoc.outputSettings().charset("UTF-8");
        Element dst = dstDoc.body();
        NodeVisitor v = new TruncateVisitor(dst, len - 3);

        try {
            NodeTraversor t = new NodeTraversor();
            t.traverse(v, srcDoc.body());
        } catch (IllegalStateException ex) {}

        return dst.html() + "...";
    }
}