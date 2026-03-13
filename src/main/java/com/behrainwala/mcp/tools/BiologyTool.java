package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * MCP tool for university-level biology calculations.
 * Covers genetics, population biology, molecular biology, ecology, and bioinformatics.
 */
@Service
public class BiologyTool {

    private static final MathContext MC = new MathContext(10);

    // Standard genetic code
    private static final Map<String, String> CODON_TABLE = new LinkedHashMap<>();
    static {
        CODON_TABLE.put("UUU", "Phe"); CODON_TABLE.put("UUC", "Phe"); CODON_TABLE.put("UUA", "Leu"); CODON_TABLE.put("UUG", "Leu");
        CODON_TABLE.put("CUU", "Leu"); CODON_TABLE.put("CUC", "Leu"); CODON_TABLE.put("CUA", "Leu"); CODON_TABLE.put("CUG", "Leu");
        CODON_TABLE.put("AUU", "Ile"); CODON_TABLE.put("AUC", "Ile"); CODON_TABLE.put("AUA", "Ile"); CODON_TABLE.put("AUG", "Met");
        CODON_TABLE.put("GUU", "Val"); CODON_TABLE.put("GUC", "Val"); CODON_TABLE.put("GUA", "Val"); CODON_TABLE.put("GUG", "Val");
        CODON_TABLE.put("UCU", "Ser"); CODON_TABLE.put("UCC", "Ser"); CODON_TABLE.put("UCA", "Ser"); CODON_TABLE.put("UCG", "Ser");
        CODON_TABLE.put("CCU", "Pro"); CODON_TABLE.put("CCC", "Pro"); CODON_TABLE.put("CCA", "Pro"); CODON_TABLE.put("CCG", "Pro");
        CODON_TABLE.put("ACU", "Thr"); CODON_TABLE.put("ACC", "Thr"); CODON_TABLE.put("ACA", "Thr"); CODON_TABLE.put("ACG", "Thr");
        CODON_TABLE.put("GCU", "Ala"); CODON_TABLE.put("GCC", "Ala"); CODON_TABLE.put("GCA", "Ala"); CODON_TABLE.put("GCG", "Ala");
        CODON_TABLE.put("UAU", "Tyr"); CODON_TABLE.put("UAC", "Tyr"); CODON_TABLE.put("UAA", "Stop"); CODON_TABLE.put("UAG", "Stop");
        CODON_TABLE.put("CAU", "His"); CODON_TABLE.put("CAC", "His"); CODON_TABLE.put("CAA", "Gln"); CODON_TABLE.put("CAG", "Gln");
        CODON_TABLE.put("AAU", "Asn"); CODON_TABLE.put("AAC", "Asn"); CODON_TABLE.put("AAA", "Lys"); CODON_TABLE.put("AAG", "Lys");
        CODON_TABLE.put("GAU", "Asp"); CODON_TABLE.put("GAC", "Asp"); CODON_TABLE.put("GAA", "Glu"); CODON_TABLE.put("GAG", "Glu");
        CODON_TABLE.put("UGU", "Cys"); CODON_TABLE.put("UGC", "Cys"); CODON_TABLE.put("UGA", "Stop"); CODON_TABLE.put("UGG", "Trp");
        CODON_TABLE.put("CGU", "Arg"); CODON_TABLE.put("CGC", "Arg"); CODON_TABLE.put("CGA", "Arg"); CODON_TABLE.put("CGG", "Arg");
        CODON_TABLE.put("AGU", "Ser"); CODON_TABLE.put("AGC", "Ser"); CODON_TABLE.put("AGA", "Arg"); CODON_TABLE.put("AGG", "Arg");
        CODON_TABLE.put("GGU", "Gly"); CODON_TABLE.put("GGC", "Gly"); CODON_TABLE.put("GGA", "Gly"); CODON_TABLE.put("GGG", "Gly");
    }

    // Amino acid molecular weights (average, in Da)
    private static final Map<String, Double> AA_WEIGHTS = Map.ofEntries(
            Map.entry("Gly", 57.05), Map.entry("Ala", 71.08), Map.entry("Val", 99.13), Map.entry("Leu", 113.16),
            Map.entry("Ile", 113.16), Map.entry("Pro", 97.12), Map.entry("Phe", 147.18), Map.entry("Trp", 186.21),
            Map.entry("Met", 131.20), Map.entry("Ser", 87.08), Map.entry("Thr", 101.10), Map.entry("Cys", 103.14),
            Map.entry("Tyr", 163.18), Map.entry("His", 137.14), Map.entry("Lys", 128.17), Map.entry("Arg", 156.19),
            Map.entry("Asp", 115.09), Map.entry("Glu", 129.12), Map.entry("Asn", 114.10), Map.entry("Gln", 128.13)
    );

    @Tool(name = "dna_operations", description = "Perform DNA/RNA sequence operations: complement, reverse complement, "
            + "transcription (DNA→mRNA), translation (mRNA→protein), GC content, melting temperature, "
            + "and molecular weight estimation.")
    public String dnaOperations(
            @ToolParam(description = "Operation: 'complement', 'reverse_complement', 'transcribe' (DNA→mRNA), "
                    + "'translate' (mRNA→protein or DNA→protein), 'gc_content', 'melting_temp', "
                    + "'molecular_weight', 'analyze' (all properties)") String operation,
            @ToolParam(description = "DNA or RNA sequence (e.g. 'ATGCGATCCA' or 'AUGCGAUCCA'). "
                    + "Use T for DNA, U for RNA.") String sequence) {

        String seq = sequence.strip().toUpperCase().replaceAll("\\s+", "");
        boolean isRNA = seq.contains("U");
        String op = operation.strip().toLowerCase();

        if (!seq.matches("[ATCGU]+")) {
            return "Error: Invalid sequence. Use only A, T, C, G (DNA) or A, U, C, G (RNA).";
        }

        return switch (op) {
            case "complement" -> {
                String comp = complement(seq, isRNA);
                yield seqResult("Complement", seq, "5'-" + comp + "-3'",
                        isRNA ? "A↔U, C↔G" : "A↔T, C↔G");
            }
            case "reverse_complement" -> {
                String revComp = new StringBuilder(complement(seq, isRNA)).reverse().toString();
                yield seqResult("Reverse Complement", seq, "5'-" + revComp + "-3'",
                        "Complement then reverse (antiparallel strand)");
            }
            case "transcribe" -> {
                if (isRNA) yield "Sequence is already RNA. For reverse transcription, replace U with T.";
                String mRNA = seq.replace('T', 'U');
                yield seqResult("Transcription (DNA → mRNA)", seq,
                        "mRNA: 5'-" + mRNA + "-3'", "Template strand → mRNA (T→U)");
            }
            case "translate" -> {
                String mRNA = isRNA ? seq : seq.replace('T', 'U');
                yield translateSequence(mRNA);
            }
            case "gc_content" -> {
                long gc = seq.chars().filter(c -> c == 'G' || c == 'C').count();
                double pct = (double) gc / seq.length() * 100;
                yield seqResult("GC Content", seq,
                        "GC = " + gc + "/" + seq.length() + " = " + fmt(pct) + "%",
                        "G+C count / total bases × 100");
            }
            case "melting_temp" -> {
                yield meltingTemperature(seq);
            }
            case "molecular_weight" -> {
                // Approximate: avg nucleotide MW ~330 Da for DNA, ~340 for RNA
                double mw = seq.length() * (isRNA ? 340.0 : 330.0);
                yield seqResult("Molecular Weight (approximate)", seq,
                        "MW ≈ " + fmt(mw) + " Da (" + fmt(mw / 1000) + " kDa)",
                        "Using average nucleotide mass: " + (isRNA ? "340" : "330") + " Da");
            }
            case "analyze" -> {
                yield analyzeSequence(seq, isRNA);
            }
            default -> "Unknown operation. Use: complement, reverse_complement, transcribe, translate, "
                    + "gc_content, melting_temp, molecular_weight, analyze";
        };
    }

    @Tool(name = "hardy_weinberg", description = "Solve Hardy-Weinberg equilibrium problems. "
            + "Given allele frequencies (p, q) or genotype frequencies, calculate all equilibrium values. "
            + "p² + 2pq + q² = 1 where p + q = 1.")
    public String hardyWeinberg(
            @ToolParam(description = "Known value type: 'p' (dominant allele freq), 'q' (recessive allele freq), "
                    + "'q2' (homozygous recessive frequency), 'p2' (homozygous dominant frequency), "
                    + "'heterozygous' (2pq carrier frequency)") String knownType,
            @ToolParam(description = "The known frequency value (0-1)") double value) {

        if (value < 0 || value > 1) return "Error: Frequency must be between 0 and 1.";

        double p, q;

        switch (knownType.strip().toLowerCase()) {
            case "p" -> { p = value; q = 1 - p; }
            case "q" -> { q = value; p = 1 - q; }
            case "q2" -> { q = Math.sqrt(value); p = 1 - q; }
            case "p2" -> { p = Math.sqrt(value); q = 1 - p; }
            case "heterozygous", "2pq" -> {
                // 2pq = value → p + q = 1, 2p(1-p) = value → 2p - 2p² = value
                // quadratic: 2p² - 2p + value = 0
                double disc = 4 - 8 * value;
                if (disc < 0) { return "Error: Invalid heterozygous frequency (no real solution)."; }
                p = (2 - Math.sqrt(disc)) / 4;
                q = 1 - p;
            }
            default -> { return "Unknown type. Use: p, q, q2, p2, heterozygous"; }
        }

        double p2 = p * p;
        double q2 = q * q;
        double twoFq = 2 * p * q;

        return "Hardy-Weinberg Equilibrium\n" +
                "─────────────────────────\n\n" +
                "Given: " + knownType + " = " + fmt(value) + "\n\n" +
                "ALLELE FREQUENCIES\n" +
                "  p (dominant allele):  " + fmt(p) + "\n" +
                "  q (recessive allele): " + fmt(q) + "\n" +
                "  p + q = " + fmt(p + q) + " ✓\n\n" +
                "GENOTYPE FREQUENCIES\n" +
                "  p² (AA homozygous dominant):  " + fmt(p2) + " (" + fmt(p2 * 100) + "%)\n" +
                "  2pq (Aa heterozygous/carrier): " + fmt(twoFq) + " (" + fmt(twoFq * 100) + "%)\n" +
                "  q² (aa homozygous recessive):  " + fmt(q2) + " (" + fmt(q2 * 100) + "%)\n" +
                "  p² + 2pq + q² = " + fmt(p2 + twoFq + q2) + " ✓\n\n" +
                "IN A POPULATION OF 10,000\n" +
                "  AA: ~" + Math.round(p2 * 10000) + " individuals\n" +
                "  Aa: ~" + Math.round(twoFq * 10000) + " carriers\n" +
                "  aa: ~" + Math.round(q2 * 10000) + " affected";
    }

    @Tool(name = "punnett_square", description = "Generate a Punnett square for a genetic cross. "
            + "Supports monohybrid (single gene) and dihybrid (two gene) crosses. "
            + "Shows genotype and phenotype ratios.")
    public String punnettSquare(
            @ToolParam(description = "Parent 1 genotype (e.g. 'Aa', 'AaBb', 'AA', 'aabb')") String parent1,
            @ToolParam(description = "Parent 2 genotype (e.g. 'Aa', 'AaBb', 'aa', 'AaBb')") String parent2,
            @ToolParam(description = "Optional: dominant trait name (e.g. 'Tall')", required = false) String dominantTrait,
            @ToolParam(description = "Optional: recessive trait name (e.g. 'Short')", required = false) String recessiveTrait) {

        String p1 = parent1.strip();
        String p2 = parent2.strip();

        if (p1.length() != p2.length() || p1.length() % 2 != 0) {
            return "Error: Both parents must have the same number of gene pairs (e.g. 'Aa' × 'Aa' or 'AaBb' × 'AaBb')";
        }

        // Generate gametes
        List<String> gametes1 = generateGametes(p1);
        List<String> gametes2 = generateGametes(p2);

        StringBuilder sb = new StringBuilder();
        sb.append("Punnett Square: ").append(p1).append(" × ").append(p2).append("\n");
        sb.append("─────────────────────────────\n\n");

        // Build the square
        Map<String, Integer> genotypeCount = new LinkedHashMap<>();
        Map<String, Integer> phenotypeCount = new LinkedHashMap<>();

        int total = gametes1.size() * gametes2.size();

        // Header
        sb.append("        ");
        for (String g : gametes2) sb.append(String.format("%-8s", g));
        sb.append("\n");

        for (String g1 : gametes1) {
            sb.append(String.format("%-8s", g1));
            for (String g2 : gametes2) {
                String genotype = combineGametes(g1, g2);
                sb.append(String.format("%-8s", genotype));
                genotypeCount.merge(normalizeGenotype(genotype), 1, Integer::sum);
                String phenotype = getPhenotype(genotype, dominantTrait, recessiveTrait);
                phenotypeCount.merge(phenotype, 1, Integer::sum);
            }
            sb.append("\n");
        }

        sb.append("\nGENOTYPE RATIOS\n");
        genotypeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sb.append("  ").append(e.getKey()).append(": ")
                        .append(e.getValue()).append("/").append(total)
                        .append(" (").append(fmt((double) e.getValue() / total * 100)).append("%)\n"));

        sb.append("\nPHENOTYPE RATIOS\n");
        phenotypeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sb.append("  ").append(e.getKey()).append(": ")
                        .append(e.getValue()).append("/").append(total)
                        .append(" (").append(fmt((double) e.getValue() / total * 100)).append("%)\n"));

        return sb.toString();
    }

    @Tool(name = "population_growth", description = "Model population growth: exponential growth, logistic growth, "
            + "doubling time, and population viability. Common in ecology courses.")
    public String populationGrowth(
            @ToolParam(description = "Model: 'exponential' (N=N0*e^rt), 'logistic' (N=K/(1+((K-N0)/N0)*e^(-rt))), "
                    + "'doubling_time' (t=ln2/r), 'growth_rate' (r from two population counts)") String model,
            @ToolParam(description = """
                    Comma-separated values:
                      exponential: N0 (initial pop), r (growth rate), t (time)
                      logistic: N0, r, K (carrying capacity), t
                      doubling_time: r (growth rate)
                      growth_rate: N0, Nt (final pop), t (time)""") String values) {

        try {
            double[] v = parseValues(values);

            return switch (model.strip().toLowerCase()) {
                case "exponential" -> {
                    check(v, 3, "N0, r, t");
                    double N = v[0] * Math.exp(v[1] * v[2]);
                    double doubling = Math.log(2) / v[1];
                    yield popResult("Exponential Growth", "N(t) = N₀e^(rt)",
                            "N(" + fmt(v[2]) + ") = " + fmt(N) + "\nDoubling time = " + fmt(doubling) + " time units\n"
                                    + "Growth from " + fmt(v[0]) + " to " + fmt(N) + " (" + fmt((N / v[0] - 1) * 100) + "% increase)",
                            "N₀ = " + fmt(v[0]) + ", r = " + fmt(v[1]) + ", t = " + fmt(v[2]));
                }
                case "logistic" -> {
                    check(v, 4, "N0, r, K, t");
                    double N = v[2] / (1 + ((v[2] - v[0]) / v[0]) * Math.exp(-v[1] * v[3]));
                    double dNdt = v[1] * N * (1 - N / v[2]); // growth rate at time t
                    yield popResult("Logistic Growth", "N(t) = K / (1 + ((K-N₀)/N₀)e^(-rt))",
                            "N(" + fmt(v[3]) + ") = " + fmt(N)
                                    + "\nGrowth rate dN/dt at t: " + fmt(dNdt)
                                    + "\nCapacity used: " + fmt(N / v[2] * 100) + "%"
                                    + "\nMax growth rate at N = K/2 = " + fmt(v[2] / 2),
                            "N₀ = " + fmt(v[0]) + ", r = " + fmt(v[1]) + ", K = " + fmt(v[2]) + ", t = " + fmt(v[3]));
                }
                case "doubling_time" -> {
                    check(v, 1, "r");
                    double t = Math.log(2) / v[0];
                    yield popResult("Doubling Time", "t_d = ln(2)/r",
                            "Doubling time = " + fmt(t) + " time units",
                            "r = " + fmt(v[0]));
                }
                case "growth_rate" -> {
                    check(v, 3, "N0, Nt, t");
                    double r = Math.log(v[1] / v[0]) / v[2];
                    double doubling = Math.log(2) / r;
                    yield popResult("Growth Rate Calculation", "r = ln(Nt/N0) / t",
                            "r = " + fmt(r) + " per time unit\nDoubling time = " + fmt(doubling) + " time units",
                            "N₀ = " + fmt(v[0]) + ", N(t) = " + fmt(v[1]) + ", t = " + fmt(v[2]));
                }
                default -> "Unknown model. Use: exponential, logistic, doubling_time, growth_rate";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "enzyme_kinetics", description = "Solve enzyme kinetics problems: Michaelis-Menten equation, "
            + "Lineweaver-Burk plot values, competitive/uncompetitive inhibition, "
            + "and enzyme efficiency (kcat/Km).")
    public String enzymeKinetics(
            @ToolParam(description = "Formula: 'michaelis_menten' (V = Vmax[S]/(Km+[S])), "
                    + "'lineweaver_burk' (1/V vs 1/[S] values), "
                    + "'competitive_inhibition' (apparent Km with inhibitor), "
                    + "'kcat' (catalytic efficiency)") String formula,
            @ToolParam(description = """
                    Comma-separated values:
                      michaelis_menten: Vmax, Km, [S] (substrate concentration)
                      lineweaver_burk: Vmax, Km (generates plot data points)
                      competitive_inhibition: Km, [I] (inhibitor conc), Ki (inhibition constant)
                      kcat: Vmax, [E]total (enzyme concentration) [optional: Km for efficiency]""") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "michaelis_menten" -> {
                    check(v, 3, "Vmax, Km, [S]");
                    double velocity = v[0] * v[2] / (v[1] + v[2]);
                    double fractionVmax = velocity / v[0] * 100;
                    yield popResult("Michaelis-Menten Kinetics", "V = Vmax[S] / (Km + [S])",
                            "V = " + fmt(velocity) + " (units match Vmax)\n"
                                    + "Fraction of Vmax: " + fmt(fractionVmax) + "%\n"
                                    + "At [S] = Km: V = Vmax/2 = " + fmt(v[0] / 2) + "\n"
                                    + "At [S] >> Km: V ≈ Vmax = " + fmt(v[0]),
                            "Vmax = " + fmt(v[0]) + ", Km = " + fmt(v[1]) + ", [S] = " + fmt(v[2]));
                }
                case "lineweaver_burk" -> {
                    check(v, 2, "Vmax, Km");
                    StringBuilder data = new StringBuilder();
                    data.append("Lineweaver-Burk Plot Data (1/V vs 1/[S])\n");
                    data.append("──────────────────────────────────────────\n");
                    data.append("Vmax = ").append(fmt(v[0])).append(", Km = ").append(fmt(v[1])).append("\n\n");
                    data.append("y-intercept (1/Vmax) = ").append(fmt(1 / v[0])).append("\n");
                    data.append("x-intercept (-1/Km) = ").append(fmt(-1 / v[1])).append("\n");
                    data.append("Slope (Km/Vmax) = ").append(fmt(v[1] / v[0])).append("\n\n");
                    data.append(String.format("%-12s %-12s %-12s %-12s\n", "[S]", "V", "1/[S]", "1/V"));
                    double[] substrates = {0.1 * v[1], 0.25 * v[1], 0.5 * v[1], v[1], 2 * v[1], 5 * v[1], 10 * v[1]};
                    for (double s : substrates) {
                        double vel = v[0] * s / (v[1] + s);
                        data.append(String.format("%-12s %-12s %-12s %-12s\n", fmt(s), fmt(vel), fmt(1 / s), fmt(1 / vel)));
                    }
                    yield data.toString();
                }
                case "competitive_inhibition" -> {
                    check(v, 3, "Km, [I], Ki");
                    double apparentKm = v[0] * (1 + v[1] / v[2]);
                    yield popResult("Competitive Inhibition", "Km_app = Km(1 + [I]/Ki)",
                            "Apparent Km = " + fmt(apparentKm)
                                    + "\nFold increase: " + fmt(apparentKm / v[0]) + "×"
                                    + "\nNote: Vmax is unchanged in competitive inhibition",
                            "Km = " + fmt(v[0]) + ", [I] = " + fmt(v[1]) + ", Ki = " + fmt(v[2]));
                }
                case "kcat" -> {
                    check(v, 2, "Vmax, [E]total");
                    double kcat = v[0] / v[1];
                    String efficiency = "";
                    if (v.length > 2) {
                        double eff = kcat / v[2];
                        efficiency = "\nCatalytic efficiency (kcat/Km) = " + sci(eff) + " M⁻¹s⁻¹"
                                + (eff > 1e8 ? " (near diffusion limit!)" : "");
                    }
                    yield popResult("Catalytic Constant", "kcat = Vmax / [E]total",
                            "kcat = " + fmt(kcat) + " s⁻¹ (turnover number)\n"
                                    + "Each enzyme molecule converts " + fmt(kcat) + " substrate molecules per second"
                                    + efficiency,
                            "Vmax = " + fmt(v[0]) + ", [E] = " + fmt(v[1])
                                    + (v.length > 2 ? ", Km = " + fmt(v[2]) : ""));
                }
                default -> "Unknown formula. Use: michaelis_menten, lineweaver_burk, competitive_inhibition, kcat";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ── DNA/RNA Helpers ──

    private String complement(String seq, boolean isRNA) {
        StringBuilder comp = new StringBuilder();
        for (char c : seq.toCharArray()) {
            comp.append(switch (c) {
                case 'A' -> isRNA ? 'U' : 'T';
                case 'T' -> 'A';
                case 'U' -> 'A';
                case 'C' -> 'G';
                case 'G' -> 'C';
                default -> c;
            });
        }
        return comp.toString();
    }

    private String translateSequence(String mRNA) {
        StringBuilder sb = new StringBuilder();
        sb.append("Translation (mRNA → Protein)\n");
        sb.append("────────────────────────────\n");
        sb.append("mRNA: 5'-").append(mRNA).append("-3'\n\n");

        // Find start codon
        int start = mRNA.indexOf("AUG");
        if (start == -1) {
            sb.append("No start codon (AUG) found in sequence.");
            return sb.toString();
        }

        sb.append("Start codon found at position ").append(start + 1).append("\n\n");
        sb.append("Codons → Amino Acids:\n");

        List<String> aminoAcids = new ArrayList<>();
        double totalWeight = 0;

        for (int i = start; i + 2 < mRNA.length(); i += 3) {
            String codon = mRNA.substring(i, i + 3);
            String aa = CODON_TABLE.getOrDefault(codon, "???");
            sb.append("  ").append(codon).append(" → ").append(aa);

            if ("Stop".equals(aa)) {
                sb.append(" (STOP)\n");
                break;
            }

            aminoAcids.add(aa);
            Double weight = AA_WEIGHTS.get(aa);
            if (weight != null) totalWeight += weight;
            sb.append("\n");
        }

        sb.append("\nProtein: ").append(String.join("-", aminoAcids));
        sb.append("\nLength: ").append(aminoAcids.size()).append(" amino acids");
        sb.append("\nApprox. MW: ").append(fmt(totalWeight)).append(" Da (").append(fmt(totalWeight / 1000)).append(" kDa)");

        return sb.toString();
    }

    private String meltingTemperature(String seq) {
        long gc = seq.chars().filter(c -> c == 'G' || c == 'C').count();
        long at = seq.length() - gc;

        StringBuilder sb = new StringBuilder();
        sb.append("Melting Temperature (Tm)\n");
        sb.append("───────────────────────\n");
        sb.append("Sequence: ").append(seq).append(" (").append(seq.length()).append(" bp)\n\n");

        if (seq.length() < 14) {
            double tm = 2 * at + 4 * gc; // Wallace rule
            sb.append("Wallace Rule (< 14 bp):\n");
            sb.append("  Tm = 2(A+T) + 4(G+C) = 2(").append(at).append(") + 4(").append(gc).append(") = ").append(fmt(tm)).append(" °C\n");
        }

        // Basic Tm formula
        double tm = 64.9 + 41 * (gc - 16.4) / seq.length();
        sb.append("\nBasic Formula:\n");
        sb.append("  Tm = 64.9 + 41(GC - 16.4)/N = ").append(fmt(tm)).append(" °C\n");

        // Salt-adjusted
        double tmSalt = 81.5 + 16.6 * Math.log10(0.05) + 41 * ((double) gc / seq.length()) - 600.0 / seq.length();
        sb.append("\nSalt-adjusted (50mM Na⁺):\n");
        sb.append("  Tm ≈ ").append(fmt(tmSalt)).append(" °C");

        return sb.toString();
    }

    private String analyzeSequence(String seq, boolean isRNA) {
        int len = seq.length();
        long a = seq.chars().filter(c -> c == 'A').count();
        long t = seq.chars().filter(c -> c == 'T').count();
        long u = seq.chars().filter(c -> c == 'U').count();
        long g = seq.chars().filter(c -> c == 'G').count();
        long cCount = seq.chars().filter(c -> c == 'C').count();
        long gc = g + cCount;

        StringBuilder sb = new StringBuilder();
        sb.append("Sequence Analysis\n");
        sb.append("─────────────────\n");
        sb.append("Type: ").append(isRNA ? "RNA" : "DNA").append("\n");
        sb.append("Length: ").append(len).append(isRNA ? " nt" : " bp").append("\n\n");

        sb.append("COMPOSITION\n");
        sb.append("  A: ").append(a).append(" (").append(fmt((double) a / len * 100)).append("%)\n");
        if (!isRNA) sb.append("  T: ").append(t).append(" (").append(fmt((double) t / len * 100)).append("%)\n");
        if (isRNA) sb.append("  U: ").append(u).append(" (").append(fmt((double) u / len * 100)).append("%)\n");
        sb.append("  G: ").append(g).append(" (").append(fmt((double) g / len * 100)).append("%)\n");
        sb.append("  C: ").append(cCount).append(" (").append(fmt((double) cCount / len * 100)).append("%)\n\n");

        sb.append("GC Content: ").append(fmt((double) gc / len * 100)).append("%\n");
        sb.append("AT Content: ").append(fmt((double) (len - gc) / (double) len * 100)).append("%\n\n");

        double mw = len * (isRNA ? 340.0 : 330.0);
        sb.append("Molecular Weight ≈ ").append(fmt(mw)).append(" Da\n");

        if (!isRNA) {
            sb.append("\nComplement: 5'-").append(complement(seq, false)).append("-3'\n");
            sb.append("Rev. Complement: 5'-")
                    .append(new StringBuilder(complement(seq, false)).reverse()).append("-3'");
        }

        return sb.toString();
    }

    // ── Genetics Helpers ──

    private List<String> generateGametes(String genotype) {
        List<List<Character>> alleles = new ArrayList<>();
        for (int i = 0; i < genotype.length(); i += 2) {
            alleles.add(List.of(genotype.charAt(i), genotype.charAt(i + 1)));
        }

        List<String> gametes = new ArrayList<>();
        generateGametesRecursive(alleles, 0, new StringBuilder(), gametes);
        return gametes;
    }

    private void generateGametesRecursive(List<List<Character>> alleles, int index, StringBuilder current, List<String> result) {
        if (index == alleles.size()) {
            result.add(current.toString());
            return;
        }
        for (char allele : alleles.get(index)) {
            current.append(allele);
            generateGametesRecursive(alleles, index + 1, current, result);
            current.deleteCharAt(current.length() - 1);
        }
    }

    private String combineGametes(String g1, String g2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < g1.length(); i++) {
            char a = g1.charAt(i), b = g2.charAt(i);
            if (Character.isUpperCase(a) || (!Character.isUpperCase(b))) {
                result.append(a).append(b);
            } else {
                result.append(b).append(a);
            }
        }
        return result.toString();
    }

    private String normalizeGenotype(String genotype) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < genotype.length(); i += 2) {
            char a = genotype.charAt(i), b = genotype.charAt(i + 1);
            if (Character.isUpperCase(b) && !Character.isUpperCase(a)) {
                result.append(b).append(a);
            } else {
                result.append(a).append(b);
            }
        }
        return result.toString();
    }

    private String getPhenotype(String genotype, String dominant, String recessive) {
        StringBuilder pheno = new StringBuilder();
        for (int i = 0; i < genotype.length(); i += 2) {
            char a = genotype.charAt(i), b = genotype.charAt(i + 1);
            boolean isDominant = Character.isUpperCase(a) || Character.isUpperCase(b);
            if (!pheno.isEmpty()) pheno.append(", ");
            if (i == 0 && dominant != null && recessive != null) {
                pheno.append(isDominant ? dominant : recessive);
            } else {
                pheno.append(isDominant ? "Dominant" : "Recessive");
                pheno.append("(").append(Character.toUpperCase(a)).append(")");
            }
        }
        return pheno.toString();
    }

    // ── Common Helpers ──

    private double[] parseValues(String values) {
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i].strip());
        return result;
    }

    private void check(double[] v, int min, String names) {
        if (v.length < min) throw new IllegalArgumentException("Need " + min + " values: " + names);
    }

    private String seqResult(String title, String input, String output, String note) {
        return title + "\n" + "─".repeat(title.length()) + "\n"
                + "Input: 5'-" + input + "-3'\n\n"
                + output + "\n\n"
                + "Note: " + note;
    }

    private String popResult(String title, String formula, String answer, String given) {
        return title + "\n" + "─".repeat(title.length()) + "\n"
                + "Formula: " + formula + "\n"
                + "Given: " + given + "\n\n"
                + "Result:\n  " + answer.replace("\n", "\n  ");
    }

    private String fmt(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }

    private String sci(double v) { return String.format("%.4e", v); }
}
