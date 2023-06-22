package example.name;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Egor Puzanov
 */
@Slf4j
public class HtmlNameStandardizerTest {

    NameStandardizer standardizer = getStandardizer();

    private NameStandardizer getStandardizer() {
        try {
            return (new NameStandardizerConfiguration()).nameStandardizer();
        } catch (Exception ex) {
            return null;
        }
    }

    @Test
    public void testMinimalStandardization() {
        String input = "Substance name   2 \u0005derivative";
        String expected = "Substance name 2 derivative";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardization2() {
        String input = "Chemical \u0005\u0004material";
        String expected = "Chemical material";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardization3() {
        String input = "alpha-linolenic acid";
        String expected = "alpha-linolenic acid";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardization4() {
        String input = "<- +/- e = m*c\u00B2 ->";
        String expected = "\u2190 \u00B1 e = m*c<sup>2</sup> \u2192";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardization5() {
        String input = "<i>Allowed Tags<sup>Supers</sup><sub>Sub</sub></i><small>L</small><script>Bad Tags</script>";
        String expected = "<i>Allowed Tags<sup>Supers</sup><sub>Sub</sub></i>ʟ";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardizationGreekLetterHtmlEntry() {
        String input = "&Alpha;, &alpha;, &Beta;, &beta;, &Gamma;, &gamma;, &Delta;, &delta;, &Epsilon;, &epsilon;, &Zeta;, &zeta;, &Eta;, &eta;, &Theta;, &theta;, &Iota;, &iota;, &Kappa;, &kappa;, &Lambda;, &lambda;, &Mu;, &mu;, &Nu;, &nu;, &Xi;, &xi;, &Omicron;, &omicron;, &Pi;, &pi;, &Rho;, &rho;, &Sigma;, &sigma;, &Tau;, &tau;, &Upsilon;, &upsilon;, &Phi;, &phi;, &Chi;, &chi;, &Psi;, &psi;, &Omega;, &omega;";
        String expected = "Α, α, Β, β, Γ, γ, Δ, δ, Ε, ε, Ζ, ζ, Η, η, Θ, θ, Ι, ι, Κ, κ, Λ, λ, Μ, μ, Ν, ν, Ξ, ξ, Ο, ο, Π, π, Ρ, ρ, Σ, σ, Τ, τ, Υ, υ, Φ, φ, Χ, χ, Ψ, ψ, Ω, ω";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }
}
